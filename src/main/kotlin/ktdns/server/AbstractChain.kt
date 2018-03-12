package ktdns.server

import ktdns.core.message.Message

abstract class AbstractChain : Interceptor.Chain, Cloneable {
    abstract override var message: Message

    abstract override fun addInterceptor(interceptor: Interceptor): Boolean

    abstract override fun proceed(message: Message)

    public override fun clone(): Any {
        return super.clone()
    }
}