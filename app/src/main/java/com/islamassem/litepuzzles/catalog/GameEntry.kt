package com.islamassem.litepuzzles.catalog

import androidx.compose.runtime.Composable

enum class GameCategory(val title: String) {
    RIVER_CROSSING("River Crossing"),
    POURING("Pouring & Measuring"),
    WEIGHING("Balance Scale"),
    SLIDING("Sliding & Moving"),
    GRID_LOGIC("Grid Logic"),
}

enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
}

/**
 * Everything the app needs to list and launch a game.
 *
 * [content] draws the whole game screen. Engine-based puzzles should use
 * [com.islamassem.litepuzzles.ui.game.PuzzleScreen], which supplies the top bar,
 * undo/redo/restart/hint, and the win/lose dialog.
 */
data class GameEntry(
    /** Stable unique key; used for navigation and (later) saved progress. Never change it once shipped. */
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: GameCategory,
    val difficulty: Difficulty,
    val content: @Composable (onExit: () -> Unit) -> Unit,
)
