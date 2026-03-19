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
    //MainViewModel: objeto donde se guardan y gestionan datos como asignaturas y exámenes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // hace que la interfaz pueda ocupar más pantalla, llegando hasta los bordes

        lifecycleScope.launch {
            viewModel.error.collect { err ->
                err?.let { Log.e("Firebase", "Error: $it") }
            }
        } //Esto escucha errores del ViewModel y los manda al log.

        setContent {
            SofiaTestTheme {
                SofiaTestApp(viewModel = viewModel)
            }
        }
    }
}
