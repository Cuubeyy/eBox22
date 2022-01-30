package com.example.ebox22

enum class State {
    UNPLUGGED, IDLE, CHARGING
}

class Wallbox {
    private var wbKeba = WallboxKeba()
    private var status: State = State.IDLE
    init {
        updateWbState()
    }

    fun updateWbState(): String {
        return status.name
        status = wbKeba.getStatus()
        val stateStr = status.name
        println("  new status: ${stateStr}")
        return stateStr
    }

    fun startCharging(): Boolean {
        if (this.status == State.IDLE) {
            val success = this.doStart()
            if (success) {
                this.status = State.CHARGING
            }
        }
        return this.status == State.CHARGING
    }

    fun stopCharging(): Boolean {
        if (this.status == State.CHARGING) {
            val success = this.doStop()
            if (success) {
                this.status = State.IDLE
            }
        }
        return this.status == State.IDLE
    }

    private fun doStart(): Boolean {
        // TODO: start wallbox
        return true
    }

    private fun doStop(): Boolean {
        // TODO: stop wallbox
        return true
    }
}