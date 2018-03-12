package ktdns.server

import ktdns.core.message.Message

interface Interceptor {

    fun intercept(chain: Chain)

    interface Chain {
        var message: Message

        fun proceed(message: Message)

        fun addInterceptor(interceptor: Interceptor): Boolean
    }
}