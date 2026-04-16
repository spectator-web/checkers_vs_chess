package com.CheckersVsChess.checkersvschess.game

// Звездочка на конце означает "импортировать ВСЕ классы из папки model".
// Это навсегда избавит нас от ошибок Unresolved reference.
import com.CheckersVsChess.checkersvschess.model.*

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

    fun hasAnyCaptures(board: Array<Array<GamePiece?>>, color: PieceColor): Boolean {
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
            } else { // Для простой шашки
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

                        // РУССКИЕ ШАШКИ: Проверяем, стала ли шашка дамкой прямо во время прыжка
                        val simulatedPiece = if (landRow == 0 && piece.color == PieceColor.BLACK) {
                            CheckerPiece(piece.color, isKing = true) // Надели корону!
                        } else {
                            piece
                        }

                        // Продолжаем просчет комбо с новой (или старой) фигурой
                        val chainCaptures =
                            getCaptureMoves(board, landRow, landCol, simulatedPiece, newCaptured)
                        if (chainCaptures.isNotEmpty()) {
                            moves.removeAt(moves.size - 1)
                            moves.addAll(chainCaptures.map {
                                it.copy(
                                    fromRow = row,
                                    fromCol = col
                                )
                            })
                        }
                    }
                }
            }
        }

        return moves
    }

    // --- ФУНКЦИИ ДЛЯ ШАХМАТ ---

    fun getMovesForMorphingBishop(
        board: Array<Array<GamePiece?>>,
        row: Int,
        col: Int,
        piece: ChessPiece
    ): List<Move> {
        return when (col) {
            0, 7 -> getRookMoves(board, row, col, piece.color)     // Вертикали A и H
            1, 6 -> getKnightMoves(board, row, col, piece.color)   // Вертикали B и G
            else -> getBishopMoves(board, row, col, piece.color)   // Вертикали C, D, E, F
        }
    }

    private fun getRookMoves(board: Array<Array<GamePiece?>>, row: Int, col: Int, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)) // Вверх, вниз, влево, вправо

        for ((dr, dc) in directions) {
            var r = row + dr
            var c = col + dc
            // Исправлено: используем isValidPos
            while (isValidPos(r, c)) {
                val target = board[r][c]
                if (target == null) {
                    moves.add(Move(row, col, r, c))
                } else {
                    if (target.color != color) moves.add(Move(row, col, r, c, listOf(Position(r, c)))) // Взятие
                    break // Дальше прыгать нельзя
                }
                r += dr
                c += dc
            }
        }
        return moves
    }

    private fun getKnightMoves(board: Array<Array<GamePiece?>>, row: Int, col: Int, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        // Все 8 возможных прыжков коня буквой "Г"
        val knightJumps = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )

        for ((dr, dc) in knightJumps) {
            val r = row + dr
            val c = col + dc
            // Исправлено: используем isValidPos
            if (isValidPos(r, c)) {
                val target = board[r][c]
                if (target == null) {
                    moves.add(Move(row, col, r, c))
                } else if (target.color != color) {
                    moves.add(Move(row, col, r, c, listOf(Position(r, c)))) // Взятие
                }
            }
        }
        return moves
    }

    private fun getBishopMoves(board: Array<Array<GamePiece?>>, row: Int, col: Int, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))

        for ((dr, dc) in directions) {
            var r = row + dr
            var c = col + dc
            // Исправлено: используем isValidPos
            while (isValidPos(r, c)) {
                val target = board[r][c]
                if (target == null) {
                    moves.add(Move(row, col, r, c))
                } else {
                    if (target.color != color) moves.add(Move(row, col, r, c, listOf(Position(r, c))))
                    break
                }
                r += dr
                c += dc
            }
        }
        return moves
    }
    // Добавь эти методы в MoveValidator.kt

    fun getMovesForChessPiece(board: Array<Array<GamePiece?>>, row: Int, col: Int): List<Move> {
        val piece = board[row][col] as? ChessPiece ?: return emptyList()
        return when (piece.type) {
            ChessPieceType.PAWN -> getPawnMoves(board, row, col, piece.color)
            ChessPieceType.ROOK -> getRookMoves(board, row, col, piece.color)
            ChessPieceType.KNIGHT -> getKnightMoves(board, row, col, piece.color)
            ChessPieceType.BISHOP -> getBishopMoves(board, row, col, piece.color)
            ChessPieceType.QUEEN -> getRookMoves(board, row, col, piece.color) + getBishopMoves(board, row, col, piece.color)
            ChessPieceType.KING -> getKingMoves(board, row, col, piece.color)
            ChessPieceType.MORPHING_BISHOP -> getMovesForMorphingBishop(board, row, col, piece)
        }
    }

    private fun getPawnMoves(board: Array<Array<GamePiece?>>, row: Int, col: Int, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        val direction = 1 // Белые ходят вниз (0 -> 7)

        // Ход вперед
        if (isValidPos(row + direction, col) && board[row + direction][col] == null) {
            moves.add(Move(row, col, row + direction, col))
            // Первый ход на 2 клетки
            if (row == 1 && board[row + direction * 2][col] == null) {
                moves.add(Move(row, col, row + direction * 2, col))
            }
        }
        // Взятие по диагонали
        for (dc in listOf(-1, 1)) {
            val nr = row + direction
            val nc = col + dc
            if (isValidPos(nr, nc)) {
                val target = board[nr][nc]
                if (target != null && target.color != color) {
                    moves.add(Move(row, col, nr, nc, listOf(Position(nr, nc))))
                }
            }
        }
        return moves
    }

    private fun getKingMoves(board: Array<Array<GamePiece?>>, row: Int, col: Int, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        val piece = board[row][col] as? ChessPiece ?: return emptyList()

        // Обычные ходы короля (на 1 клетку вокруг)
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = row + dr
                val nc = col + dc
                if (isValidPos(nr, nc)) {
                    val target = board[nr][nc]
                    if (target == null || target.color != color) {
                        val captured = if (target != null) listOf(Position(nr, nc)) else emptyList()
                        moves.add(Move(row, col, nr, nc, captured))
                    }
                }
            }
        }

        // РОКИРОВКА (Только если король еще не ходил и не под шахом)
        if (!piece.hasMoved && !isKingInCheck(board, color)) {
            // Длинная рокировка (влево к колонке 0)
            val leftRook = board[row][0] as? ChessPiece
            if (leftRook != null && leftRook.type == ChessPieceType.ROOK && !leftRook.hasMoved) {
                if (board[row][1] == null && board[row][2] == null) {
                    moves.add(Move(row, col, row, 1)) // Король прыгает на 1
                }
            }
            // Короткая рокировка (вправо к колонке 7)
            val rightRook = board[row][7] as? ChessPiece
            if (rightRook != null && rightRook.type == ChessPieceType.ROOK && !rightRook.hasMoved) {
                if (board[row][4] == null && board[row][5] == null && board[row][6] == null) {
                    moves.add(Move(row, col, row, 5)) // Король прыгает на 5
                }
            }
        }
        return moves
    }
    // Добавь это в объект MoveValidator в MoveValidator.kt

    fun isKingInCheck(board: Array<Array<GamePiece?>>, color: PieceColor): Boolean {
        var kingPos: Position? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p is ChessPiece && p.type == ChessPieceType.KING && p.color == color) {
                    kingPos = Position(r, c)
                    break
                }
            }
        }
        if (kingPos == null) return false

        // Ищем, есть ли у какой-нибудь черной шашки ход, который "съедает" позицию короля
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p is CheckerPiece && p.color != color) {
                    val captures = getCaptureMoves(board, r, c, p)
                    if (captures.any { move -> move.captured.contains(kingPos) }) {
                        return true
                    }
                }
            }
        }
        return false
    }

    // 2. Симуляция: приведет ли этот ход к шаху нашему королю?
    fun wouldMoveResultInCheck(board: Array<Array<GamePiece?>>, move: Move, color: PieceColor): Boolean {
        // Создаем временную копию доски
        val tempBoard = Array(8) { r -> Array(8) { c -> board[r][c] } }

        // Делаем виртуальный ход
        val piece = tempBoard[move.fromRow][move.fromCol]
        for (cap in move.captured) {
            tempBoard[cap.row][cap.col] = null
        }
        tempBoard[move.toRow][move.toCol] = piece
        tempBoard[move.fromRow][move.fromCol] = null

        // Смотрим, не убивают ли нашего короля в этой альтернативной реальности
        return isKingInCheck(tempBoard, color)
    }
    private fun isValidPos(r: Int, c: Int) = r in 0..7 && c in 0..7
}