package com.CheckersVsChess.checkersvschess.model

enum class PieceColor { BLACK, WHITE }

interface GamePiece {
    val color: PieceColor
}

data class CheckerPiece(
    override val color: PieceColor,
    var isKing: Boolean = false
) : GamePiece

enum class ChessPieceType(val penalty: Int) {
    PAWN(1),
    ROOK(10),
    KNIGHT(10),
    BISHOP(10),
    QUEEN(20),
    KING(0), // За короля штрафа нет, так как это конец игры
    MORPHING_BISHOP(10)
}

data class ChessPiece(
    override val color: PieceColor,
    val type: ChessPieceType,
    var hasMoved: Boolean = false // <--- Добавили память о ходах
) : GamePiece