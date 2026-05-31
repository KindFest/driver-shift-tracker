package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DriverEntity
import com.example.domain.ComplianceCalculator
import com.example.domain.ShiftRecord
import com.example.domain.WeeklyCompliance
import com.example.ui.MainViewModel
import com.example.ui.UIState
import com.example.ui.theme.MyApplicationTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val viewModel: MainViewModel = viewModel()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentDriver by viewModel.currentDriver.collectAsStateWithLifecycle()
    val isFormOpen by viewModel.isFormOpen.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == MainViewModel.Tab.DASHBOARD,
                    onClick = { viewModel.setActiveTab(MainViewModel.Tab.DASHBOARD) },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Дашборд") },
                    label = { Text("Дашборд") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == MainViewModel.Tab.SHIFTS,
                    onClick = { viewModel.setActiveTab(MainViewModel.Tab.SHIFTS) },
                    icon = { Icon(Icons.Filled.LocalShipping, contentDescription = "Смены") },
                    label = { Text("Смены") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_shifts")
                )
                NavigationBarItem(
                    selected = activeTab == MainViewModel.Tab.REPORTS,
                    onClick = { viewModel.setActiveTab(MainViewModel.Tab.REPORTS) },
                    icon = { Icon(Icons.Filled.Assessment, contentDescription = "Отчеты") },
                    label = { Text("Отчеты") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = activeTab == MainViewModel.Tab.PROFILE,
                    onClick = { viewModel.setActiveTab(MainViewModel.Tab.PROFILE) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Профиль") },
                    label = { Text("Профиль") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        },
        floatingActionButton = {
            if (!isFormOpen && (activeTab == MainViewModel.Tab.DASHBOARD || activeTab == MainViewModel.Tab.SHIFTS)) {
                FloatingActionButton(
                    onClick = { viewModel.openNewShiftForm() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_shift")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить смену")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is UIState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UIState.Success -> {
                    AnimatedContent(
                        targetState = activeTab,
                        label = "tabTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            MainViewModel.Tab.DASHBOARD -> DashboardScreen(viewModel)
                            MainViewModel.Tab.SHIFTS -> ShiftsScreen(viewModel)
                            MainViewModel.Tab.REPORTS -> ReportsScreen(viewModel)
                            MainViewModel.Tab.PROFILE -> ProfileScreen(viewModel, (uiState as UIState.Success).driver)
                        }
                    }
                }
            }

            // Shift Entry Form Overlay
            if (isFormOpen) {
                ShiftFormOverlay(viewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// DASHBOARD TAB COMPONENT
// -------------------------------------------------------------
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val weeklyCompliance by viewModel.weeklyCompliance.collectAsStateWithLifecycle()
    val currentAnchor by viewModel.selectedWeekAnchor.collectAsStateWithLifecycle()
    val driver by viewModel.currentDriver.collectAsStateWithLifecycle()

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val (weekStart, weekEnd) = ComplianceCalculator.getWeekBounds(currentAnchor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Profile header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Привет, ${driver?.name ?: "Водитель"}!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Учёт рабочего времени • Режим труда и отдыха ЕС",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Calendar controller
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setSelectedWeekAnchor(currentAnchor.minusWeeks(1)) },
                modifier = Modifier.testTag("btn_prev_week")
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Предыдущая неделя")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Недельный отчет",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${weekStart.format(formatter)} - ${weekEnd.minusDays(1).format(formatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = { viewModel.setSelectedWeekAnchor(currentAnchor.plusWeeks(1)) },
                modifier = Modifier.testTag("btn_next_week")
            ) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Следующая неделя")
            }
        }

        val comp = weeklyCompliance
        if (comp == null || comp.records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ContentPasteSearch,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Нет смен за этот период",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.openNewShiftForm() },
                        modifier = Modifier.testTag("btn_add_first_shift")
                    ) {
                        Text("Добавить смену")
                    }
                }
            }
        } else {
            // Compliance Stats row
            Text(
                text = "Лимиты на неделю (Регламент ЕС № 561/2006):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ComplianceCard(
                    title = "Смены > 13ч",
                    count = comp.over13hShiftsCount,
                    limit = 3,
                    exceeded = comp.over13hLimitExceeded,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                ComplianceCard(
                    title = "Вождение > 9ч",
                    count = comp.over9hDrivingCount,
                    limit = 2,
                    exceeded = comp.over9hDrivingLimitExceeded,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                ComplianceCard(
                    title = "Отдых < 11ч",
                    count = comp.shortRestsCount,
                    limit = 3,
                    exceeded = comp.shortRestsLimitExceeded,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            // Week Totals card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Итоговые показатели",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TotalMetric(
                            icon = Icons.Outlined.Timer,
                            label = "Времени за рулем",
                            value = ComplianceCalculator.formatMinutes(comp.totalDrivingMinutes)
                        )
                        TotalMetric(
                            icon = Icons.Outlined.AttachMoney,
                            label = "Сумма расходов",
                            value = "%.2f €".format(comp.totalExpenses)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TotalMetric(
                            icon = Icons.Outlined.Schedule,
                            label = "Обычные смены",
                            value = ComplianceCalculator.formatHours(comp.totalShiftHours)
                        )
                        TotalMetric(
                            icon = Icons.Outlined.Schedule,
                            label = "Смены по тако",
                            value = ComplianceCalculator.formatHours(comp.totalTachoShiftHours)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TotalMetric(
                            icon = Icons.Outlined.NightlightRound,
                            label = "Ночевок в пути",
                            value = "${comp.nightsCount} ноч."
                        )
                    }
                }
            }

            // Filtered shifts header info
            Text(
                text = "Зарегистрированные смены этой недели:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            comp.records.forEach { shift ->
                ShiftMiniCard(record = shift, onEdit = {
                    viewModel.openEditShiftForm(shift)
                })
            }
        }
    }
}

@Composable
fun ComplianceCard(
    title: String,
    count: Int,
    limit: Int,
    exceeded: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (exceeded) {
        MaterialTheme.colorScheme.errorContainer
    } else if (count == limit) {
        Color(0xFFFFF3CD) // Light amber / Warning state color
    } else {
        Color(0xFFE8F5E9) // Clean light emerald color
    }

    val contentColor = if (exceeded) {
        MaterialTheme.colorScheme.onErrorContainer
    } else if (count == limit) {
        Color(0xFF856404)
    } else {
        Color(0xFF2E7D32)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
            Text(
                text = "$count из $limit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (exceeded) "Превышен!" else "Норма",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TotalMetric(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(160.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShiftMiniCard(record: ShiftRecord, onEdit: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM")
    val formatterTime = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.workDate.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Редактировать смену",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Обычная смена: ${record.shiftStart.format(formatterTime)} - ${record.shiftEnd.format(formatterTime)} (${record.formattedShiftDuration})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Тако: ${record.tachoStart.format(formatterTime)} - ${record.tachoEnd.format(formatterTime)} (${record.formattedTachoShiftDuration})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Вождение: ${record.formattedDriving}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SHIFTS LOG TAB COMPONENT
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftsScreen(viewModel: MainViewModel) {
    val shifts by viewModel.allShifts.collectAsStateWithLifecycle()
    var shiftToDelete by remember { mutableStateOf<ShiftRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Журнал смен (${shifts.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (shifts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Журнал пуст. Внесите свою первую рабочую смену!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.openNewShiftForm() }) {
                        Text("Внести смену")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("shifts_lazy_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shifts, key = { it.id }) { shift ->
                    ShiftLargeCard(
                        record = shift,
                        onEdit = { viewModel.openEditShiftForm(shift) },
                        onDelete = { shiftToDelete = shift }
                    )
                }
            }
        }
    }

    // Delete Confirmation dialog
    if (shiftToDelete != null) {
        AlertDialog(
            onDismissRequest = { shiftToDelete = null },
            title = { Text("Удалить смену?") },
            text = { Text("Вы уверены, что хотите безвозвратно удалить смену за ${shiftToDelete?.workDate} из журнала?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        shiftToDelete?.let { viewModel.deleteShift(it.id) }
                        shiftToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { shiftToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ShiftLargeCard(
    record: ShiftRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatDay = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")
    val formatTime = DateTimeFormatter.ofPattern("HH:mm")

    val isOver13Tacho = record.tachoShiftHours > 13.0
    val isOver9Driving = record.drivingHours > 9.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shift_card_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.workDate.format(formatDay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_shift_${record.id}")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Quick warning markers
            if (isOver13Tacho || isOver9Driving) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isOver13Tacho) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Смена > 13 часов (Тако)", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isOver9Driving) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Вождение > 9 часов", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Timers grid details styled vertically: standard first, then tachograph
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text("Рабочая Смена (Обычная):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${record.shiftStart.format(formatTime)} - ${record.shiftEnd.format(formatTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Длительность: ${record.formattedShiftDuration}", style = MaterialTheme.typography.labelSmall)
                }
                Column {
                    Text("Режим ТАКО (Тахограф):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${record.tachoStart.format(formatTime)} - ${record.tachoEnd.format(formatTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Длительность: ${record.formattedTachoShiftDuration}", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Indicators row (Nights stop, Driving, expenses)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (record.nightStop) Icons.Filled.NightlightRound else Icons.Outlined.LightMode,
                        contentDescription = null,
                        tint = if (record.nightStop) Color(0xFF512DA8) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (record.nightStop) "Ночь в дороге" else "Домашняя ночь",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    Text(
                        text = "${record.expenses} €",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REPORTS TAB COMPONENT (WITH TEXT REPORT COPY SUPPORT)
// -------------------------------------------------------------
@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val reportStart by viewModel.reportStartDate.collectAsStateWithLifecycle()
    val reportEnd by viewModel.reportEndDate.collectAsStateWithLifecycle()
    val weeklySummaries by viewModel.customReportWeeksCompliance.collectAsStateWithLifecycle()
    val driver by viewModel.currentDriver.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    // Compile dynamic string matching equivalent text reports format
    val textReportStr = remember(reportStart, reportEnd, weeklySummaries, driver) {
        val lines = mutableListOf<String>()
        val driverName = driver?.name ?: "Водитель"
        lines.add("Месячный отчет: $driverName")
        lines.add("Период: ${reportStart.format(formatter)} - ${reportEnd.format(formatter)}")
        lines.add("")

        weeklySummaries.forEachIndexed { i, comp ->
            val wStartStr = comp.weekStart.format(DateTimeFormatter.ofPattern("dd.MM"))
            val wEndStr = comp.weekEnd.minusDays(1).format(DateTimeFormatter.ofPattern("dd.MM"))
            
            val totalMins = comp.totalDrivingMinutes
            val fDrivingStr = "${totalMins / 60} ч ${totalMins % 60} мин"
            
            val totalHrs = comp.totalShiftHours.toInt()
            val fShiftStr = "$totalHrs ч ${Math.round((comp.totalShiftHours - totalHrs) * 60)} мин"

            val totalTachoHrs = comp.totalTachoShiftHours.toInt()
            val fTachoShiftStr = "$totalTachoHrs ч ${Math.round((comp.totalTachoShiftHours - totalTachoHrs) * 60)} мин"

            lines.add(
                "Неделя ${i + 1} ($wStartStr-$wEndStr): " +
                "смены (обычные) $fShiftStr, смены (тахограф) $fTachoShiftStr, вождение $fDrivingStr, " +
                "ночи ${comp.nightsCount}, расходы ${comp.totalExpenses}"
            )
        }

        val totalDrivingMinutes = weeklySummaries.sumOf { it.totalDrivingMinutes }
        val fTotalDriving = "${totalDrivingMinutes / 60} ч ${totalDrivingMinutes % 60} мин"

        val totalShiftHours = weeklySummaries.sumOf { it.totalShiftHours }
        val totalShiftHrsInt = totalShiftHours.toInt()
        val fTotalShiftStr = "$totalShiftHrsInt ч ${Math.round((totalShiftHours - totalShiftHrsInt) * 60)} мин"

        val totalTachoShiftHours = weeklySummaries.sumOf { it.totalTachoShiftHours }
        val totalTachoShiftHrsInt = totalTachoShiftHours.toInt()
        val fTotalTachoShiftStr = "$totalTachoShiftHrsInt ч ${Math.round((totalTachoShiftHours - totalTachoShiftHrsInt) * 60)} мин"

        lines.add("")
        lines.add("Итого:")
        lines.add("Обычные смены: $fTotalShiftStr")
        lines.add("Смены по тахографу: $fTotalTachoShiftStr")
        lines.add("Вождение: $fTotalDriving")
        lines.add("Ночи: ${weeklySummaries.sumOf { it.nightsCount }}")
        lines.add("Расходы: ${weeklySummaries.sumOf { it.totalExpenses }}")

        lines.joinToString("\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Сводные отчеты",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Date selection cards
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Диапазон дат для отчета:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ClickableField(
                        label = "Начало периода",
                        value = reportStart.format(formatter),
                        icon = Icons.Filled.DateRange,
                        onClick = showDatePicker(reportStart) { viewModel.setReportStartDate(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    ClickableField(
                        label = "Конец периода",
                        value = reportEnd.format(formatter),
                        icon = Icons.Filled.DateRange,
                        onClick = showDatePicker(reportEnd) { viewModel.setReportEndDate(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action block to copy formatted text report
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Текстовый отчет для отправки",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Удобно скопировать для отправки диспетчеру через мессенджеры или почту.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(textReportStr))
                        Toast.makeText(context, "Отчет скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_copy_report"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Скопировать отчет")
                }
            }
        }

        // Live Preview report
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Предпросмотр отчета:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = textReportStr,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// PROFILE / GUIDES TAB COMPONENT
// -------------------------------------------------------------
@Composable
fun ProfileScreen(viewModel: MainViewModel, driver: DriverEntity) {
    var nameText by remember { mutableStateOf(driver.name) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Профиль водителя",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Имя водителя") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_driver_name")
                )

                Button(
                    onClick = {
                        viewModel.updateDriverProfile(nameText.trim(), 0L)
                        Toast.makeText(context, "Профиль успешно обновлен!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_save_profile")
                ) {
                    Text("Сохранить профиль")
                }
            }
        }

        // Regulation EC Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Регламент ЕС № 561/2006",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Основные нормативы ЕС для грузовых перевозок:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()

                GuideItem(
                    emoji = "🚚",
                    title = "Максимальная смена тако (Tacho Shift):",
                    desc = "Стандартная смена составляет 13 часов. Ограничение может превышать 13 часов (но до 15 часов) не более 3 раз за рабочую неделю."
                )
                GuideItem(
                    emoji = "⏱️",
                    title = "Максимальное вождение за сутки:",
                    desc = "Стандартный дневной лимит составляет 9 часов. Допускается увеличение вождения до 10 часов не более 2 раз за рабочую неделю."
                )
                GuideItem(
                    emoji = "🛌",
                    title = "Ежедневный отдых:",
                    desc = "Регулярный отдых составляет не менее 11 часов подряд между рабочими сменами. Отдых менее 11 часов (но более 9 часов) считается сокращенным отдыхом."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GuideItem(emoji: String, title: String, desc: String) {
    Row {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------
// OVERLAY MODAL FORM COMPONENT
// -------------------------------------------------------------
@Composable
fun ShiftFormOverlay(viewModel: MainViewModel) {
    val editingId by viewModel.editingShiftId.collectAsStateWithLifecycle()
    val workDate by viewModel.formWorkDate.collectAsStateWithLifecycle()
    val shiftStart by viewModel.formShiftStart.collectAsStateWithLifecycle()
    val tachoStart by viewModel.formTachoStart.collectAsStateWithLifecycle()
    val shiftEnd by viewModel.formShiftEnd.collectAsStateWithLifecycle()
    val tachoEnd by viewModel.formTachoEnd.collectAsStateWithLifecycle()
    val drivingHoursText by viewModel.formDrivingHoursText.collectAsStateWithLifecycle()
    val nightStop by viewModel.formNightStop.collectAsStateWithLifecycle()
    val expensesText by viewModel.formExpensesText.collectAsStateWithLifecycle()
    val errorText by viewModel.formValidationError.collectAsStateWithLifecycle()

    val formatterDate = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val formatterTime = DateTimeFormatter.ofPattern("HH:mm")

    // Full screen popup design with dark overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { /* Block clicking background to prevent closing */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("shift_form_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingId == null) "Новая смена" else "Редактирование смены",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.closeForm() }, modifier = Modifier.testTag("btn_close_form")) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                    }
                }
                HorizontalDivider()

                // Form validation error indicator
                if (errorText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorText!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Date Picker Block
                ClickableField(
                    label = "Дата смены (ДД.ММ.ГГГГ)",
                    value = workDate.format(formatterDate),
                    icon = Icons.Filled.DateRange,
                    onClick = showDatePicker(workDate) { viewModel.setFormWorkDate(it) },
                    modifier = Modifier.testTag("field_work_date")
                )

                // Timeline sections (WORK shifts times)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Общая Рабочая Смена (Обычная):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ClickableField(
                                label = "Начало работы",
                                value = shiftStart.format(formatterTime),
                                icon = Icons.Filled.Schedule,
                                onClick = showTimePicker(shiftStart) { viewModel.setFormShiftStart(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("field_work_start")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ClickableField(
                                label = "Окончание работы",
                                value = shiftEnd.format(formatterTime),
                                icon = Icons.Filled.Schedule,
                                onClick = showTimePicker(shiftEnd) { viewModel.setFormShiftEnd(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("field_work_end")
                            )
                        }
                    }
                }

                // Timeline sections (TACO times)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Смена по Тахографу (ТАКО):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ClickableField(
                                label = "Начало Тако",
                                value = tachoStart.format(formatterTime),
                                icon = Icons.Filled.Schedule,
                                onClick = showTimePicker(tachoStart) { viewModel.setFormTachoStart(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("field_tacho_start")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ClickableField(
                                label = "Окончание Тако",
                                value = tachoEnd.format(formatterTime),
                                icon = Icons.Filled.Schedule,
                                onClick = showTimePicker(tachoEnd) { viewModel.setFormTachoEnd(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("field_tacho_end")
                            )
                        }
                    }
                }

                // Technical hours input field
                // Technical hours input field (now using TimePickerDialog)
                val initialDrivingTime = remember(drivingHoursText) {
                    try {
                        val cleaned = drivingHoursText.replace(",", ".").trim()
                        if (cleaned.contains(":")) {
                            val parts = cleaned.split(":")
                            val h = parts[0].toInt().coerceIn(0, 23)
                            val m = parts[1].toInt().coerceIn(0, 59)
                            LocalTime.of(h, m)
                        } else if (cleaned.isNotEmpty()) {
                            val floatVal = cleaned.toFloat()
                            val h = floatVal.toInt().coerceIn(0, 23)
                            val m = ((floatVal - h) * 60).toInt().coerceIn(0, 59)
                            LocalTime.of(h, m)
                        } else {
                            LocalTime.of(4, 30)
                        }
                    } catch (e: Exception) {
                        LocalTime.of(4, 30)
                    }
                }

                ClickableField(
                    label = "Часы вождения (ЧЧ:ММ)",
                    value = drivingHoursText,
                    icon = Icons.Filled.Schedule,
                    onClick = showTimePicker(initialDrivingTime) { time ->
                        viewModel.setFormDrivingHoursText("%d:%02d".format(time.hour, time.minute))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_driving_hours")
                )

                // Night stop check status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.setFormNightStop(!nightStop) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = nightStop,
                        onCheckedChange = { viewModel.setFormNightStop(it) },
                        modifier = Modifier.testTag("checkbox_night_stop")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Остановка на ночь", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Отметьте, если была ночёвка в пути", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Cash expenses input
                OutlinedTextField(
                    value = expensesText,
                    onValueChange = { viewModel.setFormExpensesText(it) },
                    label = { Text("Дорожные расходы (€)") },
                    placeholder = { Text("Например: 12.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_expenses")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Submit controllers
                Button(
                    onClick = { viewModel.submitShift() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_shift")
                ) {
                    Text("Подтвердить и сохранить")
                }
            }
        }
    }
}

@Composable
fun ClickableField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// -------------------------------------------------------------
// NATIVE TIME & DATE COMPOSE COMPATIBILITY HELPERS
// -------------------------------------------------------------
@Composable
fun showDatePicker(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit): () -> Unit {
    val context = LocalContext.current
    return {
        DatePickerDialog(
            context,
            R.style.CustomDatePickerTheme,
            { _, yr, mo, dy -> onDateSelected(LocalDate.of(yr, mo + 1, dy)) },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }
}

@Composable
fun showTimePicker(initialTime: LocalTime, onTimeSelected: (LocalTime) -> Unit): () -> Unit {
    val context = LocalContext.current
    return {
        TimePickerDialog(
            context,
            R.style.CustomDatePickerTheme,
            { _, hr, min -> onTimeSelected(LocalTime.of(hr, min)) },
            initialTime.hour,
            initialTime.minute,
            true // 24-hour mode
        ).show()
    }
}
