package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "telegram_user_id") val telegramUserId: Long,
    val name: String,
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP") val createdAt: String = "CURRENT_TIMESTAMP"
)

@Entity(
    tableName = "shifts",
    foreignKeys = [
        ForeignKey(
            entity = DriverEntity::class,
            parentColumns = ["id"],
            childColumns = ["driver_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "driver_id") val driverId: Int,
    @ColumnInfo(name = "work_date") val workDate: String, // "YYYY-MM-DD"
    @ColumnInfo(name = "shift_start") val shiftStart: String, // "HH:MM"
    @ColumnInfo(name = "tacho_start") val tachoStart: String, // "HH:MM"
    @ColumnInfo(name = "shift_end") val shiftEnd: String, // "HH:MM"
    @ColumnInfo(name = "tacho_end") val tachoEnd: String, // "HH:MM"
    @ColumnInfo(name = "driving_hours") val drivingHours: String, // e.g. "8.5"
    @ColumnInfo(name = "night_stop") val nightStop: Int, // 0 or 1
    val expenses: String, // e.g. "125.50"
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP") val createdAt: String = "CURRENT_TIMESTAMP"
)

@Dao
interface DriverDao {
    @Query("SELECT * FROM drivers")
    fun getAllDriversFlow(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers LIMIT 1")
    suspend fun getFirstDriver(): DriverEntity?

    @Query("SELECT * FROM drivers WHERE id = :id LIMIT 1")
    suspend fun getDriverById(id: Int): DriverEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DriverEntity): Long

    @Update
    suspend fun updateDriver(driver: DriverEntity)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY work_date DESC, tacho_start DESC")
    fun getAllShiftsFlow(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE driver_id = :driverId ORDER BY work_date DESC, tacho_start DESC")
    fun getShiftsByDriverFlow(driverId: Int): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE driver_id = :driverId AND id = :id LIMIT 1")
    suspend fun getShiftById(driverId: Int, id: Int): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteShiftById(id: Int)
}

@Database(entities = [DriverEntity::class, ShiftEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun driverDao(): DriverDao
    abstract fun shiftDao(): ShiftDao
}
