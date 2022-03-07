package com.example.ebox22

import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class WallboxKeba(_ip:String = "192.168.178.6", _port: Int = 7090) {
    private val port = _port
    private val ip = _ip
    private var rfidTag:String = "NONE"

    private fun openBroadcastSocket(): DatagramSocket {
        println(" Opening broadcast socket.")
        val socket = DatagramSocket()
        socket.broadcast = true
        return socket
    }
    private fun openReceiveSocket(): DatagramSocket {
        println(" Opening receiving socket.")
        val socketRcv = DatagramSocket(port)
        socketRcv.broadcast = true
        socketRcv.setSoTimeout(1000)
        return socketRcv
    }
    private fun getMessageReply(messageStr: String, expectKey: String = "", expectValue: Int = -1): String {
        val socket = openBroadcastSocket()
        val socketRcv = openReceiveSocket()

        val sendData = messageStr.toByteArray()
        val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getByName(ip), port)
        socket.send(sendPacket)

        var receivedKey: String
        var receivedValue: Int
        var reply: String
        var retries: Int = 0
        do {
            retries += 1
            receivedKey = ""
            receivedValue = -1
            reply = receiveUDP(socketRcv)
            if (expectKey != "") {
                try {
                    val jsonReply = JSONTokener(reply).nextValue() as JSONObject
                    if (jsonReply.has(expectKey)) {
                        receivedKey = expectKey
                        receivedValue = jsonReply.getInt(expectKey)
                        println("key: $expectKey found. Received $receivedValue / $expectValue expected")
                    } else {
                        println("key: $expectKey is missing")
                    }
                } catch (e: JSONException) {
                    println("No JSON received: $reply")
                }
            }
        } while (((receivedKey != expectKey) or (receivedValue != expectValue)) and (retries <= 4))

        socket.close()
        socketRcv.close()
        return reply
    }
    private fun receiveUDP(socket: DatagramSocket): String {
        val buffer = ByteArray(2048)
        val packet = DatagramPacket(buffer, buffer.size)
        var answer: String
        try {
            socket.receive(packet)
            println(" packet received: " + buffer)
            println(" string received: " + buffer.toString(Charsets.ISO_8859_1))
            val answ = packet.data.toString(Charsets.ISO_8859_1)
            val bytes = answ.toByteArray(Charsets.ISO_8859_1)
            answer = bytes.toString(Charsets.UTF_8)
        } catch (e: SocketTimeoutException){
            //val answ = "[B@ed4d550"
            val answ = """{"ERROR": "TIMEOUT", "ID": "2", "State": 3, "Error1": 0, "Curr HW": 16000, "RFID tag": "da25f9b300000000", "Plug": "7"}"""
            val bytes = answ.toByteArray(Charsets.ISO_8859_1)
            println(" converting " + bytes + " to " + bytes.toString(Charsets.UTF_8))
            answer = bytes.toString(Charsets.UTF_8)
        }
        return answer
    }
    fun getStatus(): State {
        val reply = getMessageReply("report 2", "ID", 2)
        //val random_id = Random().nextInt(State.values().size)
        //return State.values()[random_id]
        val jsonReply = JSONTokener(reply).nextValue() as JSONObject
        val currentState = jsonReply.getInt("State")
        val currentPlug = jsonReply.getInt("Plug")
        if ((currentState == 3) or (currentState == 2)) {
            return State.CHARGING
        }
        if ((currentState == 1) or (currentState == 5)) {
            if (currentPlug == 7)
                return State.IDLE
        }
        return State.UNPLUGGED
    }

    private fun initializeRfidTag() {
        if (this.rfidTag != "NONE") return

        val reply = getMessageReply("report 100", "ID", 100)
        val jsonReply = JSONTokener(reply).nextValue() as JSONObject
        val latestRfidTag = jsonReply.getString("RFID tag")
        this.rfidTag = latestRfidTag
    }

    fun startCharging(): String {
        initializeRfidTag()
        return getMessageReply("start $rfidTag")
    }

    fun stopCharging(): String {
        initializeRfidTag()
        return getMessageReply("stop $rfidTag")
    }
}
