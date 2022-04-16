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
import android.widget.Switch
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@SuppressLint("UseSwitchCompatOrMaterialCode")
class MainActivity() : AppCompatActivity() {
    private val logTag = "MainActivity"
    private var region: EnergyRegions = EnergyRegions.DE
    private var country: EnergyPriceTypes = EnergyPriceTypes.DE_LUX
    private val menuMap = HashMap<Int, Int>()
    private var energyMarketData = EnergyMarketData(7, region)
    private val chargeSwitch: Switch by lazy {findViewById(R.id.switch_charge)}
    private val productionSwitch: Switch by lazy {findViewById(R.id.switch_production)}
    private val logText: TextView by lazy{findViewById(R.id.text_logMessages)}
    private val reportText: TextView by lazy {findViewById(R.id.text_report) }
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
        reportText.text = getEnergyText()
        logText.movementMethod = ScrollingMovementMethod()
        registerForContextMenu(lineChart)
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
            energyMarketData = EnergyMarketData(7, region)
            energyText.text = getEnergy()
            swipeRefresher.isRefreshing = false
        }
    }

    private fun getEnergyText(): String {
        return "current session: ${wb.getCurrentEnergy_kWh()} kWh"
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        when (v.id) {
            energyText.id -> {
                menuMap[menu.hashCode()] = energyText.id
                EnergyRegions.values().forEach {
                    menu.add(it.region).setOnMenuItemClickListener { item: MenuItem? ->
                        region = it
                        true
                    }
                }
            }
            lineChart.id -> {
                menuMap[menu.hashCode()] = lineChart.id
                EnergyPriceTypes.values().forEach {
                    menu.add(it.name).setOnMenuItemClickListener { item: MenuItem? ->
                        country = it
                        true
                    }
                }
            }
        }
    }

    override fun onContextMenuClosed(menu: Menu) {
        Log.i(logTag, "menu closed: ${menu.toString()}")
        when (menuMap[menu.hashCode()]) {
            energyText.id -> energyText.text = getEnergy()
            lineChart.id -> getDataAndUpdatePriceChart()
        }
    }

    private fun initializeChart() {
        lineChart.xAxis.labelRotationAngle = 0f
        lineChart.axisRight.isEnabled = true
        lineChart.axisLeft.isEnabled = false

        lineChart.setTouchEnabled(false)
        lineChart.setPinchZoom(false)

        lineChart.description.text = "time"
        lineChart.setNoDataText("No price yet")

        lineChart.animateX(1800, Easing.EaseInExpo)
    }

    private fun updateLineChart(dataArray: ArrayList<List<Float>>, nameArray: ArrayList<String>) {
        // https://medium.com/@yilmazvolkan/kotlinlinecharts-c2a730226ff1

        // refresh does not work yet, therefore: initialize each time
        initializeChart()
        val yFactor = .1f // convert Euro/MWh to Cent/kWH
        var maxY = 0f
        var maxIdX = 0f
        var i = 0
        val colorList = listOf(R.color.purple_200, R.color.purple_500, R.color.purple_700)
        val dataSetArray = ArrayList<ILineDataSet>()
        dataArray.forEach { data ->
            val entries = ArrayList<Entry>()
            var idX = 1f
            data.indices.forEach {
                entries.add(Entry(idX, data[it] * yFactor))
                idX += 1f
            }
            Log.i(logTag, "data entries: %.0f".format(idX))
            maxIdX = max(idX, maxIdX)
            maxY = max(min(Collections.max(data), 100f), maxY)

            val lineDataSet = LineDataSet(entries, "${nameArray[i]} Cent/kWh")
            lineDataSet.setDrawValues(false)
            lineDataSet.setDrawFilled(false)
            lineDataSet.lineWidth = 1f
            lineDataSet.setDrawCircles(false)
            lineDataSet.color = colorList[i % (colorList.lastIndex + 1)]
            //vl.fillColor = R.color.gray
            //vl.fillAlpha = R.color.red

            dataSetArray.add(lineDataSet)
            i++
        }
        lineChart.data = LineData(dataSetArray)

        lineChart.axisRight.granularity = round(maxY * yFactor / 10) * 10 / 4f
        //lineChart.axisRight.axisMaximum = maxY + 1f
        lineChart.xAxis.axisMaximum = maxIdX + 0.1f
        lineChart.data.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        lineChart.animateX(1800, Easing.EaseInElastic)

        reportText.text = getEnergyText()
        //val markerView = CustomMarker(this@ShowForexActivity, R.layout.marker_view)
        //lineChart.marker = markerView
    }

    private fun getDataAndUpdatePriceChart() {
        Log.i(logTag, "getDataAndUpdatePriceChart: $country")
        val countriesOfInterest = arrayOf(country) //(EnergyPriceTypes.DE_LUX, EnergyPriceTypes.NORWAY2)
        val energyMarketDataArray = ArrayList<List<Float>>()
        val energyMarketNamesArray = ArrayList<String>()

        countriesOfInterest.forEach { energyPriceType ->
            energyMarketDataArray.add(energyMarketData.getPriceData(energyPriceType))
            energyMarketNamesArray.add(energyPriceType.name)
        }
        updateLineChart(energyMarketDataArray, energyMarketNamesArray)
    }

    private fun getEnergy(): String {
        val infoStringBuilder = StringBuilder("region: ${region.region}\n")
        //energyMarketData.printMinMax()
        energyMarketData = EnergyMarketData(7, region)
        val (renewable, conventional) = energyMarketData.getCurrentEnergyMix()
        infoStringBuilder.append(
            "renewable: %.2f GWh, conv: %.2f GWh, %.1f%%\n".format(
                renewable / 1000f,
                conventional / 1000f,
                renewable / (renewable + conventional) * 100f
            )
        )
        val load = energyMarketData.getCurrentLoad()
        infoStringBuilder.append(
            "total load: %.2f GWh, excess: %.2f, renew: %.1f%%\n".format(
                load / 1000f,
                (renewable + conventional - load) / 1000f,
                renewable / load * 100f
            )
        )
        val price = energyMarketData.getCurrentPricePerkWh()
        infoStringBuilder.append("price: %.1f Cent/kWh".format(price * 100))
        getDataAndUpdatePriceChart()
        return infoStringBuilder.toString()
    }
}
