package com.noapp.container.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * [to], arriving by turning out of [from]: [from] spins away and fades while [to] spins in from
 * the other half-turn. Played once, when this first appears, and only if [from] is given.
 * Used where one screen's top-bar action sits exactly where the next screen's did (Configure's
 * gear and Settings' share), so the change of screen reads as the same button changing its job.
 */
@Composable
fun MorphIcon(to: ImageVector, contentDescription: String?, from: ImageVector? = null) {
    val progress = remember { Animatable(if (from == null) 1f else 0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(MORPH_MS, easing = FastOutSlowInEasing)) }
    val p = progress.value
    Box(contentAlignment = Alignment.Center) {
        if (from != null && p < 1f) {
            Icon(
                from,
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    rotationZ = p * 180f
                    alpha = 1f - p
                    scaleX = 1f - p * 0.4f
                    scaleY = 1f - p * 0.4f
                }
            )
        }
        Icon(
            to,
            contentDescription = contentDescription,
            modifier = Modifier.graphicsLayer {
                rotationZ = (p - 1f) * 180f
                alpha = p
                scaleX = 0.6f + p * 0.4f
                scaleY = 0.6f + p * 0.4f
            }
        )
    }
}

/** A cog that turns into place as it appears, like a gear catching. */
@Composable
fun Modifier.spinInOnAppear(fromDegrees: Float = -150f): Modifier {
    val turn = remember { Animatable(fromDegrees) }
    LaunchedEffect(Unit) {
        turn.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    return graphicsLayer { rotationZ = turn.value }
}

/** Grows out of a smaller, see-through self as it appears, with a little overshoot. */
@Composable
fun Modifier.popInOnAppear(): Modifier {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
    }
    return graphicsLayer {
        val g = grow.value
        alpha = g.coerceIn(0f, 1f)
        scaleX = 0.6f + 0.4f * g
        scaleY = 0.6f + 0.4f * g
    }
}

/**
 * Fades in and rises a few dp into place, [order] steps after the first one, so a list opens as a
 * quick cascade rather than all at once. The delay stops growing after a handful of rows: further
 * down, a row scrolled into view must not be kept waiting.
 */
@Composable
fun Modifier.riseInOnAppear(order: Int): Modifier {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        shown.animateTo(1f, tween(RISE_MS, delayMillis = order.coerceAtMost(RISE_MAX_STEPS) * RISE_STEP_MS, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = shown.value
        translationY = (1f - shown.value) * RISE_DP * density
    }
}

private const val RISE_MS = 260
private const val RISE_STEP_MS = 35
private const val RISE_MAX_STEPS = 6
private const val RISE_DP = 12f
private const val MORPH_MS = 420
