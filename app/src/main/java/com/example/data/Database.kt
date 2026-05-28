package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "timetable_classes")
data class TimetableClass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val className: String,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val timeSlot: String // e.g. "09:00 - 10:00"
)

@Entity(tableName = "attendance_records", indices = [Index(value = ["date", "className"], unique = true)])
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val className: String,
    val status: String // "ATTENDED", "ABSENT"
)

@Entity(tableName = "suspended_classes", indices = [Index(value = ["date", "className"], unique = true)])
data class SuspendedClass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val className: String,
    val date: String // YYYY-MM-DD
)

@Entity(tableName = "holidays", indices = [Index(value = ["date"], unique = true)])
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val holidayName: String
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

// --- DAOs ---

@Dao
interface TimetableClassDao {
    @Query("SELECT * FROM timetable_classes ORDER BY dayOfWeek, timeSlot")
    fun getAllClasses(): Flow<List<TimetableClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classItem: TimetableClass): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<TimetableClass>)

    @Query("DELETE FROM timetable_classes WHERE id = :id")
    suspend fun deleteClass(id: Int)

    @Query("DELETE FROM timetable_classes")
    suspend fun clearAllClasses()
}

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecord>)

    @Query("DELETE FROM attendance_records WHERE date = :date AND className = :className")
    suspend fun deleteRecord(date: String, className: String)

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun getRecordsForDate(date: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getRecordsForDateFlow(date: String): Flow<List<AttendanceRecord>>

    @Query("DELETE FROM attendance_records WHERE date < :date")
    suspend fun deleteRecordsOlderThan(date: String)

    @Query("DELETE FROM attendance_records WHERE date LIKE :yearMonthPattern")
    suspend fun deleteRecordsByMonth(yearMonthPattern: String)
}

@Dao
interface SuspendedClassDao {
    @Query("SELECT * FROM suspended_classes ORDER BY date DESC")
    fun getAllSuspensions(): Flow<List<SuspendedClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuspension(suspended: SuspendedClass)

    @Query("DELETE FROM suspended_classes WHERE id = :id")
    suspend fun deleteSuspension(id: Int)

    @Query("DELETE FROM suspended_classes WHERE className = :className AND date = :date")
    suspend fun deleteSuspensionByDetails(className: String, date: String)

    @Query("DELETE FROM suspended_classes WHERE date < :date")
    suspend fun deleteSuspensionsOlderThan(date: String)

    @Query("DELETE FROM suspended_classes WHERE date LIKE :yearMonthPattern")
    suspend fun deleteSuspensionsByMonth(yearMonthPattern: String)
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY date DESC")
    fun getAllHolidays(): Flow<List<Holiday>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<Holiday>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: Holiday)

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteHoliday(id: Int)

    @Query("DELETE FROM holidays")
    suspend fun clearAllHolidays()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSetting?

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<AppSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
}

// --- Database ---

@Database(
    entities = [
        TimetableClass::class,
        AttendanceRecord::class,
        SuspendedClass::class,
        Holiday::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timetableClassDao(): TimetableClassDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun suspendedClassDao(): SuspendedClassDao
    abstract fun holidayDao(): HolidayDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_tracker_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
