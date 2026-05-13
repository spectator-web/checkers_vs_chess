package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SmartChessBotTacticsTest {

    private val bot = SmartChessBot()

    // Вспомогательный логгер
    private fun logBotDecision(testName: String, move: Move?, eval: Int) {
        val logFile = File("bot_logic_log.txt")
        logFile.appendText("[$testName] Бот выбрал: ${move?.fromRow},${move?.fromCol} -> ${move?.toRow},${move?.toCol} | Eval: $eval\n")
    }

    @Test
    fun testPriority_KillPotentialKing() {
        println("🧪 ЭТЮД: Приоритет — уничтожение будущей дамки")
        val board = Board().apply { clear() }

        // Наша Ладья на [7,0]
        board.grid[7][0] = ChessPiece(PieceColor.WHITE, ChessPieceType.ROOK)

        // Сценарий:
        // 1. Обычная шашка на [7,7] (далеко)
        board.grid[7][7] = CheckerPiece(PieceColor.BLACK)
        // 2. Опасная шашка на [1,6] (один шаг до дамки!)
        board.grid[1][6] = CheckerPiece(PieceColor.BLACK)

        val move = bot.getMove(board, PieceColor.WHITE)

        // Ладья должна пойти убивать ту, что на ряду 1
        assertEquals("Бот обязан убить потенциальную дамку!", 1, move?.toRow)
        logBotDecision("PotentialKingPriority", move, 0)
    }

    @Test
    fun testSelection_UnprotectedVsProtected() {
        println("🧪 ЭТЮД: Выбор между защищенной и беззащитной шашкой")
        val board = Board().apply { clear() }

        // Белый Конь на [4,4]
        board.grid[4][4] = ChessPiece(PieceColor.WHITE, ChessPieceType.KNIGHT)

        // 1. Беззащитная шашка на [2,3]
        board.grid[2][5] = CheckerPiece(PieceColor.BLACK)

        // 2. Шашка под защитой на [6,5], за ней стоит еще одна на [7,6]
        board.grid[6][5] = CheckerPiece(PieceColor.BLACK)
        board.grid[7][6] = CheckerPiece(PieceColor.BLACK)

        val move = bot.getMove(board, PieceColor.WHITE)

        // Конь должен взять ту, за которой нет "хвоста", чтобы не быть съеденным в ответ
        assertNotEquals("Бот не должен брать защищенную шашку!", 6, move?.toRow)
        logBotDecision("UnprotectedSelection", move, 0)
    }
}