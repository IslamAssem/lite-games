package com.islamassem.litepuzzles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.islamassem.litepuzzles.catalog.GameRegistry
import com.islamassem.litepuzzles.ui.home.HomeScreen
import com.islamassem.litepuzzles.ui.theme.LitePuzzlesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LitePuzzlesTheme { App() }
        }
    }
}

/** Two-level navigation: the game list, or the open game (by id, so it survives rotation). */
@Composable
private fun App() {
    var openGameId by rememberSaveable { mutableStateOf<String?>(null) }
    val game = openGameId?.let(GameRegistry::find)

    if (game == null) {
        HomeScreen(games = GameRegistry.games, onOpen = { openGameId = it.id })
    } else {
        BackHandler { openGameId = null }
        game.content { openGameId = null }
    }
}
