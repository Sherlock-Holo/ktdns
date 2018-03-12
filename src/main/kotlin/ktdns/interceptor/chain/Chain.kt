package ktdns.interceptor.chain

import ktdns.core.message.Message
import ktdns.interceptor.Interceptor

interface Chain {
    var message: Message

    fun proceed(message: Message): Message

    fun addInterceptor(interceptor: Interceptor): Boolean
}