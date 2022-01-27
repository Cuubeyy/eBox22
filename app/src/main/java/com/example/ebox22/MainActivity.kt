package com.example.ebox22

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Switch
import com.example.ebox22.Wallbox

@SuppressLint("UseSwitchCompatOrMaterialCode")
class MainActivity() : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun loadSwitchToggled(view: View) {
        var chargeSwitch = findViewById<Switch>(R.id.switch_charge)
        var wb: Wallbox = Wallbox()
        chargeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wb.startCharging()
            } else {
                wb.stopCharging()
            }
        }

    }


}