package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PresetCategory
import com.example.data.model.PresetRepository
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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FotoGemini", appName)
  }

  @Test
  fun `verify edit presets exist`() {
    val presets = PresetRepository.presets
    assertTrue(presets.isNotEmpty())
    assertTrue(presets.any { it.category == PresetCategory.ARTISTIC })
    assertTrue(presets.any { it.category == PresetCategory.BACKGROUND })
  }
}
