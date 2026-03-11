package com.example.pearpressure

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.pearpressure.ui.SofiaTestApp
import com.example.pearpressure.ui.theme.SofiaTestTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            viewModel.error.collect { err ->
                err?.let { Log.e("Firebase", "Error: $it") }
            }
        }

        setContent {
            SofiaTestTheme {
                SofiaTestApp(viewModel = viewModel)
            }
        }
    }
}
