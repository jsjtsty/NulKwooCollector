package com.nulstudio.kwoocollector.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class TokenManager(private val context: Context) {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    suspend fun saveToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token == null) {
                prefs.remove(TOKEN_KEY)
            } else {
                prefs[TOKEN_KEY] = token
            }
        }
    }
}