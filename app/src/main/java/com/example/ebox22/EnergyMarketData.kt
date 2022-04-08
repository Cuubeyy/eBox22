package com.example.ebox22

// https://www.techiedelight.com/send-http-get-post-requests-kotlin/
import android.util.Log
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.reflect.typeOf

enum class EnergyProductionTypes(val id: Long, val isRenewable: Boolean) {
    DATE(0, false),
    TIME_OF_DAY(1, false),
    BIOMASS(1001224,true),
    HYDROPOWER(1004066,true),
    WIND_OFFSHORE(1004067,true),
    WIND_ONSHORE(1004068,true),
    PHOTOVOLTAICS(1001223,true),
    OTHER_RENEWABLE(1004069,true),
    NUCLEAR(1004071,false),
    LIGNITE(1004070,false),
    HARD_COLE(1001226,false),
    FOSSIL_GAS(1001228,false),
    HYDRO_PUMPED_STORAGE_OUT(1001227,true),
    OTHER_CONVENTIONAL(1001225, false)
}

enum class EnergyConsumptionTypes(val id: Long) {
    DATE(0),
    TIME_OF_DAY(1),
    TOTAL_GRID_LOAD(5000410),
    RESIDUAL_LOAD(5004387),
    HYDRO_PUMPED_STORAGE_IN(5004359)
}
enum class EnergyPriceTypes(val id: Long) {
    DATE(0),
    TIME_OF_DAY(1),
    DE_LUX(8004169),
    DENMARK1(8004170),
    DENMARK2(8004996),
    FRANCE(8004997),
    NORTH_ITALY(8000251),
    NETHERLANDS(8000252),
    POLAND(8000253),
    SWEDEN4(8000254),
    SWITZERLAND(8000255),
    SLOVENIA(8000256),
    CZECH_REPUBLIC(8000257),
    HUNGARY(8000258),
    AUSTRIA(8000259),
    BELGIUM(8000261),
    NORWAY2(8000262)
}

class EnergyMarketData(var offsetInDays: Int = 2) {
    private val productionData = parseProduction(getMarketProduction())
    private val priceData = parseCSVData<EnergyPriceTypes>(getMarketPrices())
    private val consumptionData = parseConsumption(getMarkedConsumption())

    private fun parseProduction(csvParser: CSVParser): MutableList<MutableList<Any>> {
        val dataStructure: MutableList<MutableList<Any>> = ArrayList<MutableList<Any>>()
        EnergyProductionTypes.values().forEach {
            if (it.ordinal < 2) {
                dataStructure.add(it.ordinal, ArrayList<String>() as MutableList<Any>)
            } else {
                dataStructure.add(it.ordinal, ArrayList<Float>() as MutableList<Any>)
            }
        }
        csvParser.forEach { dataRecord ->
            EnergyProductionTypes.values().forEach {
                if (it.ordinal < 2) {
                    val valueStr = dataRecord.get(it.ordinal)
                    // TODO: convert date and time
                    dataStructure[it.ordinal].add(valueStr)
                } else {
                    val valueStr = dataRecord.get(it.ordinal).replace(",", "")
                    val value = if (valueStr == "-") {
                        if (dataStructure[it.ordinal].isEmpty()) Float.NaN
                        else dataStructure[it.ordinal].last() as Float
                    } else valueStr.toFloat()
                    dataStructure[it.ordinal].add(value)
                }
            }
        }
        return dataStructure
    }

    private fun parseConsumption(csvParser: CSVParser): MutableList<MutableList<Any>> {
        val dataStructure: MutableList<MutableList<Any>> = ArrayList<MutableList<Any>>()
        EnergyConsumptionTypes.values().forEach {
            if (it.ordinal < 2) {
                dataStructure.add(it.ordinal, ArrayList<String>() as MutableList<Any>)
            } else {
                dataStructure.add(it.ordinal, ArrayList<Float>() as MutableList<Any>)
            }
        }
        csvParser.forEach { dataRecord ->
            EnergyConsumptionTypes.values().forEach {
                if (it.ordinal < 2) {
                    val valueStr = dataRecord.get(it.ordinal)
                    // TODO: convert date and time
                    dataStructure[it.ordinal].add(valueStr)
                } else {
                    val valueStr = dataRecord.get(it.ordinal).replace(",", "")
                    val value = if (valueStr == "-") {
                        if (dataStructure[it.ordinal].isEmpty()) Float.NaN
                        else dataStructure[it.ordinal].last() as Float
                    } else valueStr.toFloat()
                    dataStructure[it.ordinal].add(value)
                }
            }
        }
        return dataStructure
    }

