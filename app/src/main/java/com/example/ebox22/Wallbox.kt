package com.example.ebox22

enum class State {
    UNPLUGGED, IDLE, CHARGING
}

class Wallbox {
    var status: State = State.UNPLUGGED
        private set
    init {
        status = this.updateInfo()
    }

    private fun updateInfo(): State {
        val info = getWbInfo()
        return if (info.contains("is_plugged")) {
            if (info.contains("is_charging")) {
                State.CHARGING
            } else {
                State.IDLE
            }
        }
        else {
            State.UNPLUGGED
        }
    }

    private fun getWbInfo(): String {
        return "is_plugged"
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