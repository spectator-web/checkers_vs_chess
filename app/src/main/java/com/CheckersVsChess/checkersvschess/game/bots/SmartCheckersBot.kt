package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.game.MoveValidator
import com.CheckersVsChess.checkersvschess.model.*

class SmartCheckersBot : Bot {

    override fun getMove(board: Board, botColor: PieceColor): Move? {
        val allValidMoves = getAllValidMoves(board, botColor)
        if (allValidMoves.isEmpty()) return null

        // Выбираем ход с максимальной оценкой
        // maxByOrNull автоматически выберет самый выгодный захват (с наибольшим количеством жертв)
        return allValidMoves.maxByOrNull { evaluateMove(board, it, botColor) }
    }

    private fun evaluateMove(board: Board, move: Move, color: PieceColor): Int {
        var score = 0
        val toRow = move.toRow
        val toCol = move.toCol
        val fromRow = move.fromRow

        // --- 1. ПРИОРИТЕТ ВЗЯТИЯ ---
        if (move.isCapture) {
            // Чем больше фигур съели за раз (multi-jump), тем выше балл
            score += 1000 * move.captured.size

            for (pos in move.captured) {
                val p = board.grid[pos.row][pos.col]
                if (p is ChessPiece) {
                    score += p.type.penalty * 50 // Охота на Ферзей и Ладей
                }
            }
        }

        // --- 2. СТРАХ СМЕРТИ (ЗОНА БОЯ) ---
        // Если клетка, куда мы идем, простреливается шахматами — это почти табу
        if (isSquareUnderChessAttack(board.grid, toRow, toCol, color)) {
            score -= 1500
        }

        // --- 3. ЗАЩИТА БАЗЫ (ПОСЛЕДНЯЯ ЛИНИЯ) ---
        // Штраф за уход с r=0 для черных (не пускаем пешки в дамки)
        if (color == PieceColor.BLACK && fromRow == 0 && toRow != 0) {
            score -= 100
        }

        // --- 4. ПОДДЕРЖКА СТРОЯ ---
        if (hasSupport(board.grid, toRow, toCol, color)) {
            score += 50
        }

        // --- 5. ЦЕНТРАЛИЗАЦИЯ И БОРТА ---
        if (toCol in 2..5) {
            score += 30 // Центр — это хорошо
        }
        if (toCol == 0 || toCol == 7) {
            score -= 40 // Борта — это опасно (твоё требование)
        }

        // --- 6. ПРОДВИЖЕНИЕ К ДАМКЕ ---
        score += toRow * 10

        return score
    }

    // Проверка: бьют ли шахматы эту клетку?
    private fun isSquareUnderChessAttack(grid: Array<Array<GamePiece?>>, r: Int, c: Int, myColor: PieceColor): Boolean {
        val enemyColor = if (myColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        for (row in 0..7) {
            for (col in 0..7) {
                val p = grid[row][col]
                if (p != null && p.color == enemyColor && p is ChessPiece) {
                    val moves = MoveValidator.getMovesForChessPiece(grid, row, col)
                    if (moves.any { it.toRow == r && it.toCol == c }) return true
                }
            }
        }
        return false
    }

    private fun hasSupport(grid: Array<Array<GamePiece?>>, r: Int, c: Int, color: PieceColor): Boolean {
        val backRow = if (color == PieceColor.BLACK) r - 1 else r + 1
        if (backRow !in 0..7) return false

        // Если сзади нас по диагонали есть своя шашка — нас нельзя срубить
        val left = c - 1
        val right = c + 1
        if (left >= 0 && grid[backRow][left]?.color == color) return true
        if (right <= 7 && grid[backRow][right]?.color == color) return true
        return false
    }

    private fun getAllValidMoves(board: Board, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board.grid[r][c]
                if (p != null && p.color == color) {
                    moves.addAll(if (p is CheckerPiece) {
                        MoveValidator.getValidMovesForPiece(board.grid, r, c, color)
                    } else {
                        MoveValidator.getMovesForChessPiece(board.grid, r, c).filter {
                            !MoveValidator.wouldMoveResultInCheck(board.grid, it, color)
                        }
                    })
                }
            }
        }
        return moves
    }
}