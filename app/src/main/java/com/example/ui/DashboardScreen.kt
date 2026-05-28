package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.DayActivity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: AttendanceViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isParsingTimetable by viewModel.isParsingTimetable.collectAsState()
    val isParsingHolidays by viewModel.isParsingHolidays.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val operationSuccess by viewModel.operationSuccess.collectAsState()

    val stats by viewModel.stats.collectAsState()
    val activities by viewModel.dayActivities.collectAsState()
    val classes by viewModel.allClasses.collectAsState()
    val holidays by viewModel.allHolidays.collectAsState()
    val suspensions by viewModel.allSuspensions.collectAsState()
    val records by viewModel.allRecords.collectAsState()

    val timetableUploaded by viewModel.timetableUploaded.collectAsState()
    val holidaysUploaded by viewModel.holidaysUploaded.collectAsState()

    val hasApiKey by viewModel.hasApiKey.collectAsState()
    val savedApiKey by viewModel.geminiApiKey.collectAsState()

    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("Tracker") }

    // Dialog state controllers
    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddHolidayDialog by remember { mutableStateOf(false) }
    var showSetGoalDialog by remember { mutableStateOf(false) }
    var showSuspendedDialog by remember { mutableStateOf(false) }
    var showMonthView by remember { mutableStateOf(false) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // Pickers for images and PDFs
    val timetableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val bitmaps = uriToBitmaps(context, it)
            if (bitmaps.isNotEmpty()) {
                viewModel.uploadTimetableImages(bitmaps)
            }
        }
    }

    val holidayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val bitmaps = uriToBitmaps(context, it)
            if (bitmaps.isNotEmpty()) {
                viewModel.uploadHolidaySheetImages(bitmaps)
            }
        }
    }

    // Color definitions
    val darkBg = Color(0xFF0F172A)     // Slate 900
    val cardBg = Color(0xFF1E293B)     // Slate 800
    val neonTeal = Color(0xFF10B981)   // Emerald 500
    val softTeal = Color(0x1F10B981)
    val coralRed = Color(0xFFEF4444)   // Red 500
    val softGold = Color(0xFFF59E0B)   // Amber 500
    val lightText = Color(0xFFF8FAFC)  // Slate 50
    val mutedText = Color(0xFF94A3B8)  // Slate 400

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Elegant modern bottom navigation tabs
            NavigationBar(
                containerColor = darkBg,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == "Tracker",
                    onClick = { currentTab = "Tracker" },
                    label = { Text("Daily tracker", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "Tracker") Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Tracker"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonTeal,
                        selectedTextColor = neonTeal,
                        indicatorColor = cardBg,
                        unselectedIconColor = mutedText,
                        unselectedTextColor = mutedText
                    )
                )

                NavigationBarItem(
                    selected = currentTab == "Schedules",
                    onClick = { currentTab = "Schedules" },
                    label = { Text("Timetable & Holidays", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "Schedules") Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Timetables"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonTeal,
                        selectedTextColor = neonTeal,
                        indicatorColor = cardBg,
                        unselectedIconColor = mutedText,
                        unselectedTextColor = mutedText
                    )
                )

                NavigationBarItem(
                    selected = currentTab == "History",
                    onClick = { currentTab = "History" },
                    label = { Text("History & Goals", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "History") Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonTeal,
                        selectedTextColor = neonTeal,
                        indicatorColor = cardBg,
                        unselectedIconColor = mutedText,
                        unselectedTextColor = mutedText
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBg)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // Application header with stats card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when (currentTab) {
                                    "Tracker" -> "Attendance Tracker"
                                    "Schedules" -> "Timetable & Holidays"
                                    "History" -> "History & Goals"
                                    else -> "Attendance Tracker"
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = lightText
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Gemini API key — highlighted gold until a key is set
                                IconButton(onClick = { showApiKeyDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "Gemini API Key",
                                        tint = if (hasApiKey) mutedText else softGold
                                    )
                                }
                                // Quick settings button for target goal
                                IconButton(onClick = { showSetGoalDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Set Attendance Goal",
                                        tint = mutedText
                                    )
                                }
                            }
                        }

                        if (currentTab != "Schedules") {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress and Stats summary card
                            MainStatsCard(stats = stats, softGold = softGold, neonTeal = neonTeal, coralRed = coralRed, lightText = lightText, mutedText = mutedText, cardBg = cardBg)
                        }
                    }
                }

                // Error and success snackbars mapped inside lists
                if (apiError != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, coralRed.copy(0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = coralRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Extraction alert", fontWeight = FontWeight.Bold, color = coralRed, fontSize = 14.sp)
                                    Text(apiError ?: "", color = lightText, fontSize = 12.sp)
                                }
                                IconButton(onClick = { viewModel.clearApiError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = mutedText)
                                }
                            }
                        }
                    }
                }

                if (operationSuccess != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x3310B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, neonTeal.copy(0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = neonTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = operationSuccess ?: "",
                                    color = lightText,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearOperationSuccess() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = mutedText)
                                }
                            }
                        }
                    }
                }

                // Loading indicators
                if (isParsingTimetable || isParsingHolidays) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = neonTeal, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = if (isParsingTimetable) "Gemini is analyzing timetable image..." else "Gemini is parsing holiday sheet...",
                                    color = lightText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Render tab content dynamically in the lazy column
                when (currentTab) {
                    "Tracker" -> {
                        // Date switcher header
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.setPreviousDay() },
                                            modifier = Modifier.testTag("prev_date_button")
                                        ) {
                                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = lightText)
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { showMonthView = !showMonthView }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = formatDateFull(selectedDate),
                                                    color = lightText,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = if (showMonthView) Icons.Default.CalendarToday else Icons.Default.CalendarMonth,
                                                    contentDescription = "Toggle Month View",
                                                    tint = neonTeal,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = "Day: " + getDayOfWeekName(selectedDate) + " (Click to toggle Calendar)",
                                                color = mutedText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.setNextDay() },
                                            modifier = Modifier.testTag("next_date_button")
                                        ) {
                                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = lightText)
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = showMonthView,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        CalendarMonthView(
                                            selectedDate = selectedDate,
                                            onDateSelected = { date ->
                                                viewModel.setSelectedDate(date)
                                            },
                                            allRecords = records,
                                            allSuspensions = suspensions,
                                            allHolidays = holidays,
                                            cardBg = cardBg,
                                            lightText = lightText,
                                            mutedText = mutedText,
                                            neonTeal = neonTeal,
                                            coralRed = coralRed,
                                            softGold = softGold
                                        )
                                    }
                                }
                            }
                        }

                        // Checklist header
                        item {
                            Text(
                                text = "Daily Checklist Calendar",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = lightText,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (activities.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBg, RoundedCornerShape(16.dp))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.EventNote,
                                            contentDescription = "No classes",
                                            tint = mutedText.copy(alpha = 0.5f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No classes scheduled for " + getDayOfWeekName(selectedDate),
                                            color = lightText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Upload a timetable image in the 'Timetable' tab, or manually add a class schedule to populate days automatically.",
                                            color = mutedText,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Render list of scheduled classes for today
                            items(activities) { activity ->
                                DailyChecklistItemCard(
                                    activity = activity,
                                    date = selectedDate,
                                    onMarkAttended = { viewModel.markAttendance(selectedDate, activity.className, "ATTENDED") },
                                    onMarkAbsent = { viewModel.markAttendance(selectedDate, activity.className, "ABSENT") },
                                    onClearAttendance = { viewModel.clearAttendance(selectedDate, activity.className) },
                                    onToggleSuspension = { suspend -> viewModel.toggleSuspension(activity.className, selectedDate, suspend) },
                                    lightText = lightText,
                                    mutedText = mutedText,
                                    neonTeal = neonTeal,
                                    coralRed = coralRed,
                                    softGold = softGold,
                                    cardBg = cardBg
                                )
                            }
                        }
                    }

                    "Schedules" -> {
                        // Prompt for an API key if none is configured (AI upload needs it)
                        if (!hasApiKey) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x33F59E0B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, softGold.copy(0.5f), RoundedCornerShape(12.dp))
                                        .clickable { showApiKeyDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = softGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Gemini API key needed", fontWeight = FontWeight.Bold, color = softGold, fontSize = 14.sp)
                                            Text("AI timetable & holiday parsing needs a key. Tap to add yours (free).", color = lightText, fontSize = 12.sp)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = softGold)
                                    }
                                }
                            }
                        }

                        // Timetable controls card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Timetable Source",
                                        fontWeight = FontWeight.Bold,
                                        color = lightText,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (timetableUploaded) "✓ Timetable source loaded & parsed with AI" else "No timetable file initialized yet.",
                                        color = if (timetableUploaded) neonTeal else mutedText,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { timetableLauncher.launch(arrayOf("image/*", "application/pdf")) },
                                            colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("upload_timetable_btn")
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Upload Timetable Image / PDF")
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { showAddClassDialog = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = lightText),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, mutedText),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .testTag("add_class_manually_btn")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Manual", maxLines = 1)
                                            }

                                            if (classes.isNotEmpty()) {
                                                OutlinedButton(
                                                    onClick = { viewModel.resetTimetable() },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = coralRed),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, coralRed),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .testTag("clear_classes_btn")
                                                ) {
                                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Reset", maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Holiday sheet controls card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Holiday Sheet Source",
                                        fontWeight = FontWeight.Bold,
                                        color = lightText,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (holidaysUploaded) "✓ Holiday sheet parsed with AI" else "No holiday documentation parsed.",
                                        color = if (holidaysUploaded) neonTeal else mutedText,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { holidayLauncher.launch(arrayOf("image/*", "application/pdf")) },
                                            colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("upload_holiday_btn")
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Upload Holiday Image / PDF")
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { showAddHolidayDialog = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = lightText),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, mutedText),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .testTag("add_holiday_manually_btn")
                                            ) {
                                                Icon(Icons.Default.BeachAccess, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Manual", maxLines = 1)
                                            }

                                            if (holidays.isNotEmpty()) {
                                                OutlinedButton(
                                                    onClick = { viewModel.resetHolidays() },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = coralRed),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, coralRed),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .testTag("clear_holidays_btn")
                                                ) {
                                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Reset", maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Structured lists below
                        item {
                            Text(
                                text = "Active Class Schedule (${classes.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = lightText,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (classes.isEmpty()) {
                            item {
                                Text(
                                    "No class entries loaded yet. Load above.",
                                    color = mutedText,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        } else {
                            items(classes) { classItem ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(classItem.className, color = lightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(softTeal, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = getDayNameFromInt(classItem.dayOfWeek),
                                                        color = neonTeal,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(classItem.timeSlot, color = mutedText, fontSize = 12.sp)
                                            }
                                        }

                                        IconButton(onClick = { viewModel.deleteClass(classItem.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Class", tint = coralRed)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Registered Holidays (${holidays.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = lightText,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (holidays.isEmpty()) {
                            item {
                                Text(
                                    "No holidays added. Mark manual working days as holiday as desired.",
                                    color = mutedText,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        } else {
                            items(holidays) { holidayItem ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(holidayItem.holidayName, color = lightText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(holidayItem.date, color = mutedText, fontSize = 12.sp)
                                        }

                                        IconButton(onClick = { viewModel.deleteHoliday(holidayItem.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Holiday", tint = coralRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "History" -> {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Scheduled Cancellations",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = lightText
                                    )
                                    Text(
                                        text = "Exempt selected classes from goals",
                                        color = mutedText,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.wrapContentWidth()) {
                                    OutlinedButton(
                                        onClick = { showSuspendedDialog = true },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = lightText),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, softGold),
                                        modifier = Modifier.testTag("add_suspension_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = softGold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Suspend Class", color = softGold, maxLines = 1)
                                    }
                                }
                            }
                        }

                        if (suspensions.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No classes are currently marked as suspended. Use suspensions to cancel single sessions without hurting your 75% attendance quota.",
                                        color = mutedText,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(suspensions) { suspension ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(suspension.className, color = lightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = softGold, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Suspended Class on ${suspension.date}", color = softGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        IconButton(onClick = { viewModel.deleteSuspension(suspension.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Active Rule Cancel", tint = coralRed)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Marked Logs History (${records.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = lightText
                                )
                                OutlinedButton(
                                    onClick = { showBulkDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = lightText),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, softGold),
                                    modifier = Modifier.testTag("bulk_ops_btn")
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = softGold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bulk Tools", color = softGold, fontSize = 12.sp)
                                }
                            }
                        }

                        if (records.isEmpty()) {
                            item {
                                Text(
                                    "No logs entries recorded yet. Go to Daily tab to begin logging.",
                                    color = mutedText,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        } else {
                            items(records) { record ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(record.className, color = lightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Recorded on ${record.date}", color = mutedText, fontSize = 12.sp)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (record.status == "ATTENDED") neonTeal.copy(alpha = 0.2f) else coralRed.copy(alpha = 0.2f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = record.status,
                                                    color = if (record.status == "ATTENDED") neonTeal else coralRed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            IconButton(onClick = { viewModel.clearAttendance(record.date, record.className) }) {
                                                Icon(Icons.Default.Close, contentDescription = "Delete Record", tint = mutedText)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs Configurations ---

    // 1. Set goal dialog
    if (showSetGoalDialog) {
        var tempGoal by remember { mutableStateOf(stats.targetGoal.toInt().toString()) }

        Dialog(onDismissRequest = { showSetGoalDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Target Attendance Goal", color = lightText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Enter preferred target threshold percentage to help keep you accountable (Default is 75%).", color = mutedText, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tempGoal,
                        onValueChange = { tempGoal = it },
                        label = { Text("Quorom Percentage", color = mutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = lightText,
                            unfocusedTextColor = lightText,
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = mutedText
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_goal_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSetGoalDialog = false }) {
                            Text("Cancel", color = mutedText)
                        }
                        Button(
                            onClick = {
                                val floatVal = tempGoal.toFloatOrNull() ?: 75f
                                viewModel.updateGoalPercentage(floatVal)
                                showSetGoalDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Target")
                        }
                    }
                }
            }
        }
    }

    // 1b. Gemini API key dialog
    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(savedApiKey) }
        var revealed by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showApiKeyDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gemini API Key", color = lightText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "AI timetable & holiday parsing uses Google Gemini. Paste your own free key from aistudio.google.com/apikey. It is stored only on this device and never shared.",
                        color = mutedText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("API Key", color = mutedText) },
                        singleLine = true,
                        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { revealed = !revealed }) {
                                Icon(
                                    imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (revealed) "Hide key" else "Show key",
                                    tint = mutedText
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = lightText,
                            unfocusedTextColor = lightText,
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = mutedText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showApiKeyDialog = false }) {
                            Text("Cancel", color = mutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.saveGeminiApiKey(tempKey)
                                showApiKeyDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Key")
                        }
                    }
                }
            }
        }
    }

    // 2. Add manual class dialog
    if (showAddClassDialog) {
        var inputClassName by remember { mutableStateOf("") }
        var inputDayOfWeek by remember { mutableStateOf(1) } // 1..7 (Mon..Sun)
        var inputHour by remember { mutableStateOf("09:00 - 10:00") }

        Dialog(onDismissRequest = { showAddClassDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Class Schedule", color = lightText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputClassName,
                        onValueChange = { inputClassName = it },
                        label = { Text("Class / Subject Name", color = mutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = lightText,
                            unfocusedTextColor = lightText,
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = mutedText
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_class_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Day of week", color = mutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Day selectors
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..7) {
                            val dayName = getDayNameFromInt(i).take(3)
                            FilterChip(
                                selected = inputDayOfWeek == i,
                                onClick = { inputDayOfWeek = i },
                                label = { Text(dayName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = neonTeal,
                                    selectedLabelColor = darkBg,
                                    containerColor = cardBg,
                                    labelColor = lightText
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputHour,
                        onValueChange = { inputHour = it },
                        label = { Text("Time Slot Range (e.g. 10:00 - 11:30)", color = mutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = lightText,
                            unfocusedTextColor = lightText,
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = mutedText
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddClassDialog = false }) {
                            Text("Cancel", color = mutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputClassName.isNotBlank()) {
                                    viewModel.addManualClass(inputClassName, inputDayOfWeek, inputHour)
                                    showAddClassDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add Schedule")
                        }
                    }
                }
            }
        }
    }

    // 3. Add manual holiday dialog
    if (showAddHolidayDialog) {
        var inputHolidayName by remember { mutableStateOf("") }
        var inputHolidayDate by remember { mutableStateOf(selectedDate) }

        Dialog(onDismissRequest = { showAddHolidayDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Calendar Holiday", color = lightText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputHolidayName,
                        onValueChange = { inputHolidayName = it },
                        label = { Text("Holiday Detail (e.g. Winter Break)", color = mutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = lightText,
                            unfocusedTextColor = lightText,
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = mutedText
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("holiday_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputHolidayDate,
                        onValueChange = { inputHolidayDate = it },
                        label = { Text("Holiday Date (YYYY-MM-DD)", color = mutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = lightText,
                            unfocusedTextColor = lightText,
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = mutedText
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("holiday_date_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddHolidayDialog = false }) {
                            Text("Cancel", color = mutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputHolidayName.isNotBlank() && inputHolidayDate.isNotBlank()) {
                                    viewModel.addManualHoliday(inputHolidayDate, inputHolidayName)
                                    showAddHolidayDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add Holiday")
                        }
                    }
                }
            }
        }
    }

    // 4. Add manual suspension scheduling dialog
    if (showSuspendedDialog) {
        var inputClassName by remember { mutableStateOf("") }
        var inputDate by remember { mutableStateOf(selectedDate) }

        Dialog(onDismissRequest = { showSuspendedDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Suspend Class Session", color = lightText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Declare a specific class as suspended/cancelled for a date so it is exempt from attendance percentages.", color = mutedText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputClassName,
                        onValueChange = { inputClassName = it },
                        label = { Text("Subject / Class Name (e.g. Maths)", color = mutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = lightText,
                            unfocusedTextColor = lightText,
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = mutedText
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("suspension_class_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputDate,
                        onValueChange = { inputDate = it },
                        label = { Text("Suspended Date (YYYY-MM-DD)", color = mutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = lightText,
                            unfocusedTextColor = lightText,
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = mutedText
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("suspension_date_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSuspendedDialog = false }) {
                            Text("Cancel", color = mutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputClassName.isNotBlank() && inputDate.isNotBlank()) {
                                    viewModel.addManualSuspension(inputClassName, inputDate)
                                    showSuspendedDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Schedule Suspension")
                        }
                    }
                }
            }
        }
    }

    // 5. Bulk Database Operations Dialog
    if (showBulkDialog) {
        var bulkMode by remember { mutableStateOf("range") } // "range" or "clear"

        // Monthwise clear states
        var clearYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }
        var clearMonth by remember { mutableStateOf(String.format("%02d", Calendar.getInstance().get(Calendar.MONTH) + 1)) }

        // Custom date range mark states
        var startRangeDate by remember { mutableStateOf(selectedDate) }
        var endRangeDate by remember { mutableStateOf(selectedDate) }
        var markStatus by remember { mutableStateOf("ATTENDED") } // "ATTENDED" or "ABSENT"

        Dialog(onDismissRequest = { showBulkDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Bulk Database Operations", color = lightText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardBg.copy(0.6f))
                            .border(1.dp, mutedText.copy(0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { bulkMode = "range" }
                                .background(if (bulkMode == "range") neonTeal else Color.Transparent)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Mark Date Range",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bulkMode == "range") Color(0xFF0F172A) else lightText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { bulkMode = "clear" }
                                .background(if (bulkMode == "clear") neonTeal else Color.Transparent)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Clear Month-wise",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bulkMode == "clear") Color(0xFF0F172A) else lightText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (bulkMode == "range") {
                        Text(
                            text = "Mass mark attendance status for all lessons scheduled in your timetable over a selected period:",
                            color = mutedText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = startRangeDate,
                            onValueChange = { startRangeDate = it },
                            label = { Text("Start Date (YYYY-MM-DD)", color = mutedText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = lightText,
                                unfocusedTextColor = lightText,
                                focusedBorderColor = neonTeal,
                                unfocusedBorderColor = mutedText
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("bulk_start_date_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = endRangeDate,
                            onValueChange = { endRangeDate = it },
                            label = { Text("End Date (YYYY-MM-DD)", color = mutedText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = lightText,
                                unfocusedTextColor = lightText,
                                focusedBorderColor = neonTeal,
                                unfocusedBorderColor = mutedText
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("bulk_end_date_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { markStatus = "ATTENDED" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (markStatus == "ATTENDED") neonTeal else cardBg.copy(0.4f)
                                ),
                                border = if (markStatus == "ATTENDED") null else BorderStroke(1.dp, mutedText.copy(0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Present", color = if (markStatus == "ATTENDED") Color(0xFF0F172A) else lightText)
                            }

                            Button(
                                onClick = { markStatus = "ABSENT" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (markStatus == "ABSENT") coralRed else cardBg.copy(0.4f)
                                ),
                                border = if (markStatus == "ABSENT") null else BorderStroke(1.dp, mutedText.copy(0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Absent", color = if (markStatus == "ABSENT") Color(0xFF0F172A) else lightText)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showBulkDialog = false }) {
                                Text("Cancel", color = mutedText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.massMarkAttendanceRange(startRangeDate, endRangeDate, markStatus)
                                    showBulkDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("execute_bulk_range_btn")
                            ) {
                                Text("Mark Range", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Select a specific month to completely erase all attendance logs and cancellations recorded in that month:",
                            color = mutedText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = clearYear,
                                onValueChange = { clearYear = it },
                                label = { Text("Year (YYYY)", color = mutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = lightText,
                                    unfocusedTextColor = lightText,
                                    focusedBorderColor = neonTeal,
                                    unfocusedBorderColor = mutedText
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("bulk_clear_year_input")
                            )

                            OutlinedTextField(
                                value = clearMonth,
                                onValueChange = { clearMonth = it },
                                label = { Text("Month (MM)", color = mutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = lightText,
                                    unfocusedTextColor = lightText,
                                    focusedBorderColor = neonTeal,
                                    unfocusedBorderColor = mutedText
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("bulk_clear_month_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showBulkDialog = false }) {
                                Text("Cancel", color = mutedText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (clearYear.length == 4 && clearMonth.length == 2) {
                                        viewModel.massClearLogsByMonth("$clearYear-$clearMonth")
                                        showBulkDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = coralRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("execute_bulk_clear_btn")
                            ) {
                                Text("Mass Clear", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainStatsCard(
    stats: com.example.viewmodel.AttendanceStats,
    softGold: Color,
    neonTeal: Color,
    coralRed: Color,
    lightText: Color,
    mutedText: Color,
    cardBg: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left progress circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .weight(0.35f)
            ) {
                // Background circle track
                Canvas(modifier = Modifier.size(90.dp)) {
                    drawCircle(
                        color = Color(0xFF334155), // Slate 700
                        radius = size.minDimension / 2,
                        style = Stroke(width = 8.dp.toPx())
                    )
                }

                val strokeProgress = animateFloatAsState(targetValue = stats.percentage / 100f)

                // Fill sweep indicator
                Canvas(modifier = Modifier.size(90.dp)) {
                    drawArc(
                        color = if (stats.percentage >= stats.targetGoal) neonTeal else softGold,
                        startAngle = -90f,
                        sweepAngle = strokeProgress.value * 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.0f%%", stats.percentage),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = lightText
                    )
                    Text(
                        text = "Goal: ${stats.targetGoal.toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right analytical stats
            Column(modifier = Modifier.weight(0.65f)) {
                Text(
                    text = if (stats.percentage >= stats.targetGoal) "On Track!" else "Goal Alert!",
                    color = if (stats.percentage >= stats.targetGoal) neonTeal else softGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Total classes logs: ${stats.totalExpectedClasses}  |  Attended: ${stats.totalAttendedClasses}",
                    color = lightText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Personalized analytical helper
                if (stats.totalExpectedClasses == 0) {
                    Text(
                        text = "No records logged yet. Your goal is to keep attendance above ${stats.targetGoal.toInt()}%. Begin logging below!",
                        color = mutedText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                } else if (stats.percentage >= stats.targetGoal) {
                    Text(
                        text = "Looking strong! You are ahead of schedule and can safely skip up to ${stats.skipsAllowed} class session(s) while maintaining your target ${stats.targetGoal.toInt()}% quota.",
                        color = mutedText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                } else {
                    Text(
                        text = "You need to attend ${stats.consecutiveToAttend} consecutive classes in a row to catch up to the ${stats.targetGoal.toInt()}% threshold quota.",
                        color = mutedText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DailyChecklistItemCard(
    activity: DayActivity,
    date: String,
    onMarkAttended: () -> Unit,
    onMarkAbsent: () -> Unit,
    onClearAttendance: () -> Unit,
    onToggleSuspension: (Boolean) -> Unit,
    lightText: Color,
    mutedText: Color,
    neonTeal: Color,
    coralRed: Color,
    softGold: Color,
    cardBg: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("class_card_${activity.classId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Top Row Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.className,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = lightText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = mutedText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activity.timeSlot,
                            fontSize = 12.sp,
                            color = mutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // If class is suspended, display large alert status banner
                if (activity.isSuspended) {
                    Box(
                        modifier = Modifier
                            .background(softGold.copy(0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("SUSPENDED", color = softGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (activity.isHoliday) {
                    Box(
                        modifier = Modifier
                            .background(mutedText.copy(0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "HOLIDAY" + (activity.holidayName?.let { ": $it" } ?: ""),
                            color = mutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (activity.attendanceStatus != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (activity.attendanceStatus == "ATTENDED") neonTeal.copy(0.2f) else coralRed.copy(0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = activity.attendanceStatus,
                            color = if (activity.attendanceStatus == "ATTENDED") neonTeal else coralRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Action section at bottom
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Row 1: Instant class suspension switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = if (activity.isSuspended) softGold else mutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Suspended / Cancelled Session",
                            color = if (activity.isSuspended) softGold else mutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = activity.isSuspended,
                            onCheckedChange = { isChecked -> onToggleSuspension(isChecked) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = softGold,
                                checkedTrackColor = softGold.copy(alpha = 0.5f),
                                uncheckedThumbColor = mutedText,
                                uncheckedTrackColor = cardBg
                            ),
                            modifier = Modifier
                                .scale(0.8f)
                                .testTag("suspend_switch_${activity.classId}")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activity.isSuspended) "Yes" else "No",
                            color = if (activity.isSuspended) softGold else mutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Right hand logging controllers: Mark attendance blocks or Exempt badges
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!activity.isSuspended && !activity.isHoliday) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activity.attendanceStatus != null) {
                                // If marked, show retract option
                                TextButton(
                                    onClick = onClearAttendance,
                                    modifier = Modifier.testTag("clear_btn_${activity.classId}")
                                ) {
                                    Text("Retract Marking", color = mutedText, fontSize = 12.sp)
                                }
                            } else {
                                // Absent button
                                Button(
                                    onClick = onMarkAbsent,
                                    colors = ButtonDefaults.buttonColors(containerColor = coralRed),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("absent_btn_${activity.classId}")
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Absent", fontSize = 12.sp)
                                }

                                // Present/Attended button
                                Button(
                                    onClick = onMarkAttended,
                                    colors = ButtonDefaults.buttonColors(containerColor = neonTeal),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("attended_btn_${activity.classId}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Present", fontSize = 12.sp)
                                }
                            }
                        }
                    } else if (activity.isHoliday) {
                        Text(
                            text = "Exempt (Holiday)",
                            color = mutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                        )
                    } else {
                        Text(
                            text = "Exempt (Suspension)",
                            color = softGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper functions for localized formats
fun formatDateFull(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = inputFormat.parse(dateStr) ?: return dateStr
        val outputFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)
        outputFormat.format(date)
    } catch (e: Exception) {
        dateStr
    }
}

fun getDayOfWeekName(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = inputFormat.parse(dateStr) ?: return ""
        val outputFormat = SimpleDateFormat("EEEE", Locale.US)
        outputFormat.format(date)
    } catch (e: Exception) {
        ""
    }
}

fun getDayNameFromInt(day: Int): String {
    return when (day) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Monday"
    }
}

fun uriToBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        android.util.Log.e("uriToBitmap", "Failed conversion", e)
        null
    }
}

fun uriToBitmaps(context: android.content.Context, uri: Uri): List<Bitmap> {
    val mimeType = context.contentResolver.getType(uri) ?: ""
    android.util.Log.d("uriToBitmaps", "Selected URI: $uri MIME Type: $mimeType")
    
    if (mimeType == "application/pdf" || uri.path?.lowercase(Locale.US)?.endsWith(".pdf") == true) {
        return renderPdfToBitmaps(context, uri)
    }
    
    val single = uriToBitmap(context, uri)
    return if (single != null) listOf(single) else emptyList()
}

fun renderPdfToBitmaps(context: android.content.Context, uri: Uri): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        if (fileDescriptor != null) {
            val renderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount.coerceAtMost(3) // limit to top 3 pages for safe processing
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = (page.width * 2.5).toInt()
                val height = (page.height * 2.5).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
            fileDescriptor.close()
        }
    } catch (e: Exception) {
        android.util.Log.e("renderPdfToBitmaps", "Error converting PDF pages: ", e)
    }
    return bitmaps
}

@Composable
fun CalendarMonthView(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    allRecords: List<com.example.data.AttendanceRecord>,
    allSuspensions: List<com.example.data.SuspendedClass>,
    allHolidays: List<com.example.data.Holiday>,
    cardBg: Color,
    lightText: Color,
    mutedText: Color,
    neonTeal: Color,
    coralRed: Color,
    softGold: Color
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val curDate = try { sdf.parse(selectedDate) } catch (e: Exception) { Date() } ?: Date()
    
    // Remember current viewed month/year in calendar view
    var calendarInstance by remember(selectedDate) {
        val cal = Calendar.getInstance()
        cal.time = curDate
        mutableStateOf(cal)
    }
    
    val viewedYear = calendarInstance.get(Calendar.YEAR)
    val viewedMonth = calendarInstance.get(Calendar.MONTH) // 0-indexed
    
    // Format month and year display
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    val monthYearText = monthYearFormat.format(calendarInstance.time)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFF0F172A).copy(0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val cal = Calendar.getInstance()
                cal.time = calendarInstance.time
                cal.add(Calendar.MONTH, -1)
                calendarInstance = cal
            }) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Prev Month", tint = lightText, modifier = Modifier.size(16.dp))
            }
            
            Text(
                text = monthYearText,
                color = lightText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            
            IconButton(onClick = {
                val cal = Calendar.getInstance()
                cal.time = calendarInstance.time
                cal.add(Calendar.MONTH, 1)
                calendarInstance = cal
            }) {
                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Month", tint = lightText, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Days of week header
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    color = mutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Month days grid
        val daysInMonthList = remember(viewedYear, viewedMonth) {
            getDaysInMonth(viewedYear, viewedMonth)
        }
        
        val rows = daysInMonthList.chunked(7)
        rows.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                week.forEach { dateItem ->
                    if (dateItem == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val cellDateStr = sdf.format(dateItem)
                        val isSelected = cellDateStr == selectedDate
                        val cal = Calendar.getInstance().apply { time = dateItem }
                        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                        
                        // Check if any attendance recorded, suspended, or holiday on this date
                        val dayRecords = allRecords.filter { it.date == cellDateStr }
                        val hasAttended = dayRecords.any { it.status == "ATTENDED" }
                        val hasAbsent = dayRecords.any { it.status == "ABSENT" }
                        val isDaySuspended = allSuspensions.any { it.date == cellDateStr }
                        val isDayHoliday = allHolidays.any { it.date == cellDateStr }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) neonTeal else if (isDayHoliday) Color(0xFF1E293B) else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else if (isDayHoliday) mutedText.copy(0.3f) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onDateSelected(cellDateStr)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    color = if (isSelected) Color(0xFF0F172A) else if (isDayHoliday) mutedText else lightText,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                
                                // Indicators row
                                if (!isSelected && (hasAttended || hasAbsent || isDaySuspended || isDayHoliday)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 1.dp)
                                    ) {
                                        if (isDayHoliday) {
                                            Box(modifier = Modifier.size(4.dp).background(mutedText, CircleShape))
                                        } else {
                                            if (isDaySuspended) {
                                                Box(modifier = Modifier.size(4.dp).background(softGold, CircleShape))
                                            }
                                            if (hasAttended) {
                                                Box(modifier = Modifier.size(4.dp).background(neonTeal, CircleShape))
                                            }
                                            if (hasAbsent) {
                                                Box(modifier = Modifier.size(4.dp).background(coralRed, CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Pad last incomplete week
                if (week.size < 7) {
                    for (k in 0 until (7 - week.size)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

fun getDaysInMonth(year: Int, month: Int): List<Date?> {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val days = mutableListOf<Date?>()
    
    // Convert firstDayOfWeek (Sunday=1, Monday=2, ..., Saturday=7)
    // to leading blanks such that Monday is the first column.
    // If firstDayOfWeek is Sunday(1) -> 6 blank cells
    // If Monday(2) -> 0 blank cells
    // If Tuesday(3) -> 1 blank cell, etc.
    val leadingBlanks = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
    
    for (i in 0 until leadingBlanks) {
        days.add(null)
    }
    
    for (day in 1..maxDay) {
        val dateCal = Calendar.getInstance()
        dateCal.set(Calendar.YEAR, year)
        dateCal.set(Calendar.MONTH, month)
        dateCal.set(Calendar.DAY_OF_MONTH, day)
        days.add(dateCal.time)
    }
    
    return days
}
