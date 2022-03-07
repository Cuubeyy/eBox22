package com.example.ebox22

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Switch
import android.widget.TextView

@SuppressLint("UseSwitchCompatOrMaterialCode")
class MainActivity() : AppCompatActivity() {
    private val chargeSwitch: Switch by lazy {findViewById(R.id.switch_charge)}
    private val productionSwitch: Switch by lazy {findViewById(R.id.switch_production)}
    private val chargingStateText: TextView by lazy{findViewById(R.id.text_chargingState)}
    private val logText: TextView by lazy{findViewById(R.id.text_logMessages)}
    private val wb: Wallbox = Wallbox()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logText.text = wb.getLog()
        chargingStateText.text = wb.updateWbState()
        logText.text = wb.getLog()
        logText.setMovementMethod(ScrollingMovementMethod());
    }

    fun loadSwitchToggled(view: View) {
        logText.text = wb.getLog()
        productionSwitch.setOnCheckedChangeListener { _, isChecked ->
            wb.toggleSimulate()
            logText.text = wb.getLog()
        }
        chargeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wb.startCharging()
                logText.text = wb.getLog()
            } else {
                wb.stopCharging()
                logText.text = wb.getLog()
            }
            chargingStateText.text = wb.updateWbState()
            logText.text = wb.getLog()
        }
    }
}
