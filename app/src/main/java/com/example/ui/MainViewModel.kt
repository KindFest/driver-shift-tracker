package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.DriverEntity
import com.example.data.ShiftEntity
import com.example.domain.ComplianceCalculator
import com.example.domain.ShiftRecord
import com.example.domain.WeeklyCompliance
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UIState {
    object Loading : UIState()
    data class Success(val driver: DriverEntity) : UIState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Database & Repository initialization
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "driver_reports.sqlite3" // Name of physical file in the system
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: AppRepository by lazy {
        AppRepository(database.driverDao(), database.shiftDao())
    }

    // Tab navigation state
    enum class Tab {
        DASHBOARD, SHIFTS, REPORTS, PROFILE
    }

    private val _activeTab = MutableStateFlow(Tab.DASHBOARD)
    val activeTab: StateFlow<Tab> = _activeTab.asStateFlow()

    fun setActiveTab(tab: Tab) {
        _activeTab.value = tab
    }

    // Driver/Profile states
    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    private val _currentDriver = MutableStateFlow<DriverEntity?>(null)
    val currentDriver: StateFlow<DriverEntity?> = _currentDriver.asStateFlow()

    // Target week anchor selector on Dashboard
    private val _selectedWeekAnchor = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedWeekAnchor: StateFlow<LocalDate> = _selectedWeekAnchor.asStateFlow()

    fun setSelectedWeekAnchor(date: LocalDate) {
        _selectedWeekAnchor.value = date
    }

    // Reporting range dates
    private val _reportStartDate = MutableStateFlow<LocalDate>(LocalDate.now().minusWeeks(4))
    val reportStartDate: StateFlow<LocalDate> = _reportStartDate.asStateFlow()

    private val _reportEndDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val reportEndDate: StateFlow<LocalDate> = _reportEndDate.asStateFlow()

    fun setReportStartDate(date: LocalDate) {
        _reportStartDate.value = date
    }

    fun setReportEndDate(date: LocalDate) {
        _reportEndDate.value = date
    }

    init {
        loadDriver()
    }

    private fun loadDriver() {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val driver = repository.getFirstOrInsertDefaultDriver()
            _currentDriver.value = driver
            _uiState.value = UIState.Success(driver)
        }
    }

    fun updateDriverProfile(name: String, telegramUserId: Long) {
        viewModelScope.launch {
            val driver = _currentDriver.value ?: return@launch
            val updated = driver.copy(name = name, telegramUserId = telegramUserId)
            repository.updateDriver(updated)
            _currentDriver.value = updated
        }
    }

    // Convert raw ShiftEntity elements to processed domain ShiftRecords
    val allShifts: StateFlow<List<ShiftRecord>> = repository.allShifts
        .combine(currentDriver) { entities, driver ->
            if (driver == null) emptyList()
            else {
                entities.filter { it.driverId == driver.id }
                    .mapNotNull {
                        try {
                            ComplianceCalculator.parseShiftEntity(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Compute weekly compliance rules for the selected anchor week
    val weeklyCompliance: StateFlow<WeeklyCompliance?> = combine(
        selectedWeekAnchor,
        allShifts
    ) { anchor, shifts ->
        ComplianceCalculator.calculateWeeklyCompliance(anchor, shifts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Series of weekly calculations matching the requested reporting period
    val customReportWeeksCompliance: StateFlow<List<WeeklyCompliance>> = combine(
        reportStartDate,
        reportEndDate,
        allShifts
    ) { start, end, shifts ->
        val ranges = ComplianceCalculator.buildPeriodWeekRanges(start, end)
        ranges.map { (weekStart, weekEnd) ->
            ComplianceCalculator.calculateWeeklyCompliance(weekStart, shifts)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Shift entry form state variables
    private val _editingShiftId = MutableStateFlow<Int?>(null)
    val editingShiftId: StateFlow<Int?> = _editingShiftId.asStateFlow()

    private val _formWorkDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val formWorkDate: StateFlow<LocalDate> = _formWorkDate.asStateFlow()

    private val _formShiftStart = MutableStateFlow<LocalTime>(LocalTime.of(7, 30))
    val formShiftStart: StateFlow<LocalTime> = _formShiftStart.asStateFlow()

    private val _formTachoStart = MutableStateFlow<LocalTime>(LocalTime.of(7, 30))
    val formTachoStart: StateFlow<LocalTime> = _formTachoStart.asStateFlow()

    private val _formShiftEnd = MutableStateFlow<LocalTime>(LocalTime.of(17, 30))
    val formShiftEnd: StateFlow<LocalTime> = _formShiftEnd.asStateFlow()

    private val _formTachoEnd = MutableStateFlow<LocalTime>(LocalTime.of(17, 30))
    val formTachoEnd: StateFlow<LocalTime> = _formTachoEnd.asStateFlow()

    private val _formDrivingHoursText = MutableStateFlow("4:30")
    val formDrivingHoursText: StateFlow<String> = _formDrivingHoursText.asStateFlow()

    private val _formNightStop = MutableStateFlow(false)
    val formNightStop: StateFlow<Boolean> = _formNightStop.asStateFlow()

    private val _formExpensesText = MutableStateFlow("0")
    val formExpensesText: StateFlow<String> = _formExpensesText.asStateFlow()

    private val _formValidationError = MutableStateFlow<String?>(null)
    val formValidationError: StateFlow<String?> = _formValidationError.asStateFlow()

    private val _isFormOpen = MutableStateFlow(false)
    val isFormOpen: StateFlow<Boolean> = _isFormOpen.asStateFlow()

    fun openNewShiftForm() {
        val lastShift = allShifts.value.firstOrNull()
        _editingShiftId.value = null
        _formWorkDate.value = lastShift?.workDate?.plusDays(1) ?: LocalDate.now()
        _formShiftStart.value = lastShift?.shiftStart ?: LocalTime.of(7, 30)
        _formTachoStart.value = lastShift?.tachoStart ?: LocalTime.of(7, 30)
        _formShiftEnd.value = lastShift?.shiftEnd ?: LocalTime.of(17, 30)
        _formTachoEnd.value = lastShift?.tachoEnd ?: LocalTime.of(17, 30)
        
        if (lastShift != null) {
            val totalMins = (lastShift.drivingHours * 60).toInt()
            val hrs = totalMins / 60
            val mins = totalMins % 60
            _formDrivingHoursText.value = "%d:%02d".format(hrs, mins)
        } else {
            _formDrivingHoursText.value = "4:30"
        }
        
        _formNightStop.value = lastShift?.nightStop ?: false
        _formExpensesText.value = lastShift?.let { if (it.expenses > 0) "%.2f".format(it.expenses).replace(",", ".") else "" } ?: ""
        _formValidationError.value = null
        _isFormOpen.value = true
    }

    fun openEditShiftForm(record: ShiftRecord) {
        _editingShiftId.value = record.id
        _formWorkDate.value = record.workDate
        _formShiftStart.value = record.shiftStart
        _formTachoStart.value = record.tachoStart
        _formShiftEnd.value = record.shiftEnd
        _formTachoEnd.value = record.tachoEnd
        
        // Populate standard formats
        val totalMins = (record.drivingHours * 60).toInt()
        val hrs = totalMins / 60
        val mins = totalMins % 60
        _formDrivingHoursText.value = "%d:%02d".format(hrs, mins)
        _formNightStop.value = record.nightStop
        _formExpensesText.value = if (record.expenses > 0) "%.2f".format(record.expenses).replace(",", ".") else ""
        _formValidationError.value = null
        _isFormOpen.value = true
    }

    fun closeForm() {
        _isFormOpen.value = false
        _formValidationError.value = null
    }

    fun setFormWorkDate(date: LocalDate) { _formWorkDate.value = date }
    fun setFormShiftStart(time: LocalTime) { _formShiftStart.value = time }
    fun setFormTachoStart(time: LocalTime) { _formTachoStart.value = time }
    fun setFormShiftEnd(time: LocalTime) { _formShiftEnd.value = time }
    fun setFormTachoEnd(time: LocalTime) { _formTachoEnd.value = time }
    fun setFormDrivingHoursText(text: String) { _formDrivingHoursText.value = text }
    fun setFormNightStop(stop: Boolean) { _formNightStop.value = stop }
    fun setFormExpensesText(text: String) { _formExpensesText.value = text }

    fun submitShift() {
        val driver = _currentDriver.value ?: return
        
        val drivingHoursVal = try {
            ComplianceCalculator.parseHours(_formDrivingHoursText.value)
        } catch (e: Exception) {
            _formValidationError.value = "Ошибка часов вождения: Введите в формате ЧЧ:ММ (напр., 4:30) или ЧЧ.ММ"
            return
        }

        val expensesVal = try {
            ComplianceCalculator.parseMoney(_formExpensesText.value)
        } catch (e: Exception) {
            _formValidationError.value = "Ошибка расходов: Введите корректное число (напр., 125.50)"
            return
        }

        val entity = ShiftEntity(
            id = _editingShiftId.value ?: 0,
            driverId = driver.id,
            workDate = _formWorkDate.value.toString(),
            shiftStart = "%02d:%02d".format(_formShiftStart.value.hour, _formShiftStart.value.minute),
            tachoStart = "%02d:%02d".format(_formTachoStart.value.hour, _formTachoStart.value.minute),
            shiftEnd = "%02d:%02d".format(_formShiftEnd.value.hour, _formShiftEnd.value.minute),
            tachoEnd = "%02d:%02d".format(_formTachoEnd.value.hour, _formTachoEnd.value.minute),
            drivingHours = drivingHoursVal.toString(),
            nightStop = if (_formNightStop.value) 1 else 0,
            expenses = expensesVal.toString()
        )

        viewModelScope.launch {
            if (_editingShiftId.value == null) {
                repository.saveShift(entity)
            } else {
                repository.updateShift(entity)
            }
            _isFormOpen.value = false
            _formValidationError.value = null
        }
    }

    fun deleteShift(shiftId: Int) {
        viewModelScope.launch {
            repository.deleteShiftById(shiftId)
        }
    }
}
