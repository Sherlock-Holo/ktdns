package ktdns.server

import ktdns.KtdnsException
import ktdns.core.parse.Parse
import ktdns.interceptor.Interceptor
import ktdns.interceptor.chain.AbstractChain
import ktdns.interceptor.chain.SimpleChain
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class Server(private val chain: AbstractChain) {

    constructor() : this(SimpleChain())

    private val interceptors = ArrayList<Interceptor>()

    private val threadPool = Executors.newCachedThreadPool()

    fun addInterceptor(interceptor: Interceptor): Server {
        interceptors.add(interceptor)
        return this
    }

    var bindAddress: InetSocketAddress? = null
        set(value) {
            field = value
            socket = DatagramSocket(value)
        }

    private lateinit var socket: DatagramSocket

    fun start() {
        if (interceptors.isEmpty()) {
            throw KtdnsException("no interceptor")
        }

        while (true) {
            val buf = ByteArray(4096)
            val packet = DatagramPacket(buf, buf.size)
            try {
                socket.receive(packet)
            } catch (e: IOException) {
                continue
            }

            threadPool.submit {
                try {
                    val message = Parse.parseQuery(buf.copyOf(packet.length), socket, packet.address, packet.port)
                    val chain = this.chain.clone() as AbstractChain
                    interceptors.forEach { chain.addInterceptor(it) }

                    val outBuf = chain.proceed(message).apply { this.setAnswerMessage(true) }.byteArray

                    val outPacket = DatagramPacket(outBuf, outBuf.size, message.souceAddress, message.sourcePort)
                    val socket = message.socket
                    socket.send(outPacket)
                } finally {
                }
            }
        }
    }
}