package ktdns.core.message.record

import ktdns.extend.BytesNumber

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

    fun string2RDATA(s: String): ByteArray {
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
