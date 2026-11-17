package com.example.camel.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun CamelBootSplash() {
    var targetScale by remember { mutableFloatStateOf(0.62f) }
    val logoScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
        label = "camel_boot_logo_scale"
    )
    LaunchedEffect(Unit) {
        delay(120)
        targetScale = 1.12f
        delay(1900)
        targetScale = 1f
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF050916)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_camel_boot),
                contentDescription = "Camel OS",
                modifier = Modifier.size(190.dp).scale(logoScale)
            )
            Spacer(Modifier.height(26.dp))
            Text(
                text = "CAMEL OS",
                color = Color(0xFFE0BD55),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "One System. One Ecosystem. Unlimited Possibilities.",
                color = Color(0xFFB7C0D4),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
