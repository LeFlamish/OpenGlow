package com.example.openglow.data.local

import androidx.room.TypeConverter
import com.example.openglow.data.entity.IdentifierType
import com.example.openglow.data.entity.SenderType

class Converters {
    @TypeConverter
    fun fromSenderType(value: SenderType) = value.name

    @TypeConverter
    fun toSenderType(value: String) = SenderType.valueOf(value)

    @TypeConverter
    fun fromIdentifierType(value: IdentifierType) = value.name

    @TypeConverter
    fun toIdentifierType(value: String) = IdentifierType.valueOf(value)
}
