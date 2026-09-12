package com.example.blackbox.di

import android.content.Context
import com.example.blackbox.data.api.AWSBackendApi
import com.example.blackbox.data.api.RetrofitClient
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.db.EmergencyContactDao
import com.example.blackbox.data.db.IncidentReportDao
import com.example.blackbox.data.db.SensorEventDao
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.pdf.PdfReportGenerator
import com.example.blackbox.domain.trigger.SimulatedTriggerEngine
import com.example.blackbox.domain.trigger.TriggerDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideKeyManagementService(@ApplicationContext context: Context): KeyManagementService {
        return KeyManagementService(context)
    }

    @Provides
    @Singleton
    fun provideAWSBackendApi(keyManagementService: KeyManagementService): AWSBackendApi {
        return RetrofitClient.createApiService(keyManagementService)
    }

    @Provides
    @Singleton
    fun provideBlackboxRepository(
        sensorEventDao: SensorEventDao,
        incidentReportDao: IncidentReportDao,
        emergencyContactDao: EmergencyContactDao,
        keyManagementService: KeyManagementService,
        apiService: AWSBackendApi
    ): BlackboxRepository {
        return BlackboxRepository(
            sensorEventDao,
            incidentReportDao,
            emergencyContactDao,
            keyManagementService,
            apiService
        )
    }

    @Provides
    @Singleton
    fun provideTriggerDetector(): TriggerDetector {
        return TriggerDetector()
    }

    @Provides
    @Singleton
    fun provideSimulatedTriggerEngine(
        repository: BlackboxRepository,
        triggerDetector: TriggerDetector
    ): SimulatedTriggerEngine {
        return SimulatedTriggerEngine(repository, triggerDetector)
    }

    @Provides
    @Singleton
    fun providePdfReportGenerator(@ApplicationContext context: Context): PdfReportGenerator {
        return PdfReportGenerator(context)
    }
}
