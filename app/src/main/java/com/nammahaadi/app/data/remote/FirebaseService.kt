package com.nammahaadi.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nammahaadi.app.data.model.PathModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// This class ONLY talks to Firebase directly.
// It does NOT contain business logic — that goes in Repository.

class FirebaseService {

    private val db: FirebaseFirestore = Firebase.firestore
    private val pathsCollection = db.collection("paths")
    private val contributorsCollection = db.collection("contributors")

    // ── PATHS ─────────────────────────────────────────

    // Listen to all paths in real-time
    fun observePaths(): Flow<List<PathModel>> = callbackFlow {
        val listener = pathsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val paths = snapshot?.documents?.mapNotNull {
                    it.toObject(PathModel::class.java)
                } ?: emptyList()
                trySend(paths)
            }
        awaitClose { listener.remove() }
    }

    // Add a new path document to Firestore
    suspend fun addPath(data: Map<String, Any>): String {
        val docRef = pathsCollection.add(data).await()
        return docRef.id
    }

    // Update an existing path's fields
    suspend fun updatePath(pathId: String, data: Map<String, Any>) {
        pathsCollection.document(pathId).update(data).await()
    }

    // Delete a path
    suspend fun deletePath(pathId: String) {
        pathsCollection.document(pathId).delete().await()
    }

    // ── CONTRIBUTORS / LEADERBOARD ────────────────────

    // Listen to leaderboard in real-time (top 20)
    fun observeLeaderboard(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = contributorsCollection
            .orderBy("totalPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply { put("userId", doc.id) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Increment a contributor's score field by 1
    suspend fun incrementScore(userId: String, field: String) {
        if (userId.isBlank()) return
        val docRef = contributorsCollection.document(userId)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            // Document exists — just increment
            docRef.update(
                field,
                com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
            // Also update totalPoints
            docRef.update(
                "totalPoints",
                com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
        } else {
            // First time this user contributes — create the document
            docRef.set(
                mapOf(
                    "userId"       to userId,
                    "name"         to "User_${userId.take(5)}",
                    "pathsAdded"   to if (field == "pathsAdded") 1 else 0,
                    "reportsCount" to if (field == "reportsCount") 1 else 0,
                    "totalPoints"  to 1
                )
            ).await()
        }
    }
}