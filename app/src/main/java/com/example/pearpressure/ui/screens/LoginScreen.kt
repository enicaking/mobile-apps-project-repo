/* LoginScreen.kt
Loads the sign in and sign up option */
package com.example.pearpressure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.pearpressure.R
import androidx.compose.ui.res.stringResource

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterNeedsProfile: () -> Unit,
    viewModel: com.example.pearpressure.MainViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = if (isRegistering) stringResource(R.string.login_title_create_account)
            else stringResource(R.string.login_title_welcome_back),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.login_label_email)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_label_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (isRegistering) {
                    viewModel.signUp(email, password, onRegisterNeedsProfile)
                } else {
                    viewModel.signIn(email, password, onLoginSuccess)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isRegistering) stringResource(R.string.login_button_sign_up)
                else stringResource(R.string.login_button_login)
            )
        }

        TextButton(onClick = { isRegistering = !isRegistering }) {
            Text(
                if (isRegistering) stringResource(R.string.login_toggle_have_account)
                else stringResource(R.string.login_toggle_new_here)
            )
        }
    }
}