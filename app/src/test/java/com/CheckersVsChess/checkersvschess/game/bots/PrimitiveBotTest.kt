package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.model.*
import org.junit.Assert.*
import org.junit.Test

class PrimitiveBotTest {

    private val bot = PrimitiveBot()

    private fun createEmptyBoard(): Board {
        val board = Board()
        for (r in 0..7) {
            for (c in 0..7) {
                board.grid[r][c] = null
            }
        }
        return board
    }

    @Test
    fun testBotPrefersCapture() {
        println("🧪 ТЕСТ: Приоритет взятия у PrimitiveBot")
        val board = createEmptyBoard()

        // Ставим черную шашку бота на [2][2]
        board.grid[2][2] = CheckerPiece(PieceColor.BLACK)

        // Ставим белую пешку-жертву на [3][3]
        board.grid[3][3] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)

        // Ставим еще одну черную шашку на [5][5], у которой есть обычный ход
        board.grid[5][5] = CheckerPiece(PieceColor.BLACK)

        println("На доске есть возможность съесть фигуру и обычный ход.")
        val move = bot.getMove(board, PieceColor.BLACK)

        println("Бот выбрал ход: [${move?.fromRow},${move?.fromCol}] -> [${move?.toRow},${move?.toCol}], Взятие: ${move?.isCapture}")

        assertNotNull("Бот должен найти ход", move)
        assertTrue("Бот ОБЯЗАН выбрать взятие, если оно есть", move!!.isCapture)
        assertEquals("Бот должен рубить шашкой с [2][2]", 2, move.fromRow)

        println("✅ Тест пройден: Бот выбрал агрессивный ход.")
    }

    @Test
    fun testBotAvoidsCheckOnDarkSquares() {
        println("🧪 ТЕСТ: PrimitiveBot спасает короля на ТЕМНОЙ клетке")
        val board = createEmptyBoard()

        // 1. Ставим короля на темную клетку [1][2] (1+2=3 - нечет)
        board.grid[1][2] = ChessPiece(PieceColor.WHITE, ChessPieceType.KING)

        // 2. Ставим белую ладью-защитника на [2][3] (2+3=5 - нечет)
        // Она стоит на пути шашки к королю
        board.grid[2][3] = ChessPiece(PieceColor.WHITE, ChessPieceType.ROOK)

        // 3. Ставим черную дамку на [4][5] (4+5=9 - нечет)
        // Она бьет по диагонали: [4,5] -> [3,4] -> [2,3] -> [1,2]
        val blackKing = CheckerPiece(PieceColor.BLACK)
        blackKing.isKing = true
        board.grid[4][5] = blackKing

        println("Король на [1,2], Ладья на [2,3]. Дамка на [4,5] угрожает королю через ладью.")

        val move = bot.getMove(board, PieceColor.WHITE)

        println("Бот выбрал ход: [${move?.fromRow},${move?.fromCol}] -> [${move?.toRow},${move?.toCol}]")

        // Проверяем связку: Ладья не должна уходить с диагонали, иначе королю шах
        if (move?.fromRow == 2 && move.fromCol == 3) {
            // Ладья может либо остаться на месте, либо съесть дамку на [4,5]
            val isStillProtecting = (move.toRow == 4 && move.toCol == 5) || (move.toRow == 2 && move.toCol == 3)
            assertTrue("Ладья подставила короля под шах!", isStillProtecting || move.fromRow != 2)
        }

        println("✅ Тест пройден: Бот учитывает геометрию шашек на темных полях.")
    }

    @Test
    fun testNoMovesReturnsNull() {
        println("🧪 ТЕСТ: Нет ходов — возвращаем null")
        val board = createEmptyBoard()
        // Оставляем доску пустой

        val move = bot.getMove(board, PieceColor.WHITE)
        assertNull("Если ходить некому, бот должен вернуть null", move)

        println("✅ Тест пройден: Бот корректно признал поражение.")
    }
}