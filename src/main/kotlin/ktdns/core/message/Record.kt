package ktdns.core.message

import ktdns.KtdnsException
import ktdns.extend.BytesNumber
import ktdns.extend.toUInt
import java.net.InetAddress

abstract class Record {
    enum class RecordType(val type: Int) {
        A(1),
        NS(2),
        AAAA(28),
        CNAME(5),
        EDNS(41)
    }

    /**
    0  1  2  3  4  5  6  7  0  1  2  3  4  5  6  7
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                                               |
    /                                               /
    /                      NAME                     /
    |                                               |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                      TYPE                     |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                     CLASS                     |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                      TTL                      |
    |                                               |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                   RDLENGTH                    |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
    /                     RDATA                     /
    /                                               /
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

    # Answer/Authority/Additional (这3个字段的格式都是一样的)
    ## NAME 资源记录包含的域名
    ## TYPE 表示DNS协议的类型
    ## CLASS 表示RDATA的类
    ## TTL 4字节无符号整数表示资源记录可以缓存的时间。0代表只能被传输，但是不能被缓存
    ## RDLENGTH 2个字节无符号整数表示RDATA的长度
    ## RDATA 不定长字符串来表示记录，格式根TYPE和CLASS有关。比如，TYPE是A，CLASS 是 IN，那么RDATA就是一个4个字节的ARPA网络地址
     */

    abstract val NAME: String
    abstract val TYPE: RecordType
    abstract val CLASS: Int
    abstract val TTL: Int
    open val RDLENGTH get() = RDATA.size
    abstract val RDATA: ByteArray

    open fun toByteArray(offset: Int?): ByteArray {
        val arrayList = ArrayList<Byte>()
        if (offset != null) {
            arrayList.addAll(BytesNumber.getShortByteArray((offset or 0b11000000_00000000).toShort()).toTypedArray())

        } else {
            val list = NAME.substring(0, NAME.length - 1).split('.')
            list.forEach {
                arrayList.add(it.length.toByte())
                arrayList.addAll(it.toByteArray().toTypedArray())
            }
            arrayList.add(0.toByte())
        }

        arrayList.addAll(BytesNumber.getShortByteArray(TYPE.type.toShort()).toTypedArray())
        arrayList.addAll(BytesNumber.getShortByteArray(CLASS.toShort()).toTypedArray())
        arrayList.addAll(BytesNumber.getIntByteArray(TTL).toTypedArray())
        arrayList.addAll(BytesNumber.getShortByteArray(RDLENGTH.toShort()).toTypedArray())
        arrayList.addAll(RDATA.toTypedArray())

        return arrayList.toByteArray()
    }

    class AAnswer(
            override val NAME: String,
            override val CLASS: Int,
            override val TTL: Int,
            val address: InetAddress
    ) : Record() {

        override val TYPE = RecordType.A
        override val RDLENGTH = 4
        override val RDATA = address.address
    }

    class AAAAAnswer(
            override val NAME: String,
            override val CLASS: Int,
            override val TTL: Int,
            val address: InetAddress
    ) : Record() {

        override val TYPE = RecordType.AAAA
        override val RDLENGTH = 16
        override val RDATA = address.address
    }

    class CNAMEAnswer(
            override val NAME: String,
            override val CLASS: Int,
            override val TTL: Int,
            val cname: String
    ) : Record() {
        override val TYPE = RecordType.CNAME

        override val RDATA get() = super.string2RDATA(cname)

        override val RDLENGTH get() = RDATA.size
    }

