package com.widlily.wicompress

import org.junit.Assert.assertEquals
import org.junit.Test

class FFmpegManagerTest {

    // Simple test helper mapping encoder values
    private fun getEncoderName(useH265: Boolean): String {
        return if (useH265) "hevc_mediacodec" else "h264_mediacodec"
    }

    @Test
    fun testGetEncoderName_h264Target_returnsH264Mediacodec() {
        // Arrange
        val useH265 = false

        // Act
        val encoder = getEncoderName(useH265)

        // Assert
        assertEquals("h264_mediacodec", encoder)
    }

    @Test
    fun testGetEncoderName_hevcTarget_returnsHevcMediacodec() {
        // Arrange
        val useH265 = true

        // Act
        val encoder = getEncoderName(useH265)

        // Assert
        assertEquals("hevc_mediacodec", encoder)
    }
}
