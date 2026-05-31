package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val driverDao: DriverDao,
    private val shiftDao: ShiftDao
) {
    val allShifts: Flow<List<ShiftEntity>> = shiftDao.getAllShiftsFlow()

    fun getShiftsForDriver(driverId: Int): Flow<List<ShiftEntity>> {
        return shiftDao.getShiftsByDriverFlow(driverId)
    }

    suspend fun getDriverById(driverId: Int): DriverEntity? {
        return driverDao.getDriverById(driverId)
    }

    suspend fun getFirstOrInsertDefaultDriver(defaultName: String = "Водитель"): DriverEntity {
        val existing = driverDao.getFirstDriver()
        if (existing != null) {
            return existing
        }
        val defaultDriver = DriverEntity(name = defaultName, telegramUserId = 0L)
        val id = driverDao.insertDriver(defaultDriver)
        return defaultDriver.copy(id = id.toInt())
    }

    suspend fun updateDriver(driver: DriverEntity) {
        driverDao.updateDriver(driver)
    }

    suspend fun updateDriverName(driverId: Int, newName: String) {
        val existing = driverDao.getDriverById(driverId)
        if (existing != null) {
            driverDao.updateDriver(existing.copy(name = newName))
        }
    }

    suspend fun saveShift(shift: ShiftEntity): Long {
        return shiftDao.insertShift(shift)
    }

    suspend fun updateShift(shift: ShiftEntity) {
        shiftDao.updateShift(shift)
    }

    suspend fun getShiftById(driverId: Int, shiftId: Int): ShiftEntity? {
        return shiftDao.getShiftById(driverId, shiftId)
    }

    suspend fun deleteShiftById(shiftId: Int) {
        shiftDao.deleteShiftById(shiftId)
    }
}
