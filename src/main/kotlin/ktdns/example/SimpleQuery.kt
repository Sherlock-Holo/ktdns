package ktdns.example

import ktdns.core.message.Message
import ktdns.core.parse.Parse
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class SimpleQuery {
    companion object {
        fun query(message: Message, serverAddress: InetSocketAddress): Message {
            val server = DatagramSocket()
            val queryByteArray = message.byteArray
            val queryPacket = DatagramPacket(queryByteArray, queryByteArray.size, serverAddress)
            server.send(queryPacket)
            val answerByteArray = ByteArray(4096)
            val answerPacket = DatagramPacket(answerByteArray, answerByteArray.size, serverAddress)
            server.receive(answerPacket)
            return Parse.parseAnswer(answerByteArray).apply { this.setAnswerMessage(true) }
        }
    }
}