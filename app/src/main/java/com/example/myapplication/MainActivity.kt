package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SkyjoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SkyjoApp(viewModel)
            }
        }
    }
}

@Composable
fun SkyjoApp(viewModel: SkyjoViewModel) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            when (viewModel.currentScreen.value) {
                Screen.Main -> MainScreen(viewModel)
                Screen.NewGame -> NewGameScreen(viewModel)
                Screen.Game -> GameScreen(viewModel)
                Screen.ScoreEntry -> ScoreEntryScreen(viewModel)
                Screen.Winner -> WinnerScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: SkyjoViewModel) {
    val savedGroups by viewModel.savedGroups.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Skyjo Games", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.goToNewGame() }) {
                Text("New Game")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Saved Games (Tap to Play):", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        if (savedGroups.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No games created yet.", color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.goToNewGame() }) {
                    Text("Create Your First Game")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedGroups.keys.toList()) { name ->
                    val groupPlayers = savedGroups[name]!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.loadGroup(name, groupPlayers) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Players: " + groupPlayers.joinToString(", ") { it.name },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                val total = groupPlayers.sumOf { it.scores.sum() }
                                val rounds = groupPlayers.firstOrNull()?.scores?.size ?: 0
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Rounds: $rounds | Total Score: $total",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteGroup(name) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Game", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewGameScreen(viewModel: SkyjoViewModel) {
    var gameName by remember { mutableStateOf("") }
    var playerName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("New Game", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.goToMain() }) {
                Text("Cancel")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = gameName,
            onValueChange = { gameName = it },
            label = { Text("Game Name (e.g. Friday Night)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = playerName,
            onValueChange = { playerName = it },
            label = { Text("Player Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                if (playerName.isNotBlank()) {
                    viewModel.addPlayer(playerName)
                    playerName = ""
                }
            },
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
        ) {
            Text("Add Player")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Players (${viewModel.players.size}):", fontWeight = FontWeight.SemiBold)

        LazyColumn(
            modifier = Modifier.height(180.dp).fillMaxWidth()
        ) {
            items(viewModel.players) { player ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("- ${player.name}", fontSize = 16.sp)
                    IconButton(onClick = { viewModel.removePlayer(player) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.createNewGame(gameName)
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            enabled = gameName.isNotBlank() && viewModel.players.size >= 2
        ) {
            Text("Start Game & Enter Scores")
        }

        if (gameName.isBlank() || viewModel.players.size < 2) {
            Text(
                text = if (gameName.isBlank()) "Please enter a game name" else "Please add at least 2 players",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun GameScreen(viewModel: SkyjoViewModel) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(viewModel.activeGroupName.value ?: "Current Scores", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.goToMain() }) {
                Text("Back to Games")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(viewModel.players) { player ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(player.name, fontWeight = FontWeight.Bold)
                        Text("Total: ${player.totalScore}")
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showResetConfirmation = true },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Reset Scores")
                    }
                    Button(
                        onClick = { viewModel.currentScreen.value = Screen.ScoreEntry },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Enter Round Scores")
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset Scores") },
            text = { Text("Are you sure you want to reset all scores? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetScores()
                        showResetConfirmation = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScoreEntryScreen(viewModel: SkyjoViewModel) {
    val newScores = remember { mutableStateMapOf<String, String>() }
    var finisherName by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enter Scores", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.currentScreen.value = Screen.Game }) {
                Text("Back to Game")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Finish", fontSize = 12.sp, modifier = Modifier.width(48.dp))
            Text("Player", fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("Total", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Text("Round", fontSize = 12.sp, modifier = Modifier.width(80.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.players) { player ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = finisherName == player.name,
                        onClick = { finisherName = player.name },
                        modifier = Modifier.width(48.dp)
                    )
                    
                    Text(
                        player.name, 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${player.totalScore}",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 16.sp
                    )

                    OutlinedTextField(
                        value = newScores[player.name] ?: "",
                        onValueChange = { newScores[player.name] = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }
            }
            
            item {
                Button(
                    onClick = {
                        val scoreMap = newScores.mapValues { it.value.toIntOrNull() ?: 0 }
                        viewModel.enterScores(scoreMap, finisherName)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    enabled = finisherName != null
                ) {
                    Text("Submit Scores")
                }
                if (finisherName == null) {
                    Text(
                        "Please select who finished the round",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WinnerScreen(viewModel: SkyjoViewModel) {
    val rankedPlayers = remember(viewModel.players) {
        viewModel.players.sortedBy { it.totalScore }
    }
    val roundsCount = viewModel.players.firstOrNull()?.scores?.size ?: 0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Game Over!", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Total Rounds Played: $roundsCount", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))
        
        Text("Final Standings:", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(rankedPlayers) { index, player ->
                val isWinner = index == 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isWinner) {
                        androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        androidx.compose.material3.CardDefaults.cardColors()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = player.name,
                                    fontSize = 18.sp,
                                    fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isWinner) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isWinner) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "👑 Winner",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${player.totalScore} pts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isWinner) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.goToMain() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Games List")
        }
    }
}
