package com.example.blackbox.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [SensorEvent::class, IncidentReport::class, EmergencyContact::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BlackboxDatabase : RoomDatabase() {

    abstract fun sensorEventDao(): SensorEventDao
    abstract fun incidentReportDao(): IncidentReportDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: BlackboxDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `emergency_contacts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `email` TEXT NOT NULL, `relationship` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        fun getInstance(context: Context, passphrase: String): BlackboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase.toByteArray(Charsets.UTF_8))
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlackboxDatabase::class.java,
                    "blackbox_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
