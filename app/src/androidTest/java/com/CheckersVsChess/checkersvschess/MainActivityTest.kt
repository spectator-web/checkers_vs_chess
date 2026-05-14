package com.CheckersVsChess.checkersvschess

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val TAG = "GAME_UI_TEST"

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testFullGameFlow() {
        Log.i(TAG, "--- Начало большого теста интерфейса ---")

        // 1. Проверяем начальный текст (Ход шахмат)
        Log.i(TAG, "Проверка текста начального хода...")
        onView(withId(R.id.tvTimer))
            .check(matches(withText(containsString("Ход Шахмат"))))

        // 2. Имитируем сдачу игрока
        Log.i(TAG, "Нажатие кнопки сдачи (меню)...")
        onView(withId(R.id.btnSmallMenu)).perform(click())

        // 3. Проверяем, что появилось окно финала
        Log.i(TAG, "Проверка появления кнопки выхода...")
        onView(withId(R.id.btnExitToMenu))
            .check(matches(isDisplayed()))

        Log.i(TAG, "--- Тест успешно завершен ---")
    }
}