package com.CheckersVsChess.checkersvschess

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.CheckersVsChess.checkersvschess.game.MoveValidator
import com.CheckersVsChess.checkersvschess.game.bots.Bot
import com.CheckersVsChess.checkersvschess.game.bots.PrimitiveBot
import com.CheckersVsChess.checkersvschess.game.bots.SmartChessBot
import com.CheckersVsChess.checkersvschess.game.bots.SmartCheckersBot
import com.CheckersVsChess.checkersvschess.model.*

class MainActivity : AppCompatActivity() {

    private val gameBoard = Board()
    private lateinit var gridLayout: GridLayout
    private lateinit var tvTimer: TextView

    private var selectedPos: Position? = null
    private var currentPossibleMoves: List<Move> = emptyList()

    private var currentTurn = PieceColor.WHITE
    private var chessTimeLeft = 60
    private var checkersTimeLeft = 240
    private var timer: CountDownTimer? = null

    private var isGameOver = false
    private var isSinglePlayer = false
    private var isSmartBot = false // <--- ТА САМАЯ ПЕРЕМЕННАЯ
    private var humanPlayerColor = PieceColor.BLACK
    private var isPenaltiesEnabled = true
    private var isFlipEnabled = true

    // --- ФЛАГИ БЕСПЛАТНОГО ПЕРВОГО ХОДА ---
    private var isWhiteFirstMove = true
    private var isBlackFirstMove = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ЧИТАЕМ НАСТРОЙКИ ИЗ ИНТЕНТА
        isSinglePlayer = intent.getBooleanExtra("IS_SINGLE_PLAYER", false)
        isSmartBot = intent.getBooleanExtra("IS_SMART_BOT", false) // <--- И ЕЕ ЧТЕНИЕ
        val playerIsBlack = intent.getBooleanExtra("PLAYER_IS_BLACK", true)
        humanPlayerColor = if (playerIsBlack) PieceColor.BLACK else PieceColor.WHITE

        isPenaltiesEnabled = intent.getBooleanExtra("USE_PENALTIES", true)
        isFlipEnabled = intent.getBooleanExtra("USE_FLIP_BOARD", true)

        findViewById<Button>(R.id.btnSmallMenu).setOnClickListener {
            if (isGameOver) {
                finish()
                return@setOnClickListener
            }
            val winner = if (currentTurn == PieceColor.WHITE) "Шашки (Черные)" else "Шахматы (Белые)"
            timer?.cancel()
            endGame("Игрок сдался. Победили $winner")
        }

        findViewById<Button>(R.id.btnExitToMenu).setOnClickListener { finish() }

        gridLayout = findViewById(R.id.boardGrid)
        tvTimer = findViewById(R.id.tvTimer)

        chessTimeLeft = intent.getIntExtra("CHESS_TIME", 60)
        checkersTimeLeft = intent.getIntExtra("CHECKERS_TIME", chessTimeLeft * 4)

        drawBoard()
        startTimer()

