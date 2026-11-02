package com.islamassem.litepuzzles.games.hanoi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.islamassem.litepuzzles.engine.PuzzleSession
import com.islamassem.litepuzzles.ui.game.PuzzleScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DiscColors = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF5E35B1), Color(0xFFD81B60), Color(0xFF00897B),
)
private val WoodColor = Color(0xFF8D6E63)

@Composable
fun HanoiGame(title: String, rules: String, createPuzzle: () -> HanoiPuzzle, onExit: () -> Unit) {
    PuzzleScreen(title = title, rules = rules, createPuzzle = createPuzzle, onExit = onExit) { session, modifier ->
        Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            HanoiBoard(session, session.puzzle as HanoiPuzzle)
            Spacer(Modifier.weight(1f))
        }
    }
}

/** Pixel geometry of the board; disc positions are the top-centre of each disc. */
private class Geometry(val width: Float, val height: Float, val discHeight: Float, discCount: Int) {
    val baseY = height - discHeight * 0.6f
    val pegTop = baseY - (discCount + 1.2f) * discHeight
    val liftY = pegTop - discHeight * 1.4f
    private val slot = width / 3f
    private val minWidth = slot * 0.32f
    private val maxWidth = slot * 0.94f
    private val discCount = discCount

    fun pegX(peg: Int) = slot * (peg + 0.5f)
    fun discWidth(disc: Int) =
        if (discCount == 1) maxWidth else minWidth + (maxWidth - minWidth) * (disc - 1) / (discCount - 1)

    fun restPosition(state: HanoiState, disc: Int): Offset {
        val peg = state.pegs.indexOfFirst { disc in it }
        val level = state.pegs[peg].indexOf(disc)
        return Offset(pegX(peg), baseY - (level + 1) * discHeight)
    }

    fun liftedPosition(peg: Int) = Offset(pegX(peg), liftY)
}

@Composable
private fun HanoiBoard(session: PuzzleSession<HanoiState, HanoiMove>, puzzle: HanoiPuzzle) {
    val state = session.state
    val busy = session.isAnimating
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var lifted by remember(state) { mutableStateOf<Int?>(null) } // peg whose top disc is raised
    var message by remember(state) { mutableStateOf<String?>(null) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.25f)) {
            val geo = with(density) {
                Geometry(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), 26.dp.toPx(), puzzle.discCount)
            }
            val discs = remember(geo.width, geo.height) {
                (1..puzzle.discCount).associateWith { Animatable(geo.restPosition(state, it), Offset.VectorConverter) }
            }
            // Undo / redo / restart: put every disc where the state says, instantly.
            LaunchedEffect(state, discs) { discs.forEach { (disc, a) -> a.snapTo(geo.restPosition(state, disc)) } }

            fun onPegTap(peg: Int) {
                if (busy) return
                val from = lifted
                when {
                    from == null -> {
                        val disc = state.top(peg) ?: return
                        lifted = peg
                        message = null
                        scope.launch { discs.getValue(disc).animateTo(geo.liftedPosition(peg), tween(180)) }
                    }
                    from == peg -> {
                        val disc = state.top(peg)!!
                        lifted = null
                        scope.launch { discs.getValue(disc).animateTo(geo.restPosition(state, disc), tween(180)) }
                    }
                    else -> {
                        val move = HanoiMove(from, peg)
                        val disc = state.top(from)!!
                        val anim = discs.getValue(disc)
                        if (!puzzle.isLegal(state, move)) {
                            message = "A bigger disc can't go on a smaller one."
                            scope.launch { shake(anim, geo.liftedPosition(from)) }
                            return
                        }
                        scope.launch {
                            session.playAnimated(move) {
                                anim.animateTo(geo.liftedPosition(peg), tween(320, easing = FastOutSlowInEasing))
                                val landing = Offset(geo.pegX(peg), geo.baseY - (state.pegs[peg].size + 1) * geo.discHeight)
                                anim.animateTo(landing, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                            }
                        }
                    }
                }
            }

            // Pegs and base
            val pegWidth = 10.dp
            val pegWidthPx = with(density) { pegWidth.toPx() }
            for (peg in HanoiPuzzle.PEGS) {
                Box(
                    Modifier
                        .offset { IntOffset((geo.pegX(peg) - pegWidthPx / 2).roundToInt(), geo.pegTop.roundToInt()) }
                        .size(pegWidth, with(density) { (geo.baseY - geo.pegTop).toDp() })
                        .background(WoodColor, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                )
            }
            Box(
                Modifier
                    .offset { IntOffset(0, geo.baseY.roundToInt()) }
                    .fillMaxWidth()
                    .height(with(density) { (geo.height - geo.baseY).toDp() })
                    .background(WoodColor, RoundedCornerShape(6.dp)),
            )

            // Discs
            val movingDisc = lifted?.let { state.top(it) }
            discs.forEach { (disc, anim) ->
                val w = geo.discWidth(disc)
                val pos = anim.value
                Box(
                    Modifier
                        .zIndex(if (disc == movingDisc || busy) 1f else 0f)
                        .offset { IntOffset((pos.x - w / 2).roundToInt(), pos.y.roundToInt()) }
                        .size(with(density) { w.toDp() }, with(density) { (geo.discHeight - 3f).toDp() })
                        .background(DiscColors[(disc - 1) % DiscColors.size], RoundedCornerShape(50)),
                )
            }

            // Invisible tap targets: one full-height column per peg, above everything.
            Row(Modifier.fillMaxSize().zIndex(2f)) {
                for (peg in HanoiPuzzle.PEGS) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (lifted == peg) Color(0x14FFB300) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !busy,
                                onClick = { onPegTap(peg) },
                            ),
                    )
                }
            }
        }
        Text(
            message ?: when {
                busy -> "Moving…"
                lifted != null -> "Tap a peg to drop the disc (or the same peg to put it back)."
                else -> "Tap a peg to pick up its top disc."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

private suspend fun shake(anim: Animatable<Offset, *>, home: Offset) {
    for (dx in listOf(14f, -12f, 8f, -5f, 0f)) {
        anim.animateTo(home + Offset(dx, 0f), tween(45))
    }
}
