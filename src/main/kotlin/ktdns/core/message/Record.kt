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

    class EDNSRecord(
            payloadSize: Int,
            val extended_RCODE: Int,
            val EDNSVersion: Int,
            val Z: ByteArray
    ) : Record() {

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

        val optionDatas = ArrayList<OPTION_DATA>()

        fun addOptionData(option_data: OPTION_DATA): EDNSRecord {
            optionDatas.add(option_data)
            return this
        }

        override val NAME = "0"
        override val TYPE = RecordType.EDNS
        override val CLASS = payloadSize

        override val TTL get() = BytesNumber.getInt(byteArrayOf(extended_RCODE.toByte(), EDNSVersion.toByte()) + Z)

        override val RDATA: ByteArray
            get() {
                val arrayList = ArrayList<Byte>()

                optionDatas.forEach { arrayList.addAll(it.toByteArray().toTypedArray()) }

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

        abstract class OPTION_DATA {
            abstract val optionCode: Int
            open val optionLength get() = optionData.size
            abstract val optionData: ByteArray

            open fun toByteArray(): ByteArray {
                val arrayList = ArrayList<Byte>()

                arrayList.addAll(BytesNumber.getShortByteArray(optionCode.toShort()).toTypedArray())
                arrayList.addAll(BytesNumber.getShortByteArray(optionLength.toShort()).toTypedArray())
                arrayList.addAll(optionData.toTypedArray())

                return arrayList.toByteArray()
            }
        }

        class ECS_DATA(
                val ip: InetAddress,
                var sourceNetMask: Int,
                var scopeNetMask: Int
        ) : OPTION_DATA() {

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

            override val optionCode = 8

            val ECS_IP: InetAddress
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

            override val optionData: ByteArray
                get() {
                    val arrayList = ArrayList<Byte>()

                    val ECS_IPByteArray = ECS_IP.address
                    val family =
                            if (ECS_IPByteArray.size == 4) BytesNumber.getShortByteArray(1)
                            else BytesNumber.getShortByteArray(2)
                    arrayList.addAll(family.toTypedArray())

                    arrayList.addAll(arrayListOf(sourceNetMask.toByte(), scopeNetMask.toByte()))

                    when {
                        sourceNetMask <= 8 -> arrayList.add(ECS_IPByteArray[0])

                        sourceNetMask <= 16 -> arrayList.addAll(ECS_IPByteArray.copyOfRange(0, 2).toTypedArray())

                        sourceNetMask <= 24 -> arrayList.addAll(ECS_IPByteArray.copyOfRange(0, 3).toTypedArray())

                        else -> arrayList.addAll(ECS_IPByteArray.toTypedArray())
                    }


                    return arrayList.toByteArray()
                }
        }

        class UnknownData(
                override val optionCode: Int,
                override val optionData: ByteArray
        ) : OPTION_DATA()
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
