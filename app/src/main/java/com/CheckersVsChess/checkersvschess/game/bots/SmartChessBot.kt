package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.game.MoveValidator
import com.CheckersVsChess.checkersvschess.model.*

class SmartChessBot : Bot {

    private val SEARCH_DEPTH = 4
    private val WIN_SCORE = 99999999
    private val LOSE_SCORE = -99999999

    override fun getMove(board: Board, botColor: PieceColor): Move? {
        val moves = getWhiteMoves(board.grid)
        if (moves.isEmpty()) return null

        var bestMove: Move? = null
        var maxEval = Int.MIN_VALUE

        for (move in moves) {
            val simulatedBoard = simulateMove(board.grid, move)
            val eval = minimax(simulatedBoard, SEARCH_DEPTH - 1, Int.MIN_VALUE, Int.MAX_VALUE, false)

            if (eval > maxEval) {
                maxEval = eval
                bestMove = move
            }
        }

        return bestMove ?: moves.random()
    }

    private fun minimax(grid: Array<Array<GamePiece?>>, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        val eval = evaluateBoard(grid)
        if (Math.abs(eval) > 90000000) return eval // Досрочный выход при победе/поражении

        if (depth <= 0) {
            val captures = getCaptureMovesOnly(grid, isMaximizing)

            if (captures.isEmpty() || depth <= -6) {
                return eval
            }

            var a = alpha
            var b = beta

            if (isMaximizing) {
                var maxEval = eval
                for (move in captures) {
                    val newGrid = simulateMove(grid, move)
                    val currentEval = minimax(newGrid, depth - 1, a, b, false)
                    maxEval = maxOf(maxEval, currentEval)
                    a = maxOf(a, currentEval)
                    if (b <= a) break
                }
                return maxEval
            } else {
                var minEval = Int.MAX_VALUE
                for (move in captures) {
                    val newGrid = simulateMove(grid, move)
                    val currentEval = minimax(newGrid, depth - 1, a, b, true)
                    minEval = minOf(minEval, currentEval)
                    b = minOf(b, currentEval)
                    if (b <= a) break
                }
                return minEval
            }
        }

        var a = alpha
        var b = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            val moves = getWhiteMoves(grid)
            if (moves.isEmpty()) return LOSE_SCORE

            for (move in moves) {
                val newGrid = simulateMove(grid, move)
                val currentEval = minimax(newGrid, depth - 1, a, b, false)
                maxEval = maxOf(maxEval, currentEval)
                a = maxOf(a, currentEval)
                if (b <= a) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            val moves = getBlackMoves(grid)
            if (moves.isEmpty()) return LOSE_SCORE

            for (move in moves) {
                val newGrid = simulateMove(grid, move)
                val currentEval = minimax(newGrid, depth - 1, a, b, true)
                minEval = minOf(minEval, currentEval)
                b = minOf(b, currentEval)
                if (b <= a) break
            }
            return minEval
        }
    }

