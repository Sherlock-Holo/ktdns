package ktdns.core.message

import ktdns.core.parse.Parse
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address

fun main(args: Array<String>) {
    val udpServer = DatagramSocket(5454)
    while (true) {
        val buf = ByteArray(512)
        val packet = DatagramPacket(buf, buf.size)
        udpServer.receive(packet)

        val parse = Parse()
        val message = parse.parseQuery(buf, udpServer, packet.address, packet.port)

        val outMessage = message.clone() as Message
        outMessage.setAnswerMessage(true)
//    outMessage.header.QR = 1
        outMessage
                .addAnswer(Message.Companion.CNAMEAnswer("www.qq.com", 1, 64, "ipv6.qq.com."))
                .addAnswer(Message.Companion.AAnswer("ipv6.qq.com", 1, 64, Inet4Address.getByName("14.17.32.211")))

        val outByteArray = outMessage.byteArray

        val outPacket = DatagramPacket(outByteArray, outByteArray.size, packet.address, packet.port)
        udpServer.send(outPacket)
    }
//    outMessage.header.byteArray
//    println("done")
}