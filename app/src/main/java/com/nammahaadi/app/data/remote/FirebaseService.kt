package com.nammahaadi.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nammahaadi.app.data.model.PathModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseService {

    private val db                     = Firebase.firestore
    private val pathsCollection        = db.collection("paths")
    private val contributorsCollection = db.collection("contributors")

    // ── PATHS ──────────────────────────────────────────────────────────────

    fun observePaths(): Flow<List<PathModel>> = callbackFlow {
        val listener = pathsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val paths = snapshot?.documents
                ?.mapNotNull { it.toObject(PathModel::class.java) }
                ?: emptyList()
            trySend(paths)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addPath(data: Map<String, Any>): String {
        val docRef = pathsCollection.add(data).await()
        return docRef.id
    }

    suspend fun updatePath(pathId: String, data: Map<String, Any>) {
        pathsCollection.document(pathId).update(data).await()
    }

    suspend fun deletePath(pathId: String) {
        pathsCollection.document(pathId).delete().await()
    }

    // ── LEADERBOARD ────────────────────────────────────────────────────────

    fun observeLeaderboard(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = contributorsCollection
            .orderBy("totalPoints", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply { put("userId", doc.id) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ✅ Now accepts displayName so "Raju" appears on leaderboard
    // instead of "user_demo"
    // pathsAdded   = +10 points (harder, worth more)
    // reportsCount = +5  points (easier status update)
    suspend fun incrementScore(
        userId      : String,
        displayName : String,   // ← real name from WelcomeScreen
        field       : String    // "pathsAdded" or "reportsCount"
    ) {
        if (userId.isBlank()) return

        val pointsForAction = when (field) {
            "pathsAdded"   -> 10L
            "reportsCount" -> 5L
            else           -> 1L
        }

        val docRef   = contributorsCollection.document(userId)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            docRef.update(
                field,         FieldValue.increment(1),
                "totalPoints", FieldValue.increment(pointsForAction),
                "name",        displayName   // keep name updated
            ).await()
        } else {
            // First time — create document with real display name
            docRef.set(
                mapOf(
                    "userId"       to userId,
                    "name"         to displayName,  // ✅ "Raju" not "user_demo"
                    "pathsAdded"   to if (field == "pathsAdded") 1 else 0,
                    "reportsCount" to if (field == "reportsCount") 1 else 0,
                    "totalPoints"  to pointsForAction
                )
            ).await()
        }
    }
}