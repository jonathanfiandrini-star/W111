package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp-land")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Fluent 11", appName)
    }

    @Test
    fun `taskbar toggles start menu search widgets and action center`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("taskbar_start_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_menu_flyout").assertIsDisplayed()

        composeRule.onNodeWithTag("taskbar_search_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_flyout").assertIsDisplayed()

        composeRule.onNodeWithTag("taskbar_widgets_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("widgets_panel_flyout").assertIsDisplayed()

        composeRule.onNodeWithTag("taskbar_system_tray").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("action_center_flyout").assertIsDisplayed()
    }

    @Test
    fun `window caption bar supports resize minimize restore and close`() {
        composeRule.waitForIdle()
        // Open Launcher Settings window from Start Menu
        composeRule.onNodeWithTag("taskbar_start_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_launcher_settings_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_window_internal_settings").assertIsDisplayed()

        // Resize cycle (Compact -> Split Left -> Split Right -> Maximized)
        composeRule.onNodeWithTag("win_btn_resize_internal_settings").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("win_btn_resize_internal_settings").performClick()
        composeRule.waitForIdle()

        // Minimize keeps window icon in Taskbar
        composeRule.onNodeWithTag("win_btn_minimize_internal_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_window_internal_settings").assertDoesNotExist()
        composeRule.onNodeWithTag("taskbar_win_internal_settings")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        // Restored!
        composeRule.onNodeWithTag("desktop_window_internal_settings").assertIsDisplayed()

        // Close removes window and Taskbar icon
        composeRule.onNodeWithTag("win_btn_close_internal_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_window_internal_settings").assertDoesNotExist()
        composeRule.onNodeWithTag("taskbar_win_internal_settings").assertDoesNotExist()
    }

    @Test
    fun `taskbar window popover allows minimize resize and close`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("taskbar_start_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_launcher_settings_btn").performClick()
        composeRule.waitForIdle()

        // Long-press taskbar window button to open TaskbarWindowControlPopover
        composeRule.onNodeWithTag("taskbar_win_internal_settings")
            .performScrollTo()
            .performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("taskbar_window_popover").assertIsDisplayed()

        // Minimize via popover top-right control
        composeRule.onNodeWithTag("popover_btn_minimize").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_window_internal_settings").assertDoesNotExist()
        composeRule.onNodeWithTag("taskbar_win_internal_settings").assertIsDisplayed()

        // Re-open popover and close window
        composeRule.onNodeWithTag("taskbar_win_internal_settings")
            .performScrollTo()
            .performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("popover_btn_close").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("taskbar_win_internal_settings").assertDoesNotExist()
    }

    @Test
    fun `action center and settings expose default launcher and overlay bar controls`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("taskbar_system_tray").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("action_tile_home").assertIsDisplayed()
        composeRule.onNodeWithTag("action_tile_overlay_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("action_tile_gesture_nav").assertIsDisplayed()

        composeRule.onNodeWithTag("taskbar_start_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_launcher_settings_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_grant_overlay_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_keep_taskbar_switch")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_gesture_nav_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_default_home_btn")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `copilot opens from taskbar and search shortcut`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("taskbar_copilot_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copilot_flyout").assertIsDisplayed()
        composeRule.onNodeWithTag("copilot_input_field").assertIsDisplayed()

        // Switch to Search and jump back into Copilot via shortcut
        composeRule.onNodeWithTag("taskbar_search_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_ask_copilot_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copilot_flyout").assertIsDisplayed()
    }

    @Test
    fun `missing overlay permission shows setup chip and guided launch prompt`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_overlay_setup_chip").assertIsDisplayed()

        composeRule.activity.runOnUiThread {
            val vm = androidx.lifecycle.ViewModelProvider(composeRule.activity)[
                com.example.ui.LauncherViewModel::class.java
            ]
            vm.launchApp("com.example.fakeapp")
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overlay_permission_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("overlay_prompt_grant_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("overlay_prompt_skip_btn")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overlay_permission_dialog").assertDoesNotExist()
    }
}
