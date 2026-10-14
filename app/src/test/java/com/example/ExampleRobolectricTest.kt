package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.actions.AssistantAction
import com.example.domain.actions.DeviceActionHandler
import com.example.ui.components.MarkdownSegment
import com.example.ui.components.parseMarkdownSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string resource from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Miya AI", appName)
    }

    @Test
    fun `detect alarm intent from text`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = DeviceActionHandler(context)

        val action = handler.detectActionFromText("set alarm for 7:30 am")
        assertTrue(action is AssistantAction.SetAlarm)
        val alarm = action as AssistantAction.SetAlarm
        assertEquals(7, alarm.hour)
        assertEquals(30, alarm.minute)
    }

    @Test
    fun `detect timer intent from text`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = DeviceActionHandler(context)

        val action = handler.detectActionFromText("set a timer for 15 minutes")
        assertTrue(action is AssistantAction.SetTimer)
        val timer = action as AssistantAction.SetTimer
        assertEquals(15 * 60, timer.durationSeconds)
    }

    @Test
    fun `detect app launch intent from text`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = DeviceActionHandler(context)

        val action = handler.detectActionFromText("open youtube")
        assertTrue(action is AssistantAction.OpenApp)
        val app = action as AssistantAction.OpenApp
        assertEquals("Youtube", app.appName)
    }

    @Test
    fun `markdown parser segments code and text correctly`() {
        val markdown = """
            Here is a Kotlin function:
            ```kotlin
            fun hello() { println("Hi Miya") }
            ```
            * Bullet 1
            * Bullet 2
        """.trimIndent()

        val segments = parseMarkdownSegments(markdown)
        assertTrue(segments.any { it is MarkdownSegment.CodeBlock })
        assertTrue(segments.any { it is MarkdownSegment.BulletItem })
    }
}
