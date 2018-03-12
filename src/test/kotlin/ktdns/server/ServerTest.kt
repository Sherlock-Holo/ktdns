package ktdns.server

import ktdns.core.message.Message
import java.net.InetAddress
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val server = Server()
    val bindAddress = InetSocketAddress(5454)
    server.bindAddress = bindAddress
    server
            .addInterceptor(CNAMEInterceptor())
            .addInterceptor(AInterceptor())
    server.start()
}

class CNAMEInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Message {
        val message = chain.message
        if (message.questions[0].QNAME == "www.qq.com.") {
            message.addAnswer(Message.Companion.CNAMEAnswer("www.qq.com.", 1, 64, "ipv6.qq.com."))
        }
        return chain.proceed(message)
    }
}

class AInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Message {
        val message = chain.message
        message.addAnswer(Message.Companion.AAnswer("ipv6.qq.com", 1, 64, InetAddress.getByName("127.1.1.1")))
        message.setAnswerMessage(true)
        return chain.proceed(message)
    }
}