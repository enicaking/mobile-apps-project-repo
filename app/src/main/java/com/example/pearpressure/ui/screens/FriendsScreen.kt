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
import com.example.pearpressure.OutgoingFriendRequestUi
import com.example.pearpressure.R
import androidx.compose.ui.res.stringResource
import com.example.pearpressure.ui.theme.*


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
                Text(stringResource(R.string.friends_screen_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                            label = { Text(stringResource(R.string.friends_search_label)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.searchUserByEmail(email) },
                            modifier = Modifier.height(56.dp), // Match TextField height
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.search))
                        }
                    }

                    if (searchError != null) Text(searchError!!, color = MaterialTheme.colorScheme.error)
                    searchResult?.let { u ->
                        val canAdd = u.uid.isNotBlank() && u.uid != myUid && !friendUids.contains(u.uid)
                        UserCard(
                            title = stringResource(R.string.friends_search_result_title),
                            user = u,
                            trailing = {
                                OutlinedButton(
                                    onClick = { viewModel.sendFriendRequest(u.uid) },
                                    enabled = canAdd
                                ) {
                                    Text(if (u.uid == myUid) stringResource(R.string.friends_add_button_you) else if (!canAdd) stringResource(R.string.friends_add_button_added) else stringResource(R.string.add)) }

                            }
                        )
                    }
                }
            }

            // INCOMING REQUESTS
            if (incoming.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.friends_section_incoming), color = FriendRequestColor) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(incoming) { item ->
                            Card(modifier = Modifier.width(190.dp)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.from.fullName.ifBlank { stringResource(R.string.friends_no_name) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(item.from.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                        Button(onClick = { viewModel.acceptRequest(item.request) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.accept), fontSize = 12.sp) }
                                        OutlinedButton(onClick = { viewModel.declineRequest(item.request) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.decline), fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // OUTGOING REQUESTS
            if (outgoing.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.friends_section_outgoing), color = FriendRequestColor) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(outgoing) { item ->
                            Card(
                                modifier = Modifier.width(200.dp), //Width tightened so Cancel isn't too far
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.friends_pending_to),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = FriendRequestColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(item.to.fullName.ifBlank { stringResource(R.string.friends_no_name) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(item.to.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }

                                    TextButton(
                                        onClick = { requestToCancel = item },
                                        contentPadding = PaddingValues(start = 4.dp)
                                    ) {
                                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // Friends list
            item { SectionTitle(stringResource(R.string.friends_section_your_friends), color = MaterialTheme.colorScheme.primary) }
            if (friends.isEmpty()) {
                item { EmptyHint(stringResource(R.string.friends_empty_hint)) }
            } else {
                items(friends) { u ->
                    UserCard(
                        title = null,
                        user = u,
                        trailing = {
                            TextButton(onClick = { userToRemove = u }) {
                                Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }

    // DIALOGS
    userToRemove?.let { user ->
        AlertDialog(
            onDismissRequest = { userToRemove = null },
            title = { Text(stringResource(R.string.friends_dialog_remove_title)) },
            text = { Text(stringResource(R.string.friends_dialog_remove_text, user.fullName.ifBlank { user.email })) },
            confirmButton = {
                Button(onClick = { viewModel.removeFriend(user.uid); userToRemove = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.remove))  }
            },
            dismissButton = { TextButton(onClick = { userToRemove = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    requestToCancel?.let { item ->
        AlertDialog(
            onDismissRequest = { requestToCancel = null },
            title = { Text(text = stringResource(R.string.friends_dialog_cancel_title)) },
            text = { Text(text = stringResource(R.string.friends_dialog_cancel_text, item.to.fullName)) },
            confirmButton = {
                Button(onClick = { viewModel.declineRequest(item.request); requestToCancel = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { requestToCancel = null }) { Text(stringResource(R.string.back)) } }
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
                Text(user.fullName.ifBlank { stringResource(R.string.friends_no_name) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) trailing()
        }
    }
}