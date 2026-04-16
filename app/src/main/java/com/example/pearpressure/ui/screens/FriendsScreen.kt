package com.example.pearpressure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pearpressure.MainViewModel
import com.example.pearpressure.data.UserProfile
import com.example.pearpressure.IncomingFriendRequestUi
import com.example.pearpressure.OutgoingFriendRequestUi

@Composable
fun FriendsScreen(viewModel: MainViewModel = viewModel()) {

    val friends by viewModel.friends.collectAsState()
    val incoming by viewModel.incomingRequests.collectAsState()
    val outgoing by viewModel.outgoingRequests.collectAsState()

    val searchError by viewModel.friendSearchError.collectAsState()
    val searchResult by viewModel.friendSearchResult.collectAsState()

    var email by remember { mutableStateOf("") }
    val myUid = viewModel.getCurrentUserId()
    val friendUids = remember(friends) { friends.map { it.uid }.toSet() }

    // UI State for Removal Confirmation
    var userToRemove by remember { mutableStateOf<UserProfile?>(null) }
    // UI State for Cancel Confirmation
    var requestToCancel by remember { mutableStateOf<OutgoingFriendRequestUi?>(null) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Friends", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            // Search Section - Button now on the right to save vertical space
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Search email") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.searchUserByEmail(email) },
                            modifier = Modifier.height(56.dp), // Match TextField height
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Search")
                        }
                    }

                    if (searchError != null) Text(searchError!!, color = MaterialTheme.colorScheme.error)

                    searchResult?.let { u ->
                        val canAdd = u.uid.isNotBlank() && u.uid != myUid && !friendUids.contains(u.uid)
                        UserCard(
                            title = "Search result",
                            user = u,
                            trailing = {
                                OutlinedButton(
                                    onClick = { viewModel.sendFriendRequest(u.uid) },
                                    enabled = canAdd
                                ) { Text(if (u.uid == myUid) "You" else if (!canAdd) "Added" else "Add") }
                            }
                        )
                    }
                }
            }

            // --- INCOMING REQUESTS ---
            if (incoming.isNotEmpty()) {
                item { SectionTitle("Incoming requests", color = Color(0xFF4DB6AC)) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(incoming) { item ->
                            Card(modifier = Modifier.width(190.dp)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.from.fullName.ifBlank { "No name" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(item.from.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                        Button(onClick = { viewModel.acceptRequest(item.request) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Accept", fontSize = 12.sp) }
                                        OutlinedButton(onClick = { viewModel.declineRequest(item.request) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Decline", fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- OUTGOING REQUESTS --- (Width tightened so Cancel isn't too far)
            if (outgoing.isNotEmpty()) {
                item { SectionTitle("Outgoing requests", color = Color(0xFF4DB6AC)) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(outgoing) { item ->
                            Card(
                                modifier = Modifier.width(200.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Pending to:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF4DB6AC),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(item.to.fullName.ifBlank { "No name" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(item.to.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }

                                    TextButton(
                                        onClick = { requestToCancel = item },
                                        contentPadding = PaddingValues(start = 4.dp)
                                    ) {
                                        Text("Cancel", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // Friends list
            item { SectionTitle("Your friends", color = MaterialTheme.colorScheme.primary) }
            if (friends.isEmpty()) {
                item { EmptyHint("No friends yet.") }
            } else {
                items(friends) { u ->
                    UserCard(
                        title = null,
                        user = u,
                        trailing = {
                            TextButton(onClick = { userToRemove = u }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---
    userToRemove?.let { user ->
        AlertDialog(
            onDismissRequest = { userToRemove = null },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove ${user.fullName.ifBlank { user.email }}?") },
            confirmButton = {
                Button(onClick = { viewModel.removeFriend(user.uid); userToRemove = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { userToRemove = null }) { Text("Cancel") } }
        )
    }

    requestToCancel?.let { item ->
        AlertDialog(
            onDismissRequest = { requestToCancel = null },
            title = { Text("Cancel Request") },
            text = { Text("Do you want to cancel the request to ${item.to.fullName}?") },
            confirmButton = {
                Button(onClick = { viewModel.declineRequest(item.request); requestToCancel = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { requestToCancel = null }) { Text("Back") } }
        )
    }
}

@Composable private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable private fun EmptyHint(text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Text(text, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun UserCard(title: String?, user: UserProfile, trailing: (@Composable (() -> Unit))?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (title != null) Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(user.fullName.ifBlank { "No name" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) trailing()
        }
    }
}