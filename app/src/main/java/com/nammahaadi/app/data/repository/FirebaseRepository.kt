package com.nammahaadi.app.data.repository

import com.nammahaadi.app.data.model.PathModel
import com.nammahaadi.app.data.model.PathStatus
import com.nammahaadi.app.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow

class FirebaseRepository(
    private val service: FirebaseService = FirebaseService()
) {

    fun getAllPaths(): Flow<List<PathModel>> = service.observePaths()

    // ✅ Now takes displayName so leaderboard shows real name
    suspend fun savePath(
        name        : String,
        coordinates : List<Map<String, Double>>,
        userId      : String,
        displayName : String,           // ← real name e.g. "Raju"
        safeAfterDark: Boolean = true
    ): Result<String> {
        return try {
            val data = mapOf(
                "name"          to name.ifBlank { "Unnamed Shortcut" },
                "coordinates"   to coordinates,
                "status"        to "DRY",
                "reportedBy"    to userId,
                "lastUpdated"   to System.currentTimeMillis(),
                "safeAfterDark" to safeAfterDark
            )
            val id = service.addPath(data)
            service.incrementScore(userId, displayName, "pathsAdded")
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePathStatus(
        pathId      : String,
        newStatus   : String,
        userId      : String,
        displayName : String    // ← real name
    ): Result<Unit> {
        return try {
            service.updatePath(
                pathId,
                mapOf(
                    "status"      to newStatus,
                    "reportedBy"  to userId,
                    "lastUpdated" to System.currentTimeMillis()
                )
            )
            service.incrementScore(userId, displayName, "reportsCount")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLeaderboard(): Flow<List<Map<String, Any>>> = service.observeLeaderboard()
}