package ktdns.extend

import java.nio.ByteBuffer

fun Byte.toUInt() = this.toInt() and 0xff

class BytesNumber {
    companion object {
        private val intBuffer = ByteBuffer.allocate(4)

        private val shortBuffer = ByteBuffer.allocate(2)

        private val longBuffer = ByteBuffer.allocate(8)

        fun getIntByteArray(int: Int): ByteArray {
            synchronized(intBuffer) {
                intBuffer.clear()
                intBuffer.putInt(int)
                return intBuffer.array()
            }
        }

        fun getShortByteArray(short: Short): ByteArray {
            synchronized(shortBuffer) {
                shortBuffer.clear()
                shortBuffer.putShort(short)
                return shortBuffer.array()
            }
        }

        fun getLongByteArray(long: Long): ByteArray {
            synchronized(longBuffer) {
                longBuffer.clear()
                longBuffer.putLong(long)
                return longBuffer.array()
            }
        }

        fun getNumber(byteArray: ByteArray): Any {
            when (byteArray.size) {
                2 -> {
                    shortBuffer.clear()
                    shortBuffer.put(byteArray)
                    shortBuffer.flip()
                    return shortBuffer.short
                }

                4 -> {
                    intBuffer.clear()
                    intBuffer.put(byteArray)
                    intBuffer.flip()
                    return intBuffer.int
                }

                8 -> {
                    longBuffer.clear()
                    longBuffer.put(byteArray)
                    longBuffer.flip()
                    return longBuffer.long
                }

                else -> TODO("not yet support byteArray size: ${byteArray.size}")
            }
        }
    }
}

operator fun String.times(times: Int): String {
    val sb = StringBuilder()
    for (i in 0 until times) {
        sb.append(this)
    }
    return sb.toString()
}