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
import com.example.pearpressure.data.FriendRequest
import com.example.pearpressure.data.UserProfile
import com.example.pearpressure.IncomingFriendRequestUi
import com.example.pearpressure.OutgoingFriendRequestUi

@Composable
fun FriendsScreen() {
    val vm: MainViewModel = viewModel()

    val friends by vm.friends.collectAsState()
    val incoming by vm.incomingRequests.collectAsState()
    val outgoing by vm.outgoingRequests.collectAsState()
    val buddies by vm.studyBuddies.collectAsState()

    val searchError by vm.friendSearchError.collectAsState()
    val searchResult by vm.friendSearchResult.collectAsState()

    var email by remember { mutableStateOf("") }
    val myUid = vm.getCurrentUserId()
    val friendUids = remember(friends) { friends.map { it.uid }.toSet() }

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Friends", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

            // Search by email
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
            ) { Text("Search") }

            if (searchError != null) Text(searchError!!, color = MaterialTheme.colorScheme.error)

            if (searchResult != null) {
                val u = searchResult!!
                val canAdd = u.uid.isNotBlank() && u.uid != myUid && !friendUids.contains(u.uid)

                UserCard(
                    title = "Search result",
                    user = u,
                    trailing = {
                        OutlinedButton(
                            onClick = { vm.sendFriendRequest(u.uid) },
                            enabled = canAdd
                        ) { Text(if (u.uid == myUid) "This is you" else if (!canAdd) "Added" else "Add") }
                    }
                )
            }

            Divider()

            // Incoming requests
            SectionTitle("Incoming requests")
            if (incoming.isEmpty()) {
                EmptyHint("No incoming requests.")
            } else {
                incoming.forEach { item ->
                    RequestCardIncoming(
                        item = item,
                        onAccept = { vm.acceptRequest(item.request) },
                        onDecline = { vm.declineRequest(item.request) }
                    )
                }
            }

            // Outgoing requests
            SectionTitle("Outgoing requests")
            if (outgoing.isEmpty()) {
                EmptyHint("No outgoing requests.")
            } else {
                outgoing.forEach { item ->
                    RequestCardOutgoing(item = item)
                }
            }

            Divider()

            // Friends list
            SectionTitle("Your friends")
            if (friends.isEmpty()) {
                EmptyHint("No friends yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(friends) { u ->
                        UserCard(
                            title = null,
                            user = u,
                            trailing = {
                                TextButton(onClick = { vm.removeFriend(u.uid) }) { Text("Remove") }
                            }
                        )
                    }
                }
            }

            Divider()

            // Study buddies
            SectionTitle("Study buddies (from shared subjects)")
            if (buddies.isEmpty()) {
                EmptyHint("They appear when you share subjects (owner/members).")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
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

@Composable private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold)
}

@Composable private fun EmptyHint(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RequestCardIncoming(
    item: IncomingFriendRequestUi,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("From:", fontWeight = FontWeight.SemiBold)
            Text(item.from.name.ifBlank { "No name" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.from.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accept") }
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) { Text("Decline") }
            }
        }
    }
}

@Composable
private fun RequestCardOutgoing(item: OutgoingFriendRequestUi) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pending to:", fontWeight = FontWeight.SemiBold)
            Text(item.to.name.ifBlank { "No name" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.to.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        text = user.name.ifBlank { "No name" },
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