package ktdns.core.message

import ktdns.extend.getIntByteArray
import ktdns.extend.getShortByteArray
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.ArrayList

abstract class Record {
    enum class RecordType(val type: Int) {
        A(1),
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
    abstract val RDLENGTH: Int
    abstract val RDATA: ByteArray

    open fun toByteArray(offset: Int?): ByteArray {
        val arrayList = ArrayList<Byte>()
        val tmp = ByteBuffer.allocate(2)
        if (offset != null) {
            arrayList.addAll(tmp.getShortByteArray((offset or 0b11000000_00000000).toShort()).toTypedArray())

        } else {
            val list = NAME.substring(0, NAME.length - 1).split('.')
            list.forEach {
                arrayList.add(it.length.toByte())
                arrayList.addAll(it.toByteArray().toTypedArray())
            }
            arrayList.add(0.toByte())
        }

        arrayList.addAll(tmp.getShortByteArray(TYPE.type.toShort()).toTypedArray())
        arrayList.addAll(tmp.getShortByteArray(CLASS.toShort()).toTypedArray())
        val ttl = ByteBuffer.allocate(4)
        arrayList.addAll(ttl.getIntByteArray(TTL).toTypedArray())
        arrayList.addAll(tmp.getShortByteArray(RDLENGTH.toShort()).toTypedArray())
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

        override val RDATA: ByteArray
            get() {
                val list = cname.substring(0, cname.length - 1).split('.')
                val arrayList = ArrayList<Byte>()

                list.forEach {
                    arrayList.add(it.length.toByte())
                    arrayList.addAll(it.toByteArray().toTypedArray())
                }
                arrayList.add(0.toByte())

                return arrayList.toByteArray()
            }

        override val RDLENGTH get() = RDATA.size
    }
}