package com.CheckersVsChess.checkersvschess

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SinglePlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_player)

        val etChessTime = findViewById<EditText>(R.id.etChessTimeSingle)
        val tvCheckersTime = findViewById<TextView>(R.id.tvCheckersTimeSingle)
        val swPenalties = findViewById<Switch>(R.id.swPenaltiesSingle)

        // Авто-расчет времени 1 к 4
        etChessTime.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val time = s.toString().toIntOrNull() ?: 0
                tvCheckersTime.text = "Время для Шашек: ${time * 4} сек"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<Button>(R.id.btnBackFromSingle).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnStartSingleGame).setOnClickListener {
            val chessTime = etChessTime.text.toString().toIntOrNull() ?: 60
            if (chessTime <= 30) {
                Toast.makeText(this, "Время должно быть > 30 сек", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isPlayingCheckers = findViewById<RadioButton>(R.id.rbCheckers).isChecked

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("CHESS_TIME", chessTime)
                putExtra("CHECKERS_TIME", chessTime * 4)
                putExtra("USE_PENALTIES", swPenalties.isChecked)
                putExtra("IS_SINGLE_PLAYER", true)
                putExtra("PLAYER_IS_BLACK", isPlayingCheckers)
                // В одиночке нам не нужно крутить доску по кругу,
                // мы просто установим её один раз в начале
                putExtra("USE_FLIP_BOARD", false)
            }
            startActivity(intent)
            finish()
        }
    }
}