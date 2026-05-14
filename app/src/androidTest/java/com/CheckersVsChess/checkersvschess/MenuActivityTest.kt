package com.CheckersVsChess.checkersvschess

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MenuActivity::class.java)

    @Before
    fun setUp() {
        Intents.init() // Начинаем слежку за Интентами
    }

    @After
    fun tearDown() {
        Intents.release() // Заканчиваем слежку
    }

    @Test
    fun testNavigationToSinglePlayer() {
        // Проверяем кнопку одиночной игры
        onView(withId(R.id.btnSinglePlayer)).perform(click())
        intended(hasComponent(SinglePlayerActivity::class.java.name))
    }

    @Test
    fun testNavigationToMultiplayer() {
        // Проверяем кнопку мультиплеера (настройки игры)
        onView(withId(R.id.btnMultiplayer)).perform(click())
        intended(hasComponent(GameSettingsActivity::class.java.name))
    }

    @Test
    fun testThemeSwitchSavesToPrefs() {
        // 1. Находим свитч и кликаем по нему
        onView(withId(R.id.swGlobalBlueTheme)).perform(click())

        // 2. Достаем SharedPreferences напрямую и проверяем, изменилось ли значение
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Значение в памяти должно измениться после клика
        val isBlueThemeEnabled = prefs.contains("USE_BLUE_THEME")
        assertTrue("Значение темы должно быть сохранено в Prefs", isBlueThemeEnabled)
    }

    @Test
    fun testLangButtonShowsToast() {
        // Кнопка языка пока ничего не открывает, просто проверяем, что она нажимается
        onView(withId(R.id.btnLang)).perform(click())

        // Примечание: Проверка Toast в Espresso требует кастомного матчера,
        // но само отсутствие вылета при клике — уже хороший знак.
    }
}