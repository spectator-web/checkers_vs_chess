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

    private var isPenaltiesEnabled = true

    private var isFlipEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnSmallMenu).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnExitToMenu).setOnClickListener { finish() }
        isPenaltiesEnabled = intent.getBooleanExtra("USE_PENALTIES", true)
        isFlipEnabled = intent.getBooleanExtra("USE_FLIP_BOARD", true)

        gridLayout = findViewById(R.id.boardGrid)
        tvTimer = findViewById(R.id.tvTimer)

        chessTimeLeft = intent.getIntExtra("CHESS_TIME", 60)
        checkersTimeLeft = intent.getIntExtra("CHECKERS_TIME", chessTimeLeft * 4)

        drawBoard()
        startTimer()
    }

    private fun startTimer() {
        timer?.cancel()

        val timeToCount = if (currentTurn == PieceColor.WHITE) chessTimeLeft else checkersTimeLeft

        timer = object : CountDownTimer((timeToCount * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()

                if (currentTurn == PieceColor.WHITE) {
                    chessTimeLeft = secondsLeft
                    tvTimer.text = "Ход Шахмат (Белые): $chessTimeLeft сек"
                    tvTimer.setTextColor(Color.parseColor("#333333"))
                } else {
                    checkersTimeLeft = secondsLeft
                    tvTimer.text = "Ход Шашек (Черные): $checkersTimeLeft сек"
                    tvTimer.setTextColor(Color.parseColor("#C62828"))
                }
            }

            override fun onFinish() {
                val winner = if (currentTurn == PieceColor.WHITE) "Шашки" else "Шахматы"
                tvTimer.text = "Время вышло! Победили $winner"
                Toast.makeText(this@MainActivity, "Победа! $winner выиграли по времени.", Toast.LENGTH_LONG).show()
                currentTurn = PieceColor.BLACK // Не совсем корректно, лучше завершить игру
                selectedPos = null
                currentPossibleMoves = emptyList()
                drawBoard()
                gridLayout.isEnabled = false // Блокируем доску
            }
        }.start()
    }

    private fun drawBoard() {
        gridLayout.removeAllViews()
        val density = resources.displayMetrics.density
        val cellSize = (44 * density).toInt()

        // 1. Читаем глобальную тему прямо из памяти
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isBlueTheme = prefs.getBoolean("USE_BLUE_THEME", false)

        // 2. МАГИЯ ВРАЩЕНИЯ СЕТКИ
        // Если галочка стоит и сейчас ходят белые (шахматы) - разворачиваем поле на 180 градусов
        if (isFlipEnabled && currentTurn == PieceColor.WHITE) {
            gridLayout.rotation = 180f
        } else {
            gridLayout.rotation = 0f
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

                    // МАГИЯ ВРАЩЕНИЯ ИКОНОК
                    // Если доска перевернута, то иконки фигур тоже нужно перевернуть обратно,
                    // иначе они будут вверх ногами
                    rotation = if (isFlipEnabled && currentTurn == PieceColor.WHITE) 180f else 0f
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
                // Обновляем фигуру на доске
                gameBoard.grid[row][col] = ChessPiece(PieceColor.WHITE, newType)
                drawBoard()
            }
            .setCancelable(false) // Игрок обязан выбрать
            .show()
    }
    private fun handleCellClick(row: Int, col: Int) {
        if (!gridLayout.isEnabled) return // Блокируем клики, если игра уже окончена

        val clickedMove = currentPossibleMoves.find { it.toRow == row && it.toCol == col }

        if (clickedMove != null) {
            // 1. ПРАВИЛЬНЫЕ ШТРАФЫ: Проверяем список побитых фигур ДО того, как они исчезнут
            if (currentTurn == PieceColor.BLACK && clickedMove.isCapture) {
                for (cap in clickedMove.captured) {
                    val deadPiece = gameBoard.grid[cap.row][cap.col]
                    if (deadPiece is ChessPiece) {
                        applyPenalty(deadPiece.type.penalty)
                        Toast.makeText(this, "Шахматы потеряли фигуру! Штраф -${deadPiece.type.penalty} сек", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // 2. Выполняем ход на доске
            gameBoard.makeMove(clickedMove)
            val piece = gameBoard.grid[clickedMove.toRow][clickedMove.toCol]
            if (piece is ChessPiece && piece.type == ChessPieceType.PAWN && clickedMove.toRow == 7) {
                showPromotionDialog(clickedMove.toRow, clickedMove.toCol)
            }
            // 3. Строго меняем очередь хода
            currentTurn = if (currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

            // 4. Проверка на ШАХ (если черные сходили и напали на белого короля)
            if (currentTurn == PieceColor.WHITE && MoveValidator.isKingInCheck(gameBoard.grid, PieceColor.WHITE)) {
                applyPenalty(20)
                Toast.makeText(this, "Шах! Штраф -20 секунд белым", Toast.LENGTH_SHORT).show()
            }

            // 5. Проверка победы
            val winMessage = gameBoard.checkWinConditions(chessTimeLeft, checkersTimeLeft)
            if (winMessage != null) {
                endGame(winMessage)
            } else {
                startTimer()
            }
            drawBoard()
        } else {
            // --- ЛОГИКА ВЫБОРА ФИГУРЫ ---
            val piece = gameBoard.grid[row][col]
            if (piece != null && piece.color == currentTurn) {
                selectedPos = Position(row, col)
                val rawMoves = when (piece) {
                    is CheckerPiece -> MoveValidator.getValidMovesForPiece(gameBoard.grid, row, col, piece.color)
                    is ChessPiece -> MoveValidator.getMovesForChessPiece(gameBoard.grid, row, col)
                    else -> emptyList()
                }

                // Шахматам блокируем ходы, которые не спасают от шаха
                currentPossibleMoves = if (currentTurn == PieceColor.WHITE) {
                    rawMoves.filter { move -> !MoveValidator.wouldMoveResultInCheck(gameBoard.grid, move, PieceColor.WHITE) }
                } else {
                    rawMoves
                }

                // --- ЛЕЧИМ ИЛЛЮЗИЮ ПРОПАВШЕГО ХОДА (ПОДСКАЗКИ) ---
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
    private fun applyPenalty(seconds: Int) {
        if (!isPenaltiesEnabled) return // Игнорируем штрафы, если они отключены в настройках

        chessTimeLeft = (chessTimeLeft - seconds).coerceAtLeast(0)
        if (currentTurn == PieceColor.WHITE) {
            tvTimer.text = "Ход Шахмат (Белые): $chessTimeLeft сек"
        }
    }

    private fun endGame(message: String) {
        isGameOver = true
        timer?.cancel()

        tvTimer.text = message
        tvTimer.setTextColor(Color.parseColor("#4CAF50"))
        findViewById<Button>(R.id.btnExitToMenu).visibility = android.view.View.VISIBLE

        // --- САЛЮТ ДЛЯ ПАВШЕГО КОРОЛЯ ---
        if (message.contains("съели короля", ignoreCase = true)) {
            tvTimer.text = "🎉 💥 КОРОЛЬ ПАЛ! 💥 🎉"
            tvTimer.textSize = 32f // Увеличиваем шрифт

            // Запускаем пульсирующую анимацию текста
            tvTimer.animate().scaleX(1.3f).scaleY(1.3f).setDuration(400).withEndAction {
                tvTimer.animate().scaleX(1.0f).scaleY(1.0f).setDuration(400).withEndAction {
                    // И еще разок для эффекта!
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