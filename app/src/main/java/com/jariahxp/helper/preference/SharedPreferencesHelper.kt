package com.jariahxp.helper.preference

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {

    private const val PREF_NAME = "user_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"


    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUsername(username: String, context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
    }
    fun saveEmail(email: String, context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
    }
    fun getEmail(context: Context): String? {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(KEY_EMAIL, null)
    }
    fun getUsername(context: Context): String? {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(KEY_USERNAME, null)
    }
    fun clearSession(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }

    fun deleteUsername(context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().remove(KEY_USERNAME).apply()
    }
}