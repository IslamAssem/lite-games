package com.islamassem.litepuzzles.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islamassem.litepuzzles.engine.Puzzle
import com.islamassem.litepuzzles.engine.PuzzleSession
import com.islamassem.litepuzzles.engine.PuzzleStatus
import kotlinx.coroutines.launch

/**
 * Standard screen for any engine-based [Puzzle]. Games only provide the [board];
 * the toolbar, move counter, hints, rules and end-of-game dialogs come from here.
 *
 * [createPuzzle] is called once per screen; Restart replays the same puzzle. Set [randomized]
 * for games whose [createPuzzle] deals a fresh random puzzle: the win dialog then offers
 * "New puzzle", which calls it again and rebuilds the board (all its remembered state resets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <S : Any, M : Any> PuzzleScreen(
    title: String,
    rules: String,
    createPuzzle: () -> Puzzle<S, M>,
    onExit: () -> Unit,
    randomized: Boolean = false,
    board: @Composable (session: PuzzleSession<S, M>, modifier: Modifier) -> Unit,
) {
    var deal by remember { mutableIntStateOf(0) }
    val session = remember(deal) { PuzzleSession(createPuzzle()) }
    var showRules by remember { mutableStateOf(true) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onExit) { Text("←", fontSize = 22.sp) } },
                actions = { TextButton(onClick = { showRules = true }) { Text("?", fontSize = 20.sp) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            PuzzleToolbar(
                session = session,
                onHint = {
                    val text = session.hint()?.let { "Hint: " + session.puzzle.describe(it) }
                        ?: "No solution from here — try undoing a few moves."
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(text)
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            key(deal) { board(session, Modifier.fillMaxSize()) }
        }
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            title = { Text("How to play") },
            text = { Text(rules) },
            confirmButton = { TextButton(onClick = { showRules = false }) { Text("Play") } },
        )
    }

    when (val status = session.status) {
        PuzzleStatus.Won -> {
            val stars = session.stars()
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Solved!") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 40.sp, color = MaterialTheme.colorScheme.tertiary)
                        Text(
                            "${session.moveCount} moves" + (session.optimalMoveCount?.let { " (best possible: $it)" } ?: ""),
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                confirmButton = {
                    if (randomized) {
                        TextButton(onClick = { deal++ }) { Text("New puzzle") }
                    } else {
                        TextButton(onClick = session::restart) { Text("Play again") }
                    }
                },
                dismissButton = { TextButton(onClick = onExit) { Text("All games") } },
            )
        }
        is PuzzleStatus.Lost -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Oops!") },
            text = { Text(status.reason) },
            confirmButton = { TextButton(onClick = session::undo) { Text("Undo") } },
            dismissButton = { TextButton(onClick = session::restart) { Text("Restart") } },
        )
        PuzzleStatus.Playing -> Unit
    }
}

@Composable
private fun PuzzleToolbar(session: PuzzleSession<*, *>, onHint: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp)) {
            Text(
                "Moves: ${session.moveCount}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = session::undo, enabled = session.canUndo) { Text("↶ Undo") }
                TextButton(onClick = session::redo, enabled = session.canRedo) { Text("↷ Redo") }
                TextButton(onClick = session::restart, enabled = session.canUndo) { Text("⟲ Restart") }
                TextButton(onClick = onHint, enabled = session.canHint) { Text("💡 Hint") }
            }
        }
    }
}
