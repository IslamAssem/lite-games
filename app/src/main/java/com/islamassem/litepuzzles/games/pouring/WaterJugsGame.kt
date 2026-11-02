package com.islamassem.litepuzzles.games.pouring

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islamassem.litepuzzles.engine.PuzzleSession
import com.islamassem.litepuzzles.ui.game.PuzzleScreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val WaterColor = Color(0xFF29B6F6)
private val GlassColor = Color(0x2290A4AE)
private val SelectedColor = Color(0xFFFFB300)

private const val TILT_DEGREES = 28f

/**
 * Board for any [WaterJugsPuzzle]. Tap a jug to pick it up, then tap another to pour into it.
 * Fill/Empty buttons appear under each jug when the puzzle allows the tap and drain.
 * Water levels and the tilt animate first; the move is committed when the pour finishes.
 */
@Composable
fun WaterJugsGame(
    title: String,
    rules: String,
    createPuzzle: () -> WaterJugsPuzzle,
    onExit: () -> Unit,
) {
    PuzzleScreen(title = title, rules = rules, createPuzzle = createPuzzle, onExit = onExit) { session, modifier ->
        JugsBoard(session, session.puzzle as WaterJugsPuzzle, modifier)
    }
}

@Composable
private fun JugsBoard(session: PuzzleSession<JugsState, JugMove>, puzzle: WaterJugsPuzzle, modifier: Modifier) {
    val state = session.state
    val scope = rememberCoroutineScope()
    val busy = session.isAnimating

    // Animated water level per jug (in litres) and tilt per jug (degrees).
    val levels = remember { state.amounts.map { Animatable(it.toFloat()) } }
    val tilts = remember { state.amounts.map { Animatable(0f) } }
    LaunchedEffect(state) { levels.forEachIndexed { i, a -> a.snapTo(state.amounts[i].toFloat()) } }

    var selected by remember(state) { mutableStateOf<Int?>(null) }
    var message by remember(state) { mutableStateOf<String?>(null) }

    fun perform(move: JugMove) {
        if (!puzzle.isLegal(state, move)) return
        val next = puzzle.apply(state, move)
        scope.launch {
            session.playAnimated(move) {
                val pourer = (move as? JugMove.Pour)?.from
                val tilt = if (move is JugMove.Pour) (if (move.to > move.from) TILT_DEGREES else -TILT_DEGREES) else 0f
                if (pourer != null) tilts[pourer].animateTo(tilt, tween(250))
                coroutineScope {
                    levels.forEachIndexed { i, a ->
                        launch { a.animateTo(next.amounts[i].toFloat(), tween(700, easing = FastOutSlowInEasing)) }
                    }
                }
                if (pourer != null) tilts[pourer].animateTo(0f, tween(250))
            }
        }
    }

    fun onJugTap(i: Int) {
        if (busy) return
        val from = selected
        when {
            from == null -> if (state.amounts[i] > 0) selected = i else message = "That jug is empty — fill it first."
            from == i -> selected = null
            state.amounts[i] == puzzle.capacities[i] -> message = "The ${puzzle.jugName(i)} is already full."
            else -> perform(JugMove.Pour(from, i))
        }
    }

    // Size jugs so the biggest one is ~260dp tall.
    val unit: Dp = (260 / puzzle.capacities.max()).dp

    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(puzzle.goalText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            puzzle.capacities.forEachIndexed { i, capacity ->
                Jug(
                    capacity = capacity,
                    level = levels[i].value,
                    tilt = tilts[i].value,
                    unit = unit,
                    selected = selected == i,
                    onTap = { onJugTap(i) },
                    tapAndDrain = puzzle.tapAndDrain,
                    enabled = !busy,
                    onFill = { perform(JugMove.Fill(i)) },
                    onEmpty = { perform(JugMove.Empty(i)) },
                    canFill = state.amounts[i] < capacity,
                    canEmpty = state.amounts[i] > 0,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            message ?: when {
                busy -> "Pouring…"
                selected != null -> "Now tap the jug to pour into (or the same jug to put it down)."
                else -> "Tap a jug to pick it up."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Jug(
    capacity: Int,
    level: Float,
    tilt: Float,
    unit: Dp,
    selected: Boolean,
    onTap: () -> Unit,
    tapAndDrain: Boolean,
    enabled: Boolean,
    onFill: () -> Unit,
    onEmpty: () -> Unit,
    canFill: Boolean,
    canEmpty: Boolean,
) {
    val lift by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "lift")
    val outline = if (selected) SelectedColor else MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${level.roundToInt()} / $capacity L",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .graphicsLayer { translationY = -lift * 16.dp.toPx() }
                .padding(bottom = 8.dp),
        )
        Box(
            Modifier
                .graphicsLayer {
                    translationY = -lift * 16.dp.toPx()
                    rotationZ = tilt
                }
                .width(84.dp)
                .height(unit * capacity)
                .clip(shape)
                .background(GlassColor)
                .border(3.dp, outline, shape)
                .clickable(enabled = enabled, onClick = onTap),
        ) {
            // Water
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight((level / capacity).coerceIn(0f, 1f))
                    .background(WaterColor),
            )
            // One tick per litre so players can read the level.
            val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            Box(
                Modifier.fillMaxWidth().fillMaxHeight().drawBehind {
                    for (l in 1 until capacity) {
                        val y = size.height * (1 - l.toFloat() / capacity)
                        drawLine(tickColor, Offset(0f, y), Offset(size.width * 0.3f, y), strokeWidth = 2.dp.toPx())
                    }
                },
            )
        }
        Text("$capacity L", fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        if (tapAndDrain) {
            FilledTonalButton(onClick = onFill, enabled = enabled && canFill, modifier = Modifier.padding(top = 8.dp)) {
                Text("🚰 Fill")
            }
            FilledTonalButton(onClick = onEmpty, enabled = enabled && canEmpty) {
                Text("🗑 Empty")
            }
        }
    }
}
