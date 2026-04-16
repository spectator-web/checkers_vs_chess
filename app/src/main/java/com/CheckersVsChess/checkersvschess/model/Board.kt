package com.CheckersVsChess.checkersvschess.model

import com.CheckersVsChess.checkersvschess.game.MoveValidator

class Board {
    val grid: Array<Array<GamePiece?>> = Array(8) { Array(8) { null } }
    var lastMove: Move? = null
    init {
        setupBlackPieces() // Игрок (Шашки) – чёрные
        setupWhitePieces() // Бот (Шахматы) – белые
    }

    private fun setupBlackPieces() {
        // Черные шашки стоят на 5, 6 и 7 строках, ТОЛЬКО на темных клетках
        for (row in 5..7) {
            for (col in 0..7) {
                if ((row + col) % 2 != 0) {
                    grid[row][col] = CheckerPiece(PieceColor.BLACK)
                }
            }
        }
    }

    private fun setupWhitePieces() {
        // Белые шахматные пешки на 1 строке
        for (col in 0..7) {
            grid[1][col] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)
        }

        // Белые шахматные фигуры на 0 строке
        grid[0][0] = ChessPiece(PieceColor.WHITE, ChessPieceType.ROOK)
        grid[0][1] = ChessPiece(PieceColor.WHITE, ChessPieceType.KNIGHT)
        // Слон-хамелеон на вертикалях C(2) и F(5)
        grid[0][2] = ChessPiece(PieceColor.WHITE, ChessPieceType.MORPHING_BISHOP)
        grid[0][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.QUEEN)
        grid[0][3] = ChessPiece(PieceColor.WHITE, ChessPieceType.KING)
        grid[0][5] = ChessPiece(PieceColor.WHITE, ChessPieceType.MORPHING_BISHOP)
        grid[0][6] = ChessPiece(PieceColor.WHITE, ChessPieceType.KNIGHT)
        grid[0][7] = ChessPiece(PieceColor.WHITE, ChessPieceType.ROOK)
    }

    /**
     * Возвращает все допустимые ходы для чёрной шашки (игрока) на указанной клетке.
     */
    fun getPossibleMoves(row: Int, col: Int): List<Move> {
        return MoveValidator.getValidMovesForPiece(grid, row, col, PieceColor.BLACK)
    }

    /**
     * Выполняет ход на доске.
     * Исправлено: сначала удаляются побитые фигуры, затем перемещается атакующая.
     */
    fun makeMove(move: Move) {
        val piece = grid[move.fromRow][move.fromCol] ?: return

        for (capturedPos in move.captured) {
            grid[capturedPos.row][capturedPos.col] = null
        }

        grid[move.toRow][move.toCol] = piece
        grid[move.fromRow][move.fromCol] = null

        // Обработка рокировки и статуса hasMoved
        if (piece is ChessPiece) {
            if (piece.type == ChessPieceType.KING && kotlin.math.abs(move.toCol - move.fromCol) == 2) {
                if (move.toCol == 1) { // Длинная
                    val rook = grid[move.fromRow][0]
                    grid[move.fromRow][2] = rook
                    grid[move.fromRow][0] = null
                    if (rook is ChessPiece) rook.hasMoved = true
                } else if (move.toCol == 5) { // Короткая
                    val rook = grid[move.fromRow][7]
                    grid[move.fromRow][4] = rook
                    grid[move.fromRow][7] = null
                    if (rook is ChessPiece) rook.hasMoved = true
                }
            }
            piece.hasMoved = true
        }

        if (piece is CheckerPiece && !piece.isKing) {
            if (move.toRow == 0 || move.captured.any { it.row == 1 }) {
                piece.isKing = true
            }
        }
        lastMove = move
    }

    /**
     * Проверяет условия победы.
     * Возвращает строку с сообщением о победителе или null, если игра продолжается.
     */
    fun checkWinConditions(chessTime: Int, checkersTime: Int): String? {
        if (chessTime <= 0) return "Шашки победили по времени!"
        if (checkersTime <= 0) return "Шахматы победили по времени!"

        var whitePieces = 0
        var blackPieces = 0
        var blackKings = 0
        var kingFound = false

        for (row in 0..7) {
            for (col in 0..7) {
                val p = grid[row][col] ?: continue
                when (p.color) {
                    PieceColor.WHITE -> {
                        whitePieces++
                        if (p is ChessPiece && p.type == ChessPieceType.KING) kingFound = true
                    }
                    PieceColor.BLACK -> {
                        blackPieces++
                        if (p is CheckerPiece && p.isKing) blackKings++
                    }
                }
            }
        }

        if (blackPieces == 0) return "Шахматы уничтожили все шашки!"
        if (blackKings >= 2) return "Шашки победили (2 дамки прошли)!"
        if (!kingFound) return "Шашки съели короля!"

        return null
    }
}