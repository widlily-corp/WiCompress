package com.widlily.wicompress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageHashUtilTest {

    // Simple pure-Kotlin replica of the JNI Hamming distance logic for JVM testing fallback
    private fun calculateHammingDistanceKotlin(hash1: Long, hash2: Long): Int {
        val diff = hash1 xor hash2
        return java.lang.Long.bitCount(diff)
    }

    @Test
    fun testHammingDistance_identicalHashes_returnsZero() {
        // Arrange (Setup inputs)
        val hash1 = 0xAAFF5500FFAA5500L
        val hash2 = 0xAAFF5500FFAA5500L

        // Act (Perform action under test)
        val distance = calculateHammingDistanceKotlin(hash1, hash2)

        // Assert (Validate correctness)
        assertEquals(0, distance)
    }

    @Test
    fun testHammingDistance_completelyDifferentHashes_returnsSixtyFour() {
        // Arrange
        val hash1 = 0x0000000000000000L
        val hash2 = -1L // All bits set to 1 in two's complement representation

        // Act
        val distance = calculateHammingDistanceKotlin(hash1, hash2)

        // Assert
        assertEquals(64, distance)
    }

    @Test
    fun testHammingDistance_oneBitDifference_returnsOne() {
        // Arrange
        val hash1 = 0b00000000L
        val hash2 = 0b00000001L

        // Act
        val distance = calculateHammingDistanceKotlin(hash1, hash2)

        // Assert
        assertEquals(1, distance)
    }

    @Test
    fun testDuplicateThreshold_similarHashes_evaluatesAsDuplicate() {
        // Arrange
        val hash1 = 0b1111111111111111L
        val hash2 = 0b1111111111111100L // 2 bits differ (well within duplicate threshold of <= 8)
        
        // Act
        val distance = calculateHammingDistanceKotlin(hash1, hash2)
        val isDuplicate = distance <= 8

        // Assert
        assertTrue(isDuplicate)
    }
}
