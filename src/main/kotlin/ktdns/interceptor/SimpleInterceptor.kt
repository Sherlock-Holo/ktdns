package ktdns.interceptor

import ktdns.core.message.Message
import ktdns.core.parse.Parse
import ktdns.interceptor.chain.Chain
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class SimpleInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val queryMessage = chain.message
        val queryBuf = queryMessage.byteArray
        val queryPacket = DatagramPacket(queryBuf, queryBuf.size, nameserverAddress)
        nameserver.send(queryPacket)

        val answerBuf = ByteArray(4096)

        val answerPacket = DatagramPacket(answerBuf, answerBuf.size, nameserverAddress)

        nameserver.receive(answerPacket)

        val answerMessage = Parse.parseAnswer(answerBuf.copyOfRange(0, answerPacket.length))
        answerMessage.answers.forEach {
            queryMessage.addAnswer(it)
        }

        return chain.proceed(queryMessage)
    }

    companion object {
        lateinit var nameserverAddress: InetSocketAddress

        private val nameserver = DatagramSocket()
    }
}