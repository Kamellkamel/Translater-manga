package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MangaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Manga Translator Reader", appName)
  }

  @Test
  fun `instantiate viewmodel`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    try {
      val viewModel = MangaViewModel(context)
      assertNotNull(viewModel)
      assertNotNull(viewModel.scrapers)
      assertEquals(2, viewModel.scrapers.size)
    } catch (e: Throwable) {
      throw IllegalStateException("TEST EXCEPTION ENCOUNTERED: ${e.message}\nStacktrace: ${e.stackTraceToString()}", e)
    }
  }

  @Test
  fun `launch main activity`() {
    try {
      androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        assertNotNull(scenario)
      }
    } catch (e: Throwable) {
      throw IllegalStateException("ACTIVITY LAUNCH EXCEPTION: ${e.message}\nStacktrace: ${e.stackTraceToString()}", e)
    }
  }
}
