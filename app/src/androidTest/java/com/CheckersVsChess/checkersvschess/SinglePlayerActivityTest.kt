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
class SinglePlayerActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SinglePlayerActivity::class.java)

    @Before
    fun setUp() {
        Intents.init() // Начинаем отлавливать переходы между экранами
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testLiveTimeCalculationSingle() {
        // Проверяем, что коэффициент 1:4 работает и здесь
        onView(withId(R.id.etChessTimeSingle))
            .perform(clearText(), typeText("40"), closeSoftKeyboard())

        onView(withId(R.id.tvCheckersTimeSingle))
            .check(matches(withText(containsString("160 сек"))))
    }

    @Test
    fun testStartGameAsCheckersPlayer() {
        // 1. Выбираем RadioButton "Шашки"
        onView(withId(R.id.rbCheckers)).perform(click())

        // 2. Устанавливаем время
        onView(withId(R.id.etChessTimeSingle))
            .perform(clearText(), typeText("50"), closeSoftKeyboard())

        // 3. Выключаем штрафы (просто для разнообразия теста)
        onView(withId(R.id.swPenaltiesSingle)).perform(click())

        // 4. Жмем старт
        onView(withId(R.id.btnStartSingleGame)).perform(click())

        // 5. Проверяем, что в MainActivity улетели ПРАВИЛЬНЫЕ настройки
        intended(allOf(
            hasComponent(MainActivity::class.java.name),
            hasExtra("IS_SINGLE_PLAYER", true),
            hasExtra("PLAYER_IS_BLACK", true), // Игрок выбрал шашки
            hasExtra("USE_PENALTIES", false),
            hasExtra("CHESS_TIME", 50),
            hasExtra("CHECKERS_TIME", 200)
        ))
    }

    @Test
    fun testValidationTimeShortSingle() {
        // Проверка лимита в 30 секунд (защита от "блица", который бот не переварит)
        onView(withId(R.id.etChessTimeSingle))
            .perform(clearText(), typeText("20"), closeSoftKeyboard())

        onView(withId(R.id.btnStartSingleGame)).perform(click())

        // MainActivity не должен запуститься, мы остаемся на том же экране
        onView(withId(R.id.btnStartSingleGame)).check(matches(isDisplayed()))
    }
}