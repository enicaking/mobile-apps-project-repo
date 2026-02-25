package com.example.sofiatest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sofiatest.ui.SofiaTestApp
import com.example.sofiatest.ui.theme.SofiaTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SofiaTestTheme {
                SofiaTestApp()
            }
        }
    }
}