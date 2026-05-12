package com.CheckersVsChess.checkersvschess.game.bots

import com.CheckersVsChess.checkersvschess.model.Board
import com.CheckersVsChess.checkersvschess.model.Move
import com.CheckersVsChess.checkersvschess.model.PieceColor

interface Bot {
    // Принимает доску и цвет бота, возвращает лучший (или случайный) ход
    fun getMove(board: Board, botColor: PieceColor): Move?
}