    inline fun <reified T: Enum<T>> parseCSVData(csvParser: CSVParser): MutableList<MutableList<Any>> {
        val dataStructure: MutableList<MutableList<Any>> = ArrayList<MutableList<Any>>()
        //EnergyPriceTypes.values().forEach {
        enumValues<T>().forEach {
            if (it.ordinal < 2) {
                dataStructure.add(it.ordinal, ArrayList<String>() as MutableList<Any>)
            } else {
                dataStructure.add(it.ordinal, ArrayList<Float>() as MutableList<Any>)
            }
        }
        csvParser.forEach { dataRecord ->
            enumValues<T>().forEach {
                if (it.ordinal < 2) {
                    val valueStr = dataRecord.get(it.ordinal)
                    // TODO: convert date and time
                    dataStructure[it.ordinal].add(valueStr)
                } else {
                    val valueStr = dataRecord.get(it.ordinal).replace(".", "").replace(',', '.')
                    val value = if (valueStr == "-") {
                        if (dataStructure[it.ordinal].isEmpty()) Float.NaN
                        else dataStructure[it.ordinal].last() as Float
                    } else valueStr.toFloat()
                    dataStructure[it.ordinal].add(value)
                }
            }
        }
        return dataStructure
    }

    private fun getCSVData(url: URL, postData: String): CSVParser {
        val callable = Callable {
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
            conn.setRequestProperty("Content-Length", postData.length.toString())
            conn.useCaches = false
            DataOutputStream(conn.outputStream).use { it.writeBytes(postData) }
            val bufferedReader = BufferedReader(InputStreamReader(conn.inputStream))
            val content = bufferedReader.use(BufferedReader::readText)
            CSVParser(StringReader(content), CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())
        }
        val future = Executors.newSingleThreadExecutor().submit(callable)
        return future.get()
    }

    private fun getToday(offset: Int = 0): Long {
        //val timestamp2022 = 1640991600L
        val dt = Calendar.getInstance()
        //dt.set(Calendar.HOUR_OF_DAY, 0)
        //dt.set(Calendar.MINUTE, 0)
        //dt.set(Calendar.SECOND, 0)
        //dt.set(Calendar.MILLISECOND, 0)
        //dt.add(Calendar.DAY_OF_YEAR, 1)
        dt.add(Calendar.DAY_OF_YEAR, offset)
        return dt.timeInMillis
    }

    private fun getTimestamps(): Pair<Long, Long> {
        val timestampStart = getToday(-offsetInDays)
        val timestampEnd = getToday() - 1
        return Pair(timestampStart, timestampEnd)
    }

    private fun getMarketPrices(): CSVParser {
        val (timestampStart, timestampEnd) = getTimestamps()
        val url = URL("https://www.smard.de/nip-download-manager/nip/download/market-data")
        val moduleIds = EnergyPriceTypes.values().filter{it.id > 1}.joinToString(separator=",") { it.id.toString() }
        val postData = "{\"request_form\":[{\"format\":\"CSV\",\"moduleIds\":[$moduleIds],\"region\":\"DE-LU\",\"timestamp_from\":$timestampStart,\"timestamp_to\":$timestampEnd,\"type\":\"discrete\",\"language\":\"de\"}]}"
        return getCSVData(url, postData)
    }

    private fun getMarketProduction(): CSVParser {
        val (timestampStart, timestampEnd) = getTimestamps()
        val url = URL("https://www.smard.de/nip-download-manager/nip/download/market-data")
        Log.i("energy Data", "$timestampStart $timestampEnd")
        val moduleIds = EnergyProductionTypes.values().filter{it.id > 1}.joinToString(separator=",") { it.id.toString() }
        val postData = "{\"request_form\":[{\"format\":\"CSV\",\"moduleIds\":[$moduleIds],\"region\":\"DE\",\"timestamp_from\":$timestampStart,\"timestamp_to\":$timestampEnd,\"type\":\"discrete\",\"language\":\"en\"}]}"
        return getCSVData(url, postData)
    }

    private fun getMarkedConsumption(): CSVParser {
        val (timestampStart, timestampEnd) = getTimestamps()
        Log.i("energy Data","$timestampStart $timestampEnd")
        val url = URL("https://www.smard.de/nip-download-manager/nip/download/market-data")
        val moduleIds = EnergyConsumptionTypes.values().filter{it.id > 1}.joinToString(separator=",") { it.id.toString() }
        val postData = "{\"request_form\":[{\"format\":\"CSV\",\"moduleIds\":[$moduleIds],\"region\":\"DE\",\"timestamp_from\":$timestampStart,\"timestamp_to\":$timestampEnd,\"type\":\"discrete\",\"language\":\"en\"}]}"
        return getCSVData(url, postData)
    }

