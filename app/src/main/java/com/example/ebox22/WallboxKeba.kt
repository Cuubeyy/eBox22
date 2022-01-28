package com.example.ebox22

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class WallboxKeba(_ip:String = "192.168.178.6", _port: Int = 7090) {
    private val port = _port
    private val ip = _ip

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
    private fun getMessageReply(messageStr: String): String {
        val socket = openBroadcastSocket()
        val sendData = messageStr.toByteArray()
        val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getByName(ip), port)
        println(" sending packet with message <${messageStr}>)")
        socket.send(sendPacket)
        socket.close()
        println(" receiving answer.")
        val socketRcv = openReceiveSocket()
        val reply = receiveUDP(socketRcv)
        socketRcv.close()
        return reply
    }
    private fun receiveUDP(socket: DatagramSocket): String {
        val buffer = ByteArray(2048)
        val packet = DatagramPacket(buffer, buffer.size)
        var answer: String = ""
        try {
            socket.receive(packet)
            println(" packet received: " + packet.data)
            println(" packet received: " + buffer)
            println(" packet received: " + buffer.toString(Charsets.ISO_8859_1))
            val answ = packet.data.toString(Charsets.ISO_8859_1)
            val bytes = answ.toByteArray(Charsets.ISO_8859_1)
            println(" converting " + bytes + " to " + bytes.toString(Charsets.UTF_8))
            answer = bytes.toString(Charsets.UTF_8)
        } catch (e: SocketTimeoutException){
            val answ = "[B@ed4d550"
            val bytes = answ.toByteArray(Charsets.ISO_8859_1)
            println(" converting " + bytes + " to " + bytes.toString(Charsets.UTF_8))
            answer = bytes.toString(Charsets.UTF_8)
        }
        return answer
    }
    fun getStatus(): String {
        val reply = getMessageReply("report 100")
        return reply
    }
}
