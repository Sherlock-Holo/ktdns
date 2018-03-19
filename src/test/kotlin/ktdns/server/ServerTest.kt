package ktdns.server

import ktdns.core.message.Message
import ktdns.core.message.Record
import ktdns.interceptor.Interceptor
import ktdns.interceptor.chain.Chain
import java.net.InetAddress
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val server = Server()
    server.bindAddress = InetSocketAddress(5454)
    server
            .addInterceptor(CNAMEInterceptor())
            .addInterceptor(AInterceptor())
            .addInterceptor(AAAAInterceptor())
            .addInterceptor(ECSInterceptor())
    server.start()
}

class CNAMEInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        if (message.questions[0].QNAME == "www.qq.com.") {
            message.addAnswer(Record.CNAMEAnswer("www.qq.com.", 1, 64, "ipv6.qq.com."))
        }
        return chain.proceed(message)
    }
}

class AInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        message.addAnswer(Record.AAnswer("ipv6.qq.com.", 1, 64, InetAddress.getByName("127.1.1.1")))
        return chain.proceed(message)
    }
}

class AAAAInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        message.addAnswer(Record.AAAAAnswer("ipv6.qq.com.", 1, 64, InetAddress.getByName("::8")))
        return chain.proceed(message)
    }
}

class ECSInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        if (!message.additional.isEmpty()) {
            message.additional.forEach {
                it as Record.EDNS_ECS
                it.scopeNetMask = it.sourceNetMask
            }
        }
//        message.addAdditional(Record.EDNS_ECS(InetAddress.getByName("128.0.0.1"), 32, 32, 4096))
        return chain.proceed(message)
    }
}