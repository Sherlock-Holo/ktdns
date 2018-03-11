package ktdns.extend

import java.nio.ByteBuffer

fun Byte.toUInt() = this.toInt() and 0xff

fun ByteBuffer.getShortByteArray(short: Short): ByteArray {
    if (this.capacity() != 2) throw IllegalStateException("byteBuffer size is ${this.capacity()}")
    this.clear()
    this.putShort(short)
    return this.array()
}

fun ByteBuffer.getIntByteArray(int: Int): ByteArray {
    if (this.capacity() != 4) throw IllegalStateException("byteBuffer size is ${this.capacity()}")
    this.clear()
    this.putInt(int)
    return this.array()
}