package com.example.blackbox.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: IncidentReport)

    @Update
    suspend fun update(report: IncidentReport)

    @Query("SELECT * FROM incident_reports ORDER BY triggeredAt DESC")
    fun getAllReports(): Flow<List<IncidentReport>>

    @Query("SELECT * FROM incident_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): IncidentReport?

    @Query("DELETE FROM incident_reports")
    suspend fun deleteAll()
}
