package ktdns.server

import ktdns.core.message.Message
import java.net.DatagramPacket
import java.util.*

class SimpleChain : AbstractChain() {
    private val interceptors = LinkedList<Interceptor>()

    override lateinit var message: Message

    override fun addInterceptor(interceptor: Interceptor) = interceptors.add(interceptor)

    override fun proceed(message: Message) {
        if (interceptors.isEmpty()) {
            val buf = message.byteArray
            val packet = DatagramPacket(buf, buf.size, message.souceAddress, message.sourcePort)
            val socket = message.socket
            socket.send(packet)
        } else {
            this.message = message
            val interceptor = interceptors.removeFirst()
            interceptor.intercept(this)
        }
    }
}