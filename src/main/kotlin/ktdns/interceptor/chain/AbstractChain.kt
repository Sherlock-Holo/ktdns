package ktdns.interceptor.chain

import ktdns.core.message.Message
import ktdns.interceptor.Interceptor

abstract class AbstractChain : Chain, Cloneable {
    abstract override var message: Message

    abstract override fun addInterceptor(interceptor: Interceptor): Boolean

    abstract override fun proceed(message: Message): Message

    public override fun clone(): Any {
        return super.clone()
    }
}