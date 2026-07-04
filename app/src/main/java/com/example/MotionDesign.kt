package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle

/**
 * A custom modifier that provides elegant, non-bouncy card elevation,
 * translating down slightly and scaling on click/press to create realistic depth.
 */
@Composable
fun Modifier.premiumCardElevation(
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translationY by animateFloatAsState(
        targetValue = if (isPressed) 3f else 0f,
        animationSpec = tween(durationMillis = 250, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
        label = "PremiumCardTranslationY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 250, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
        label = "PremiumCardScale"
    )

    return this
        .graphicsLayer {
            this.translationY = translationY
            this.scaleX = scale
            this.scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null, // Custom visual feedback through our elevation animation
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

/**
 * Animated Integer Counter for smooth number transitions.
 */
@Composable
fun AnimatedIntText(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = ""
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
        label = "AnimatedInt"
    )
    Text(
        text = "$prefix${animatedValue.toInt()}$suffix",
        style = style,
        modifier = modifier
    )
}

/**
 * Animated Currency Text for beautiful cost transitions.
 */
@Composable
fun AnimatedCostText(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
        label = "AnimatedCost"
    )
    val formatted = remember(animatedValue) {
        formatCost(animatedValue.toInt())
    }
    Text(
        text = formatted,
        style = style,
        modifier = modifier
    )
}
