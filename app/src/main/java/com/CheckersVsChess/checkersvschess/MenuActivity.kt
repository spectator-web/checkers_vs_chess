package com.CheckersVsChess.checkersvschess

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        findViewById<Button>(R.id.btnSinglePlayer).setOnClickListener {
            Toast.makeText(this, "Пока в разработке!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Пока в разработке!", Toast.LENGTH_SHORT).show()
        }

        // Переход на экран настроек партии
        findViewById<Button>(R.id.btnMultiplayer).setOnClickListener {
            val intent = Intent(this, GameSettingsActivity::class.java)
            startActivity(intent)
            startActivity(Intent(this, Settings_Activity::class.java))
        }
    }
}