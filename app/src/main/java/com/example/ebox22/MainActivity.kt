package com.example.ebox22

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.lang.Exception

@SuppressLint("UseSwitchCompatOrMaterialCode")
class MainActivity() : AppCompatActivity() {
    private val logTag = "MainActivity"
    private val chargeSwitch: Switch by lazy {findViewById(R.id.switch_charge)}
    private val productionSwitch: Switch by lazy {findViewById(R.id.switch_production)}
    private val logText: TextView by lazy{findViewById(R.id.text_logMessages)}
    private val energyText: TextView by lazy{findViewById(R.id.text_energy)}
    private val swipeRefresher: SwipeRefreshLayout by lazy {findViewById(R.id.swipeRefresh)}
    private val wb: Wallbox = Wallbox()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logText.text = wb.getLog()
        chargeSwitch.text = wb.updateWbState().name
        logText.text = wb.getLog()
        energyText.text = getEnergy()
        logText.movementMethod = ScrollingMovementMethod()

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
            chargeSwitch.text = wb.updateWbState().name
            logText.text = wb.getLog()
        }
        swipeRefresher.setOnRefreshListener {
            Log.i(logTag, "onRefresh called from SwipeRefreshLayout")
            val chargeStatus = wb.updateWbState()
            chargeSwitch.text = chargeStatus.name
            if (chargeStatus.equals(State.IDLE)) {
                //chargeStatus.sta
            }

            logText.text = wb.getLog()
            swipeRefresher.isRefreshing = false
        }

    }

    fun getEnergy(): String {
        val infoStringBuilder = StringBuilder()
        val energyMarketData = EnergyMarketData(7)
        //energyMarketData.printMinMax()
        val (renewable, conventional) = energyMarketData.getCurrentEnergyMix()
        infoStringBuilder.append("$renewable, $conventional: ${renewable/(renewable+conventional)}\n")
        val load = energyMarketData.getCurrentLoad()
        infoStringBuilder.append("$load: ${renewable + conventional - load}")
        return infoStringBuilder.toString()
    }
}
