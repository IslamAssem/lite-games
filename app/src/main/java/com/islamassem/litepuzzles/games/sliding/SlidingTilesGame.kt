package com.islamassem.litepuzzles.games.sliding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.islamassem.litepuzzles.engine.PuzzleSession
import com.islamassem.litepuzzles.ui.game.PuzzleScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val FrameColor = Color(0xFF455A64)
private val TileColor = Color(0xFF5B3CC4)
private val TileHomeColor = Color(0xFF2E7D32)

@Composable
fun SlidingTilesGame(title: String, rules: String, createPuzzle: () -> SlidingTilesPuzzle, onExit: () -> Unit) {
    PuzzleScreen(title = title, rules = rules, createPuzzle = createPuzzle, onExit = onExit, randomized = true) { session, modifier ->
        Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            TilesBoard(session, session.puzzle as SlidingTilesPuzzle)
            Text(
                "Tap a tile next to the gap to slide it.\nGreen tiles are already in the right place.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TilesBoard(session: PuzzleSession<TilesState, SlideMove>, puzzle: SlidingTilesPuzzle) {
    val state = session.state
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val n = puzzle.size

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(FrameColor)
            .padding(6.dp),
    ) {
        val cell = constraints.maxWidth.toFloat() / n
        val gap = with(density) { 6.dp.toPx() }
        fun topLeft(index: Int) = Offset(puzzle.col(index) * cell, puzzle.row(index) * cell)

        // One animated position per tile number; undo/restart snap them to the committed state.
        val positions = remember(cell) {
            (1 until n * n).associateWith { Animatable(topLeft(state.tiles.indexOf(it)), Offset.VectorConverter) }
        }
        LaunchedEffect(state, positions) {
            positions.forEach { (tile, a) -> a.snapTo(topLeft(state.tiles.indexOf(tile))) }
        }

        val tileSize = with(density) { (cell - gap).toDp() }
        positions.forEach { (tile, anim) ->
            val home = state.tiles.indexOf(tile) == tile - 1
            val color by animateColorAsState(if (home) TileHomeColor else TileColor, tween(250), label = "tile$tile")
            val pos = anim.value
            Box(
                Modifier
                    .zIndex(if (anim.isRunning) 1f else 0f)
                    .offset { IntOffset((pos.x + gap / 2).roundToInt(), (pos.y + gap / 2).roundToInt()) }
                    .size(tileSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .clickable {
                        if (session.isAnimating) return@clickable
                        val move = puzzle.moveFor(state, tile) ?: return@clickable
                        scope.launch {
                            session.playAnimated(move) {
                                anim.animateTo(topLeft(state.blank), tween(130, easing = FastOutSlowInEasing))
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("$tile", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