    private fun getMinMaxAvg_Cur(list: MutableList<Float>, windowSize: Float = 1f): Pair<Triple<Float, Float, Float>, Float> {
        val windowIndex = list.lastIndex.toFloat() * windowSize
        val startIndex = list.lastIndex - windowIndex.toInt()
        val min = Collections.min(list.slice(startIndex..list.lastIndex))
        val max = Collections.max(list.slice(startIndex..list.lastIndex))
        val avg = (list.slice(startIndex..list.lastIndex) ).average().toFloat()
        val cur = list.last()
        return Pair(Triple(min, max, avg), cur)
    }

    fun getCurrentEnergyMix(): Pair<Float, Float> {
        var renewableProduction = 0.0f
        EnergyProductionTypes.values().filter {it.isRenewable and (it.ordinal > 1)}.forEach { renewableProduction += productionData[it.ordinal].last() as Float }
        var conventionalProduction = 0.0f
        EnergyProductionTypes.values().filter {(!it.isRenewable) and (it.ordinal > 1)}.forEach { conventionalProduction += productionData[it.ordinal].last() as Float }
        val residualLoad = getCurrentValue<EnergyConsumptionTypes>(EnergyConsumptionTypes.RESIDUAL_LOAD.ordinal)
        val totalGridLoad = getCurrentValue<EnergyConsumptionTypes>(EnergyConsumptionTypes.TOTAL_GRID_LOAD.ordinal)
        println("residual/total load: $residualLoad / $totalGridLoad")
        println("renewable/conventional: $renewableProduction / $conventionalProduction")
        println("diff total-load - renewable: ${totalGridLoad - renewableProduction} ($residualLoad)")
        println("total production (excess): ${renewableProduction + conventionalProduction} (${renewableProduction + conventionalProduction - totalGridLoad})")
        return Pair(renewableProduction, conventionalProduction)
    }

    private fun printMinMaxAvgCur(dataDesc: String, list: MutableList<Float>, windowSize: Float) {
        val df = DecimalFormat("#.##")
        val (mma, cur) = getMinMaxAvg_Cur(list, windowSize)
        val (min, max, avg) = mma
        val range = ((cur - min) / (max - min) * 100).toInt()
        println("$dataDesc: ${df.format(cur)} ${df.format(range)} % (${df.format(min)} - ${df.format(avg)} - ${df.format(max)})")

    }
    fun asciiPlot(descStr: String, list: MutableList<Float>, windowSize: Float) {
        val (totMma, cur) = getMinMaxAvg_Cur(list, 1f)
        val (windowMma, cur2) = getMinMaxAvg_Cur(list, windowSize)
        val (totMin, totMax, totAvg) = totMma
        val (windowMin, windowMax, windowAvg) = windowMma
        val totalRange = totMax //- totMin
        val hit0 = (totMin / totalRange * 10).toInt()
        val hit1 = ((windowMin /*- totMin*/) / totalRange * 10).toInt()
        val hit2 = ((windowMax /*- totMin*/) / totalRange * 10).toInt()
        val hitAvg = ((windowAvg /*- totMin*/) / totalRange * 10).toInt()
        val hitCur = ((cur /*- totMin*/) / totalRange * 10).toInt()

        val outputStr = StringBuilder("  -   -   -   -   -   -   -   -   -   -   - ] ")
        outputStr[1+hit0*4] = '['
        outputStr[1+hit1*4] = '('
        outputStr[1+hit2*4 + 2] = ')'
        outputStr[1+hitAvg * 4 + 1] = '+'
        outputStr[1+hitCur * 4 + 1] = 'O'
        if (hitAvg == hitCur)
            outputStr[1+hitCur*4 + 1] = '0'
        outputStr.append("$descStr $cur")
        println(outputStr)
    }
    fun printMinMax(windowSize: Float = .1f) {
        println("=== prices ===")
        //listOf(EnergyPriceTypes.DE_LUX, EnergyPriceTypes.Poland).forEach() {
        EnergyPriceTypes.values().forEach {
                en -> if (en.ordinal > 1) {
            asciiPlot(en.name, priceData[en.ordinal] as MutableList<Float>, windowSize)
        } //printMinMaxAvgCur("price ${en.name}", priceData[en.ordinal] as MutableList<Float>,
            //windowSize)
        }
        println()
        println("=== production ===")
        EnergyProductionTypes.values().forEach {
                en -> if (en.ordinal > 1) asciiPlot(en.name, productionData[en.ordinal] as MutableList<Float>, windowSize)//printMinMaxAvgCur(en.name, productionData[en.ordinal] as MutableList<Float>, windowSize)
        }
        println()
        println("=== consumption ===")
        EnergyConsumptionTypes.values().forEach {
                en -> if (en.ordinal > 1) asciiPlot(en.name, consumptionData[en.ordinal] as MutableList<Float>, windowSize)
        }
    }

