package com.luauai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * FlowRow simples — distribui filhos em linhas, quebrando quando necessário.
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: androidx.compose.foundation.layout.Arrangement.Horizontal =
        androidx.compose.foundation.layout.Arrangement.Start,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content  = content
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val gap = 8.dp.toPx().toInt()

        var x = 0; var y = 0; var rowHeight = 0
        val rows = mutableListOf<Pair<Int, Int>>() // (x, y) por placeable

        placeables.forEach { p ->
            if (x + p.width > constraints.maxWidth && x > 0) {
                x = 0
                y += rowHeight + gap
                rowHeight = 0
            }
            rows.add(x to y)
            x += p.width + gap
            if (p.height > rowHeight) rowHeight = p.height
        }

        val totalH = if (placeables.isEmpty()) 0 else y + rowHeight
        layout(constraints.maxWidth, totalH) {
            placeables.forEachIndexed { i, p -> p.placeRelative(rows[i].first, rows[i].second) }
        }
    }
}
