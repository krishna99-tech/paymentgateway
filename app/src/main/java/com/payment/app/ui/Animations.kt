package com.payment.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusAnimation(status: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "scale"
    )

    val color = when (status) {
        "SUCCESS" -> Color(0xFF4CAF50)
        "FAILED" -> Color(0xFFF44336)
        else -> Color(0xFFFFA000)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.size(100.dp).graphicsLayer(scaleX = scale, scaleY = scale)) {
            drawCircle(color = color.copy(alpha = 0.1f), radius = size.minDimension / 1.5f)
            drawCircle(color = color, radius = size.minDimension / 2.5f)
        }
        Text(
            text = if (status == "SUCCESS") "✓" else if (status == "FAILED") "✕" else "⏳",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyHistoryAnimation() {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "clock")
        val secondHandRotation by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "seconds"
        )
        
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawCircle(color = Color.LightGray.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
            }
            Box(modifier = Modifier.size(60.dp).rotate(secondHandRotation), contentAlignment = Alignment.TopCenter) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight(0.5f).background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(1.dp)))
            }
            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("No History Found", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Your transactions will appear here.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}
