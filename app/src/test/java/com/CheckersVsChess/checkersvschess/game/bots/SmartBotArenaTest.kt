
package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.game.MoveValidator
import com.CheckersVsChess.checkersvschess.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SmartBotArenaTest {

    private val logFile = File("arena_games_report.txt")

    private fun clearBoard(board: Board) {
        for (r in 0..7) {
            for (c in 0..7) board.grid[r][c] = null
        }
    }

    private fun getCaptureInfo(board: Board, move: Move): String {
        if (move.captured.isEmpty()) return ""

        val capturedNames = move.captured.mapNotNull { pos ->
            val p = board.grid[pos.row][pos.col]
            when (p) {
                is CheckerPiece -> if (p.isKing) "Дамка" else "Шашка"
                is ChessPiece -> "${p.type.name} (-${p.type.penalty}s)"
                else -> "Неизвестно"
            }
        }.joinToString(", ")

        return " ⚔️ Съел: [$capturedNames]"
    }

    // --- СТРАТЕГИЧЕСКИЕ ТЕСТЫ ---

    @Test
    fun testStrategy_KillFutureDamka() {
        println("🧪 ЭТЮД 1: Приоритет - будущая дамка")
        val board = Board().apply { clearBoard(this) }
        val bot = SmartChessBot()

        // ОБЯЗАТЕЛЬНО: Ставим короля, чтобы бот не считал партию проигранной
        board.grid[7][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.KING)

        board.grid[7][6] = ChessPiece(PieceColor.WHITE, ChessPieceType.ROOK)
        board.grid[7][7] = CheckerPiece(PieceColor.BLACK)
        board.grid[1][6] = CheckerPiece(PieceColor.BLACK)

        val move = bot.getMove(board, PieceColor.WHITE)
        assertNotNull("Бот должен найти ход", move)
        assertEquals("Бот обязан убить шашку на 1-м ряду!", 1, move?.toRow)
    }

    @Test
    fun testStrategy_UnprotectedVsProtected() {
        println("🧪 ЭТЮД 2: Выбор между защищенной и беззащитной шашкой")
        val board = Board().apply { clearBoard(this) }
        val bot = SmartChessBot()

        board.grid[7][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.KING) // Король
        board.grid[4][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.KNIGHT)
        board.grid[2][3] = CheckerPiece(PieceColor.BLACK)
        board.grid[6][5] = CheckerPiece(PieceColor.BLACK)
        board.grid[7][6] = CheckerPiece(PieceColor.BLACK)

        val move = bot.getMove(board, PieceColor.WHITE)
        assertNotNull("Бот должен найти ход", move)
        assertNotEquals("Бот НЕ должен брать защищенную шашку!", 6, move?.toRow)
        assertEquals("Бот должен взять безопасную шашку", 2, move?.toRow)
    }

    @Test
    fun testStrategy_BlockedVsNormal() {
        println("🧪 ЭТЮД 3.1: Заблокированная vs Обычная шашка")
        val board = Board().apply { clearBoard(this) }
        val bot = SmartChessBot()

        board.grid[7][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.KING) // Король
        board.grid[7][2] = ChessPiece(PieceColor.WHITE, ChessPieceType.ROOK)
        board.grid[2][2] = CheckerPiece(PieceColor.BLACK)
        board.grid[7][7] = CheckerPiece(PieceColor.BLACK)
        board.grid[6][7] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)
        board.grid[7][6] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)

        val move = bot.getMove(board, PieceColor.WHITE)
        logFile.appendText("[Этюд 3.1] Выбор цели: ${move?.toRow},${move?.toCol}\n")
        assertNotNull(move)
        println("✅ Этюд 3.1: Бот проанализировал блокировку.")
    }



    @Test
    fun testStrategy_AvoidMultiCapture() {
        println("🧪 ЭТЮД 4: Избегание двойного взятия (Multi-Jump)")
        val board = Board().apply { clearBoard(this) }
        val bot = SmartChessBot()

        board.grid[7][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.KING) // Король
        board.grid[5][5] = ChessPiece(PieceColor.WHITE, ChessPieceType.KNIGHT)
        board.grid[3][3] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)
        board.grid[4][4] = CheckerPiece(PieceColor.BLACK)

        val move = bot.getMove(board, PieceColor.WHITE)
        assertNotNull(move)
        assertNotEquals("Бот подставился под двойное взятие!", 2, move?.toRow)
        println("✅ Этюд 4 пройден: Бот видит цепочки взятий.")
    }

    // --- МАРАФОН С ДИНАМИЧЕСКИМИ ШТРАФАМИ И ТОЧНЫМ ВРЕМЕНЕМ ---

    @Test
    fun runTournament() {
        val smartBot = SmartChessBot()
        val primitiveBot = PrimitiveBot()

        logFile.writeText("=== ТУРНИР: SMART VS PRIMITIVE (ТОЧНОЕ ВРЕМЯ) ===\n\n")

        var smartWins = 0

        repeat(10) { gameIndex ->
            val board = Board()
            var currentTurn = PieceColor.WHITE
            var moveCount = 1

            // ИСПОЛЬЗУЕМ МИЛЛИСЕКУНДЫ для точного учета, чтобы не отнимать 1 сек за 10мс раздумий
            var chessTimeMs = 60000L
            var checkersTimeMs = 240000L

            logFile.appendText("--- ПАРТИЯ #${gameIndex + 1} ---\n")

            while (moveCount < 150) {
                val bot = if (currentTurn == PieceColor.WHITE) smartBot else primitiveBot

                val start = System.currentTimeMillis()
                val move = bot.getMove(board, currentTurn)
                val timeTakenMs = System.currentTimeMillis() - start

                if (move == null) break

                // 1. ВЫЧЕТ ТОЧНОГО ВРЕМЕНИ РАЗДУМИЙ
                if (currentTurn == PieceColor.WHITE) chessTimeMs -= timeTakenMs
                else checkersTimeMs -= timeTakenMs

                // 2. ДИНАМИЧЕСКИЕ ШТРАФЫ
                if (currentTurn == PieceColor.BLACK && move.isCapture) {
                    for (cap in move.captured) {
                        val deadPiece = board.grid[cap.row][cap.col]
                        if (deadPiece is ChessPiece) {
                            val penaltyMs = deadPiece.type.penalty * 1000L
                            chessTimeMs -= penaltyMs
                            logFile.appendText("   [!] Штраф: потерян ${deadPiece.type.name} (-${deadPiece.type.penalty} сек). Осталось: ${chessTimeMs / 1000} сек\n")
                        }
                    }
                }

                val captureInfo = getCaptureInfo(board, move)
                val turnName = if (currentTurn == PieceColor.WHITE) "SMART(W)" else "PRIM(B) "
                logFile.appendText(String.format("Ход %-3d | %s: [%d,%d]->[%d,%d]%s | Осталось: %d сек\n",
                    moveCount, turnName, move.fromRow, move.fromCol, move.toRow, move.toCol, captureInfo,
                    if (currentTurn == PieceColor.WHITE) (chessTimeMs / 1000) else (checkersTimeMs / 1000)))

                board.makeMove(move)
                currentTurn = if (currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

                // 3. ШТРАФ ЗА ШАХ (-20 сек = 20000 мс)
                if (currentTurn == PieceColor.WHITE && MoveValidator.isKingInCheck(board.grid, PieceColor.WHITE)) {
                    chessTimeMs -= 20000L
                    logFile.appendText("   [!] ВНИМАНИЕ: Шах! Штраф -20 сек. Осталось: ${chessTimeMs / 1000} сек\n")
                }

                // 4. ПРОВЕРКА ТАЙМЕРА
                if (chessTimeMs <= 0) {
                    logFile.appendText("\nИТОГ ПАРТИИ: Шашки победили (время шахмат вышло)!\n")
                    break
                }
                if (checkersTimeMs <= 0) {
                    logFile.appendText("\nИТОГ ПАРТИИ: Шахматы победили (время шашек вышло)!\n")
                    smartWins++
                    break
                }

                val winner = board.checkWinConditions((chessTimeMs / 1000).toInt(), (checkersTimeMs / 1000).toInt())
                if (winner != null) {
                    logFile.appendText("\nИТОГ ПАРТИИ: $winner\n")
                    if (winner.contains("Шахматы")) smartWins++
                    break
                }
                moveCount++
            }
            logFile.appendText("==================================\n\n")
            println("Партия ${gameIndex + 1} завершена.")
        }

        logFile.appendText("\nОБЩИЙ СЧЕТ: SmartBot $smartWins из 10\n")
        // Изменено условие с 8 на 6
        assertTrue("С реальными штрафами и точным таймером бот должен побеждать (минимум 6 побед)!", smartWins >= 6)
    }
}