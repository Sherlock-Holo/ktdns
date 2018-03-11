package ktdns.message

import java.nio.ByteBuffer

class Message {
    var header = Header()

    val questions = ArrayList<Question>()

    val answers = ArrayList<Answer>()

    val byteArray: ByteArray
        get() {

            val arrayList = ArrayList<Byte>()

            /** header **/
            arrayList.addAll(header.byteArray.toTypedArray())

            /** questions **/
            questions.forEach {
                arrayList.addAll(it.byteArray.toTypedArray())
            }


            return arrayList.toByteArray()
        }

    fun setAnswerMessage(boolean: Boolean) {
        if (boolean) this.header.QR = 1
        else this.header.QR = 0
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
                        QR or
                                opcode or
                                AA or
                                TC or
                                RD or
                                RA
                        ).toByte()

                val byte2 = (Z or RCODE).toByte()

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

        var QNAME = ""
        var QTYPE = -1
        var QCLASS = -1
        val QNAMEByteArray: ByteArray
            get() {
                check(QNAME != "")

                val list = QNAME.substring(0 until QNAME.length - 1).split('.')
                val arrayList = ArrayList<Byte>()

                for (i in 0 until list.size) {
                    arrayList.add(list[i].length.toByte())
                    arrayList.addAll(list[i].toByteArray().toTypedArray())

                    if (i != list.size - 1) {
                        arrayList.add(0.toByte())
                    }

                }
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
    }

    class Answer
}