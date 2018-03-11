package ktdns.parse

import java.net.DatagramPacket
import java.net.DatagramSocket

fun main(args: Array<String>) {
    val udpServer = DatagramSocket(5454)
    val buf = ByteArray(512)
    val packet = DatagramPacket(buf, buf.size)
    udpServer.receive(packet)

    val parse = Parse()
    val message = parse.parseQuery(buf)
    println(message.questions[0].QNAME)
}