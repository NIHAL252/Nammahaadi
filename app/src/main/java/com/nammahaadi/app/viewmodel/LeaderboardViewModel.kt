package com.nammahaadi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammahaadi.app.data.model.Contributor
import com.nammahaadi.app.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.*

class LeaderboardViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    // ✅ FIX 1: Correct package — model is in data.model, not data
    // ✅ FIX 2: Map raw Map<String,Any> from Firestore → typed Contributor objects
    val leaderboard: StateFlow<List<Contributor>> = repository.getLeaderboard()
        .map { rawList ->
            rawList.map { map ->
                Contributor(
                    userId       = map["userId"] as? String ?: "",
                    name         = map["name"] as? String ?: "",
                    reportsCount = (map["reportsCount"] as? Long)?.toInt() ?: 0,
                    pathsAdded   = (map["pathsAdded"] as? Long)?.toInt() ?: 0,
                    totalPoints  = (map["totalPoints"] as? Long)?.toInt() ?: 0
                )
            }.sortedByDescending { it.totalPoints }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}