package com.example.data

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val timetableClassDao: TimetableClassDao,
    private val attendanceRecordDao: AttendanceRecordDao,
    private val suspendedClassDao: SuspendedClassDao,
    private val holidayDao: HolidayDao,
    private val appSettingDao: AppSettingDao
) {
    val allClasses: Flow<List<TimetableClass>> = timetableClassDao.getAllClasses()
    val allRecords: Flow<List<AttendanceRecord>> = attendanceRecordDao.getAllRecords()
    val allSuspensions: Flow<List<SuspendedClass>> = suspendedClassDao.getAllSuspensions()
    val allHolidays: Flow<List<Holiday>> = holidayDao.getAllHolidays()

    suspend fun insertClass(classItem: TimetableClass) = timetableClassDao.insertClass(classItem)
    suspend fun insertClasses(classes: List<TimetableClass>) = timetableClassDao.insertClasses(classes)
    suspend fun deleteClass(id: Int) = timetableClassDao.deleteClass(id)
    suspend fun clearAllClasses() = timetableClassDao.clearAllClasses()

    suspend fun insertRecord(record: AttendanceRecord) = attendanceRecordDao.insertRecord(record)
    suspend fun insertRecords(records: List<AttendanceRecord>) = attendanceRecordDao.insertRecords(records)
    suspend fun deleteRecord(date: String, className: String) = attendanceRecordDao.deleteRecord(date, className)
    suspend fun getRecordsForDate(date: String) = attendanceRecordDao.getRecordsForDate(date)
    fun getRecordsForDateFlow(date: String) = attendanceRecordDao.getRecordsForDateFlow(date)
    suspend fun deleteRecordsOlderThan(date: String) = attendanceRecordDao.deleteRecordsOlderThan(date)
    suspend fun deleteRecordsByMonth(yearMonthPattern: String) = attendanceRecordDao.deleteRecordsByMonth(yearMonthPattern)

    suspend fun insertSuspension(suspended: SuspendedClass) = suspendedClassDao.insertSuspension(suspended)
    suspend fun deleteSuspension(id: Int) = suspendedClassDao.deleteSuspension(id)
    suspend fun deleteSuspensionByDetails(className: String, date: String) = suspendedClassDao.deleteSuspensionByDetails(className, date)
    suspend fun deleteSuspensionsOlderThan(date: String) = suspendedClassDao.deleteSuspensionsOlderThan(date)
    suspend fun deleteSuspensionsByMonth(yearMonthPattern: String) = suspendedClassDao.deleteSuspensionsByMonth(yearMonthPattern)

    suspend fun insertHolidays(holidays: List<Holiday>) = holidayDao.insertHolidays(holidays)
    suspend fun insertHoliday(holiday: Holiday) = holidayDao.insertHoliday(holiday)
    suspend fun deleteHoliday(id: Int) = holidayDao.deleteHoliday(id)
    suspend fun clearAllHolidays() = holidayDao.clearAllHolidays()

    suspend fun getSetting(key: String): String? = appSettingDao.getSetting(key)?.value
    fun getSettingFlow(key: String): Flow<AppSetting?> = appSettingDao.getSettingFlow(key)
    suspend fun saveSetting(key: String, value: String) = appSettingDao.insertSetting(AppSetting(key, value))
}
