package ktdns.core.parse

import ktdns.example.SimpleQuery
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val udpServer = DatagramSocket(5454)
    val buf = ByteArray(512)
    val packet = DatagramPacket(buf, buf.size)
    udpServer.receive(packet)

    val queryMessage = Parse.parseQuery(buf, udpServer, packet.address, packet.port)

    val answerMessage = SimpleQuery.query(queryMessage, InetSocketAddress("127.0.0.1", 53))
}