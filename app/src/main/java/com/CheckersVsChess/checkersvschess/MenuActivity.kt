package com.CheckersVsChess.checkersvschess

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // ОДИНОЧНАЯ ИГРА
        findViewById<Button>(R.id.btnSinglePlayer).setOnClickListener {
            startActivity(Intent(this, SinglePlayerActivity::class.java))
        }

        // СОВМЕСТНАЯ ИГРА
        findViewById<Button>(R.id.btnMultiplayer).setOnClickListener {
            startActivity(Intent(this, GameSettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnLang).setOnClickListener {
            Toast.makeText(this, "Пока в разработке!", Toast.LENGTH_SHORT).show()
        }

        val swTheme = findViewById<Switch>(R.id.swGlobalBlueTheme)
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        swTheme.isChecked = prefs.getBoolean("USE_BLUE_THEME", false)

        swTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("USE_BLUE_THEME", isChecked).apply()
        }
    }
}