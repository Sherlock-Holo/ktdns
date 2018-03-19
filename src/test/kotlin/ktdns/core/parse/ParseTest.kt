package ktdns.core.parse

import ktdns.core.message.Record
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val udpServer = DatagramSocket(5454)
    val buf = ByteArray(512)
    val packet = DatagramPacket(buf, buf.size)
    udpServer.receive(packet)

    val message = Parse.parseQuery(buf, udpServer, packet.address, packet.port)

    message.addAdditional(Record.EDNS_ECS(InetAddress.getByName("128.0.0.1"), 24, 0, 4096).apply {
        extended_RCODE = 0
        EDNS_VERSION = 0
    })

    val nameserver = DatagramSocket()

    nameserver.send(DatagramPacket(message.byteArray, message.byteArray.size, InetSocketAddress("127.0.0.1", 53)))

    val answerBuf = ByteArray(4096)

    val answerPacket = DatagramPacket(answerBuf, answerBuf.size, InetSocketAddress("127.0.0.1", 53))

    nameserver.receive(answerPacket)

    val answer = Parse.parseAnswer(answerBuf)

    answer.answers.forEach {
        println(it.NAME + " " + it.TYPE + " " + if (it.TYPE == Record.RecordType.CNAME) String(it.RDATA) else InetAddress.getByAddress(it.RDATA).hostAddress)
    }
}