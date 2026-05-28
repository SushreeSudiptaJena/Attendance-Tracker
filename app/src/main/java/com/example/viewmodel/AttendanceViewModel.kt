package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiManager
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class AttendanceViewModel(private val repository: AttendanceRepository) : ViewModel() {

    private val TAG = "AttendanceViewModel"

    init {
        purgeOldLogs()
    }

    private fun purgeOldLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -4)
                val thresholdDateStr = sdf.format(cal.time)
                repository.deleteRecordsOlderThan(thresholdDateStr)
                repository.deleteSuspensionsOlderThan(thresholdDateStr)
                Log.d(TAG, "Purged old attendance records and suspensions older than $thresholdDateStr")
            } catch (e: Exception) {
                Log.e(TAG, "Error performing auto-purge of old logs: ", e)
            }
        }
    }

    fun massClearLogsByMonth(yearMonth: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pattern = "$yearMonth-%"
                repository.deleteRecordsByMonth(pattern)
                repository.deleteSuspensionsByMonth(pattern)
                withContext(Dispatchers.Main) {
                    _operationSuccess.value = "Successfully cleared records and suspensions for $yearMonth."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _apiError.value = "Failed to clear logs: ${e.message}"
                }
            }
        }
    }

    fun massMarkAttendanceRange(startDateStr: String, endDateStr: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val startDate = sdf.parse(startDateStr) ?: return@launch
                val endDate = sdf.parse(endDateStr) ?: return@launch

                if (startDate.after(endDate)) {
                    withContext(Dispatchers.Main) {
                        _apiError.value = "Start date must be before or equal to End date."
                    }
                    return@launch
                }

                val timetableList = allClasses.value
                if (timetableList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _apiError.value = "Active class schedule / timetable is required to mass mark attendance."
                    }
                    return@launch
                }

                val newRecords = mutableListOf<AttendanceRecord>()
                val cal = Calendar.getInstance()
                cal.time = startDate

                while (!cal.time.after(endDate)) {
                    val currentDateStr = sdf.format(cal.time)

                    // Find day of week (Monday = 1, ..., Sunday = 7)
                    val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        Calendar.SUNDAY -> 7
                        else -> 1
                    }

                    // Filter classes that occur on this day of week
                    val classesOnDay = timetableList.filter { it.dayOfWeek == dayOfWeek }

                    classesOnDay.forEach { classItem ->
                        newRecords.add(
                            AttendanceRecord(
                                date = currentDateStr,
                                className = classItem.className,
                                status = status
                            )
                        )
                    }

                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                if (newRecords.isNotEmpty()) {
                    repository.insertRecords(newRecords)
                    withContext(Dispatchers.Main) {
                        _operationSuccess.value = "Successfully marked ${newRecords.size} class periods as $status from $startDateStr to $endDateStr."
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _apiError.value = "No classes found on the active days of the week in this date range."
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _apiError.value = "Error parsing dates: ${e.message}"
                }
            }
        }
    }

    // Key states
    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _isParsingTimetable = MutableStateFlow(false)
    val isParsingTimetable: StateFlow<Boolean> = _isParsingTimetable.asStateFlow()

    private val _isParsingHolidays = MutableStateFlow(false)
    val isParsingHolidays: StateFlow<Boolean> = _isParsingHolidays.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess.asStateFlow()

    // Observable sources from repository
    val allClasses = repository.allClasses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRecords = repository.allRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSuspensions = repository.allSuspensions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allHolidays = repository.allHolidays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App configurations
    val goalPercentage = repository.getSettingFlow("goal_percentage")
        .map { it?.value?.toFloatOrNull() ?: 75f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75f)

    val timetableUploaded = repository.getSettingFlow("timetable_uploaded")
        .map { it?.value == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val holidaysUploaded = repository.getSettingFlow("holidays_uploaded")
        .map { it?.value == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Derived states
    // Combined list of active items for the currently selected date
    val dayActivities: StateFlow<List<DayActivity>> = combine(
        _selectedDate,
        allClasses,
        allRecords,
        allSuspensions,
        allHolidays
    ) { date, classes, records, suspensions, holidays ->
        val dayOfWeek = getDayOfWeek(date)
        val isHoliday = holidays.any { it.date == date }
        val holidayInfo = holidays.firstOrNull { it.date == date }

        // Filter the timetable classes scheduled for this day of the week
        val classesToday = classes.filter { it.dayOfWeek == dayOfWeek }

        classesToday.map { classItem ->
            val isSuspended = suspensions.any { it.className.lowercase() == classItem.className.lowercase() && it.date == date }
            val record = records.firstOrNull { it.date == date && it.className.lowercase() == classItem.className.lowercase() }

            DayActivity(
                classId = classItem.id,
                className = classItem.className,
                timeSlot = classItem.timeSlot,
                dayOfWeek = dayOfWeek,
                isSuspended = isSuspended,
                isHoliday = isHoliday,
                holidayName = holidayInfo?.holidayName,
                attendanceStatus = record?.status
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Detailed analytics summary to help achieve 75% target
    val stats: StateFlow<AttendanceStats> = combine(
        allRecords,
        goalPercentage
    ) { records, targetGoal ->
        val activeRecords = records.filter { it.status == "ATTENDED" || it.status == "ABSENT" }
        val totalCount = activeRecords.size
        val attendedCount = activeRecords.count { it.status == "ATTENDED" }
        val percent = if (totalCount > 0) (attendedCount.toFloat() / totalCount) * 100f else 0f

        // Calculate consecutive attendance required or skips allowed
        var consecutiveNeeded = 0
        var skipsAllowed = 0

        if (totalCount == 0) {
            consecutiveNeeded = 0
            skipsAllowed = 0
        } else {
            val targetFraction = targetGoal / 100f
            // Attended / Total >= Target
            // Attended >= Target * Total
            if (percent < targetGoal) {
                // If we attend N more consecutive classes:
                // (Attended + N) / (Total + N) >= Target
                // Attended + N >= Target * Total + Target * N
                // N * (1 - Target) >= Target * Total - Attended
                // N >= (Target * Total - Attended) / (1 - Target)
                val num = (targetFraction * totalCount) - attendedCount
                val den = 1f - targetFraction
                consecutiveNeeded = if (den > 0) kotlin.math.ceil(num / den).toInt().coerceAtLeast(1) else 0
            } else {
                // If we skip S classes (marked as absent):
                // Attended / (Total + S) >= Target
                // Attended >= Target * Total + Target * S
                // S * Target <= Attended - Target * Total
                // S <= (Attended - Target * Total) / Target
                val num = attendedCount - (targetFraction * totalCount)
                skipsAllowed = if (targetFraction > 0) kotlin.math.floor(num / targetFraction).toInt().coerceAtLeast(0) else 0
            }
        }

        AttendanceStats(
            totalExpectedClasses = totalCount,
            totalAttendedClasses = attendedCount,
            percentage = percent,
            consecutiveToAttend = consecutiveNeeded,
            skipsAllowed = skipsAllowed,
            targetGoal = targetGoal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceStats())

    // Utility date setters
    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setPreviousDay() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val date = sdf.parse(_selectedDate.value) ?: return
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, -1)
            _selectedDate.value = sdf.format(cal.time)
        } catch (e: Exception) {
            Log.e(TAG, "Error switching previous date: ", e)
        }
    }

    fun setNextDay() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val date = sdf.parse(_selectedDate.value) ?: return
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, 1)
            _selectedDate.value = sdf.format(cal.time)
        } catch (e: Exception) {
            Log.e(TAG, "Error switching next date: ", e)
        }
    }

    fun clearApiError() {
        _apiError.value = null
    }

    fun clearOperationSuccess() {
        _operationSuccess.value = null
    }

    // Goal change
    fun updateGoalPercentage(value: Float) {
        viewModelScope.launch {
            repository.saveSetting("goal_percentage", value.toString())
        }
    }

    // Action handlers
    fun markAttendance(date: String, className: String, status: String) {
        viewModelScope.launch {
            repository.insertRecord(
                AttendanceRecord(
                    date = date,
                    className = className,
                    status = status
                )
            )
        }
    }

    fun clearAttendance(date: String, className: String) {
        viewModelScope.launch {
            repository.deleteRecord(date, className)
        }
    }

    fun toggleSuspension(className: String, date: String, suspend: Boolean) {
        viewModelScope.launch {
            if (suspend) {
                // If marking as suspended, insert the rule
                repository.insertSuspension(
                    SuspendedClass(
                        className = className,
                        date = date
                    )
                )
                // If attendance was already marked on a suspended day, clear or ignore it.
                // We clear it so it doesn't affect calculations.
                repository.deleteRecord(date, className)
            } else {
                repository.deleteSuspensionByDetails(className, date)
            }
        }
    }

    fun addManualSuspension(className: String, date: String) {
        toggleSuspension(className, date, true)
    }

    fun deleteSuspension(id: Int) {
        viewModelScope.launch {
            repository.deleteSuspension(id)
        }
    }

    fun addManualHoliday(date: String, name: String) {
        viewModelScope.launch {
            repository.insertHoliday(Holiday(date = date, holidayName = name))
            _operationSuccess.value = "Holiday '$name' added manually for $date."
        }
    }

    fun deleteHoliday(id: Int) {
        viewModelScope.launch {
            repository.deleteHoliday(id)
        }
    }

    fun addManualClass(className: String, dayOfWeek: Int, timeSlot: String) {
        viewModelScope.launch {
            repository.insertClass(
                TimetableClass(
                    className = className,
                    dayOfWeek = dayOfWeek,
                    timeSlot = timeSlot
                )
            )
            _operationSuccess.value = "Class '$className' added."
        }
    }

    fun deleteClass(id: Int) {
        viewModelScope.launch {
            repository.deleteClass(id)
        }
    }

    fun resetTimetable() {
        viewModelScope.launch {
            repository.clearAllClasses()
            repository.saveSetting("timetable_uploaded", "false")
            _operationSuccess.value = "Timetable cleared successfully."
        }
    }

    fun resetHolidays() {
        viewModelScope.launch {
            repository.clearAllHolidays()
            repository.saveSetting("holidays_uploaded", "false")
            _operationSuccess.value = "Holidays list cleared successfully."
        }
    }

    // Upload & Parse using Gemini SDK okhttp client wrapper for list of pages / images (PDF friendly)
    fun uploadTimetableImages(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        _isParsingTimetable.value = true
        _apiError.value = null
        viewModelScope.launch {
            val combinedList = mutableListOf<TimetableClass>()
            var anySuccess = false
            var lastErrorResult: String? = null
            
            for (bitmap in bitmaps) {
                val result = GeminiManager.parseTimetable(bitmap)
                if (result != null) {
                    try {
                        val arr = JSONArray(result)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val className = obj.getString("className")
                            val dayOfWeek = obj.getInt("dayOfWeek")
                            val timeSlot = obj.getString("timeSlot")
                            combinedList.add(
                                TimetableClass(
                                    className = className,
                                    dayOfWeek = dayOfWeek,
                                    timeSlot = timeSlot
                                )
                            )
                        }
                        anySuccess = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing timetable response block: ", e)
                        lastErrorResult = result
                    }
                }
            }
            
            if (anySuccess) {
                if (combinedList.isNotEmpty()) {
                    repository.clearAllClasses()
                    repository.insertClasses(combinedList)
                    repository.saveSetting("timetable_uploaded", "true")
                    _operationSuccess.value = "Successfully extracted ${combinedList.size} classes from uploaded file!"
                } else {
                    _apiError.value = "No classes were found. Please check if the file matches the standard format."
                }
            } else {
                _apiError.value = "Gemini API failed to parse. Please ensure your API key in the secrets panel is valid." +
                        if (lastErrorResult != null) " Details: $lastErrorResult" else ""
            }
            _isParsingTimetable.value = false
        }
    }

    fun uploadTimetableImage(bitmap: Bitmap) {
        uploadTimetableImages(listOf(bitmap))
    }

    fun uploadHolidaySheetImages(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        _isParsingHolidays.value = true
        _apiError.value = null
        viewModelScope.launch {
            val combinedList = mutableListOf<Holiday>()
            var anySuccess = false
            var lastErrorResult: String? = null
            
            for (bitmap in bitmaps) {
                val result = GeminiManager.parseHolidaySheet(bitmap)
                if (result != null) {
                    try {
                        val arr = JSONArray(result)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val date = obj.getString("date")
                            val holidayName = obj.getString("holidayName")
                            combinedList.add(
                                Holiday(
                                    date = date,
                                    holidayName = holidayName
                                )
                            )
                        }
                        anySuccess = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing holidays response block: ", e)
                        lastErrorResult = result
                    }
                }
            }
            
            if (anySuccess) {
                if (combinedList.isNotEmpty()) {
                    repository.insertHolidays(combinedList)
                    repository.saveSetting("holidays_uploaded", "true")
                    _operationSuccess.value = "Successfully extracted ${combinedList.size} holidays from uploaded file!"
                } else {
                    _apiError.value = "No holidays found in the document."
                }
            } else {
                _apiError.value = "Gemini API failed to parse holiday sheet. Please ensure your API key is valid." +
                        if (lastErrorResult != null) " Details: $lastErrorResult" else ""
            }
            _isParsingHolidays.value = false
        }
    }

    fun uploadHolidaySheetImage(bitmap: Bitmap) {
        uploadHolidaySheetImages(listOf(bitmap))
    }

    // Date computation helpers
    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun getDayOfWeek(dateStr: String): Int {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return 1
            val cal = Calendar.getInstance()
            cal.time = date
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        } catch (e: Exception) {
            return 1
        }
    }
}

// Data holder classes
data class DayActivity(
    val classId: Int,
    val className: String,
    val timeSlot: String,
    val dayOfWeek: Int,
    val isSuspended: Boolean,
    val isHoliday: Boolean,
    val holidayName: String?,
    val attendanceStatus: String? // "ATTENDED", "ABSENT", or null
)

data class AttendanceStats(
    val totalExpectedClasses: Int = 0,
    val totalAttendedClasses: Int = 0,
    val percentage: Float = 0f,
    val consecutiveToAttend: Int = 0,
    val skipsAllowed: Int = 0,
    val targetGoal: Float = 75f
)

// ViewModel factory implementation
class AttendanceViewModelFactory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
