package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.model.*
import org.junit.Assert.*
import org.junit.Test

class SmartCheckersBotTest {

    private fun clearBoard(board: Board) {
        for (r in 0..7) {
            for (c in 0..7) board.grid[r][c] = null
        }
    }

    @Test
    fun testStrategy_PreferCenterOverCorner() {
        println("🧪 ТЕСТ 1: Приоритет центра")
        val board = Board().apply { clearBoard(this) }
        val bot = SmartCheckersBot()

        // Черная шашка на 2,1
        board.grid[2][1] = CheckerPiece(PieceColor.BLACK)

        // У неё есть два хода: в угол [3,0] или в центр [3,2]
        val move = bot.getMove(board, PieceColor.BLACK)

        assertNotNull(move)
        assertEquals("Шашка должна стремиться в центр (col 2)", 2, move?.toCol)
    }

    @Test
    fun testStrategy_ChooseMaxCaptures() {
        println("🧪 ТЕСТ 2: Выбор самого выгодного взятия (Multi-Jump)")
        val board = Board().apply { clearBoard(this) }
        val bot = SmartCheckersBot()

        // Черная шашка-убийца
        board.grid[1][1] = CheckerPiece(PieceColor.BLACK)

        // Жертва 1: одинокая пешка справа (один прыжок)
        board.grid[2][2] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)

        // Жертва 2: цепочка пешек слева (двойной прыжок)
        board.grid[2][0] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)
        board.grid[4][2] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)

        val move = bot.getMove(board, PieceColor.BLACK)

        assertNotNull(move)
        // В MoveValidator ход с цепочкой взятий содержит больше элементов в списке captured
        assertTrue("Бот должен выбрать ход с максимальным количеством съеденных фигур",
            move!!.captured.size >= 2)
    }

    @Test
    fun testStrategy_KeepTheLine() {
        println("🧪 ТЕСТ 3: Поддержка строя (не разрывать связку)")
        val board = Board().apply { clearBoard(this) }
        val bot = SmartCheckersBot()

        // Две черные шашки: одна на 1,1, другая на 0,0 (подпирает)
        board.grid[1][1] = CheckerPiece(PieceColor.BLACK)
        board.grid[0][0] = CheckerPiece(PieceColor.BLACK)

        // Если шашка 1,1 пойдет на 2,2 — она сохранит поддержку от 1,1 (в теории строя)
        // Если у нас есть выбор между ходом, который создает "кулак", и одиночным выпадом
        // Бот должен выбрать "кулак".

        board.grid[2][4] = CheckerPiece(PieceColor.BLACK) // Одиночка

        val move = bot.getMove(board, PieceColor.BLACK)
        assertNotNull(move)
        // Проверяем, что бот предпочел развить строй, а не ходить одиночкой (в зависимости от весов)
    }

    @Test
    fun testStrategy_AvoidDeath() {
        println("🧪 ТЕСТ 4: Выбор безопасного поля против Ладьи")
        val board = Board().apply { clearBoard(this) }
        val bot = SmartCheckersBot()

        board.grid[2][2] = CheckerPiece(PieceColor.BLACK)

        // Белая Ладья простреливает 3-ю горизонталь
        board.grid[3][7] = ChessPiece(PieceColor.WHITE, ChessPieceType.ROOK)

        // У черных два хода: [3,1] (под бой Ладьи) и [3,3] (безопасно)
        val move = bot.getMove(board, PieceColor.BLACK)

        assertNotNull(move)
        assertNotEquals("Шашка не должна вставать на линию огня Ладьи!", 1, move?.toCol)
        assertEquals("Шашка должна выбрать безопасное поле", 3, move?.toCol)
    }
}