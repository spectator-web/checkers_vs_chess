package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.game.MoveValidator
import com.CheckersVsChess.checkersvschess.model.*

class SmartChessBot : Bot {

    // Глубина просчета (3 полухода). Идеальный баланс между умом и скоростью (0.1 - 0.5 сек)
    private val SEARCH_DEPTH = 3

    override fun getMove(board: Board, botColor: PieceColor): Move? {
        val moves = getWhiteMoves(board.grid)
        if (moves.isEmpty()) return null

        var bestMove: Move? = null
        var maxEval = Int.MIN_VALUE

        for (move in moves) {
            val simulatedBoard = simulateMove(board.grid, move)
            val eval = minimax(simulatedBoard, SEARCH_DEPTH - 1, Int.MIN_VALUE, Int.MAX_VALUE, false)

            // Легкий шум, чтобы при равных ходах бот не играл по одному шаблону
            val noise = (0..2).random()
            if (eval + noise > maxEval) {
                maxEval = eval + noise
                bestMove = move
            }
        }

        return bestMove ?: moves.random()
    }

    private fun minimax(grid: Array<Array<GamePiece?>>, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        val eval = evaluateBoard(grid)

        // Базовый случай: дно просчета или кто-то уже победил в симуляции
        if (depth == 0 || Math.abs(eval) > 900000) {
            return eval
        }

        var a = alpha
        var b = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            val moves = getWhiteMoves(grid)
            if (moves.isEmpty()) return -999999 // Поражение белых

            for (move in moves) {
                val newGrid = simulateMove(grid, move)
                val currentEval = minimax(newGrid, depth - 1, a, b, false)
                maxEval = maxOf(maxEval, currentEval)
                a = maxOf(a, currentEval)
                if (b <= a) break // Альфа-бета отсечение
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            val moves = getBlackMoves(grid)
            if (moves.isEmpty()) return -999999 // Если у шашек нет ходов - белые проиграли

            for (move in moves) {
                val newGrid = simulateMove(grid, move)
                val currentEval = minimax(newGrid, depth - 1, a, b, true)
                minEval = minOf(minEval, currentEval)
                b = minOf(b, currentEval)
                if (b <= a) break // Альфа-бета отсечение
            }
            return minEval
        }
    }

    private fun evaluateBoard(grid: Array<Array<GamePiece?>>): Int {
        var whiteKingAlive = false
        var blackPieces = 0
        var blackKings = 0
        var blackHasMoves = false

        var whiteMaterial = 0
        var blackMaterial = 0
        var stuckBlackCheckersPenalty = 0

        // 1. РАДАР УГРОЗ: Кто из наших под боем прямо сейчас?
        val blackMoves = getBlackMoves(grid)
        blackHasMoves = blackMoves.isNotEmpty()
        val threatenedWhitePieces = blackMoves.flatMap { it.captured }.toSet()

        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c] ?: continue

                if (p.color == PieceColor.WHITE) {
                    if (p is ChessPiece) {
                        if (p.type == ChessPieceType.KING) whiteKingAlive = true

                        val pieceValue = when (p.type) {
                            ChessPieceType.PAWN -> 10
                            ChessPieceType.KNIGHT, ChessPieceType.BISHOP, ChessPieceType.ROOK, ChessPieceType.MORPHING_BISHOP -> 150
                            ChessPieceType.QUEEN -> 300
                            ChessPieceType.KING -> 0
                        }

                        // Если фигура под боем, вычитаем ее стоимость (штраф за зевок)
                        // Это делает бота ультра-осторожным
                        if (threatenedWhitePieces.contains(Position(r, c))) {
                            whiteMaterial -= pieceValue
                        } else {
                            whiteMaterial += pieceValue
                        }

                        // 2. ПОЗИЦИОННЫЕ БОНУСЫ
                        val isLightSquare = (r + c) % 2 == 0
                        if (isLightSquare) whiteMaterial += 20

                        if (p.type == ChessPieceType.KNIGHT && isLightSquare && r in 2..5 && c in 2..5) {
                            whiteMaterial += 30
                        }

                        if (p.type in listOf(ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.MORPHING_BISHOP)) {
                            // Мобильность: ладья в углу = 0 бонусов, ладья в центре = много бонусов
                            val mobility = MoveValidator.getMovesForChessPiece(grid, r, c).size
                            whiteMaterial += (mobility * 2)

                            if (r > 0 && (c == 0 || c == 7)) whiteMaterial += 15
                        }

                        if (p.type == ChessPieceType.PAWN) {
                            whiteMaterial += (r * 5)
                        }
                    }
                } else {
                    blackPieces++
                    if (p is CheckerPiece) {
                        if (p.isKing) {
                            blackKings++
                            blackMaterial += 300 // Дамка страшнее Ферзя
                        } else {
                            blackMaterial += 50
                            // 3. СТРАХ ПЕРЕД ДАМКОЙ: Чем ближе шашка к нулю, тем больше штраф белым
                            blackMaterial += (7 - r) * 10
                        }

                        // Застрявшая шашка - это хорошо для белых
                        val moves = MoveValidator.getValidMovesForPiece(grid, r, c, PieceColor.BLACK)
                        if (moves.isEmpty()) {
                            stuckBlackCheckersPenalty += 80
                        }
                    }
                }
            }
        }

        // --- КРИТИЧЕСКИЕ ИСХОДЫ ---
        if (!whiteKingAlive) return -999999
        if (blackKings >= 2) return -999999
        if (blackPieces == 0) return 999999
        if (!blackHasMoves) return -999999

        return (whiteMaterial - blackMaterial - stuckBlackCheckersPenalty)
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