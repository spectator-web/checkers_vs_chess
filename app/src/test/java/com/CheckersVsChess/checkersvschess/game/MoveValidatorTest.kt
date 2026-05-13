package com.CheckersVsChess.checkersvschess.game

import com.CheckersVsChess.checkersvschess.model.*
import org.junit.Assert.*
import org.junit.Test

class MoveValidatorTest {

    // Вспомогательная функция для создания пустой доски 8х8
    private fun createEmptyGrid(): Array<Array<GamePiece?>> {
        return Array(8) { arrayOfNulls<GamePiece>(8) }
    }

    @Test
    fun testPawnMovesForward() {
        println("=== ЗАПУСК ТЕСТА: Ход Пешки ===")
        val grid = createEmptyGrid()

        // Ставим белую пешку на [1][4] (стартовая позиция)
        grid[1][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)

        println("Проверка ходов для пешки на [1][4]...")
        val moves = MoveValidator.getMovesForChessPiece(grid, 1, 4)

        println("Найдено ходов: ${moves.size}")
        for (move in moves) {
            println(" -> Возможный ход: в [${move.toRow}][${move.toCol}]")
        }

        // Со стартовой позиции пешка может пойти на 1 или 2 клетки вперед
        assertEquals("Пешка должна иметь ровно 2 хода", 2, moves.size)
        assertTrue(moves.any { it.toRow == 2 && it.toCol == 4 })
        assertTrue(moves.any { it.toRow == 3 && it.toCol == 4 })

        println("=== ТЕСТ ПРОЙДЕН УСПЕШНО ===")
        println()
    }

    @Test
    fun testKingInCheckDetection() {
        println("=== ЗАПУСК ТЕСТА: Обнаружение Шаха ===")
        val grid = createEmptyGrid()

        // Ставим белого короля на [4][4]
        grid[4][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.KING)
        // Ставим черную шашку-убийцу на [3][3], которая смотрит прямо на короля
        grid[3][3] = CheckerPiece(PieceColor.BLACK)

        println("Король на [4][4], вражеская шашка на [3][3]")

        val isInCheck = MoveValidator.isKingInCheck(grid, PieceColor.WHITE)
        println("Статус шаха для белых: $isInCheck")

        // Так как черная шашка с [3][3] бьет на [5][5] через [4][4], король под шахом!
        assertTrue("Король ДОЛЖЕН быть под шахом", isInCheck)

        println("=== ТЕСТ ПРОЙДЕН УСПЕШНО ===")
        println()
    }

    @Test
    fun testCheckerMandatoryCapture() {
        println("=== ЗАПУСК ТЕСТА: Обязательное взятие шашкой ===")
        val grid = createEmptyGrid()

        // Ставим нашу черную шашку на [2][2]
        grid[2][2] = CheckerPiece(PieceColor.BLACK)

        // Ставим белую пешку на [3][3] (под удар)
        grid[3][3] = ChessPiece(PieceColor.WHITE, ChessPieceType.PAWN)

        println("Черная шашка на [2][2], белая пешка-жертва на [3][3]")

        val moves = MoveValidator.getValidMovesForPiece(grid, 2, 2, PieceColor.BLACK)

        println("Найдено ходов для шашки: ${moves.size}")
        for (move in moves) {
            println(" -> Ход: в [${move.toRow}][${move.toCol}], съедает фигуру на [${move.captured.firstOrNull()?.row}][${move.captured.firstOrNull()?.col}]")
        }

        // По правилам шашек, если есть взятие, обычные ходы (на [3][1]) блокируются
        assertEquals("Должен быть только 1 ход (рубить)", 1, moves.size)
        assertTrue("Ход должен быть рубящим", moves[0].isCapture)
        assertEquals("Шашка должна приземлиться на [4][4]", 4, moves[0].toRow)
        assertEquals("Шашка должна приземлиться на [4][4]", 4, moves[0].toCol)

        println("=== ТЕСТ ПРОЙДЕН УСПЕШНО ===")
        println()
    }
}