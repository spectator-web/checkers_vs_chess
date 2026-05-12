package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.model.Board
import com.CheckersVsChess.checkersvschess.model.Move
import com.CheckersVsChess.checkersvschess.model.PieceColor

class SmartCheckersBot : Bot {

    override fun getMove(board: Board, botColor: PieceColor): Move? {
        // TODO: Здесь будет сложный алгоритм Минимакс для шашек.
        // А пока бот не готов, мы просто вызываем случайный ход из PrimitiveBot,
        // чтобы игра не вылетала и в нее можно было играть.

        return PrimitiveBot().getMove(board, botColor)
    }
}