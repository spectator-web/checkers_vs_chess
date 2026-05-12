package com.CheckersVsChess.checkersvschess.game

import com.CheckersVsChess.checkersvschess.model.*

object PrimitiveBot {

    // Бот анализирует доску и возвращает ОДИН ход
    fun getRandomMove(board: Board, botColor: PieceColor): Move? {
        val allValidMoves = mutableListOf<Move>()

        // 1. Собираем АБСОЛЮТНО ВСЕ возможные ходы для всех фигур бота
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board.grid[row][col]
                if (piece != null && piece.color == botColor) {
                    val rawMoves = when (piece) {
                        is CheckerPiece -> MoveValidator.getValidMovesForPiece(board.grid, row, col, botColor)
                        is ChessPiece -> MoveValidator.getMovesForChessPiece(board.grid, row, col)
                        else -> emptyList()
                    }

                    // 2. Обязательно фильтруем самоубийственные ходы, если бот играет за Шахматы
                    val safeMoves = if (botColor == PieceColor.WHITE) {
                        rawMoves.filter { move -> !MoveValidator.wouldMoveResultInCheck(board.grid, move, PieceColor.WHITE) }
                    } else {
                        rawMoves
                    }
                    allValidMoves.addAll(safeMoves)
                }
            }
        }

        if (allValidMoves.isEmpty()) return null // Бот проиграл (нет ходов)

        // 3. Логика "Тупого, но жадного": если есть кого съесть — бьем обязательно!
        val captures = allValidMoves.filter { it.isCapture }
        if (captures.isNotEmpty()) {
            return captures.random()
        }

        // 4. Если рубить некого, делаем абсолютно случайный ход
        return allValidMoves.random()
    }
}