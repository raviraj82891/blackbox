package com.example.blackbox.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [SensorEvent::class, IncidentReport::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BlackboxDatabase : RoomDatabase() {

    abstract fun sensorEventDao(): SensorEventDao
    abstract fun incidentReportDao(): IncidentReportDao

    companion object {
        @Volatile
        private var INSTANCE: BlackboxDatabase? = null

        fun getInstance(context: Context, passphrase: String): BlackboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase.toByteArray(Charsets.UTF_8))
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlackboxDatabase::class.java,
                    "blackbox_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
