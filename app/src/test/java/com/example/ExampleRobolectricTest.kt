package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
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
    assertEquals("KIFIN", appName)
  }

  @Test
  fun `test json backup format with robolectric`() {
    val json = JSONObject().apply {
      put("appName", "KIFIN")
      put("accountEmail", "user@gmail.com")
    }
    assertEquals("KIFIN", json.getString("appName"))
    assertEquals("user@gmail.com", json.getString("accountEmail"))
  }
}
