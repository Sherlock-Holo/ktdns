package ktdns.core.parse

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val udpServer = DatagramSocket(5454)
    val buf = ByteArray(512)
    val packet = DatagramPacket(buf, buf.size)
    udpServer.receive(packet)

    val parse = Parse()
    val message = parse.parseQuery(buf, udpServer, packet.address, packet.port)

    val nameserver = DatagramSocket()

    nameserver.send(DatagramPacket(message.byteArray, message.byteArray.size, InetSocketAddress("127.0.0.1", 53)))

    val answerBuf = ByteArray(512)

    val answerPacket = DatagramPacket(answerBuf, answerBuf.size, InetSocketAddress("127.0.0.1", 53))

    nameserver.receive(answerPacket)

    val answer = parse.parseAnswer(answerBuf)

    answer.answers.forEach {
        println(it.NAME + " " + it.TYPE)
    }
}