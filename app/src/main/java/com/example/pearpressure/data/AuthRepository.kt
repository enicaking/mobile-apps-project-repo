/* AuthRepository.kt
This file controls the Firebase Auth logic
Allows users to securely sign in or sign up */
package com.example.pearpressure.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signIn(email: String, pass: String): Result<FirebaseUser?> = runCatching {
        auth.signInWithEmailAndPassword(email, pass).await().user
    }

    suspend fun signUp(email: String, pass: String): Result<FirebaseUser?> = runCatching {
        auth.createUserWithEmailAndPassword(email, pass).await().user
    }

    fun signOut() {
        auth.signOut()
    }
}