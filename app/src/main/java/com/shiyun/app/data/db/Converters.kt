package com.shiyun.app.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStrings(value: List<String>): String = Json.encodeToString(listSerializer, value)

    @TypeConverter
    fun toStrings(value: String): List<String> = Json.decodeFromString(listSerializer, value)
}
