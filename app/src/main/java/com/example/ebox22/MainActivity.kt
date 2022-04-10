package com.example.ebox22

/* TODO List
  [.] add line graph for e.g. price history
  [ ] add main menu
  [ ] add config tab
  [ ] add explanation tab
 */

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Switch
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round

@SuppressLint("UseSwitchCompatOrMaterialCode")
class MainActivity() : AppCompatActivity() {
    private val logTag = "MainActivity"
    private var region: EnergyRegions = EnergyRegions.DE
    private val chargeSwitch: Switch by lazy {findViewById(R.id.switch_charge)}
    private val productionSwitch: Switch by lazy {findViewById(R.id.switch_production)}
    private val logText: TextView by lazy{findViewById(R.id.text_logMessages)}
    private val lineChart: LineChart by lazy {findViewById(R.id.lineChart)}
    private val energyText: TextView by lazy{findViewById(R.id.text_energy)}
    private val swipeRefresher: SwipeRefreshLayout by lazy {findViewById(R.id.swipeRefresh)}
    private val wb: Wallbox = Wallbox()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logText.text = wb.getLog()
        chargeSwitch.text = wb.updateWbState().name
        logText.text = wb.getLog()
        logText.movementMethod = ScrollingMovementMethod()
        registerForContextMenu(energyText)
        energyText.text = getEnergy()
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
            energyText.text = getEnergy()
            swipeRefresher.isRefreshing = false
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        val contextMenuTextView = v as TextView
        // Add menu items via menu.add
        EnergyRegions.values().forEach {
            menu.add(it.region).setOnMenuItemClickListener { item: MenuItem? ->
                region = it
                true
            }
        }
    }

    override fun onContextMenuClosed(menu: Menu) {
        energyText.text = getEnergy()
    }

    private fun updateLineChart(data: List<Float>) {
        val yFactor = .1f
        // https://medium.com/@yilmazvolkan/kotlinlinecharts-c2a730226ff1
        val entries = ArrayList<Entry>()
        var idX = 1f
        data.indices.forEach {
            entries.add(Entry(idX, data[it]*yFactor))
            idX += 1f
        }
        Log.i(logTag, "data entries: %.0f".format(idX))

        val vl = LineDataSet(entries, "price Cent/kWh")

        vl.setDrawValues(false)
        vl.setDrawFilled(true)
        vl.lineWidth = 2f
        //vl.fillColor = R.color.gray
        //vl.fillAlpha = R.color.red
        lineChart.xAxis.labelRotationAngle = 0f
        lineChart.data = LineData(vl)
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.axisMaximum = idX + 0.1f
        lineChart.axisLeft.granularity = round(Collections.max(data)*yFactor/10)*10/2f

        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)

        lineChart.description.text = "time"
        lineChart.setNoDataText("No price yet")

        lineChart.animateX(1800, Easing.EaseInExpo)

        //val markerView = CustomMarker(this@ShowForexActivity, R.layout.marker_view)
        //lineChart.marker = markerView
    }

    private fun getEnergy(): String {
        val infoStringBuilder = StringBuilder("region: ${region.region}\n")
        val energyMarketData = EnergyMarketData(7, region)
        //energyMarketData.printMinMax()
        val (renewable, conventional) = energyMarketData.getCurrentEnergyMix()
        infoStringBuilder.append("renewable: %.2f GWh, conv: %.2f GWh, %.1f%%\n".format(renewable/1000f, conventional/1000f, renewable/(renewable+conventional)*100f))
        val load = energyMarketData.getCurrentLoad()
        infoStringBuilder.append("total load: %.2f GWh, excess: %.2f, renew: %.1f%%\n".format(load/1000f, (renewable + conventional - load)/1000f, renewable/load*100f))
        val price = energyMarketData.getCurrentPricePerkWh()
        infoStringBuilder.append("price: %.3f€/kWh".format(price))
        updateLineChart(energyMarketData.getPriceData())
        return infoStringBuilder.toString()
    }
}
