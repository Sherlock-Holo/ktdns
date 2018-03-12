package ktdns.core.message

import ktdns.extend.getIntByteArray
import ktdns.extend.getShortByteArray
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class Message : Cloneable {
    var header = Header()

    val questions = ArrayList<Question>()

    val answers = ArrayList<Answer>()
    private var CNAMEPos = -1

    lateinit var socket: DatagramSocket
    lateinit var souceAddress: InetAddress
    var sourcePort = -1

    val byteArray: ByteArray
        get() {

            val arrayList = ArrayList<Byte>()

            /** header **/
            arrayList.addAll(header.byteArray.toTypedArray())

            /** questions **/
            questions.forEach {
                arrayList.addAll(it.byteArray.toTypedArray())
            }

            /*if (answers.isEmpty()) return arrayList.toByteArray()
            else {
                if (CNAMEPos == -1) {
                    answers.forEach { arrayList.addAll(it.toByteArray(12).toTypedArray()) }
                } else {
                    val cnameAnswer = answers.removeAt(CNAMEPos)
                    arrayList.addAll(cnameAnswer.toByteArray(12).toTypedArray())

                    val offset = arrayList.size - cnameAnswer.RDLENGTH

                    answers.forEach { arrayList.addAll(it.toByteArray(offset).toTypedArray()) }
                }
            }*/

            if (!answers.isEmpty()) {
                if (CNAMEPos == -1) {
                    answers.forEach { arrayList.addAll(it.toByteArray(12).toTypedArray()) }
                } else {
                    val cnameAnswer = answers.removeAt(CNAMEPos)
                    arrayList.addAll(cnameAnswer.toByteArray(12).toTypedArray())

                    val offset = arrayList.size - cnameAnswer.RDLENGTH

                    answers.forEach { arrayList.addAll(it.toByteArray(offset).toTypedArray()) }
                }
            }
            return arrayList.toByteArray()
        }

    fun setAnswerMessage(boolean: Boolean) {
        if (boolean) this.header.QR = 1
        else this.header.QR = 0
    }

    fun addAnswer(answer: Answer): Message {
        header.ANCOUNT++
        answers.add(answer)
        if (answer.TYPE == AnswerType.CNAME) {
            CNAMEPos = answers.size - 1
        }

        return this
    }

    public override fun clone(): Any {
        return super.clone()
    }

    class Header {
        lateinit var ID: ByteArray
        var QR = -1
        var opcode = -1
        var AA = -1
        var TC = -1
        var RD = -1
        var RA = -1
        var Z = -1
        var RCODE = -1

        val FLAGS: ByteArray
            get() {
                check(QR != -1)
                check(opcode != -1)
                check(AA != -1)
                check(TC != -1)
                check(RD != -1)
                check(RA != -1)
                check(Z != -1)
                check(RCODE != -1)

                val byte1 = (
                        (QR shl 7) or
                                (opcode shl 6) or
                                (AA shl 2) or
                                (TC shl 1) or
                                RD
//                                RA
                        ).toByte()

                val byte2 = (
                        (RA shl 7) or
                                (Z shl 6) or
                                RCODE
                        ).toByte()

                return byteArrayOf(byte1, byte2)
            }

        var QDCOUNT = -1
        var ANCOUNT = -1
        var NSCOUNT = -1
        var ARCOUNT = -1

        val counts: ByteArray
            get() {
                check(QDCOUNT != -1)
                check(ANCOUNT != -1)
                check(NSCOUNT != -1)
                check(ARCOUNT != -1)

                val buf = ByteBuffer.allocate(8)
                buf.putShort(QDCOUNT.toShort())
                buf.putShort(ANCOUNT.toShort())
                buf.putShort(NSCOUNT.toShort())
                buf.putShort(ARCOUNT.toShort())

                return buf.array()
            }

        val byteArray: ByteArray
            get() {
                val arrayList = ArrayList<Byte>()
                arrayList.addAll(ID.toTypedArray())
                arrayList.addAll(FLAGS.toTypedArray())
                arrayList.addAll(counts.toTypedArray())

                return arrayList.toByteArray()
            }

        /**
        ## QDCOUNT: 无符号16bit整数表示报文请求段中的问题记录数
        ## ANCOUNT: 无符号16bit整数表示报文回答段中的回答记录数
        ## NSCOUNT: 无符号16bit整数表示报文授权段中的授权记录数
        ## ARCOUNT: 无符号16bit整数表示报文附加段中的附加记录数
         */
    }

    class Question {

        /**
        # Question
        ## QNAME 无符号8bit为单位长度不限表示查询名(广泛的说就是：域名)
        ## QTYPE 无符号16bit整数表示查询的协议类型
        ## QCLASS 无符号16bit整数表示查询的类,比如，IN代表Internet
         */

        lateinit var QNAME: String
        var QTYPE = -1
        var QCLASS = -1
        val QNAMEByteArray: ByteArray
            get() {
                val list = QNAME.substring(0 until QNAME.length - 1).split('.')
                val arrayList = ArrayList<Byte>()

                for (i in 0 until list.size) {
                    arrayList.add(list[i].length.toByte())
                    arrayList.addAll(list[i].toByteArray().toTypedArray())
                }
                arrayList.add(0.toByte())

                return arrayList.toByteArray()
            }

        val byteArray: ByteArray
            get() {
                check(QTYPE != -1)
                check(QCLASS != -1)

                val arrayList = ArrayList<Byte>()

                arrayList.addAll(QNAMEByteArray.toTypedArray())

                val tmp = ByteArray(2)
                val tmpByteBuffer = ByteBuffer.wrap(tmp)

                tmpByteBuffer.putShort(QTYPE.toShort())
                arrayList.addAll(tmp.toTypedArray())

                tmpByteBuffer.clear()
                tmpByteBuffer.putShort(QCLASS.toShort())
                arrayList.addAll(tmp.toTypedArray())

                return arrayList.toByteArray()
            }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false

            val question = other as Question
            return this.QNAME == question.QNAME

        }
    }

    companion object {
        enum class AnswerType(val type: Int) {
            A(1),
            AAAA(28),
            CNAME(5)
        }

        abstract class Answer {
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
            abstract val TYPE: AnswerType
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

        }

        class AAnswer(
                override val NAME: String,
                override val CLASS: Int,
                override val TTL: Int,
                val address: InetAddress
        ) : Answer() {

            override val TYPE = AnswerType.A
            override val RDLENGTH = 4
            override val RDATA = address.address
        }

        class AAAAAnswer(
                override val NAME: String,
                override val CLASS: Int,
                override val TTL: Int,
                val address: InetAddress
        ) : Answer() {

            override val TYPE = AnswerType.AAAA
            override val RDLENGTH = 16
            override val RDATA = address.address
        }

        class CNAMEAnswer(
                override val NAME: String,
                override val CLASS: Int,
                override val TTL: Int,
                val cname: String
        ) : Answer() {
            override val TYPE = AnswerType.CNAME

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

            override val RDLENGTH: Int
                get() = RDATA.size
        }
    }
}