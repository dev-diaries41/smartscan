package com.fpf.smartscan.data.fewshot

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Type Converters pro Few-Shot Database
 *
 * Room neumí přímo ukládat FloatArray, takže konvertujeme na ByteArray.
 * Používá Little Endian byte order pro konzistenci.
 */
class FewShotConverters {

    /**
     * Konvertuje FloatArray na ByteArray pro uložení do databáze
     *
     * @param value FloatArray embedding (např. 768 floats)
     * @return ByteArray (768 floats × 4 bytes = 3072 bytes)
     */
    @TypeConverter
    fun fromFloatArray(value: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(value.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        value.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    /**
     * Konvertuje ByteArray z databáze zpět na FloatArray
     *
     * @param value ByteArray (3072 bytes)
     * @return FloatArray embedding (768 floats)
     */
    @TypeConverter
    fun toFloatArray(value: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        val result = FloatArray(value.size / 4)
        for (i in result.indices) {
            result[i] = buffer.getFloat()
        }
        return result
    }
}