    private inline fun <reified T: Enum<T>> getCurrentValue(id: Int): Float {
        if ((typeOf<T>() == typeOf<EnergyProductionTypes>()) and (id <= productionData.lastIndex))
            return productionData[id].last() as Float
        if ((typeOf<T>() == typeOf<EnergyConsumptionTypes>()) and (id <= consumptionData.lastIndex))
            return consumptionData[id].last() as Float
        if ((typeOf<T>() == typeOf<EnergyPriceTypes>()) and (id <= priceData.lastIndex))
            return priceData[id].last() as Float
        return Float.NaN
    }

    fun getCurrentLoad(): Float {
        var lastIndex = consumptionData[EnergyConsumptionTypes.TOTAL_GRID_LOAD.ordinal].lastIndex
        println(consumptionData[EnergyConsumptionTypes.TIME_OF_DAY.ordinal].slice(lastIndex-10..lastIndex))
        println(consumptionData[EnergyConsumptionTypes.TOTAL_GRID_LOAD.ordinal].slice(lastIndex-10..lastIndex).toString())
        println(consumptionData[EnergyConsumptionTypes.RESIDUAL_LOAD.ordinal].slice(lastIndex-10..lastIndex).toString())
        println("fossil gas:")
        lastIndex = priceData[EnergyPriceTypes.DE_LUX.ordinal].lastIndex
        println(productionData[EnergyProductionTypes.FOSSIL_GAS.ordinal].slice(lastIndex-10..lastIndex).toString())
        println("prices")
        lastIndex = priceData[EnergyPriceTypes.DE_LUX.ordinal].lastIndex
        println(priceData[EnergyPriceTypes.DE_LUX.ordinal].slice(lastIndex-10..lastIndex).toString())
        return consumptionData[EnergyConsumptionTypes.TOTAL_GRID_LOAD.ordinal].last() as Float
    }
}


//
//fetch("https://www.smard.de/nip-download-manager/nip/download/market-data", {
//    "headers": {
//        "accept": "application/json, text/plain, */*",
//        "accept-language": "en-US,en;q=0.9,de;q=0.8",
//        "content-type": "application/json;charset=UTF-8",
//        "sec-ch-ua": "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"100\"",
//        "sec-ch-ua-mobile": "?0",
//        "sec-ch-ua-platform": "\"Linux\"",
//        "sec-fetch-dest": "empty",
//        "sec-fetch-mode": "cors",
//        "sec-fetch-site": "same-origin"
//    },
//    "referrer": "https://www.smard.de/home/downloadcenter/download-marktdaten",
//    "referrerPolicy": "strict-origin-when-cross-origin",
//    "body": "{\"request_form\":[{\"format\":\"CSV\",\"moduleIds\":[8004169,8004170,8004996,8004997,8000251,8000252,8000253,8000254,8000255,8000256,8000257,8000258,8000259,8000260,8000261,8000262],\"region\":\"DE-LU\",\"timestamp_from\":1648162800000,\"timestamp_to\":1649109599999,\"type\":\"discrete\",\"language\":\"de\"}]}",
//    "method": "POST",
//    "mode": "cors",
//    "credentials": "include"
//});

