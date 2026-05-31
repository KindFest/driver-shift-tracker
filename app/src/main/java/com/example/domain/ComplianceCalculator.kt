package com.example.domain

import com.example.data.ShiftEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.Duration
import java.time.temporal.ChronoUnit

data class ShiftRecord(
    val id: Int,
    val driverId: Int,
    val workDate: LocalDate,
    val shiftStart: LocalTime,
    val tachoStart: LocalTime,
    val shiftEnd: LocalTime,
    val tachoEnd: LocalTime,
    val drivingHours: Double,
    val nightStop: Boolean,
    val expenses: Double
) {
    val tachoStartDt: LocalDateTime
        get() = LocalDateTime.of(workDate, tachoStart)

    val tachoEndDt: LocalDateTime
        get() {
            var endDt = LocalDateTime.of(workDate, tachoEnd)
            if (!endDt.isAfter(tachoStartDt)) {
                endDt = endDt.plusDays(1)
            }
            return endDt
        }

    val tachoShiftSeconds: Long
        get() = ChronoUnit.SECONDS.between(tachoStartDt, tachoEndDt)

    val tachoShiftHours: Double
        get() = tachoShiftSeconds / 3600.0

    val shiftStartDt: LocalDateTime
        get() = LocalDateTime.of(workDate, shiftStart)

    val shiftEndDt: LocalDateTime
        get() {
            var endDt = LocalDateTime.of(workDate, shiftEnd)
            if (!endDt.isAfter(shiftStartDt)) {
                endDt = endDt.plusDays(1)
            }
            return endDt
        }

    val shiftSeconds: Long
        get() = ChronoUnit.SECONDS.between(shiftStartDt, shiftEndDt)

    val shiftHours: Double
        get() = shiftSeconds / 3600.0

    val drivingMinutes: Int
        get() = ComplianceCalculator.decimalHoursToMinutes(drivingHours)

    val formattedDriving: String
        get() = ComplianceCalculator.formatMinutes(drivingMinutes)

    val formattedShiftDuration: String
        get() = ComplianceCalculator.formatHours(shiftHours)

    val formattedTachoShiftDuration: String
        get() = ComplianceCalculator.formatHours(tachoShiftHours)
}

data class WeeklyCompliance(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val records: List<ShiftRecord>,
    val over13hShiftsCount: Int,
    val over9hDrivingCount: Int,
    val shortRestsCount: Int,
    val nightsCount: Int,
    val totalExpenses: Double,
    val totalShiftHours: Double,
    val totalTachoShiftHours: Double,
    val totalDrivingMinutes: Int
) {
    val over13hLimitExceeded: Boolean
        get() = over13hShiftsCount > 3

    val over9hDrivingLimitExceeded: Boolean
        get() = over9hDrivingCount > 2

    val shortRestsLimitExceeded: Boolean
        get() = shortRestsCount > 3
}

object ComplianceCalculator {

