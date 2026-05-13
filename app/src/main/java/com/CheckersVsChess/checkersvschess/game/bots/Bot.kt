package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.model.Board
import com.CheckersVsChess.checkersvschess.model.Move
import com.CheckersVsChess.checkersvschess.model.PieceColor

interface Bot {
    // Добавлен параметр timeLeft
    fun getMove(board: Board, botColor: PieceColor): Move?
}