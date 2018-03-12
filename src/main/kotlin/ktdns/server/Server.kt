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

class Server(private val chain: AbstractChain) {

    constructor() : this(SimpleChain())

    private val interceptors = ArrayList<Interceptor>()

    fun addInterceptor(interceptor: Interceptor): Server {
        interceptors.add(interceptor)
        return this
    }

    var bindAddress: InetSocketAddress? = null
        set(value) {
            socket = DatagramSocket(value)
        }

    private lateinit var socket: DatagramSocket

    private val parse = Parse()

    fun start() {
        if (interceptors.isEmpty()) {
            throw KtdnsException("no interceptor")
        }

        while (true) {
            val buf = ByteArray(512)
            val packet = DatagramPacket(buf, buf.size)
            try {
                socket.receive(packet)
            } catch (e: IOException) {
                continue
            }

            Thread(Runnable {
                val message = parse.parseQuery(buf, socket, packet.address, packet.port)
                val chain = this.chain.clone() as AbstractChain
                interceptors.forEach { chain.addInterceptor(it) }

                val outBuf = chain.proceed(message).byteArray

                val outPacket = DatagramPacket(outBuf, outBuf.size, message.souceAddress, message.sourcePort)
                val socket = message.socket
                try {
                    socket.send(outPacket)
                } finally {
                }
            }).start()
        }
    }
}