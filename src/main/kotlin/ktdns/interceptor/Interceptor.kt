package ktdns.interceptor

import ktdns.core.message.Message
import ktdns.interceptor.chain.Chain

interface Interceptor {
    fun intercept(chain: Chain): Message
}