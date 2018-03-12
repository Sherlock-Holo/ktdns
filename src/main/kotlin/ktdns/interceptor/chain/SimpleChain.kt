package ktdns.interceptor.chain

import ktdns.core.message.Message
import ktdns.interceptor.Interceptor
import java.util.*

class SimpleChain : AbstractChain() {
    private val interceptors = LinkedList<Interceptor>()

    override lateinit var message: Message

    override fun addInterceptor(interceptor: Interceptor) = interceptors.add(interceptor)

    override fun proceed(message: Message): Message {
        return if (interceptors.isEmpty()) {
            message
        } else {
            this.message = message
            val interceptor = interceptors.removeFirst()
            interceptor.intercept(this)
        }
    }
}