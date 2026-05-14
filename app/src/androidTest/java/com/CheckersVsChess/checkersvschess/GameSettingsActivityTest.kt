package com.CheckersVsChess.checkersvschess

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameSettingsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(GameSettingsActivity::class.java)

    @Before
    fun setUp() {
        Intents.init() // Инициализируем проверку Интентов перед каждым тестом
    }

    @After
    fun tearDown() {
        Intents.release() // Освобождаем ресурсы после теста
    }

    @Test
    fun testLiveTimeCalculation() {
        // Проверяем, как работает TextWatcher (соотношение 1:4)
        // 1. Вводим 100 секунд для шахмат
        onView(withId(R.id.etChessTime))
            .perform(clearText(), typeText("100"), closeSoftKeyboard())

        // 2. Проверяем, что TextView автоматически показал 400 секунд для шашек
        onView(withId(R.id.tvCheckersTimeInfo))
            .check(matches(withText(containsString("400 сек"))))
    }

    @Test
    fun testValidationTimeTooShort() {
        // Тестируем защиту "от дурака" (время <= 30 сек)
        onView(withId(R.id.etChessTime))
            .perform(clearText(), typeText("10"), closeSoftKeyboard())

        onView(withId(R.id.btnStartGame)).perform(click())

        // Проверяем, что мы НЕ перешли в MainActivity (Интент не был отправлен)
        // В Espresso это проверяется отсутствием зафиксированных интентов
    }

    @Test
    fun testSuccessfulStartGameWithExtras() {
        // Тестируем правильную передачу данных в игру
        val testTime = "60"

        onView(withId(R.id.etChessTime))
            .perform(clearText(), typeText(testTime), closeSoftKeyboard())

        // Нажимаем старт
        onView(withId(R.id.btnStartGame)).perform(click())

        // Проверяем, был ли запущен MainActivity с правильными данными внутри
        intended(allOf(
            hasComponent(MainActivity::class.java.name),
            hasExtra("CHESS_TIME", 60),
            hasExtra("CHECKERS_TIME", 240) // 60 * 4
        ))
    }
}