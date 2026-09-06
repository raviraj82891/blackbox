package com.example.blackbox.di

import android.content.Context
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.BlackboxDatabase
import com.example.blackbox.data.db.EmergencyContactDao
import com.example.blackbox.data.db.IncidentReportDao
import com.example.blackbox.data.db.SensorEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        kms: KeyManagementService
    ): BlackboxDatabase {
        val passphrase = kms.getOrCreateDatabasePassphrase()
        return BlackboxDatabase.getInstance(context, passphrase)
    }

    @Provides
    fun provideSensorEventDao(db: BlackboxDatabase): SensorEventDao = db.sensorEventDao()

    @Provides
    fun provideIncidentReportDao(db: BlackboxDatabase): IncidentReportDao = db.incidentReportDao()

    @Provides
    fun provideEmergencyContactDao(db: BlackboxDatabase): EmergencyContactDao = db.emergencyContactDao()
}
