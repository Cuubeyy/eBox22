package com.example.ebox22

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView

@SuppressLint("UseSwitchCompatOrMaterialCode")
class MainActivity() : AppCompatActivity() {
    private val chargeSwitch: Switch by lazy {findViewById(R.id.switch_charge)}
    private val chargingStateText: TextView by lazy{findViewById(R.id.text_chargingState)}
    private val wb: Wallbox = Wallbox()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        chargingStateText.text = wb.updateWbState()
    }

    fun loadSwitchToggled(view: View) {
        chargeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wb.startCharging()
            } else {
                wb.stopCharging()
            }
            chargingStateText.text = wb.updateWbState()
        }
    }
}
