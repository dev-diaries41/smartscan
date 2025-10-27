package com.fpf.smartscan.data.tags

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TagConverters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(value.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        value.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

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
