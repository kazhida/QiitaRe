package com.abplua.qiitare

import android.content.Context

class QiitaTokenPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAccessToken(accessToken: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
    }

    fun getAccessToken(): String? {
        return preferences.getString(KEY_ACCESS_TOKEN, null)
    }

    companion object {
        private const val PREFS_NAME = "qiita_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
