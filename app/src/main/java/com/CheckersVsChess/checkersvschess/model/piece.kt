package com.CheckersVsChess.checkersvschess.model

// Базовые перечисления и интерфейсы
enum class PieceColor { BLACK, WHITE }

interface GamePiece {
    val color: PieceColor
}

// Сама шашка
data class CheckerPiece(
    override val color: PieceColor,
    var isKing: Boolean = false
) : GamePiece