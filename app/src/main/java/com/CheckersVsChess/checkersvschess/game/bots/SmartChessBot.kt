package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.game.MoveValidator
import com.CheckersVsChess.checkersvschess.model.*

class SmartChessBot : Bot {

    private val SEARCH_DEPTH = 4

    override fun getMove(board: Board, botColor: PieceColor): Move? {
        val moves = getWhiteMoves(board.grid)
        if (moves.isEmpty()) return null

        var bestMove: Move? = null
        var maxEval = Int.MIN_VALUE

        for (move in moves) {
            val simulatedBoard = simulateMove(board.grid, move)
            val eval = minimax(simulatedBoard, SEARCH_DEPTH - 1, Int.MIN_VALUE, Int.MAX_VALUE, false)

            // Убрал рандомный шум для тестов, чтобы бот играл максимально жестко и предсказуемо
            if (eval > maxEval) {
                maxEval = eval
                bestMove = move
            }
        }

        return bestMove ?: moves.random()
    }

    private fun minimax(grid: Array<Array<GamePiece?>>, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        val eval = evaluateBoard(grid)
        if (Math.abs(eval) > 9000000) return eval

        if (depth <= 0) {
            val captures = getCaptureMovesOnly(grid, isMaximizing)

            if (captures.isEmpty() || depth <= -2) {
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
            if (moves.isEmpty()) return -9999999

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
            if (moves.isEmpty()) return -9999999

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

    // --- ИСПРАВЛЕННАЯ ОЦЕНКА ПОЗИЦИИ ---
    private fun evaluateBoard(grid: Array<Array<GamePiece?>>): Int {
        var whiteKingAlive = false // ИСПРАВЛЕНИЕ: Теперь мы реально ищем короля!
        var blackPieces = 0
        var blackKings = 0
        var whiteMaterial = 0
        var blackMaterial = 0

        val dangerMap = getDangerMap(grid)

        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c] ?: continue

                if (p.color == PieceColor.WHITE && p is ChessPiece) {
                    if (p.type == ChessPieceType.KING) whiteKingAlive = true // <-- ИСПРАВЛЕНО!

                    val baseValue = when (p.type) {
                        ChessPieceType.PAWN -> 100
                        ChessPieceType.KNIGHT, ChessPieceType.BISHOP, ChessPieceType.MORPHING_BISHOP -> 3000
                        ChessPieceType.ROOK -> 5000 // Ладья ценнее легких фигур
                        ChessPieceType.QUEEN -> 9000
                        ChessPieceType.KING -> 0
                    }

                    var pieceEval = baseValue

                    // 1. АБСОЛЮТНАЯ БЕЗОПАСНОСТЬ (Светлые поля и края доски)
                    val isLightSquare = (r + c) % 2 == 0
                    val isEdgeSquare = (c == 0 || c == 7)

                    if (isLightSquare || isEdgeSquare) {
                        pieceEval += 150 // Колоссальный бонус, бот будет стягивать фигуры сюда
                    } else {
                        // 2. ЗОНА СМЕРТИ (Темные поля в центре)
                        // Шашки ходят только тут. Любая фигура здесь — потенциальный труп.
                        if (p.type != ChessPieceType.PAWN && p.type != ChessPieceType.KING) {
                            pieceEval -= 150 // Огромный штраф, заставляющий бежать с этих клеток
                        }
                    }

                    // 3. РАДАР УГРОЗ
                    val pos = Position(r, c)
                    if (dangerMap.contains(pos)) {
                        // ИСПРАВЛЕНО: Штраф равен (стоимость фигуры - 100).
                        // Это значит: "Фигура почти мертва (осталось 100 очков), БЕГИ!". 
                        // Бот предпочтет сбежать (вернув полные 9000 очков), чем быть съеденным (0 очков).
                        pieceEval -= (baseValue - 100)
                    }

                    if (p.type == ChessPieceType.PAWN) {
                        pieceEval += (r * 20)
                    }

                    whiteMaterial += pieceEval

                } else if (p.color == PieceColor.BLACK && p is CheckerPiece) {
                    blackPieces++
                    if (p.isKing) {
                        blackKings++
                        blackMaterial += 6000 // Дамка — это смерть для шахмат. Огромный вес!
                    } else {
                        var checkerValue = 300

                        // === ИСКЛЮЧЕНИЕ: УГРОЗА ДАМКИ ===
                        // Чем ближе к дамкам (row 0), тем опаснее шашка.
                        when (r) {
                            1 -> checkerValue += 3500 // В шаге от дамки! Ради её убийства бот ПОЖЕРТВУЕТ конем или слоном (3000)
                            2 -> checkerValue += 1000 // Очень опасно
                            3 -> checkerValue += 400
                            else -> checkerValue += ((7 - r) * 30)
                        }

                        // Структура шашек
                        var hasSupport = false
                        if (r < 7) {
                            if (c > 0 && grid[r + 1][c - 1]?.color == PieceColor.BLACK) hasSupport = true
                            if (c < 7 && grid[r + 1][c + 1]?.color == PieceColor.BLACK) hasSupport = true
                        }

                        if (hasSupport) {
                            checkerValue += 150 // Стена
                        } else {
                            checkerValue -= 100 // Одиночка
                        }

                        blackMaterial += checkerValue
                    }
                }
            }
        }

        if (!whiteKingAlive) return -9999999
        if (blackKings >= 2) return -9999999 // Если 2 дамки - бот сдается (твое правило)
        if (blackPieces == 0) return 9999999

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