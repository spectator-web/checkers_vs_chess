package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.game.MoveValidator
import com.CheckersVsChess.checkersvschess.model.*

class PrimitiveBot : Bot { // <-- Теперь это класс, который реализует интерфейс Bot!

    override fun getMove(board: Board, botColor: PieceColor): Move? {
        val allValidMoves = mutableListOf<Move>()

        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board.grid[row][col]
                if (piece != null && piece.color == botColor) {
                    val rawMoves = when (piece) {
                        is CheckerPiece -> MoveValidator.getValidMovesForPiece(board.grid, row, col, botColor)
                        is ChessPiece -> MoveValidator.getMovesForChessPiece(board.grid, row, col)
                        else -> emptyList()
                    }

                    val safeMoves = if (botColor == PieceColor.WHITE) {
                        rawMoves.filter { move -> !MoveValidator.wouldMoveResultInCheck(board.grid, move, PieceColor.WHITE) }
                    } else {
                        rawMoves
                    }
                    allValidMoves.addAll(safeMoves)
                }
            }
        }

        if (allValidMoves.isEmpty()) return null

        val captures = allValidMoves.filter { it.isCapture }
        if (captures.isNotEmpty()) return captures.random()

        return allValidMoves.random()
    }
}