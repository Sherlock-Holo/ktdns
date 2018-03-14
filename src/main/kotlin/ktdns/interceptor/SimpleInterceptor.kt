package ktdns.interceptor

import ktdns.core.message.Message
import ktdns.core.parse.Parse
import ktdns.interceptor.chain.Chain
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class SimpleInterceptor(private val parse: Parse) : Interceptor {
    override fun intercept(chain: Chain): Message {
        val queryMessage = chain.message
        val queryBuf = queryMessage.byteArray
        val queryPacket = DatagramPacket(queryBuf, queryBuf.size, nameserverAddress)
        nameserver.send(queryPacket)

        val answerBuf = ByteArray(512)

        val answerPacket = DatagramPacket(answerBuf, answerBuf.size, nameserverAddress)

        nameserver.receive(answerPacket)

        val answerMessage = parse.parseAnswer(answerBuf)
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