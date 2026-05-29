package com.example

import com.example.data.local.EncryptionUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testEncryptionUtilityIntegrity() {
    val sampleApiKey = "nvapi-someMockSecret321KeyForNvidia"
    val encrypted = EncryptionUtils.encrypt(sampleApiKey)
    assertNotEquals(sampleApiKey, encrypted)
    
    val decrypted = EncryptionUtils.decrypt(encrypted)
    assertEquals(sampleApiKey, decrypted)
  }

  @Test
  fun testEncryptionBlankValues() {
    assertEquals("", EncryptionUtils.encrypt(""))
    assertEquals("", EncryptionUtils.decrypt(""))
  }
}
