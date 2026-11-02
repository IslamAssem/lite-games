package com.islamassem.litepuzzles.games.rivercrossing

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islamassem.litepuzzles.ui.game.PuzzleScreen
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val GrassColor = Color(0xFF7CB342)
private val RiverColor = Color(0xFF4FC3F7)
private val BoatColor = Color(0xFF8D6E63)

private const val CROSSING_MILLIS = 1100

private fun Bank.bias() = if (this == Bank.START) -1f else 1f

/**
 * Board for any [RiverCrossingPuzzle]: tap passengers to board/unboard, then row across.
 *
 * Crossing choreography: the loaded boat rows across first, and only when it arrives is the
 * move committed to the session — so passengers step off onto the far bank on arrival.
 */
@Composable
fun RiverCrossingGame(
    title: String,
    rules: String,
    createPuzzle: () -> RiverCrossingPuzzle,
    onExit: () -> Unit,
) {
    PuzzleScreen(title = title, rules = rules, createPuzzle = createPuzzle, onExit = onExit) { session, modifier ->
        val puzzle = session.puzzle as RiverCrossingPuzzle
        val state = session.state
        val scope = rememberCoroutineScope()

        // Who is sitting in the boat; reset whenever the committed state changes (arrival, undo, restart).
        var aboard by remember(state) { mutableStateOf(emptySet<String>()) }
        // Boat position: -1 = start bank, 1 = goal bank. Animated during a crossing, snapped on undo/restart.
        val boatX = remember { Animatable(state.boat.bias()) }
        LaunchedEffect(state.boat) { boatX.snapTo(state.boat.bias()) }

        val busy = session.isAnimating
        val move = Crossing(aboard)
        val canRow = !busy && aboard.isNotEmpty() && puzzle.isLegal(state, move)
        val board: (String) -> Unit = { id ->
            if (!busy && aboard.size < puzzle.boatCapacity) aboard = aboard + id
        }

        Column(modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp))) {
                BankColumn(
                    puzzle = puzzle,
                    present = state.on(Bank.START) - aboard,
                    label = "Start",
                    tappable = !busy && state.boat == Bank.START,
                    onTap = board,
                )
                River(
                    puzzle = puzzle,
                    boatX = boatX.value,
                    aboard = aboard,
                    tappable = !busy,
                    onTap = { id -> aboard = aboard - id },
                    modifier = Modifier.weight(1f),
                )
                BankColumn(
                    puzzle = puzzle,
                    present = state.on(Bank.END) - aboard,
                    label = "Goal",
                    tappable = !busy && state.boat == Bank.END,
                    onTap = board,
                )
            }
            Text(
                when {
                    busy -> "Rowing…"
                    aboard.isEmpty() -> "Tap characters on the boat's side to get in."
                    !canRow -> "Someone in the boat must be able to row."
                    else -> "Ready to cross!"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(
                onClick = {
                    scope.launch {
                        session.playAnimated(move) {
                            boatX.animateTo(
                                state.boat.other.bias(),
                                tween(CROSSING_MILLIS, easing = FastOutSlowInEasing),
                            )
                        }
                    }
                },
                enabled = canRow,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("⛵ Row across")
            }
        }
    }
}

/**
 * One slot per passenger in a fixed order, so nobody shifts when someone leaves —
 * absent passengers just fade out, arriving ones pop in.
 */
@Composable
private fun BankColumn(
    puzzle: RiverCrossingPuzzle,
    present: Set<String>,
    label: String,
    tappable: Boolean,
    onTap: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxHeight().widthIn(min = 76.dp).background(GrassColor).padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
        puzzle.passengers.forEach { p ->
            val here = p.id in present
            val appear by animateFloatAsState(
                targetValue = if (here) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "appear-${p.id}",
            )
            Character(
                p,
                enabled = tappable && here,
                onClick = { onTap(p.id) },
                modifier = Modifier.graphicsLayer {
                    alpha = appear.coerceIn(0f, 1f)
                    scaleX = 0.5f + 0.5f * appear
                    scaleY = 0.5f + 0.5f * appear
                },
            )
        }
    }
}

@Composable
private fun River(
    puzzle: RiverCrossingPuzzle,
    boatX: Float,
    aboard: Set<String>,
    tappable: Boolean,
    onTap: (String) -> Unit,
    modifier: Modifier,
) {
    // 0 at either bank, 1 mid-river: drives a gentle rocking while rowing.
    val midRiver = 1f - abs(boatX)
    Box(modifier.fillMaxHeight().background(RiverColor).padding(4.dp)) {
        Column(
            Modifier
                .align(BiasAlignment(boatX, 0f))
                .graphicsLayer {
                    rotationZ = sin(midRiver * PI * 3).toFloat() * 4f
                    translationY = -midRiver * 8.dp.toPx()
                }
                .width(64.dp)
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 6.dp, topEnd = 6.dp))
                .background(BoatColor)
                .animateContentSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            puzzle.passengers.filter { it.id in aboard }.forEach { p ->
                Character(p, enabled = tappable, onClick = { onTap(p.id) })
            }
            if (aboard.isEmpty()) Text("⛵", fontSize = 28.sp)
        }
    }
}

@Composable
private fun Character(p: Passenger, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(p.emoji, fontSize = 30.sp, modifier = Modifier.size(40.dp), textAlign = TextAlign.Center)
        Text(
            p.name.substringBefore(' '),
            fontSize = 10.sp,
            color = Color.White,
            maxLines = 1,
        )
    }
}
