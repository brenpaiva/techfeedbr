package br.com.techfeedbr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(
    entities = [ContentEntity::class, ContentStateEntity::class, EvaluationEventEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(SetConverters::class)
abstract class TechFeedDatabase : RoomDatabase() {
    abstract fun dao(): TechFeedDao
}

class SetConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromSet(value: Set<String>): String = gson.toJson(value)

    @TypeConverter
    fun toSet(value: String): Set<String> =
        gson.fromJson(value, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
}

