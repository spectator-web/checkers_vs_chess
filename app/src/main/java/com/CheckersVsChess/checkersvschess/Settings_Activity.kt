package com.CheckersVsChess.checkersvschess

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Settings_Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Заглушки
        findViewById<Button>(R.id.btnLang).setOnClickListener { showToast() }
        findViewById<Button>(R.id.btnTextures).setOnClickListener { showToast() }
        findViewById<Button>(R.id.btnBgColor).setOnClickListener { showToast() }

        val swTheme = findViewById<Switch>(R.id.swGlobalBlueTheme)

        // Читаем сохраненную настройку (по умолчанию false)
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        swTheme.isChecked = prefs.getBoolean("USE_BLUE_THEME", false)

        // Сохраняем настройку при выходе
        findViewById<Button>(R.id.btnBackToMenu).setOnClickListener {
            prefs.edit().putBoolean("USE_BLUE_THEME", swTheme.isChecked).apply()
            finish()
        }
    }

    private fun showToast() {
        Toast.makeText(this, "Пока недоступно", Toast.LENGTH_SHORT).show()
    }
}