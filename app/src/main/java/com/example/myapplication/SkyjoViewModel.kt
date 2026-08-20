package com.example.myapplication

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class Player(
    val name: String,
    val scores: MutableList<Int> = mutableStateListOf()
) {
    val totalScore: Int get() = scores.sum()
}

enum class Screen {
    Setup,
    Game,
    ScoreEntry,
    Winner
}

class SkyjoViewModel(application: Application) : AndroidViewModel(application) {
    val players = mutableStateListOf<Player>()
    val currentScreen = mutableStateOf(Screen.Setup)
    val winner = mutableStateOf<Player?>(null)
    val activeGroupName = mutableStateOf<String?>(null)

    private val _savedGroups = MutableStateFlow<Map<String, List<PlayerData>>>(emptyMap())
    val savedGroups = _savedGroups.asStateFlow()

    init {
        viewModelScope.launch {
            // Load saved groups
            launch {
                PlayerStorage.getSavedGroups(getApplication()).collect {
                    _savedGroups.value = it
                }
            }
            
            // Load current game state ONCE at startup
            try {
                val state = PlayerStorage.getCurrentGame(getApplication()).first()
                state?.let { loadedState ->
                    if (players.isEmpty()) {
                        loadedState.players.forEach { pData ->
                            val player = Player(pData.name)
                            player.scores.addAll(pData.scores)
                            players.add(player)
                        }
                        currentScreen.value = Screen.valueOf(loadedState.currentScreen)
                        activeGroupName.value = loadedState.activeGroupName
                        checkWinner()
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun persistGame() {
        viewModelScope.launch {
            val playerList = players.map { PlayerData(it.name, it.scores.toList()) }
            val state = GameState(
                players = playerList,
                currentScreen = currentScreen.value.name,
                activeGroupName = activeGroupName.value
            )
            PlayerStorage.saveCurrentGame(getApplication(), state)
            
            // Auto-save scores to the specific group if one is active
            activeGroupName.value?.let { groupName ->
                PlayerStorage.saveGroup(getApplication(), groupName, playerList)
            }
        }
    }

    fun addPlayer(name: String) {
        if (name.isNotBlank() && !players.any { it.name == name }) {
            players.add(Player(name))
            activeGroupName.value = null // Modifying list breaks the group link
            persistGame()
        }
    }

    fun removePlayer(player: Player) {
        players.remove(player)
        activeGroupName.value = null // Modifying list breaks the group link
        persistGame()
    }

    fun saveCurrentGroup(groupName: String) {
        if (groupName.isNotBlank() && players.isNotEmpty()) {
            activeGroupName.value = groupName
            persistGame()
        }
    }

    fun loadGroup(groupName: String, playerDataList: List<PlayerData>) {
        activeGroupName.value = groupName
        players.clear()
        playerDataList.forEach { pData ->
            val player = Player(pData.name)
            player.scores.addAll(pData.scores)
            players.add(player)
        }
        winner.value = null
        startGame()
    }

    fun deleteGroup(groupName: String) {
        viewModelScope.launch {
            if (activeGroupName.value == groupName) {
                activeGroupName.value = null
            }
            PlayerStorage.deleteGroup(getApplication(), groupName)
        }
    }

    fun startGame() {
        if (players.size >= 2) {
            currentScreen.value = Screen.Game
            persistGame()
        }
    }

    fun goToSetup() {
        currentScreen.value = Screen.Setup
        persistGame()
    }

    fun enterScores(newScores: Map<String, Int>, finisherName: String?) {
        val scoresWithDoubling = newScores.toMutableMap()
        
        if (finisherName != null) {
            val finisherScore = newScores[finisherName] ?: 0
            val minScore = newScores.values.minOrNull() ?: 0
            
            // Skyjo Rule: If the player who finished does not have the strictly lowest score, 
            // their positive points are doubled.
            if ((finisherScore > minScore) && (finisherScore > 0)) {
                scoresWithDoubling[finisherName] = finisherScore * 2
            }
        }

        players.forEach { player ->
            scoresWithDoubling[player.name]?.let { score ->
                player.scores.add(score)
            }
        }
        checkWinner()
        persistGame()
    }

    private fun checkWinner() {
        val reached100 = players.filter { it.totalScore >= 100 }
        if (reached100.isNotEmpty()) {
            winner.value = players.minByOrNull { it.totalScore }
            currentScreen.value = Screen.Winner
        } else if (currentScreen.value == Screen.ScoreEntry) {
            currentScreen.value = Screen.Game
        }
    }

    fun resetScores() {
        players.forEach { it.scores.clear() }
        winner.value = null
        currentScreen.value = Screen.Game
        persistGame()
    }

    fun resetGame() {
        players.clear()
        winner.value = null
        activeGroupName.value = null
        currentScreen.value = Screen.Setup
        viewModelScope.launch {
            PlayerStorage.saveCurrentGame(getApplication(), null)
        }
    }
}
