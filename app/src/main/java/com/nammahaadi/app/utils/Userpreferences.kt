package com.nammahaadi.app.utils

import android.content.Context

// Saves the user's name locally on the device using SharedPreferences
// No login required — just a simple name entry on first launch
object UserPreferences {

    private const val PREFS_NAME = "namma_haadi_prefs"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ID   = "user_id"

    // Get saved name — returns null if not set yet (first launch)
    fun getUserName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, null)
    }

    // Get or create a unique userId based on the name
    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null) ?: ""
    }

    // Save name + generate a simple userId from it
    fun saveUser(context: Context, name: String) {
        val prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // UserId = name + random 4 digits so two "Raju"s don't clash
        val userId = name.trim().lowercase().replace(" ", "_") +
                "_" + (1000..9999).random()
        prefs.edit()
            .putString(KEY_USER_NAME, name.trim())
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun isUserRegistered(context: Context): Boolean {
        return getUserName(context) != null
    }
}