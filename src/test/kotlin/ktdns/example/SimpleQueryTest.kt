package ktdns.example

import ktdns.core.message.Message
import java.net.InetAddress
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val query = Message.buildQuery(Message.Question().apply {
        this.QNAME = "www.qq.com."
        this.QTYPE = 1
        this.QCLASS = 1
    })

    val answer = SimpleQuery.query(query, InetSocketAddress("101.6.6.6", 53))
    answer.answers.forEach {
        println("type: ${it.TYPE} RDATA: ${InetAddress.getByAddress(it.RDATA).hostAddress}")
    }
}