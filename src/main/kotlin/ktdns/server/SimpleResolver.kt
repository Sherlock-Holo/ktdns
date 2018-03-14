package ktdns.server

import ktdns.interceptor.SimpleInterceptor
import java.net.InetSocketAddress

class SimpleResolver(nameserverAddress: InetSocketAddress, bindAddress: InetSocketAddress) {
    private val server = Server()

    init {
        server.bindAddress = bindAddress
        SimpleInterceptor.nameserverAddress = nameserverAddress
        server.addInterceptor(SimpleInterceptor(server.parse))
    }

    fun start() = server.start()
}