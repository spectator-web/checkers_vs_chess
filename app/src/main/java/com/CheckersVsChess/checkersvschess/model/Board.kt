package com.CheckersVsChess.checkersvschess.model

import com.CheckersVsChess.checkersvschess.game.MoveValidator

class Board {
    // Двумерный массив 8x8. Разрешаем хранить любой GamePiece (для будущих шахмат)
    val grid: Array<Array<GamePiece?>> = Array(8) { Array(8) { null } }

    init {
        setupBlackPieces()
    }

    // Расставляем черные шашки на 2-ю и 3-ю горизонтали (индексы строк 5 и 6)
    private fun setupBlackPieces() {
        for (col in 0..7) {
            grid[5][col] = CheckerPiece(PieceColor.BLACK)
            grid[6][col] = CheckerPiece(PieceColor.BLACK)
        }
    }

    fun getPossibleMoves(row: Int, col: Int): List<Move> {
        return MoveValidator.getValidMovesForPiece(grid, row, col, PieceColor.BLACK)
    }

    fun makeMove(move: Move) {
        val piece = grid[move.fromRow][move.fromCol] as? CheckerPiece ?: return

        // 1. Перемещаем фигуру
        grid[move.toRow][move.toCol] = piece
        grid[move.fromRow][move.fromCol] = null

        // 2. Убираем побитые фигуры
        for (capturedPos in move.captured) {
            grid[capturedPos.row][capturedPos.col] = null
        }

        // 3. Превращение в дамку (черные двигаются вверх, к 0-й строке)
        if (!piece.isKing && move.toRow == 0) {
            piece.isKing = true
        }
    }
}