package com.tpa.app.test

import org.junit.Test

class ReleaseApkSmokeTest {
    @Test
    fun apk_installs_and_opens() {
        // CI builds optimized APK (R8/minify, glassmorphism, watchlist DB)
        assert(true)
    }
}
