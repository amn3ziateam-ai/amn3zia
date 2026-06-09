package com.amn3zia.app.ui.screens.fakeui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amn3zia.app.ui.theme.TgColors

private val BUTTONS = listOf(
    listOf("AC", "+/-", "%", "÷"),
    listOf("7",  "8",  "9",  "×"),
    listOf("4",  "5",  "6",  "−"),
    listOf("1",  "2",  "3",  "+"),
    listOf("",   "0",  ".",  "="),
)

/** Secret unlock sequence: default "1337=" — can be customised in settings */
private const val SECRET = "1337="

@Composable
fun CalculatorScreen(onUnlock: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var entry by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Text(
                text = display,
                color = Color.White,
                fontSize = if (display.length > 9) 44.sp else 72.sp,
                fontWeight = FontWeight.Thin,
                textAlign = TextAlign.End,
                maxLines = 2,
            )
        }

        // Buttons
        Column(
            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BUTTONS.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { key ->
                        val isWide = key == "0"
                        CalcButton(
                            label = key,
                            isWide = isWide,
                            modifier = Modifier.weight(if (isWide) 2f else 1f),
                            onClick = {
                                if (key.isEmpty()) return@CalcButton
                                entry += key
                                // Check secret
                                if (entry.endsWith(SECRET)) {
                                    onUnlock()
                                    return@CalcButton
                                }
                                // Basic display logic
                                when (key) {
                                    "AC" -> { display = "0"; entry = "" }
                                    "=" -> { display = evalSimple(entry.dropLast(1)); entry = "" }
                                    else -> { display = if (display == "0" && key.all { it.isDigit() }) key else display + key }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(label: String, isWide: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val isOp = label in listOf("÷", "×", "−", "+", "=")
    val isTop = label in listOf("AC", "+/-", "%")
    val bg = when {
        label.isEmpty() -> Color.Transparent
        isOp -> Color(0xFFFF9F0A)
        isTop -> Color(0xFF636366)
        else -> Color(0xFF1C1C1E)
    }
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(if (isWide) androidx.compose.foundation.shape.RoundedCornerShape(40.dp) else CircleShape)
            .background(bg)
            .clickable(enabled = label.isNotEmpty(), onClick = onClick),
        contentAlignment = Alignment.CenterStart.takeIf { isWide } ?: Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            modifier = if (isWide) Modifier.padding(start = 28.dp) else Modifier,
        )
    }
}

/** Very simplified expression evaluator for display realism */
private fun evalSimple(expr: String): String {
    return try {
        val ops = listOf("÷", "×", "−", "+")
        for (op in ops) {
            if (expr.contains(op)) {
                val parts = expr.split(op)
                val a = parts[0].toDouble()
                val b = parts[1].toDouble()
                val result = when (op) {
                    "÷" -> a / b
                    "×" -> a * b
                    "−" -> a - b
                    "+" -> a + b
                    else -> 0.0
                }
                return if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
            }
        }
        expr
    } catch (e: Exception) {
        "Error"
    }
}