        // ПЕРВЫЙ ХОД БОТА (Если бот играет за Белых)
        if (isSinglePlayer && currentTurn != humanPlayerColor) {
            executeBotMove()
        }
    }

    private fun updateTimerText() {
        if (currentTurn == PieceColor.WHITE) {
            tvTimer.text = "Ход Шахмат (Белые): ${if (chessTimeLeft > 0) "$chessTimeLeft сек" else "∞"}"
            tvTimer.setTextColor(Color.parseColor("#333333"))
        } else {
            tvTimer.text = "Ход Шашек (Черные): ${if (checkersTimeLeft > 0) "$checkersTimeLeft сек" else "∞"}"
            tvTimer.setTextColor(Color.parseColor("#C62828"))
        }
    }

    private fun startTimer() {
        timer?.cancel()

        // ЧИТ-КОД: Бесконечное время
        if (chessTimeLeft <= 0 || checkersTimeLeft <= 0) {
            updateTimerText()
            return
        }

        if (currentTurn == PieceColor.WHITE && isWhiteFirstMove) {
            updateTimerText()
            return
        }
        if (currentTurn == PieceColor.BLACK && isBlackFirstMove) {
            updateTimerText()
            return
        }

        val timeToCount = if (currentTurn == PieceColor.WHITE) chessTimeLeft else checkersTimeLeft

        timer = object : CountDownTimer((timeToCount * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                if (currentTurn == PieceColor.WHITE) {
                    chessTimeLeft = secondsLeft
                } else {
                    checkersTimeLeft = secondsLeft
                }
                updateTimerText()
            }

            override fun onFinish() {
                val winner = if (currentTurn == PieceColor.WHITE) "Шашки" else "Шахматы"
                endGame("Время вышло! Победили $winner")
            }
        }.start()
    }

    private fun drawBoard() {
        gridLayout.removeAllViews()
        val density = resources.displayMetrics.density
        val cellSize = (44 * density).toInt()

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isBlueTheme = prefs.getBoolean("USE_BLUE_THEME", false)

        if (isSinglePlayer) {
            if (humanPlayerColor == PieceColor.WHITE) {
                gridLayout.rotation = 180f
            } else {
                gridLayout.rotation = 0f
            }
        } else {
            if (isFlipEnabled && currentTurn == PieceColor.WHITE) {
                gridLayout.rotation = 180f
            } else {
                gridLayout.rotation = 0f
            }
        }

        for (row in 0..7) {
            for (col in 0..7) {
                val cellView = TextView(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cellSize
                        height = cellSize
                        rowSpec = GridLayout.spec(row)
                        columnSpec = GridLayout.spec(col)
                    }
                    gravity = Gravity.CENTER
                    textSize = 24f
                    rotation = gridLayout.rotation
                }

                val isDark = (row + col) % 2 != 0
                var bgColor = if (isBlueTheme) {
                    if (isDark) Color.parseColor("#1565C0") else Color.parseColor("#90CAF9")
                } else {
                    if (isDark) Color.parseColor("#8D6E63") else Color.parseColor("#D7CCC8")
                }

                val lastMove = gameBoard.lastMove
                if (lastMove != null && ((row == lastMove.fromRow && col == lastMove.fromCol) || (row == lastMove.toRow && col == lastMove.toCol))) {
                    bgColor = Color.parseColor("#81D4FA")
                }

                val piece = gameBoard.grid[row][col]
                if (piece is ChessPiece && piece.type == ChessPieceType.MORPHING_BISHOP) {
                    bgColor = Color.parseColor("#9C27B0")
                }

                if (selectedPos?.row == row && selectedPos?.col == col) {
                    bgColor = Color.parseColor("#FFF59D")
                } else if (currentPossibleMoves.any { it.toRow == row && it.toCol == col }) {
                    bgColor = Color.parseColor("#A5D6A7")
                }

                cellView.setBackgroundColor(bgColor)

                if (piece is CheckerPiece) {
                    cellView.text = if (piece.isKing) "👑" else "⚫"
                } else if (piece is ChessPiece) {
                    cellView.text = when (piece.type) {
                        ChessPieceType.PAWN -> "♙"
                        ChessPieceType.ROOK -> "♖"
                        ChessPieceType.KNIGHT -> "♘"
                        ChessPieceType.BISHOP -> "♗"
                        ChessPieceType.MORPHING_BISHOP -> "♗"
                        ChessPieceType.QUEEN -> "♕"
                        ChessPieceType.KING -> "♔"
                    }
                }

                cellView.setOnClickListener { handleCellClick(row, col) }
                gridLayout.addView(cellView)
            }
        }
    }

    private fun handleCellClick(row: Int, col: Int) {
        if (isGameOver || !gridLayout.isEnabled) return

        val clickedMove = currentPossibleMoves.find { it.toRow == row && it.toCol == col }

        if (clickedMove != null) {
            if (currentTurn == PieceColor.BLACK && clickedMove.isCapture) {
                for (cap in clickedMove.captured) {
                    val deadPiece = gameBoard.grid[cap.row][cap.col]
                    if (deadPiece is ChessPiece) {
                        applyPenalty(deadPiece.type.penalty)
                        Toast.makeText(this, "Шахматы потеряли фигуру! Штраф -${deadPiece.type.penalty} сек", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            gameBoard.makeMove(clickedMove)
            val piece = gameBoard.grid[clickedMove.toRow][clickedMove.toCol]

            if (piece is ChessPiece && piece.type == ChessPieceType.PAWN && clickedMove.toRow == 7) {
                showPromotionDialog(clickedMove.toRow, clickedMove.toCol)
            } else {
                passTurn()
            }
        } else {
            val piece = gameBoard.grid[row][col]
            if (piece != null && piece.color == currentTurn) {
                selectedPos = Position(row, col)
                val rawMoves = when (piece) {
                    is CheckerPiece -> MoveValidator.getValidMovesForPiece(gameBoard.grid, row, col, piece.color)
                    is ChessPiece -> MoveValidator.getMovesForChessPiece(gameBoard.grid, row, col)
                    else -> emptyList()
                }

                currentPossibleMoves = if (currentTurn == PieceColor.WHITE) {
                    rawMoves.filter { move -> !MoveValidator.wouldMoveResultInCheck(gameBoard.grid, move, PieceColor.WHITE) }
                } else {
                    rawMoves
                }

                if (currentPossibleMoves.isEmpty()) {
                    if (currentTurn == PieceColor.BLACK && MoveValidator.hasAnyCaptures(gameBoard.grid, PieceColor.BLACK)) {
                        Toast.makeText(this, "Обязательное взятие другой шашкой!", Toast.LENGTH_SHORT).show()
                    } else if (currentTurn == PieceColor.WHITE && MoveValidator.isKingInCheck(gameBoard.grid, PieceColor.WHITE)) {
                        Toast.makeText(this, "Этот ход не спасает Короля от Шаха!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "У этой фигуры нет доступных ходов", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                selectedPos = null
                currentPossibleMoves = emptyList()
            }
            drawBoard()
        }
    }

    private fun showPromotionDialog(row: Int, col: Int) {
        val items = arrayOf("Ферзь (Queen)", "Ладья (Rook)", "Слон (Bishop)", "Конь (Knight)")
        android.app.AlertDialog.Builder(this)
            .setTitle("Выберите фигуру")
            .setItems(items) { _, which ->
                val newType = when (which) {
                    0 -> ChessPieceType.QUEEN
                    1 -> ChessPieceType.ROOK
                    2 -> ChessPieceType.BISHOP
                    else -> ChessPieceType.KNIGHT
                }
                gameBoard.grid[row][col] = ChessPiece(PieceColor.WHITE, newType)
                passTurn()
            }
            .setCancelable(false)
            .show()
    }

    private fun executeBotMove() {
        gridLayout.isEnabled = false

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isGameOver) return@postDelayed

            // МАГИЯ АРХИТЕКТУРЫ: Просто выбираем нужную стратегию
            val activeBot: Bot = if (isSmartBot) {
                if (currentTurn == PieceColor.WHITE) {
                    SmartChessBot()       // Умный Жнец (готов)
                } else {
                    SmartCheckersBot()    // Умный Шашист (пока под прикрытием тупого)
                }
            } else {
                PrimitiveBot()            // Тупой режим для обоих
            }

            val botMove = activeBot.getMove(gameBoard, currentTurn)

            if (botMove != null) {
                if (currentTurn == PieceColor.BLACK && botMove.isCapture) {
                    for (cap in botMove.captured) {
                        val deadPiece = gameBoard.grid[cap.row][cap.col]
                        if (deadPiece is ChessPiece) applyPenalty(deadPiece.type.penalty)
                    }
                }

                gameBoard.makeMove(botMove)

                val piece = gameBoard.grid[botMove.toRow][botMove.toCol]
                if (piece is ChessPiece && piece.type == ChessPieceType.PAWN && botMove.toRow == 7) {
                    gameBoard.grid[botMove.toRow][botMove.toCol] = ChessPiece(PieceColor.WHITE, ChessPieceType.QUEEN)
                }

                passTurn()
            } else {
                val winner = if (currentTurn == PieceColor.WHITE) "Шашки" else "Шахматы"
                endGame("У противника нет ходов! Победили $winner")
            }
        }, 500)
    }

    private fun passTurn() {
        // 1. Снимаем флаг бесплатного первого хода
        if (currentTurn == PieceColor.WHITE) {
            isWhiteFirstMove = false
        } else {
            isBlackFirstMove = false
        }

        // 2. Передаем очередь хода оппоненту
        currentTurn = if (currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

        // 3. Проверяем базовые условия победы (съели короля, кончились шашки)
        val winMessage = gameBoard.checkWinConditions(chessTimeLeft, checkersTimeLeft)
        if (winMessage != null) {
            endGame(winMessage)
            return
        }

        // 4. КАТАСТРОФА ПРЕДОТВРАЩЕНА: Проверяем, есть ли ходы у текущего игрока
        if (!MoveValidator.hasAnyMoves(gameBoard.grid, currentTurn)) {
            if (currentTurn == PieceColor.BLACK) {
                endGame("У шашек нет ходов! Шашки победили (капкан)!")
            } else {
                endGame("У шахмат нет ходов (Мат/Пат)! Шашки победили!")
            }
            return
        }

        // 5. Проверка на шах (даем штраф)
        if (currentTurn == PieceColor.WHITE && MoveValidator.isKingInCheck(gameBoard.grid, PieceColor.WHITE)) {
            applyPenalty(20)
            Toast.makeText(this, "Шах! Штраф -20 секунд белым", Toast.LENGTH_SHORT).show()
        }

        startTimer()

        selectedPos = null
        currentPossibleMoves = emptyList()
        drawBoard()

        // 6. Вызов бота, если сейчас его очередь
        if (isSinglePlayer && currentTurn != humanPlayerColor) {
            executeBotMove()
        } else {
            gridLayout.isEnabled = true
        }
    }

    private fun applyPenalty(seconds: Int) {
        if (!isPenaltiesEnabled) return
        chessTimeLeft = (chessTimeLeft - seconds).coerceAtLeast(0)
        updateTimerText()
    }

    private fun endGame(message: String) {
        isGameOver = true
        timer?.cancel()

        tvTimer.text = message
        tvTimer.setTextColor(Color.parseColor("#4CAF50"))
        findViewById<Button>(R.id.btnExitToMenu).visibility = android.view.View.VISIBLE

        if (message.contains("съели короля", ignoreCase = true)) {
            tvTimer.text = "🎉 💥 КОРОЛЬ ПАЛ! 💥 🎉"
            tvTimer.textSize = 32f
            tvTimer.animate().scaleX(1.3f).scaleY(1.3f).setDuration(400).withEndAction {
                tvTimer.animate().scaleX(1.0f).scaleY(1.0f).setDuration(400).withEndAction {
                    tvTimer.animate().scaleX(1.3f).scaleY(1.3f).setDuration(400).withEndAction {
                        tvTimer.animate().scaleX(1.0f).scaleY(1.0f).setDuration(400).start()
                    }.start()
                }.start()
            }.start()
        }

        selectedPos = null
        currentPossibleMoves = emptyList()
        drawBoard()
    }
}