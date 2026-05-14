package com.CheckersVsChess.checkersvschess.model

// Вспомогательный класс для координат
data class Position(val row: Int, val col: Int)

// Модель хода
data class Move(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val captured: List<Position> = emptyList() // Список побитых фигур
) {
    val isCapture: Boolean get() = captured.isNotEmpty()
}