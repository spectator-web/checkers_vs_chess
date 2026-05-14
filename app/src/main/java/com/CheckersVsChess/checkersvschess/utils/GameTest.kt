package com.CheckersVsChess.checkersvschess.utils

import com.CheckersVsChess.checkersvschess.model.Board
import com.CheckersVsChess.checkersvschess.model.GamePiece
import com.CheckersVsChess.checkersvschess.model.PieceColor

fun main() {
    val board = Board()
    println("Доска инициализирована.")

    // Искусственно ставим "белую фигуру" для теста взятия
    board.grid[4][2] = object : GamePiece { override val color = PieceColor.WHITE }

    // Проверяем шашку
    val moves = board.getPossibleMoves(5, 1) // b3
    println("Доступные ходы: ${moves.size}")

    moves.forEach {
        println("Ход на (${it.toRow}, ${it.toCol}), Взятие: ${it.isCapture}")
    }
}