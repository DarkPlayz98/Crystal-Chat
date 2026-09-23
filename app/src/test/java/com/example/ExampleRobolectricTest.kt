package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.crypto.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
    assertEquals("Crystal Chat", appName)
  }

  @Test
  fun `crypto engine encrypts and decrypts correctly`() {
    val secretMessage = "Top secret post-quantum cipher test"
    val payload = CryptoEngine.encrypt(secretMessage)

    assertNotEquals(secretMessage, payload.cipherTextBase64)
    assertTrue(payload.authTagHex.isNotEmpty())
    assertTrue(payload.nonceBase64.isNotEmpty())

    val decrypted = CryptoEngine.decrypt(payload.cipherTextBase64, payload.nonceBase64)
    assertEquals(secretMessage, decrypted)
  }

  @Test
  fun `safety numbers format correctly into 12 blocks of 5 digits`() {
    val safetyNumber = CryptoEngine.generateSafetyNumber("alice_pubkey", "bob_pubkey")
    val blocks = safetyNumber.split(" ")

    assertEquals(12, blocks.size)
    for (block in blocks) {
      assertEquals(5, block.length)
      assertTrue(block.all { it.isDigit() })
    }
  }
}

