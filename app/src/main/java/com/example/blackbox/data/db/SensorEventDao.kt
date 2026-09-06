package com.example.blackbox.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SensorEvent): Long

    @Query("SELECT * FROM sensor_events ORDER BY id DESC LIMIT 1")
    suspend fun getLastEvent(): SensorEvent?

    @Query("SELECT * FROM sensor_events WHERE timestampMs >= :cutoffMs ORDER BY timestampMs ASC")
    fun getEventsSince(cutoffMs: Long): Flow<List<SensorEvent>>

    @Query("SELECT * FROM sensor_events WHERE timestampMs >= :cutoffMs ORDER BY timestampMs ASC")
    suspend fun getEventsSinceList(cutoffMs: Long): List<SensorEvent>

    @Query("SELECT * FROM sensor_events WHERE timestampMs BETWEEN :startMs AND :endMs ORDER BY timestampMs ASC")
    suspend fun getAllEventsInWindow(startMs: Long, endMs: Long): List<SensorEvent>

    @Query("DELETE FROM sensor_events WHERE timestampMs < :cutoffMs")
    suspend fun purgeOlderThan(cutoffMs: Long): Int

    @Query("SELECT COUNT(*) FROM sensor_events")
    fun getEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sensor_events")
    suspend fun getEventCountSync(): Int

    @Query("DELETE FROM sensor_events")
    suspend fun deleteAll()
}
