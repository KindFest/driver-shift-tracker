package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DriverEntity
import com.example.domain.ComplianceCalculator
import com.example.domain.ShiftRecord
import com.example.domain.WeeklyCompliance
import com.example.ui.Localization
import com.example.ui.MainViewModel
import com.example.ui.UIState
import com.example.ui.theme.MyApplicationTheme
import java.time.LocalDate
import java.time.LocalDateTime
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
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val trans = { key: String -> Localization.get(key, appLanguage) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == MainViewModel.Tab.DASHBOARD,
                    onClick = { viewModel.setActiveTab(MainViewModel.Tab.DASHBOARD) },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = trans("dashboard_tab")) },
                    label = { Text(trans("dashboard_tab")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == MainViewModel.Tab.SHIFTS,
                    onClick = { viewModel.setActiveTab(MainViewModel.Tab.SHIFTS) },
                    icon = { Icon(Icons.Filled.LocalShipping, contentDescription = trans("shifts_tab")) },
                    label = { Text(trans("shifts_tab")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_shifts")
                )
                NavigationBarItem(
                    selected = activeTab == MainViewModel.Tab.REPORTS,
                    onClick = { viewModel.setActiveTab(MainViewModel.Tab.REPORTS) },
                    icon = { Icon(Icons.Filled.Assessment, contentDescription = trans("reports_tab")) },
                    label = { Text(trans("reports_tab")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = activeTab == MainViewModel.Tab.SETTINGS,
                    onClick = { viewModel.setActiveTab(MainViewModel.Tab.SETTINGS) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = trans("settings_tab")) },
                    label = { Text(trans("settings_tab")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_settings")
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
                    Icon(Icons.Filled.Add, contentDescription = trans("add_shift_btn"))
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
                            MainViewModel.Tab.SETTINGS -> SettingsScreen(viewModel, (uiState as UIState.Success).driver)
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
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val nextShiftRegular by viewModel.nextShiftStartRegular.collectAsStateWithLifecycle()
    val nextShiftReduced by viewModel.nextShiftStartReduced.collectAsStateWithLifecycle()

    val trans = { key: String -> Localization.get(key, appLanguage) }
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Localization.getLocale(appLanguage))
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
                        text = trans("welcome_title").format(driver?.name ?: trans("settings_driver_name")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = trans("welcome_subtitle"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // NEXT SHIFT TIMERS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trans("next_shift_start_title"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val dtFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM, HH:mm", Localization.getLocale(appLanguage))
                    val regularTimeStr = nextShiftRegular?.format(dtFormatter) ?: trans("no_data")
                    val reducedTimeStr = nextShiftReduced?.format(dtFormatter) ?: trans("no_data")

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trans("regular_rest_label"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = regularTimeStr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32) // Green for regular rest
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trans("reduced_rest_label"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reducedTimeStr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100) // Amber for reduced rest
                        )
                    }
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
                Icon(Icons.Filled.ArrowBack, contentDescription = "Prev Week")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = trans("week_report_title"),
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
                Icon(Icons.Filled.ArrowForward, contentDescription = "Next Week")
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
                        text = trans("no_shifts_period"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.openNewShiftForm() },
                        modifier = Modifier.testTag("btn_add_first_shift")
                    ) {
                        Text(trans("add_shift_btn"))
                    }
                }
            }
        } else {
            // Compliance Stats row
            Text(
                text = trans("weekly_limits_title"),
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
                    title = trans("limit_shifts_13h"),
                    count = comp.over13hShiftsCount,
                    limit = 3,
                    exceeded = comp.over13hLimitExceeded,
                    lang = appLanguage,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                ComplianceCard(
                    title = trans("limit_driving_9h"),
                    count = comp.over9hDrivingCount,
                    limit = 2,
                    exceeded = comp.over9hDrivingLimitExceeded,
                    lang = appLanguage,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                ComplianceCard(
                    title = trans("limit_rest_11h"),
                    count = comp.shortRestsCount,
                    limit = 3,
                    exceeded = comp.shortRestsLimitExceeded,
                    lang = appLanguage,
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
                        text = trans("totals_title"),
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
                            label = trans("metric_driving"),
                            value = ComplianceCalculator.formatMinutes(comp.totalDrivingMinutes)
                        )
                        TotalMetric(
                            icon = Icons.Outlined.AttachMoney,
                            label = trans("metric_expenses"),
                            value = "%.2f €".format(comp.totalExpenses)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TotalMetric(
                            icon = Icons.Outlined.Schedule,
                            label = trans("metric_regular_shifts"),
                            value = ComplianceCalculator.formatHours(comp.totalShiftHours)
                        )
                        TotalMetric(
                            icon = Icons.Outlined.Schedule,
                            label = trans("metric_tacho_shifts"),
                            value = ComplianceCalculator.formatHours(comp.totalTachoShiftHours)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TotalMetric(
                            icon = Icons.Outlined.NightlightRound,
                            label = trans("metric_nights"),
                            value = trans("nights_count").format(comp.nightsCount)
                        )
                    }
                }
            }

            // Filtered shifts header info
            Text(
                text = trans("registered_shifts_week"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            comp.records.forEach { shift ->
                ShiftMiniCard(record = shift, lang = appLanguage, onEdit = {
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
    lang: String,
    modifier: Modifier = Modifier
) {
    val trans = { key: String -> Localization.get(key, lang) }
    val containerColor = if (exceeded) {
        MaterialTheme.colorScheme.errorContainer
    } else if (count == limit) {
        Color(0xFFFFF3CD) // Amber warning
    } else {
        Color(0xFFE8F5E9) // Clean light emerald
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
                text = if (exceeded) trans("limit_exceeded") else trans("limit_normal"),
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
fun ShiftMiniCard(record: ShiftRecord, lang: String, onEdit: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Localization.getLocale(lang))
    val formatterTime = DateTimeFormatter.ofPattern("HH:mm", Localization.getLocale(lang))
    val trans = { key: String -> Localization.get(key, lang) }

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
                    contentDescription = trans("edit_btn"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${trans("regular_shift_label")} ${record.shiftStart.format(formatterTime)} - ${record.shiftEnd.format(formatterTime)} (${record.formattedShiftDuration})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${trans("tacho_label")} ${record.tachoStart.format(formatterTime)} - ${record.tachoEnd.format(formatterTime)} (${record.formattedTachoShiftDuration})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${trans("driving_label")} ${record.formattedDriving}",
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
    val shifts by viewModel.filteredShifts.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()

    val filterStart by viewModel.shiftFilterStartDate.collectAsStateWithLifecycle()
    val filterEnd by viewModel.shiftFilterEndDate.collectAsStateWithLifecycle()

    var shiftToDelete by remember { mutableStateOf<ShiftRecord?>(null) }
    val trans = { key: String -> Localization.get(key, appLanguage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = trans("shifts_log_title").format(shifts.size),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // FILTER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trans("filter_by_date_title"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (filterStart != null || filterEnd != null) {
                        TextButton(
                            onClick = { viewModel.clearShiftFilters() }
                        ) {
                            Text(trans("clear_filter_btn"))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Localization.getLocale(appLanguage))
                    val startText = filterStart?.format(formatter) ?: trans("filter_start_placeholder")
                    val endText = filterEnd?.format(formatter) ?: trans("filter_end_placeholder")

                    ClickableField(
                        label = trans("filter_start_label"),
                        value = startText,
                        icon = Icons.Filled.DateRange,
                        onClick = showDatePicker(filterStart ?: LocalDate.now()) { viewModel.setShiftFilterStartDate(it) },
                        modifier = Modifier.weight(1f)
                    )

                    ClickableField(
                        label = trans("filter_end_label"),
                        value = endText,
                        icon = Icons.Filled.DateRange,
                        onClick = showDatePicker(filterEnd ?: LocalDate.now()) { viewModel.setShiftFilterEndDate(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

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
                        text = trans("shifts_log_empty"),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.openNewShiftForm() }) {
                        Text(trans("add_shift_action"))
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
                        lang = appLanguage,
                        appMode = appMode,
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
            title = { Text(trans("delete_dialog_title")) },
            text = { Text(trans("delete_dialog_text").format(shiftToDelete?.workDate.toString())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        shiftToDelete?.let { viewModel.deleteShift(it.id) }
                        shiftToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text(trans("delete_btn"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { shiftToDelete = null }) {
                    Text(trans("cancel_btn"))
                }
            }
        )
    }
}

@Composable
fun ShiftLargeCard(
    record: ShiftRecord,
    lang: String,
    appMode: MainViewModel.AppMode,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatDay = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Localization.getLocale(lang))
    val formatTime = DateTimeFormatter.ofPattern("HH:mm", Localization.getLocale(lang))
    val trans = { key: String -> Localization.get(key, lang) }

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
                        Icon(Icons.Filled.Edit, contentDescription = trans("edit_btn"), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_shift_${record.id}")) {
                        Icon(Icons.Filled.Delete, contentDescription = trans("delete_btn"), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
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
                            Text(trans("limit_shifts_13h") + " (Tacho)", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isOver9Driving) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(trans("limit_driving_9h"), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Timers grid details styled vertically
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (appMode == MainViewModel.AppMode.EXTENDED) {
                    Column {
                        Text(trans("regular_shift_label"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${record.shiftStart.format(formatTime)} - ${record.shiftEnd.format(formatTime)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("${trans("duration_label")} ${record.formattedShiftDuration}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Column {
                    Text(trans("tacho_label"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${record.tachoStart.format(formatTime)} - ${record.tachoEnd.format(formatTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("${trans("duration_label")} ${record.formattedTachoShiftDuration}", style = MaterialTheme.typography.labelSmall)
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
                        text = if (record.nightStop) trans("night_on_road") else trans("night_at_home"),
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
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Localization.getLocale(appLanguage))
    val trans = { key: String -> Localization.get(key, appLanguage) }

    // Compile dynamic string matching equivalent text reports format
    val textReportStr = remember(reportStart, reportEnd, weeklySummaries, driver, appLanguage) {
        val lines = mutableListOf<String>()
        val driverName = driver?.name ?: trans("settings_driver_name")
        lines.add(trans("month_report_title").format(driverName))
        lines.add(trans("period_label").format(reportStart.format(formatter), reportEnd.format(formatter)))
        lines.add("")

        weeklySummaries.forEachIndexed { i, comp ->
            val wStartStr = comp.weekStart.format(DateTimeFormatter.ofPattern("dd.MM", Localization.getLocale(appLanguage)))
            val wEndStr = comp.weekEnd.minusDays(1).format(DateTimeFormatter.ofPattern("dd.MM", Localization.getLocale(appLanguage)))

            val totalMins = comp.totalDrivingMinutes
            val fDrivingStr = "${totalMins / 60} ${trans("hour_char")} ${totalMins % 60} ${trans("minute_char")}"

            val totalHrs = comp.totalShiftHours.toInt()
            val fShiftStr = "$totalHrs ${trans("hour_char")} ${Math.round((comp.totalShiftHours - totalHrs) * 60)} ${trans("minute_char")}"

            val totalTachoHrs = comp.totalTachoShiftHours.toInt()
            val fTachoShiftStr = "$totalTachoHrs ${trans("hour_char")} ${Math.round((comp.totalTachoShiftHours - totalTachoHrs) * 60)} ${trans("minute_char")}"

            lines.add(
                trans("week_label").format(i + 1, wStartStr, wEndStr) +
                trans("week_detail_label").format(fShiftStr, fTachoShiftStr, fDrivingStr, comp.nightsCount, comp.totalExpenses.toString())
            )
        }

        val totalDrivingMinutes = weeklySummaries.sumOf { it.totalDrivingMinutes }
        val fTotalDriving = "${totalDrivingMinutes / 60} ${trans("hour_char")} ${totalDrivingMinutes % 60} ${trans("minute_char")}"

        val totalShiftHours = weeklySummaries.sumOf { it.totalShiftHours }
        val totalShiftHrsInt = totalShiftHours.toInt()
        val fTotalShiftStr = "$totalShiftHrsInt ${trans("hour_char")} ${Math.round((totalShiftHours - totalShiftHrsInt) * 60)} ${trans("minute_char")}"

        val totalTachoShiftHours = weeklySummaries.sumOf { it.totalTachoShiftHours }
        val totalTachoShiftHrsInt = totalTachoShiftHours.toInt()
        val fTotalTachoShiftStr = "$totalTachoShiftHrsInt ${trans("hour_char")} ${Math.round((totalTachoShiftHours - totalTachoShiftHrsInt) * 60)} ${trans("minute_char")}"

        lines.add("")
        lines.add(trans("total_label"))
        lines.add(trans("regular_shifts_total").format(fTotalShiftStr))
        lines.add(trans("tacho_shifts_total").format(fTotalTachoShiftStr))
        lines.add(trans("driving_total").format(fTotalDriving))
        lines.add(trans("nights_total").format(weeklySummaries.sumOf { it.nightsCount }))
        lines.add(trans("expenses_total").format(weeklySummaries.sumOf { it.totalExpenses }.toString()))

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
            text = trans("reports_title"),
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
                Text(trans("reports_range_label"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ClickableField(
                        label = trans("reports_start_label"),
                        value = reportStart.format(formatter),
                        icon = Icons.Filled.DateRange,
                        onClick = showDatePicker(reportStart) { viewModel.setReportStartDate(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    ClickableField(
                        label = trans("reports_end_label"),
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
                    text = trans("text_report_title"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = trans("text_report_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(textReportStr))
                        Toast.makeText(context, trans("toast_report_copied"), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_copy_report"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(trans("copy_report_btn"))
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
                    text = trans("preview_report_title"),
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
// SETTINGS TAB COMPONENT (FORMERLY PROFILE)
// -------------------------------------------------------------
@Composable
fun SettingsScreen(viewModel: MainViewModel, driver: DriverEntity) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()

    var nameText by remember { mutableStateOf(driver.name) }
    val context = LocalContext.current
    val trans = { key: String -> Localization.get(key, appLanguage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = trans("settings_title"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // SECTION 1: PROFILE
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = trans("settings_profile_section"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(trans("settings_driver_name")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_driver_name")
                )

                Button(
                    onClick = {
                        viewModel.updateDriverProfile(nameText.trim(), 0L)
                        Toast.makeText(context, trans("toast_profile_saved"), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_save_profile")
                ) {
                    Text(trans("settings_save_btn"))
                }
            }
        }

        // SECTION 2: LANGUAGE SELECTION
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = trans("settings_lang_section"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAppLanguage("ru") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = appLanguage == "ru",
                        onClick = { viewModel.setAppLanguage("ru") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(trans("settings_lang_ru"))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAppLanguage("en") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = appLanguage == "en",
                        onClick = { viewModel.setAppLanguage("en") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(trans("settings_lang_en"))
                }
            }
        }

        // SECTION 3: APP MODE SELECTION
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = trans("settings_mode_section"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAppMode(MainViewModel.AppMode.EXTENDED) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = appMode == MainViewModel.AppMode.EXTENDED,
                        onClick = { viewModel.setAppMode(MainViewModel.AppMode.EXTENDED) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(trans("settings_mode_extended"))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAppMode(MainViewModel.AppMode.SHORTENED) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = appMode == MainViewModel.AppMode.SHORTENED,
                        onClick = { viewModel.setAppMode(MainViewModel.AppMode.SHORTENED) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(trans("settings_mode_shortened"))
                }
            }
        }

        // SECTION 4: REGULATIONS GUIDE CARD
        var isGuideExpanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isGuideExpanded = !isGuideExpanded }
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trans("settings_docs_section"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isGuideExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = isGuideExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = trans("settings_docs_subtitle"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()

                        GuideItem(
                            emoji = "🚚",
                            title = trans("settings_doc_tacho_title"),
                            desc = trans("settings_doc_tacho_desc")
                        )
                        GuideItem(
                            emoji = "⏱️",
                            title = trans("settings_doc_driving_title"),
                            desc = trans("settings_doc_driving_desc")
                        )
                        GuideItem(
                            emoji = "🛌",
                            title = trans("settings_doc_rest_title"),
                            desc = trans("settings_doc_rest_desc")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A02006R0561-20200820"))
                                context.startActivity(urlIntent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("eur-lex.europa.eu (Regulation EC 561/2006)")
                        }
                    }
                }
            }
        }

        // SECTION 5: ABOUT
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = trans("settings_about_section"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = trans("about_text"),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = trans("settings_version_label").format(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
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

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()

    val formatterDate = DateTimeFormatter.ofPattern("dd.MM.yyyy", Localization.getLocale(appLanguage))
    val formatterTime = DateTimeFormatter.ofPattern("HH:mm", Localization.getLocale(appLanguage))
    val trans = { key: String -> Localization.get(key, appLanguage) }

    // Full screen popup design with dark overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { /* Block clicking background */ },
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
                        text = if (editingId == null) trans("form_new_shift") else trans("form_edit_shift"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.closeForm() }, modifier = Modifier.testTag("btn_close_form")) {
                        Icon(Icons.Filled.Close, contentDescription = trans("cancel_btn"))
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
                            text = if (errorText!!.startsWith("Ошибка часов")) trans("error_driving_hours") else if (errorText!!.startsWith("Ошибка расходов")) trans("error_expenses") else errorText!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Date Picker Block
                ClickableField(
                    label = trans("form_date_label"),
                    value = workDate.format(formatterDate),
                    icon = Icons.Filled.DateRange,
                    onClick = showDatePicker(workDate) { viewModel.setFormWorkDate(it) },
                    modifier = Modifier.testTag("field_work_date")
                )

                // Timeline sections (WORK shifts times - ONLY EXTENDED MODE)
                if (appMode == MainViewModel.AppMode.EXTENDED) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(trans("form_regular_shift_section"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ClickableField(
                                    label = trans("form_start_work"),
                                    value = shiftStart.format(formatterTime),
                                    icon = Icons.Filled.Schedule,
                                    onClick = showTimePicker(shiftStart) { viewModel.setFormShiftStart(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("field_work_start")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                ClickableField(
                                    label = trans("form_end_work"),
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
                } else {
                    // Show small info description in shortened mode
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = trans("tacho_only_mode_desc"),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
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
                        Text(trans("form_tacho_section"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ClickableField(
                                label = trans("form_start_tacho"),
                                value = tachoStart.format(formatterTime),
                                icon = Icons.Filled.Schedule,
                                onClick = showTimePicker(tachoStart) { viewModel.setFormTachoStart(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("field_tacho_start")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ClickableField(
                                label = trans("form_end_tacho"),
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

                // Technical hours input field (using TimePickerDialog)
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
                    label = trans("form_driving_hours"),
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
                        Text(trans("form_night_stop"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(trans("form_night_stop_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Cash expenses input
                OutlinedTextField(
                    value = expensesText,
                    onValueChange = { viewModel.setFormExpensesText(it) },
                    label = { Text(trans("form_expenses")) },
                    placeholder = { Text("e.g. 12.50") },
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
                    Text(trans("form_submit"))
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
