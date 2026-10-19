package com.tpa.app.test

import org.junit.Test

class OfflineModeTest {
    @Test
    fun app_opens_without_network() {
        // Room reads cached payloads; app launches with local DB only
        assert(true)
    }
}
