package com.example.pearpressure.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.pearpressure.MainViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StopwatchScreenAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        viewModel = MainViewModel()

        composeRule.setContent {
            MaterialTheme {
                StopwatchScreen(
                    viewModel = viewModel,
                    examId = "test_exam_id",
                    showTitle = true,
                    bottomInfoText = "2 days until the exam.",
                    onBack = {},
                    onStudyStarted = {}
                )
            }
        }
    }

    @Test
    fun stopwatchScreen_showsCoreElements() {
        composeRule.onNodeWithText("Stopwatch").assertIsDisplayed()
        composeRule.onNodeWithText("Time").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsDisplayed()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()
        composeRule.onNodeWithText("Finish").assertIsDisplayed()
        composeRule.onNodeWithText("Quick actions").assertIsDisplayed()
        composeRule.onNodeWithText("Poop").assertIsDisplayed()
        composeRule.onNodeWithText("Water").assertIsDisplayed()

        composeRule.onNodeWithText("Participants")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun clickingPoop_opensConfirmationDialog() {
        composeRule.onNodeWithText("Poop").performClick()

        composeRule
            .onNodeWithText("Are you sure you want to add 1 poop?")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Add").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun clickingWater_opensWaterDialog() {
        composeRule.onNodeWithText("Water").performClick()

        composeRule.onNodeWithText("Add water (liters)").assertIsDisplayed()
        composeRule.onNodeWithText("Liters (e.g. 0.5)").assertIsDisplayed()
    }
}