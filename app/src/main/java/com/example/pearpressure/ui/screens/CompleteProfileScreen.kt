package com.example.pearpressure.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.pearpressure.R
import androidx.compose.ui.res.stringResource

@Composable
fun CompleteProfileScreen(
    viewModel: com.example.pearpressure.MainViewModel,
    onProfileCompleted: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf("") }
    var birthdayEpochMs by remember { mutableStateOf(0L) }

    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    fun formattedBirthday(): String {
        if (birthdayEpochMs <= 0L) return ""
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(birthdayEpochMs))
    }

    val sexOptions = listOf(
        stringResource(R.string.profile_sex_male),
        stringResource(R.string.profile_sex_female),
        stringResource(R.string.profile_sex_other),
        stringResource(R.string.profile_sex_prefer_not_to_say)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.profile_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(stringResource(R.string.profile_label_full_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it.replace(" ", "").lowercase()
            },
            label = { Text(stringResource(R.string.profile_label_username)) },
            supportingText = { Text(stringResource(R.string.profile_username_supporting_text)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.profile_label_sex),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sexOptions.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedSex == option,
                        onClick = { selectedSex = option }
                    )
                    Text(text = option)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = formattedBirthday(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.profile_label_birthday)) },
            placeholder = { Text(stringResource(R.string.profile_birthday_placeholder)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val today = Calendar.getInstance()

                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val pickedCalendar = Calendar.getInstance()
                        pickedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                        pickedCalendar.set(Calendar.MILLISECOND, 0)
                        birthdayEpochMs = pickedCalendar.timeInMillis
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.maxDate = today.timeInMillis
                }.show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.profile_button_choose_birthday))
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.completeUserProfile(
                    fullName = fullName,
                    username = username,
                    sex = selectedSex,
                    birthdayEpochMs = birthdayEpochMs,
                    onSuccess = onProfileCompleted
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.profile_button_save))
        }
    }
}