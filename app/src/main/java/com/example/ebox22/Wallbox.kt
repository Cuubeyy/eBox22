package com.example.ebox22

import android.util.Log

enum class State {
    UNPLUGGED, IDLE, CHARGING
}

class Wallbox {
    private val logTag = "Wallbox"
    private var simulate: Boolean = true
    private var wbKeba = WallboxKeba()
    private var status: State = State.IDLE
    private var lastLog: String = "<start>\n"
    init {
        updateWbState()
    }

    fun toggleSimulate() {
        simulate = !simulate
        lastLog += "simulate: $simulate\n"
    }

    fun updateWbState(): State {
        if (this.simulate) return status

        lastLog += "getting Status\n"

        status = wbKeba.getStatus()

        lastLog += "${status.name}\n"
        val stateStr = status.name
        Log.d(logTag, "  new status: ${stateStr}")
        return status
    }

    fun startCharging(): Boolean {
        lastLog += "starting to charge\n"
        if (this.status == State.IDLE) {
            val success = this.doStartWallbox()
            lastLog += "success: {$success}\n"
            if (success) {
                this.status = State.CHARGING
            }
        }
        lastLog += "done starting to charge\n"
        return this.status == State.CHARGING
    }

    fun stopCharging(): Boolean {
        lastLog += "stopping to charge\n"
        if (this.status == State.CHARGING) {
            val success = this.doStopWallbox()
            lastLog += "success: {$success}\n"
            if (success) {
                this.status = State.IDLE
            }
        }
        lastLog += "done stopping to charge\n"
        return this.status == State.IDLE
    }

    private fun doStartWallbox(): Boolean {
        if (this.simulate) return true

        lastLog += wbKeba.startCharging()

        Thread.sleep(2000)
        status = wbKeba.getStatus()
        return status == State.CHARGING
    }

    private fun doStopWallbox(): Boolean {
        if (this.simulate) return true

        lastLog += wbKeba.stopCharging()
        Thread.sleep(2000)
        status = wbKeba.getStatus()
        return status == State.IDLE
    }

    fun getLog(): String {
        return this.lastLog
    }
}