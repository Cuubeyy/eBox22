package com.example.ebox22

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun wallboxConnectionWorks() {
        val wb: Wallbox = Wallbox()
        assertEquals(wb.updateWbState(), State.IDLE.name)
    }

    @Test
    fun mainActivityTest() {
        val wb: Wallbox = Wallbox()
        wb.toggleSimulate()
        wb.startCharging()
        wb.getLog()
        Thread.sleep(2000)
        wb.stopCharging()
        wb.getLog()
        val status = wb.updateWbState()
        wb.getLog()
        assertEquals(status,"IDLE")
    }

    @Test
    fun wallboxStart() {
        val wb: Wallbox = Wallbox()
        val success = wb.startCharging()
        assertTrue(success)
    }

    @Test
    fun wallboxStop() {
        val wb: Wallbox = Wallbox()
        val success = wb.stopCharging()
        assertTrue(success)
    }
}