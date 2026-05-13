package com.nammahaadi.app.data.repository

import com.nammahaadi.app.data.model.PathModel
import com.nammahaadi.app.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow

// Repository is the middleman between ViewModel and FirebaseService.
// Business logic (coordinate mapping, status checks) lives here.

class FirebaseRepository(
    private val service: FirebaseService = FirebaseService()
) {

    // ── PATHS ──────────────────────────────────────────

    fun getAllPaths(): Flow<List<PathModel>> = service.observePaths()

    suspend fun savePath(
        name: String,
        coordinates: List<Map<String, Double>>,
        userId: String,
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
            service.incrementScore(userId, "pathsAdded")
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePathStatus(
        pathId: String,
        newStatus: String,   // "DRY", "MUDDY", "FLOODED"
        userId: String
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
            service.incrementScore(userId, "reportsCount")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePath(pathId: String): Result<Unit> {
        return try {
            service.deletePath(pathId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── LEADERBOARD ────────────────────────────────────

    fun getLeaderboard(): Flow<List<Map<String, Any>>> = service.observeLeaderboard()
}