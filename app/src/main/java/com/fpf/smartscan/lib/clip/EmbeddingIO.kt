package com.fpf.smartscan.lib.clip

import android.content.Context
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


fun saveImageEmbeddingsToFile(context: Context, path: String, embeddingsList: List<Embedding>) {
    // Calculate total bytes: count + (id + date + length + floats) per entry
    var totalFloats = 0
    for (e in embeddingsList) totalFloats += e.embeddings.size

    val totalBytes =
        4 + // count (int)
                embeddingsList.size * (8 + 8 + 4) + // id(long) + date(long) + length(int)
                totalFloats * 4 // floats

    val buffer = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN)

    buffer.putInt(embeddingsList.size)
    for (embedding in embeddingsList) {
        buffer.putLong(embedding.id)
        buffer.putLong(embedding.date)
        buffer.putInt(embedding.embeddings.size)
        for (f in embedding.embeddings) {
            buffer.putFloat(f)
        }
    }

    buffer.flip()
    val file = File(context.filesDir, path)
    FileOutputStream(file).channel.use { ch ->
        ch.write(buffer)
    }
}

fun loadImageEmbeddingsFromFile(context: Context, path: String, size: Int? = null): List<Embedding> {
    val file = File(context.filesDir, path)

    FileInputStream(file).channel.use { ch ->
        val fileSize = ch.size()
        // Memory-map the file for fast bulk reads
        val buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, fileSize).order(ByteOrder.LITTLE_ENDIAN)

        val count = buffer.int
        val list = ArrayList<ImageEmbedding>(count)

        if (size != null) {
            val fixedLen = size
            repeat(count) {
                val id = buffer.long
                val date = buffer.long
                val length = buffer.int
                if (length != fixedLen) {
                    throw IllegalArgumentException("Expected embedding length $fixedLen but found $length in file")
                }

                val floats = FloatArray(length)
                // Create a FloatBuffer view starting at current byte position and bulk-get into the array
                val fb = buffer.asFloatBuffer()
                fb.get(floats)
                // advance the original byte buffer position by length * 4 bytes
                buffer.position(buffer.position() + length * 4)

                list.add(ImageEmbedding(id, date, floats))
            }
        } else {
            // Fallback to original per-float loop (works with variable-length entries)
            repeat(count) {
                val id = buffer.long
                val date = buffer.long
                val length = buffer.int
                val floats = FloatArray(length)
                for (i in 0 until length) {
                    floats[i] = buffer.float
                }
                list.add(ImageEmbedding(id, date, floats))
            }
        }

        return list
    }
}


fun appendEmbeddingsToFile(context: Context, path: String, newEmbeddings: List<Embedding>) {
    val file = File(context.filesDir, path)

    if (!file.exists()) {
        // File doesn't exist yet, just save normally
        saveImageEmbeddingsToFile(context, path, newEmbeddings)
        return
    }

    RandomAccessFile(file, "rw").use { raf ->
        // Read the existing count
        val existingCount = raf.readInt()
        val newCount = existingCount + newEmbeddings.size

        // Update the count at the start of the file
        raf.seek(0)
        raf.writeInt(newCount)

        // Move to the end to append new embeddings
        raf.seek(raf.length())

        val outputStream = DataOutputStream(FileOutputStream(raf.fd))
        for (embedding in newEmbeddings) {
            outputStream.writeLong(embedding.id)
            outputStream.writeLong(embedding.date)
            outputStream.writeInt(embedding.embeddings.size)
            for (f in embedding.embeddings) {
                outputStream.writeFloat(f)
            }
        }
        outputStream.flush()
    }
}
