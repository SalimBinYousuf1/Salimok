package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    // Generate a deterministic 25x25 matrix based on content hash with standard QR finder patterns
    val matrix = remember(content) {
        generateQrMatrix(content, 25)
    }

    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 24.dp)) {
            val moduleCount = matrix.size
            val moduleSize = this.size.width / moduleCount

            for (row in 0 until moduleCount) {
                for (col in 0 until moduleCount) {
                    if (matrix[row][col]) {
                        drawRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(col * moduleSize, row * moduleSize),
                            size = Size(moduleSize, moduleSize)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(content: String, size: Int): Array<BooleanArray> {
    val grid = Array(size) { BooleanArray(size) { false } }

    // 1. Draw top-left finder pattern (7x7)
    drawFinderPattern(grid, 0, 0)
    // 2. Draw top-right finder pattern (7x7)
    drawFinderPattern(grid, 0, size - 7)
    // 3. Draw bottom-left finder pattern (7x7)
    drawFinderPattern(grid, size - 7, 0)

    // 4. Draw timing patterns
    for (i in 8 until size - 8) {
        grid[6][i] = (i % 2 == 0)
        grid[i][6] = (i % 2 == 0)
    }

    // 5. Fill remaining data modules with deterministic hash stream
    val md = MessageDigest.getInstance("SHA-256")
    var currentHash = md.digest(content.toByteArray(Charsets.UTF_8))
    var bitIndex = 0

    for (row in 0 until size) {
        for (col in 0 until size) {
            // Skip finder patterns and margins
            if (isProtectedArea(row, col, size)) continue

            val bytePos = (bitIndex / 8) % currentHash.size
            val bitPos = bitIndex % 8
            val bit = ((currentHash[bytePos].toInt() shr bitPos) and 1) == 1
            grid[row][col] = bit

            bitIndex++
            if (bitIndex % (currentHash.size * 8) == 0) {
                currentHash = md.digest(currentHash)
            }
        }
    }

    return grid
}

private fun drawFinderPattern(grid: Array<BooleanArray>, startRow: Int, startCol: Int) {
    for (r in 0 until 7) {
        for (c in 0 until 7) {
            val isBorder = r == 0 || r == 6 || c == 0 || c == 6
            val isCenter = r in 2..4 && c in 2..4
            grid[startRow + r][startCol + c] = isBorder || isCenter
        }
    }
}

private fun isProtectedArea(r: Int, c: Int, size: Int): Boolean {
    // Top-left
    if (r <= 7 && c <= 7) return true
    // Top-right
    if (r <= 7 && c >= size - 8) return true
    // Bottom-left
    if (r >= size - 8 && c <= 7) return true
    // Timing lines
    if (r == 6 || c == 6) return true
    return false
}