    fun parseDate(value: String): LocalDate {
        val clean = value.trim()
        val formats = listOf(
            "dd.MM.yyyy", "dd.MM.yy", "yyyy-MM-dd", "yy-MM-dd", "dd/MM/yyyy", "dd/MM/yy"
        )
        for (fmt in formats) {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern(fmt)
                return LocalDate.parse(clean, formatter)
            } catch (e: Exception) {
                // Ignore and try next
            }
        }
        throw IllegalArgumentException("Введите дату в формате ДД.ММ.ГГГГ или ДД.ММ.ГГ")
    }

    fun parseTime(value: String): LocalTime {
        val clean = value.trim().replace(".", ":")
        val formats = listOf("HH:mm", "H:mm", "HH")
        for (fmt in formats) {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern(fmt)
                return LocalTime.parse(clean, formatter)
            } catch (e: Exception) {
                // Try padding hours if simple "H" or "HH" is entered
            }
        }
        // Handle solo hours like "7" -> "07:00"
        val hourInt = clean.toIntOrNull()
        if (hourInt != null && hourInt in 0..23) {
            return LocalTime.of(hourInt, 0)
        }
        throw IllegalArgumentException("Введите время в формате ЧЧ:ММ, например 07:30")
    }

    fun parseHours(value: String): Double {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return 0.0
        if (trimmed == "-" || trimmed == " ") return 0.0
        
        if (trimmed.contains(":") || trimmed.contains(".")) {
            val separator = if (trimmed.contains(":")) ":" else "."
            val parts = trimmed.split(separator)
            val hours = parts[0].toIntOrNull() ?: throw IllegalArgumentException()
            val minutesStr = if (parts.size > 1) parts[1] else "0"
            val minutes = minutesStr.toIntOrNull() ?: throw IllegalArgumentException()
            if (minutes < 0 || minutes >= 60 || hours < 0) {
                throw IllegalArgumentException()
            }
            return hours + (minutes / 60.0)
        } else {
            val decimalStr = trimmed.replace(",", ".")
            val res = decimalStr.toDoubleOrNull() ?: throw IllegalArgumentException()
            if (res < 0.0) throw IllegalArgumentException()
            return res
        }
    }

    fun parseMoney(value: String): Double {
        val trimmed = value.trim().replace(",", ".")
        if (trimmed.isEmpty() || trimmed == "-" || trimmed == " ") return 0.0
        val res = trimmed.toDoubleOrNull() ?: throw IllegalArgumentException()
        if (res < 0.0) throw IllegalArgumentException()
        return res
    }

    fun parseShiftEntity(entity: ShiftEntity): ShiftRecord {
        return ShiftRecord(
            id = entity.id,
            driverId = entity.driverId,
            workDate = LocalDate.parse(entity.workDate),
            shiftStart = parseTime(entity.shiftStart),
            tachoStart = parseTime(entity.tachoStart),
            shiftEnd = parseTime(entity.shiftEnd),
            tachoEnd = parseTime(entity.tachoEnd),
            drivingHours = entity.drivingHours.toDoubleOrNull() ?: 0.0,
            nightStop = entity.nightStop == 1,
            expenses = entity.expenses.toDoubleOrNull() ?: 0.0
        )
    }

    fun decimalHoursToMinutes(hours: Double): Int {
        return Math.round(hours * 60.0).toInt()
    }

    fun formatMinutes(totalMinutes: Int): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return "$h ч %02d мин".format(m)
    }

    fun formatHours(hours: Double): String {
        return formatMinutes(decimalHoursToMinutes(hours))
    }

    fun getWeekBounds(anchor: LocalDate): Pair<LocalDate, LocalDate> {
        val start = anchor.with(java.time.DayOfWeek.MONDAY)
        val end = start.plusDays(7)
        return Pair(start, end)
    }

    fun buildPeriodWeekRanges(start: LocalDate, end: LocalDate): List<Pair<LocalDate, LocalDate>> {
        val ranges = mutableListOf<Pair<LocalDate, LocalDate>>()
        var current = start
        while (current.isBefore(end)) {
            val weekEnd = minOf(current.plusDays(7), end)
            ranges.add(Pair(current, weekEnd))
            current = weekEnd
        }
        return ranges
    }

    fun calculateWeeklyCompliance(
        anchorDate: LocalDate,
        allShifts: List<ShiftRecord>
    ): WeeklyCompliance {
        val (weekStart, weekEnd) = getWeekBounds(anchorDate)
        
        // Filter week records
        val weekRecords = allShifts.filter { !it.workDate.isBefore(weekStart) && it.workDate.isBefore(weekEnd) }

        val maxTachoShiftSeconds = 13.0 * 3600.0
        val minDailyRestSeconds = 11.0 * 3600.0
        val maxDailyDrivingMinutes = 9 * 60

        val over13hShiftsCount = weekRecords.count { it.tachoShiftSeconds > maxTachoShiftSeconds }
        val over9hDrivingCount = weekRecords.count { it.drivingMinutes > maxDailyDrivingMinutes }
        val nightsCount = weekRecords.count { it.nightStop }
        val totalExpenses = weekRecords.sumOf { it.expenses }
        val totalShiftHours = weekRecords.sumOf { it.shiftSeconds } / 3600.0
        val totalTachoShiftHours = weekRecords.sumOf { it.tachoShiftSeconds } / 3600.0
        val totalDrivingMinutes = weekRecords.sumOf { it.drivingMinutes }

        // Sort all driver's shifts to trace sleep rests between days
        val sortedAllRecords = allShifts.sortedBy { it.tachoStartDt }
        val nextShiftMap = mutableMapOf<Int, ShiftRecord>()
        for (i in 0 until sortedAllRecords.size - 1) {
            nextShiftMap[sortedAllRecords[i].id] = sortedAllRecords[i + 1]
        }

        var shortRestsCount = 0
        for (record in weekRecords) {
            val nextShift = nextShiftMap[record.id]
            val restSeconds = if (nextShift != null) {
                ChronoUnit.SECONDS.between(record.tachoEndDt, nextShift.tachoStartDt)
            } else {
                null
            }

            if (record.tachoShiftSeconds > maxTachoShiftSeconds) {
                shortRestsCount++
                continue
            }

            if (restSeconds != null && restSeconds < minDailyRestSeconds) {
                shortRestsCount++
            }
        }

        return WeeklyCompliance(
            weekStart = weekStart,
            weekEnd = weekEnd,
            records = weekRecords.sortedBy { it.workDate },
            over13hShiftsCount = over13hShiftsCount,
            over9hDrivingCount = over9hDrivingCount,
            shortRestsCount = shortRestsCount,
            nightsCount = nightsCount,
            totalExpenses = totalExpenses,
            totalShiftHours = totalShiftHours,
            totalTachoShiftHours = totalTachoShiftHours,
            totalDrivingMinutes = totalDrivingMinutes
        )
    }
}
