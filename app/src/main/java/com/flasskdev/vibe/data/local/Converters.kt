package com.flasskdev.vibe.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        return try {
            Json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromReactionList(value: List<ReactionItem>?): String? {
        if (value == null) return null
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toReactionList(value: String?): List<ReactionItem>? {
        if (value == null) return null
        return try {
            Json.decodeFromString<List<ReactionItem>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromReplyMarkup(value: ReplyMarkup?): String? {
        if (value == null) return null
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toReplyMarkup(value: String?): ReplyMarkup? {
        if (value == null) return null
        return try {
            Json.decodeFromString<ReplyMarkup>(value)
        } catch (e: Exception) {
            null
        }
    }
}