    class EDNS_ECS(
            val ip: InetAddress,
            val sourceNetMask: Int,
            var scopeNetMask: Int,
            var udpLength: Int
    ) : Record() {

        override val NAME = "0"
        override val TYPE = RecordType.EDNS
        override val CLASS = udpLength

        val realEDNSIP: InetAddress
            get() {
                val ipByteArray = ip.address
                when (ipByteArray.size) {
                    4 -> {
                        val sb = StringBuilder()
                        when {
                            (32 - sourceNetMask) <= 8 -> {
                                for (i in 0 until 8) {
                                    if (i < 8 - (32 - sourceNetMask)) sb.append('1')
                                    else sb.append('0')
                                }
                                ipByteArray[3] = (ipByteArray[3].toUInt() and Integer.parseInt(sb.toString(), 2)).toByte()
                            }

                            (32 - sourceNetMask) <= 16 -> {
                                ipByteArray[3] = 0
                                for (i in 0 until 8) {
                                    if (i < 8 - (24 - sourceNetMask)) sb.append('1')
                                    else sb.append('0')
                                }
                                ipByteArray[2] = (ipByteArray[2].toUInt() and Integer.parseInt(sb.toString(), 2)).toByte()
                            }

                            (32 - sourceNetMask) <= 24 -> {
                                ipByteArray[3] = 0
                                ipByteArray[2] = 0
                                for (i in 0 until 8) {
                                    if (i < 8 - (16 - sourceNetMask)) sb.append('1')
                                    else sb.append('0')
                                }
                                ipByteArray[1] = (ipByteArray[1].toUInt() and Integer.parseInt(sb.toString(), 2)).toByte()
                            }

                            else -> {
                                ipByteArray[3] = 0
                                ipByteArray[2] = 0
                                ipByteArray[1] = 0
                                for (i in 0 until 8) {
                                    if (i < 8 - (8 - sourceNetMask)) sb.append('1')
                                    else sb.append('0')
                                }
                                ipByteArray[0] = (ipByteArray[0].toUInt() and Integer.parseInt(sb.toString(), 2)).toByte()
                            }
                        }
                        return InetAddress.getByAddress(ipByteArray)
                    }

                    16 -> TODO("ipv6 mask")

                    else -> throw KtdnsException("error address length: ${ipByteArray.size}")
                }
            }

        var extended_RCODE = 0
        var EDNS_VERSION = 0
        var Z = ByteArray(2)

        override val TTL: Int
            get() {
                val arrayList = ArrayList<Byte>()
                arrayList.add(extended_RCODE.toByte())
                arrayList.add(EDNS_VERSION.toByte())

                arrayList.addAll(Z.toTypedArray())
                return BytesNumber.getInt(arrayList.toByteArray())
            }

        /**
        +0 (MSB)                            +1 (LSB)
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        0: |                          OPTION-CODE                       |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        2: |                         OPTION-LENGTH                      |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        4: |                            FAMILY                          |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        6: |     SOURCE PREFIX-LENGTH      |     SCOPE PREFIX-LENGTH    |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        8: /                           ADDRESS...                       /
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
         */

        override val RDATA: ByteArray
            get() {
                val arrayList = ArrayList<Byte>()
                arrayList.addAll(BytesNumber.getShortByteArray(8).toTypedArray())

                val addressByteArray = realEDNSIP.address
                val optionLength = 4 + addressByteArray.size
                arrayList.addAll(BytesNumber.getShortByteArray(optionLength.toShort()).toTypedArray())

                val family = when (addressByteArray.size) {
                    4 -> 1
                    16 -> 2
                    else -> throw KtdnsException("error address length: ${addressByteArray.size}")
                }
                arrayList.addAll(BytesNumber.getShortByteArray(family.toShort()).toTypedArray())

                arrayList.addAll(arrayListOf(sourceNetMask.toByte(), scopeNetMask.toByte()))

                arrayList.addAll(addressByteArray.toTypedArray())

                return arrayList.toByteArray()
            }

        override fun toByteArray(offset: Int?): ByteArray {
            val arrayList = ArrayList<Byte>()
            arrayList.add(0)

            arrayList.addAll(BytesNumber.getShortByteArray(TYPE.type.toShort()).toTypedArray())
            arrayList.addAll(BytesNumber.getShortByteArray(CLASS.toShort()).toTypedArray())
            arrayList.addAll(BytesNumber.getIntByteArray(TTL).toTypedArray())
            arrayList.addAll(BytesNumber.getShortByteArray(RDLENGTH.toShort()).toTypedArray())
            arrayList.addAll(RDATA.toTypedArray())

            return arrayList.toByteArray()
        }
    }

    class NSRecord(
            override val NAME: String,
            override val CLASS: Int,
            override val TTL: Int,
            val server: String
    ) : Record() {
        override val TYPE = RecordType.NS

        override val RDATA get() = super.string2RDATA(server)
    }

    private fun string2RDATA(s: String): ByteArray {
        val list = s.substring(0, s.length - 1).split('.')
        val arrayList = ArrayList<Byte>()

        list.forEach {
            arrayList.add(it.length.toByte())
            arrayList.addAll(it.toByteArray().toTypedArray())
        }
        arrayList.add(0.toByte())

        return arrayList.toByteArray()
    }
}