//fetch("https://www.smard.de/nip-download-manager/nip/download/market-data", {
//    "headers": {
//        "accept": "application/json, text/plain, */*",
//        "accept-language": "en-US,en;q=0.9,de;q=0.8",
//        "content-type": "application/json;charset=UTF-8",
//        "sec-ch-ua": "\"Chromium\";v=\"97\", \" Not;A Brand\";v=\"99\"",
//        "sec-ch-ua-mobile": "?0",
//        "sec-ch-ua-platform": "\"Linux\"",
//        "sec-fetch-dest": "empty",
//        "sec-fetch-mode": "cors",
//        "sec-fetch-site": "same-origin",
//        "cookie": "guid=1ba8e304-9449-49c3-b0f6-970adfb17d85+82f7a953365206d752a613249088bb2ba891e7e1a5af5e1fd7b44b48aef49be106727e3ffbe34c0dfa4ba137f29e167ed4919be8e4cc0dd332292b7d4634d0aa11f39fdd69d4412fe63c56776ad6b6b6108637298327ebff82bd03da99fb38916797bb2f87209a7e0e5f04782de0cb8a381918246f901a5425972ee4f82ce6bc09c7cbdd4c913287cb44baac4c92c2f423490eb935ec177127ec565cfa0bdf9b2072b7dcae93c8262b4ed07819414e067f7635181ecb398040bf5522393c15af2aa5cf422f67cb661ab7f1711aa786c7e2d567d14edfd8f82458467f3435e81af11ba3ee2152b4b97b6f2b00a638ab6de352d5a0e82749d2a597b7078f47cd39; CM_SESSIONID=15B20C2E0C9C9988B3C5DEF1AF6A2CFC; bnetza_cookie=!gTAkz0hkv2Cp3v1pJz9/0DF2dceLRCHD0Vu3OougmnPwl0n1XVDmLUZewwrJ/DB0I4t7UqTiLJnb3A==; _pk_ref.1.48cf=%5B%22%22%2C%22%22%2C1643614938%2C%22https%3A%2F%2Fduckduckgo.com%2F%22%5D; _pk_ses.1.48cf=1; TS019724c0=01e68c70d5cb0bc24ca23c001596bcbea9856292391850841766bf703e1e76a706ae77eb379a75416c184063bdcfa1f378c84fcd67; _pk_id.1.48cf=723dc2a53042f8c9.1643615429.0.1643615429..",
//        "Referer": "https://www.smard.de/en/downloadcenter/download-market-data",
//        "Referrer-Policy": "strict-origin-when-cross-origin"
//    },
//    "body": "{\"request_form\":[{\"format\":\"XML\",\"moduleIds\":[1001224,1004066,1004067,1004068,1001223,1004069,1004071,1004070,1001226,1001228,1001227,1001225],\"region\":\"DE\",\"timestamp_from\":1642719600000,\"timestamp_to\":1643669999999,\"type\":\"discrete\",\"language\":\"en\"}]}",
//    "method": "POST"
//});
//

// fetch("https://www.smard.de/nip-download-manager/nip/download/market-data", {
//  "headers": {
//    "accept": "application/json, text/plain, */*",
//    "accept-language": "en-US,en;q=0.9,de;q=0.8",
//    "content-type": "application/json;charset=UTF-8",
//    "sec-ch-ua": "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"100\"",
//    "sec-ch-ua-mobile": "?0",
//    "sec-ch-ua-platform": "\"Linux\"",
//    "sec-fetch-dest": "empty",
//    "sec-fetch-mode": "cors",
//    "sec-fetch-site": "same-origin"
//  },
//  "referrer": "https://www.smard.de/home/downloadcenter/download-marktdaten",
//  "referrerPolicy": "strict-origin-when-cross-origin",
//  "body": "{\"request_form\":[{\"format\":\"CSV\",\"moduleIds\":[5000410,5004387,5004359],\"region\":\"DE\",\"timestamp_from\":1648335600000,\"timestamp_to\":1649282399999,\"type\":\"discrete\",\"language\":\"de\"}]}",
//  "method": "POST",
//  "mode": "cors",
//  "credentials": "include"
//});

// fetch("https://www.smard.de/nip-download-manager/nip/download/market-data", {
//  "headers": {
//    "accept": "application/json, text/plain, */*",
//    "accept-language": "en-US,en;q=0.9,de;q=0.8",
//    "content-type": "application/json;charset=UTF-8",
//    "sec-ch-ua": "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"100\"",
//    "sec-ch-ua-mobile": "?0",
//    "sec-ch-ua-platform": "\"Linux\"",
//    "sec-fetch-dest": "empty",
//    "sec-fetch-mode": "cors",
//    "sec-fetch-site": "same-origin"
//  },
//  "referrer": "https://www.smard.de/en/downloadcenter/download-market-data",
//  "referrerPolicy": "strict-origin-when-cross-origin",
//  "body": "{\"request_form\":[{\"format\":\"CSV\",\"moduleIds\":[5000410,5004387,5004359],\"region\":\"DE-LU\",\"timestamp_from\":1648335600000,\"timestamp_to\":1649282399999,\"type\":\"discrete\",\"language\":\"en\"}]}",
//  "method": "POST",
//  "mode": "cors",
//  "credentials": "include"
//});