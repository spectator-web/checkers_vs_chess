package com.CheckersVsChess.checkersvschess.model

data class Player(
    val color: PieceColor,
    val isHuman: Boolean // Понадобится позже для настройки игры с ботом
)