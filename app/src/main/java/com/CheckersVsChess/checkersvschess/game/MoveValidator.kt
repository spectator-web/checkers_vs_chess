package com.CheckersVsChess.checkersvschess.game

import com.CheckersVsChess.checkersvschess.model.CheckerPiece
import com.CheckersVsChess.checkersvschess.model.GamePiece
import com.CheckersVsChess.checkersvschess.model.Move
import com.CheckersVsChess.checkersvschess.model.PieceColor
import com.CheckersVsChess.checkersvschess.model.Position

object MoveValidator {

    fun getValidMovesForPiece(
        board: Array<Array<GamePiece?>>,
        row: Int,
        col: Int,
        color: PieceColor
    ): List<Move> {
        val piece = board[row][col] as? CheckerPiece ?: return emptyList()
        if (piece.color != color) return emptyList()

        val allCapturesOnBoard = hasAnyCaptures(board, color)
        val capturesForThisPiece = getCaptureMoves(board, row, col, piece)

        // Правило обязательного взятия
        if (allCapturesOnBoard) {
            return capturesForThisPiece
        }

        return getSimpleMoves(board, row, col, piece)
    }

    private fun hasAnyCaptures(board: Array<Array<GamePiece?>>, color: PieceColor): Boolean {
        for (r in board.indices) {
            for (c in board[r].indices) {
                val piece = board[r][c] as? CheckerPiece
                if (piece != null && piece.color == color) {
                    if (getCaptureMoves(board, r, c, piece).isNotEmpty()) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getSimpleMoves(
        board: Array<Array<GamePiece?>>,
        row: Int, col: Int, piece: CheckerPiece
    ): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = if (piece.isKing) {
            listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        } else {
            listOf(Pair(-1, -1), Pair(-1, 1)) // Простые шашки ходят только вперед
        }

        for ((dRow, dCol) in directions) {
            var r = row + dRow
            var c = col + dCol

            if (piece.isKing) {
                while (isValidPos(r, c) && board[r][c] == null) {
                    moves.add(Move(row, col, r, c))
                    r += dRow
                    c += dCol
                }
            } else {
                if (isValidPos(r, c) && board[r][c] == null) {
                    moves.add(Move(row, col, r, c))
                }
            }
        }
        return moves
    }

    private fun getCaptureMoves(
        board: Array<Array<GamePiece?>>,
        row: Int, col: Int, piece: CheckerPiece,
        capturedSoFar: List<Position> = emptyList()
    ): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))

        for ((dRow, dCol) in directions) {
            if (piece.isKing) {
                var r = row + dRow
                var c = col + dCol
                var foundEnemy: Position? = null

                while (isValidPos(r, c)) {
                    val target = board[r][c]
                    if (target != null) {
                        if (target.color == piece.color || capturedSoFar.contains(Position(r, c))) break
                        foundEnemy = Position(r, c)
                        break
                    }
                    r += dRow
                    c += dCol
                }

                if (foundEnemy != null) {
                    r = foundEnemy.row + dRow
                    c = foundEnemy.col + dCol
                    while (isValidPos(r, c) && board[r][c] == null) {
                        val newCaptured = capturedSoFar + foundEnemy
                        moves.add(Move(row, col, r, c, newCaptured))
                        r += dRow
                        c += dCol
                    }
                }
            } else {
                val enemyRow = row + dRow
                val enemyCol = col + dCol
                val landRow = row + dRow * 2
                val landCol = col + dCol * 2

                if (isValidPos(landRow, landCol)) {
                    val enemyPos = Position(enemyRow, enemyCol)
                    val target = board[enemyRow][enemyCol]

                    val isEnemy = target != null && target.color != piece.color
                    val isLandingEmpty = board[landRow][landCol] == null
                    val notYetCaptured = !capturedSoFar.contains(enemyPos)

                    if (isEnemy && isLandingEmpty && notYetCaptured) {
                        val newCaptured = capturedSoFar + enemyPos
                        moves.add(Move(row, col, landRow, landCol, newCaptured))

                        val chainCaptures = getCaptureMoves(board, landRow, landCol, piece, newCaptured)
                        if (chainCaptures.isNotEmpty()) {
                            moves.removeAt(moves.size - 1)
                            moves.addAll(chainCaptures.map { it.copy(fromRow = row, fromCol = col) })
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun isValidPos(r: Int, c: Int) = r in 0..7 && c in 0..7
}