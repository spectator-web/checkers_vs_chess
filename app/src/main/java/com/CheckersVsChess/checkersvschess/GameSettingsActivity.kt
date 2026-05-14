package com.CheckersVsChess.checkersvschess

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class GameSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_settings)

        // --- КНОПКА НАЗАД ---
        findViewById<Button>(R.id.btnBackFromSettings)?.setOnClickListener {
            finish()
        }

        val etChessTime = findViewById<EditText>(R.id.etChessTime)
        val tvCheckersTimeInfo = findViewById<TextView>(R.id.tvCheckersTimeInfo)

        etChessTime.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val chessTime = s.toString().toIntOrNull() ?: 0
                tvCheckersTimeInfo.text = "Время для Шашек: ${chessTime * 4} сек (1:4)"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<Button>(R.id.btnStartGame).setOnClickListener {
            val chessTime = etChessTime.text.toString().toIntOrNull() ?: 60

            if (chessTime <= 30) {
                Toast.makeText(this, "Время шахмат должно быть > 30 сек", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usePenalties = findViewById<Switch>(R.id.swPenalties).isChecked
            val useFlipBoard = findViewById<Switch>(R.id.swFlipBoard).isChecked

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("CHESS_TIME", chessTime)
                putExtra("CHECKERS_TIME", chessTime * 4)
                putExtra("USE_PENALTIES", usePenalties)
                putExtra("USE_FLIP_BOARD", useFlipBoard)
            }
            startActivity(intent)
            finish()
        }
    }
}