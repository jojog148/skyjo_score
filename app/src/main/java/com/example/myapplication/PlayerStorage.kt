package com.example.myapplication

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PlayerData(
    val name: String,
    val scores: List<Int>
)

@Serializable
data class GameState(
    val players: List<PlayerData>,
    val currentScreen: String,
    val activeGroupName: String? = null
)

val Context.dataStore by preferencesDataStore(name = "skyjo_players")

object PlayerStorage {
    private val SAVED_GROUPS = stringPreferencesKey("saved_player_groups")
    private val CURRENT_GAME = stringPreferencesKey("current_game")

    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun getSavedGroups(context: Context): Flow<Map<String, List<PlayerData>>> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[SAVED_GROUPS] ?: "{}"
            try {
                json.decodeFromString<Map<String, List<PlayerData>>>(jsonString)
            } catch (e: Exception) {
                try {
                    val oldFormat = json.decodeFromString<Map<String, List<String>>>(jsonString)
                    oldFormat.mapValues { it.value.map { name -> PlayerData(name, emptyList()) } }
                } catch (e2: Exception) {
                    emptyMap()
                }
            }
        }
    }

    suspend fun saveGroup(context: Context, groupName: String, players: List<PlayerData>) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SAVED_GROUPS] ?: "{}"
            val currentGroups = try {
                json.decodeFromString<MutableMap<String, List<PlayerData>>>(currentJson)
            } catch (e: Exception) {
                mutableMapOf()
            }
            currentGroups[groupName] = players
            preferences[SAVED_GROUPS] = json.encodeToString(currentGroups)
        }
    }

    suspend fun deleteGroup(context: Context, groupName: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SAVED_GROUPS] ?: "{}"
            val currentGroups = try {
                json.decodeFromString<MutableMap<String, List<PlayerData>>>(currentJson)
            } catch (e: Exception) {
                mutableMapOf()
            }
            currentGroups.remove(groupName)
            preferences[SAVED_GROUPS] = json.encodeToString(currentGroups)
        }
    }

    fun getCurrentGame(context: Context): Flow<GameState?> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[CURRENT_GAME] ?: return@map null
            try {
                json.decodeFromString<GameState>(jsonString)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveCurrentGame(context: Context, gameState: GameState?) {
        context.dataStore.edit { preferences ->
            if (gameState == null) {
                preferences.remove(CURRENT_GAME)
            } else {
                preferences[CURRENT_GAME] = json.encodeToString(gameState)
            }
        }
    }
}
