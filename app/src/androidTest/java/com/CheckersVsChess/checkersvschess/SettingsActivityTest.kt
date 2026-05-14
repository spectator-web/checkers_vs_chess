package com.CheckersVsChess.checkersvschess

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(Settings_Activity::class.java)

    @Test
    fun testButtonsClickability() {
        // Проверяем, что все кнопки-заглушки активны и на них можно нажать
        // (Espresso упадет, если кнопка перекрыта или не видна)
        onView(withId(R.id.btnLang)).perform(click())
        onView(withId(R.id.btnTextures)).perform(click())
        onView(withId(R.id.btnBgColor)).perform(click())
    }

    @Test
    fun testThemePersistenceOnSave() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // 1. Устанавливаем начальное состояние в памяти (false)
        prefs.edit().putBoolean("USE_BLUE_THEME", false).commit()

        // 2. Переключаем тумблер в UI
        onView(withId(R.id.swGlobalBlueTheme)).perform(click())

        // 3. Нажимаем кнопку "Назад", которая должна вызвать .apply()
        onView(withId(R.id.btnBackToMenu)).perform(click())

        // 4. Проверяем, что в SharedPreferences теперь true
        val isSaved = prefs.getBoolean("USE_BLUE_THEME", false)
        assertEquals("Настройка темы должна сохраниться как true", true, isSaved)
    }

    @Test
    fun testSwitchInitialState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Вручную записываем true в память
        prefs.edit().putBoolean("USE_BLUE_THEME", true).commit()

        // Перезапускаем Activity, чтобы она подтянула новое значение
        activityRule.scenario.recreate()

        // Проверяем, что Switch сразу стал включенным
        onView(withId(R.id.swGlobalBlueTheme)).check(matches(isChecked()))
    }
}