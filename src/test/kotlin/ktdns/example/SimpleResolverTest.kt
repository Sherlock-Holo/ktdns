package ktdns.example

import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val simpleResolver = SimpleResolver(InetSocketAddress("127.0.0.1", 53), InetSocketAddress(5454))
    simpleResolver.start()
}