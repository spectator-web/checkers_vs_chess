package com.CheckersVsChess.checkersvschess.model

import org.junit.Assert.*
import org.junit.Test

class BoardTest {

    @Test
    fun testWinCondition_NoCheckers() {
        println("🧪 ТЕСТ: Победа шахмат (ноль шашек)")
        val board = Board()

        // Очищаем доску от всех черных шашек
        for (r in 0..7) {
            for (c in 0..7) {
                if (board.grid[r][c]?.color == PieceColor.BLACK) {
                    board.grid[r][c] = null
                }
            }
        }

        val result = board.checkWinConditions(60, 240)
        println("Результат проверки: $result")

        // ИСПРАВЛЕНО: Ищем правильную фразу, которую выдает игра
        assertTrue("Должно быть сообщение о победе шахмат",
            result?.contains("уничтожили все шашки") == true)
    }

    @Test
    fun testWinCondition_KingCaptured() {
        println("🧪 ТЕСТ: Победа шашек (король съеден)")
        val board = Board()

        // Убираем белого короля
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board.grid[r][c]
                if (p is ChessPiece && p.type == ChessPieceType.KING) {
                    board.grid[r][c] = null
                }
            }
        }

        val result = board.checkWinConditions(60, 240)
        println("Результат проверки: $result")

        assertTrue("Должно быть сообщение о гибели короля",
            result?.contains("съели короля") == true)
    }
}