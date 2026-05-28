package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.AttendanceViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database, Repository, and ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AttendanceRepository(
            database.timetableClassDao(),
            database.attendanceRecordDao(),
            database.suspendedClassDao(),
            database.holidayDao(),
            database.appSettingDao()
        )
        val factory = AttendanceViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[AttendanceViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
