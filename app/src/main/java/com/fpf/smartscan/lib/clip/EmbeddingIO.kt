package com.fpf.smartscan.lib.clip

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


fun saveEmbeddingsToFile(context: Context, path: String, embeddingsList: List<Embedding>) {
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

fun loadEmbeddingsFromFile(context: Context, path: String, size: Int? = null, ): List<Embedding> {
    val file = File(context.filesDir, path)

    FileInputStream(file).channel.use { ch ->
        val fileSize = ch.size()
        // Memory-map the file for fast bulk reads
        val buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, fileSize).order(ByteOrder.LITTLE_ENDIAN)

        val count = buffer.int
        val list = ArrayList<Embedding>(count)

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

                list.add(Embedding(id, date, floats))
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
                list.add(Embedding(id, date, floats))
            }
        }

        return list
    }
}


fun appendEmbeddingsToFile(context: Context, path: String, newEmbeddings: List<Embedding>) {
    val file = File(context.filesDir, path)

    if (!file.exists()) {
        saveEmbeddingsToFile(context, path, newEmbeddings)
        return
    }

    RandomAccessFile(file, "rw").use { raf ->
        val channel = raf.channel

        // Read the 4-byte header as little-endian
        val headerBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        channel.position(0)
        val read = channel.read(headerBuf)
        if (read != 4) {
            throw IOException("Failed to read header count (file too small/corrupted)")
        }
        headerBuf.flip()
        val existingCount = headerBuf.int

        // Basic validation: each existing entry is at least 20 bytes (id(8)+date(8)+len(4)).
        // If the recorded count is wildly larger than file size, treat as corruption.
        val minEntryBytes = 8 + 8 + 4
        val maxCountFromSize = (channel.size() / minEntryBytes).toInt()
        if (existingCount < 0 || existingCount > maxCountFromSize + 10_000) {
            throw IOException("Corrupt embeddings header: count=$existingCount, fileSize=${channel.size()}")
        }

        val newCount = existingCount + newEmbeddings.size

        // Write the updated count back as little-endian
        headerBuf.clear()
        headerBuf.putInt(newCount).flip()
        channel.position(0)
        while (headerBuf.hasRemaining()) channel.write(headerBuf)

        // Move to the end to append new entries
        channel.position(channel.size())

        // Append each embedding using little-endian ByteBuffers
        for (embedding in newEmbeddings) {
            // allocate exact size for this entry to avoid extra copies
            val entryBytes = (8 + 8 + 4) + embedding.embeddings.size * 4
            val buf = ByteBuffer.allocate(entryBytes).order(ByteOrder.LITTLE_ENDIAN)
            buf.putLong(embedding.id)
            buf.putLong(embedding.date)
            buf.putInt(embedding.embeddings.size)
            for (f in embedding.embeddings) buf.putFloat(f)
            buf.flip()
            while (buf.hasRemaining()) {
                channel.write(buf)
            }
        }
        channel.force(false)
    }
}
