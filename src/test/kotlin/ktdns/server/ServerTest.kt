package ktdns.server

import ktdns.core.message.Message
import ktdns.core.message.record.NSRecord
import ktdns.core.message.record.SOARecord
import ktdns.interceptor.Interceptor
import ktdns.interceptor.chain.Chain
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val server = Server()
    server.bindAddress = InetSocketAddress(5454)
    server
//            .addInterceptor(CNAMEInterceptor())
//            .addInterceptor(AInterceptor())
//            .addInterceptor(AAAAInterceptor())
//            .addInterceptor(EDNSInterceptor())
            .addInterceptor(NSIntercetpor())
            .addInterceptor(SOAInterceptor())
    server.start()
}
/*

class CNAMEInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        if (message.questions[0].QNAME == "www.qq.com.") {
            message.addAnswer(CNAMERecord("www.qq.com.", 1, 64, "ipv6.qq.com."))
        }
        return chain.proceed(message)
    }
}

class AInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        message.addAnswer(ARecord("www.qq.com.", 1, 64, InetAddress.getByName("112.90.83.112")))
        return chain.proceed(message)
    }
}

class AAAAInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        message.addAnswer(AAAARecord("ipv6.qq.com.", 1, 64, InetAddress.getByName("::8")))
        return chain.proceed(message)
    }
}

class NSIntercetpor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        message.addAURecord(NSRecord("www.qq.com.", 1, 64, "ns.sherlock.com."))
        return chain.proceed(message)
    }
}

@Deprecated("just use for debug")
class EDNSInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        return chain.proceed(chain.message.apply { this.header.ARCOUNT = 1 })
    }
}*/

class SOAInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        message.addAURecord(SOARecord(
                "ns1.a.shifen.com.",
                "baidu_dns_master.baidu.com.",
                1803290016,
                5,
                5,
                2592000,
                3600,
                "www.baidu.com.",
                64
        ))

        return chain.proceed(message)
    }
}

class NSIntercetpor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message
        message.addAURecord(NSRecord("www.baidu.com.", 1, 64, "ns.sherlock.com."))
        return chain.proceed(message)
    }
}