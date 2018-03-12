package ktdns.server

import ktdns.core.message.Message

interface Interceptor {

    fun intercept(chain: Chain): Message

    interface Chain {
        var message: Message

        fun proceed(message: Message): Message

        fun addInterceptor(interceptor: Interceptor): Boolean
    }
}