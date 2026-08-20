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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
                Screen.Setup -> SetupScreen(viewModel)
                Screen.Game -> GameScreen(viewModel)
                Screen.ScoreEntry -> ScoreEntryScreen(viewModel)
                Screen.Winner -> WinnerScreen(viewModel)
            }
        }
    }
}

@Composable
fun SetupScreen(viewModel: SkyjoViewModel) {
    var playerName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    val savedGroups by viewModel.savedGroups.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("skyjo_score", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = playerName,
            onValueChange = { playerName = it },
            label = { Text("Player Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = {
                viewModel.addPlayer(playerName)
                playerName = ""
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Add Player")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Current Players:", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (viewModel.players.size >= 2) {
                TextButton(onClick = { viewModel.startGame() }) {
                    Text("Start")
                }
            }
            if (viewModel.players.isNotEmpty()) {
                TextButton(onClick = { showSaveDialog = true }) {
                    Text("Save List")
                }
            }
        }
        
        LazyColumn(modifier = Modifier.height(150.dp)) {
            items(viewModel.players) { player ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("- ${player.name}")
                    IconButton(onClick = { viewModel.removePlayer(player) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        Text("Saved Groups (Tap to Play):", fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(savedGroups.keys.toList()) { name ->
                val groupPlayers = savedGroups[name]!!
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.loadGroup(name, groupPlayers) }.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold)
                        Text(
                            "Players: " + (savedGroups[name]?.joinToString(", ") { it.name } ?: ""),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        val total = savedGroups[name]?.sumOf { it.scores.sum() } ?: 0
                        if (total > 0) {
                            Text("Total Score: $total", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { viewModel.deleteGroup(name) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Group")
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Player List") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name (e.g. Family)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCurrentGroup(groupName)
                    showSaveDialog = false
                    groupName = ""
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
            Text("Current Scores", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.goToSetup() }) {
                Text("Back to Setup")
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
        Text("Enter Scores", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Game Over!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("The winner is:", fontSize = 20.sp)
        Text(
            viewModel.winner.value?.name ?: "Unknown",
            fontSize = 36.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
        Text("Total Score: ${viewModel.winner.value?.totalScore ?: 0}", fontSize = 24.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = { viewModel.resetGame() }) {
            Text("New Game")
        }
    }
}