    private fun getDangerMap(grid: Array<Array<GamePiece?>>): HashSet<Position> {
        val dangerZone = HashSet<Position>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c]
                if (p is CheckerPiece && p.color == PieceColor.BLACK) {
                    val moves = MoveValidator.getValidMovesForPiece(grid, r, c, PieceColor.BLACK)
                    for (move in moves) {
                        if (move.isCapture) {
                            dangerZone.addAll(move.captured)
                        }
                    }
                }
            }
        }
        return dangerZone
    }

    private fun getCaptureMovesOnly(grid: Array<Array<GamePiece?>>, isWhite: Boolean): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c] ?: continue
                if (isWhite && p.color == PieceColor.WHITE && p is ChessPiece) {
                    val raw = MoveValidator.getMovesForChessPiece(grid, r, c).filter { it.isCapture }
                    moves.addAll(raw.filter { !MoveValidator.wouldMoveResultInCheck(grid, it, PieceColor.WHITE) })
                } else if (!isWhite && p.color == PieceColor.BLACK && p is CheckerPiece) {
                    val raw = MoveValidator.getValidMovesForPiece(grid, r, c, PieceColor.BLACK)
                    moves.addAll(raw.filter { it.isCapture })
                }
            }
        }
        return moves
    }

    private fun evaluateBoard(grid: Array<Array<GamePiece?>>): Int {
        var whiteKingAlive = false
        var blackPieces = 0
        var blackKings = 0
        var whiteMaterial = 0
        var blackMaterial = 0

        val dangerMap = getDangerMap(grid)

        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c] ?: continue
                val pos = Position(r, c)

                val isLightSquare = (r + c) % 2 == 0

                if (p.color == PieceColor.WHITE && p is ChessPiece) {

                    val baseValue = when (p.type) {
                        ChessPieceType.KING -> 10000000
                        ChessPieceType.PAWN -> 30000                 // Пешка дешевле шашки! Отдаем на размен.
                        ChessPieceType.KNIGHT -> 400000
                        ChessPieceType.BISHOP -> 400000
                        ChessPieceType.MORPHING_BISHOP -> 400000
                        ChessPieceType.ROOK -> 600000
                        ChessPieceType.QUEEN -> 1200000
                    }

                    if (p.type == ChessPieceType.KING) whiteKingAlive = true

                    var pieceEval = baseValue

                    // Умеренный бонус за бункер. Не мешает атаковать.
                    if (isLightSquare) {
                        pieceEval += 5000
                        if (p.type == ChessPieceType.KING) pieceEval += 20000 // Король предпочитает белые клетки
                    }

                    // Штрафы за угрозу
                    if (dangerMap.contains(pos)) {
                        if (p.type == ChessPieceType.KING) {
                            pieceEval -= 20000000 // Шах = поражение. Бежим любой ценой.
                        } else {
                            pieceEval -= (baseValue - 500) // Фигура списана со счетов
                        }
                    }

                    if (p.type == ChessPieceType.PAWN) {
                        pieceEval += (r * 2500) // Жесткий стимул бежать в Ферзи
                    }

                    whiteMaterial += pieceEval

                } else if (p.color == PieceColor.BLACK && p is CheckerPiece) {
                    blackPieces++
                    if (p.isKing) {
                        blackKings++
                        blackMaterial += 150000 // Дамка
                    } else {
                        var checkerValue = 45000 // Обычная шашка дороже пешки (30к). Размен выгоден Белым.

                        when (r) {
                            1 -> checkerValue += 45000 // Пред-дамка (90к). Разменяем 2 пешки, но не Коня!
                            2 -> checkerValue += 20000
                            3 -> checkerValue += 10000
                            else -> checkerValue += ((7 - r) * 1000)
                        }

                        // Структура шашек
                        var hasSupport = false
                        if (r < 7) {
                            if (c > 0 && grid[r + 1][c - 1]?.color == PieceColor.BLACK) hasSupport = true
                            if (c < 7 && grid[r + 1][c + 1]?.color == PieceColor.BLACK) hasSupport = true
                        }

                        if (hasSupport) {
                            checkerValue += 5000
                        }

                        blackMaterial += checkerValue
                    }
                }
            }
        }

        if (!whiteKingAlive) return LOSE_SCORE
        if (blackKings >= 2) return LOSE_SCORE
        if (blackPieces == 0) return WIN_SCORE

        return (whiteMaterial - blackMaterial)
    }

    private fun getWhiteMoves(grid: Array<Array<GamePiece?>>): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c]
                if (p is ChessPiece && p.color == PieceColor.WHITE) {
                    val raw = MoveValidator.getMovesForChessPiece(grid, r, c)
                    moves.addAll(raw.filter { !MoveValidator.wouldMoveResultInCheck(grid, it, PieceColor.WHITE) })
                }
            }
        }
        return moves
    }

    private fun getBlackMoves(grid: Array<Array<GamePiece?>>): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c]
                if (p is CheckerPiece && p.color == PieceColor.BLACK) {
                    moves.addAll(MoveValidator.getValidMovesForPiece(grid, r, c, PieceColor.BLACK))
                }
            }
        }
        return moves
    }

    private fun simulateMove(grid: Array<Array<GamePiece?>>, move: Move): Array<Array<GamePiece?>> {
        val newGrid = Array(8) { r -> Array(8) { c ->
            when (val p = grid[r][c]) {
                is CheckerPiece -> p.copy()
                is ChessPiece -> p.copy()
                else -> null
            }
        }}

        val piece = newGrid[move.fromRow][move.fromCol]
        for (cap in move.captured) {
            newGrid[cap.row][cap.col] = null
        }
        newGrid[move.toRow][move.toCol] = piece
        newGrid[move.fromRow][move.fromCol] = null

        if (piece is ChessPiece && piece.type == ChessPieceType.PAWN && move.toRow == 7) {
            newGrid[move.toRow][move.toCol] = ChessPiece(PieceColor.WHITE, ChessPieceType.QUEEN)
        }

        if (piece is CheckerPiece && !piece.isKing) {
            if (move.toRow == 0 || move.captured.any { it.row == 1 }) {
                (newGrid[move.toRow][move.toCol] as CheckerPiece).isKing = true
            }
        }

        return newGrid
    }
}
