package com.example.pearpressure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.data.UserProfile

@Composable
fun FriendsScreen() {
    val vm: MainViewModel = viewModel()

    val buddies by vm.friends.collectAsState()
    val searchError by vm.friendSearchError.collectAsState()
    val searchResult by vm.friendSearchResult.collectAsState()

    var email by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Friends",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            // Search (by email)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Search by email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { vm.searchUserByEmail(email) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Search")
            }

            if (searchError != null) {
                Text(
                    text = searchError!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (searchResult != null) {
                UserCard(
                    title = "Found user",
                    user = searchResult!!,
                    trailing = {
                        // Template button (later: send friend request / invite to subject)
                        OutlinedButton(onClick = { /* TODO later */ }) {
                            Text("Add")
                        }
                    }
                )
            }

            Divider()

            Text("Study buddies (from shared subjects)", fontWeight = FontWeight.SemiBold)

            if (buddies.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("No buddies yet", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "They appear when you share subjects (owner/members).",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(buddies) { u ->
                        UserCard(title = null, user = u, trailing = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    title: String?,
    user: UserProfile,
    trailing: (@Composable (() -> Unit))?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (!title.isNullOrBlank()) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (user.name.isNotBlank()) user.name else "No name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (trailing != null) trailing()
            }
        }
    }
}