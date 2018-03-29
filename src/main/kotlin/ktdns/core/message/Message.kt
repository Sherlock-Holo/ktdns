package ktdns.core.message

import ktdns.core.message.record.CNAMERecord
import ktdns.core.message.record.EDNSRecord
import ktdns.core.message.record.Record
import ktdns.extend.BytesNumber
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.HashMap

class Message : Cloneable {
    var header = Header()

    private val stringMap = HashMap<String, Int>()

    val questions = ArrayList<Question>()

    val answers = ArrayList<Record>()
    private var CNAMEPos = -1

    val authRecords = ArrayList<Record>()

    val additional = ArrayList<Record>()

    lateinit var socket: DatagramSocket
    lateinit var souceAddress: InetAddress
    var sourcePort = -1

    val byteArray: ByteArray
        get() {

            val arrayList = ArrayList<Byte>()

            this.header.QDCOUNT = questions.size
            this.header.ANCOUNT = answers.size
            this.header.AUCOUNT = authRecords.size
            this.header.ARCOUNT = additional.size

            /** header **/
            arrayList.addAll(header.byteArray.toTypedArray())

            /** questions **/
            questions.forEach {
                if (stringMap[it.QNAME] == null) {
                    stringMap[it.QNAME] = arrayList.size
                }

                arrayList.addAll(it.byteArray.toTypedArray())
            }

            /** answers **/
            answers.forEach {
                arrayList.addAll(it.toByteArray(stringMap[it.NAME]).toTypedArray())

                when (it.TYPE) {
                    Record.RecordType.CNAME -> {
                        if (stringMap[(it as CNAMERecord).cname] == null) stringMap[it.cname] = arrayList.size - it.RDLENGTH
                    }
                }
            }

            /** authRecords **/
            authRecords.forEach { arrayList.addAll(it.toByteArray(stringMap[it.NAME]).toTypedArray()) }

            /** additional **/
            additional.forEach {
                it.apply {
                    when (it.TYPE) {

                        Record.RecordType.EDNS -> {

                            it as EDNSRecord
                            it.optionDatas.forEach {
                                when {
                                    EDNSRecord.ECS_DATA::class.java.isInstance(it) -> {
                                        if (header.QR == 1) {
                                            it as EDNSRecord.ECS_DATA
                                            it.scopeNetMask = it.sourceNetMask
                                        }
                                    }
                                }
                            }

                            arrayList.addAll(it.toByteArray(null).toTypedArray())
                        }

//                        else -> TODO("other additional record")
                    }
                }
            }

            return arrayList.toByteArray()
        }

    fun setAnswerMessage(boolean: Boolean) {
        if (boolean) {
            this.header.QR = 1

        } else this.header.QR = 0
    }

    fun addQuestion(question: Question): Message {
        header.QDCOUNT++
        questions.add(question)
        return this
    }

    fun addAnswer(answer: Record): Message {
        header.ANCOUNT++
        answers.add(answer)
        if (answer.TYPE == Record.RecordType.CNAME) {
            CNAMEPos = answers.size - 1
        }

        return this
    }

    fun addAURecord(record: Record): Message {
        header.AUCOUNT++
        authRecords.add(record)
        return this
    }

    fun addAdditionalRecord(record: Record): Message {
        header.ARCOUNT++
        additional.add(record)
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
        var AUCOUNT = -1
        var ARCOUNT = -1

        val counts: ByteArray
            get() {
                check(QDCOUNT != -1)
                check(ANCOUNT != -1)
                check(AUCOUNT != -1)
                check(ARCOUNT != -1)

                val buf = ByteBuffer.allocate(8)
                buf.putShort(QDCOUNT.toShort())
                buf.putShort(ANCOUNT.toShort())
                buf.putShort(AUCOUNT.toShort())
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
        ## AUCOUNT: 无符号16bit整数表示报文授权段中的授权记录数
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

                arrayList.addAll(BytesNumber.getShortByteArray(QTYPE.toShort()).toTypedArray())

                arrayList.addAll(BytesNumber.getShortByteArray(QCLASS.toShort()).toTypedArray())

                return arrayList.toByteArray()
            }
    }

    companion object {
        fun buildQuery(question: Question): Message {
            val message = Message()
            val header = message.header
            val random = Random()

            header.ID = ByteArray(2).apply { random.nextBytes(this) }

            header.QR = 0
            header.opcode = 0
            header.AA = 0
            header.TC = 0
            header.RD = 1
            header.RA = 0
            header.Z = 0
            header.RCODE = 0
            header.QDCOUNT = 0
            header.ANCOUNT = 0
            header.AUCOUNT = 0
            header.ARCOUNT = 0

            message.addQuestion(question)

            return message
        }
    }
}