package ktdns.interceptor.chain

import ktdns.interceptor.Interceptor
import ktdns.core.message.Message
import ktdns.interceptor.chain.AbstractChain
import java.util.*

class SimpleChain : AbstractChain() {
    private val interceptors = LinkedList<Interceptor>()

    override lateinit var message: Message

    override fun addInterceptor(interceptor: Interceptor) = interceptors.add(interceptor)

    override fun proceed(message: Message): Message {
        if (interceptors.isEmpty()) {
            /*val buf = message.byteArray
            val packet = DatagramPacket(buf, buf.size, message.souceAddress, message.sourcePort)
            val socket = message.socket
            socket.send(packet)*/
            return message
        } else {
            this.message = message
            val interceptor = interceptors.removeFirst()
            return interceptor.intercept(this)
        }
    }
}