package ktdns.core.parse

import ktdns.KtdnsException
import ktdns.core.message.Message
import ktdns.core.message.record.AAAARecord
import ktdns.core.message.record.ARecord
import ktdns.core.message.record.CNAMERecord
import ktdns.core.message.record.EDNSRecord
import ktdns.extend.BytesNumber
import ktdns.extend.toUInt
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class Parse {
    private data class MessageInfo(val buf: ByteArray, val pos: Int, val message: Message)

    companion object {
        fun parseQuery(buf: ByteArray, socket: DatagramSocket, sourceAddress: InetAddress, sourcePort: Int): Message {
            val message = parseHeader(buf)


            /**
            0  1  2  3  4  5  6  7  0  1  2  3  4  5  6  7
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                                               |
            /                     QNAME                     /
            /                                               /
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                     QTYPE                     |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                     QCLASS                    |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

            # Question
            ## QNAME 无符号8bit为单位长度不限表示查询名(广泛的说就是：域名)
            ## QTYPE 无符号16bit整数表示查询的协议类型
            ## QCLASS 无符号16bit整数表示查询的类,比如，IN代表Internet
             */

            val noHeaderBuf = buf.copyOfRange(12, buf.size)

            val QNAME = getName(0, noHeaderBuf).name
            val QTYPE = BytesNumber.getShort(noHeaderBuf.copyOfRange(QNAME.length + 1, QNAME.length + 3)).toInt()
            val QCLASS = BytesNumber.getShort(noHeaderBuf.copyOfRange(QNAME.length + 3, QNAME.length + 5)).toInt()

            val question = Message.Question()
            question.QNAME = QNAME
            question.QTYPE = QTYPE
            question.QCLASS = QCLASS

            message.questions.add(question)

            val pos = QNAME.length + 5

            val messageInfo = MessageInfo(noHeaderBuf, pos, message)

            parseAR(messageInfo)

            message.socket = socket
            message.souceAddress = sourceAddress
            message.sourcePort = sourcePort

            return message
        }

        fun parseAnswer(buf: ByteArray): Message {
            val message = parseHeader(buf)


            /**
            0  1  2  3  4  5  6  7  0  1  2  3  4  5  6  7
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                                               |
            /                     QNAME                     /
            /                                               /
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                     QTYPE                     |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                     QCLASS                    |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

            # Question
            ## QNAME 无符号8bit为单位长度不限表示查询名(广泛的说就是：域名)
            ## QTYPE 无符号16bit整数表示查询的协议类型
            ## QCLASS 无符号16bit整数表示查询的类,比如，IN代表Internet
             */

            val noHeaderBuf = buf.copyOfRange(12, buf.size)

            val QNAME = getName(0, noHeaderBuf).name
            val QTYPE = BytesNumber.getShort(noHeaderBuf.copyOfRange(QNAME.length + 1, QNAME.length + 3)).toInt()
            val QCLASS = BytesNumber.getShort(noHeaderBuf.copyOfRange(QNAME.length + 3, QNAME.length + 5)).toInt()

            val question = Message.Question()
            question.QNAME = QNAME
            question.QTYPE = QTYPE
            question.QCLASS = QCLASS

            message.questions.add(question)

            val newPos = 12 + QNAME.length + 1 + 4

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

            var messageInfo = MessageInfo(buf, newPos, message)
            messageInfo = parseAN(messageInfo)
            parseAR(messageInfo)

            return message
        }

        private data class NameAndLength(val name: String, val length: Int)

        private fun getName(offset: Int, buf: ByteArray): NameAndLength {
            var length = buf[offset].toUInt()
            var stringLength = 0

            if (length != 0b11000000) {
                var pos = offset + 1
                val sb = StringBuilder()
                while (length != 0) {
                    sb.append(String(buf.copyOfRange(pos, pos + length)))
                    sb.append('.')
                    pos += length

                    stringLength += length + 1

                    length = buf[pos].toUInt()

                    if (length == 0b11000000) {
                        pos = (BytesNumber.getShort(buf.copyOfRange(pos, pos + 2))).toInt() and 0b00111111
                        sb.append(getName(pos, buf).name)
                        return NameAndLength(sb.toString(), stringLength + 2)
                    }

                    pos++
                }

                return NameAndLength(sb.toString(), stringLength + 1)
            } else {
                val pos = (BytesNumber.getShort(buf.copyOfRange(offset, offset + 2))).toInt() and 0b00111111
                return NameAndLength(getName(pos, buf).name, 2)
            }
        }

        private fun parseHeader(buf: ByteArray): Message {
            val message = Message()

            /**
            # Header
            ## ID: 2 bytes
            ## FLAGS: 2 bytes
            ### (包含 QR, opcode, AA, TC, RD, RA, Z, RCODE) {
            QR: 0表示查询报文，1表示响应报文
            opcode: 通常值为0（标准查询），其他值为1（反向查询）和2（服务器状态请求）,[3,15]保留值
            AA: 表示授权回答（authoritative answer）– 这个比特位在应答的时候才有意义，指出给出应答的服务器是查询域名的授权解析服务器
            TC: 表示可截断的（truncated）–用来指出报文比允许的长度还要长，导致被截断
            RD: 表示期望递归(Recursion Desired) – 这个比特位被请求设置，应答的时候使用的相同的值返回。如果设置了RD，就建议域名服务器进行递归解析，递归查询的支持是可选的
            RA: 表示支持递归(Recursion Available) – 这个比特位在应答中设置或取消，用来代表服务器是否支持递归查询
            Z : 保留值，暂未使用
            RCODE: 应答码(Response code) - 这4个比特位在应答报文中设置，代表的含义如下:
            0: 没有错误
            1: 报文格式错误
            2: 服务器失败
            3: 名字错误
            4: 没有实现
            5: 服务器因为策略而不予以应答
            6-15: 未使用
            }

            ## QDCOUNT: 无符号16bit整数表示报文请求段中的问题记录数
            ## ANCOUNT: 无符号16bit整数表示报文回答段中的回答记录数
            ## NSCOUNT: 无符号16bit整数表示报文授权段中的授权记录数
            ## ARCOUNT: 无符号16bit整数表示报文附加段中的附加记录数

            0  1  2  3  4  5  6  7  0  1  2  3  4  5  6  7
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                      ID                       |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |QR|   opcode  |AA|TC|RD|RA|   Z    |   RCODE   |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                    QDCOUNT                    |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                    ANCOUNT                    |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                    NSCOUNT                    |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            |                    ARCOUNT                    |
            +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
             */

            val ID = buf.copyOfRange(0, 2)
            val FLAGS = buf.copyOfRange(2, 4)
            val QR = FLAGS[0].toUInt() and 0b10000000 ushr 7
            val opcode = FLAGS[0].toUInt() and 0b01111000 ushr 3
            val AA = FLAGS[0].toUInt() and 0b00000100 ushr 2
            val TC = FLAGS[0].toUInt() and 0b00000010 ushr 1
            val RD = FLAGS[0].toUInt() and 0b00000001
            val RA = FLAGS[1].toUInt() and 0b10000000 ushr 7
            val Z = FLAGS[1].toUInt() and 0b01110000 ushr 4
            val RCODE = FLAGS[1].toUInt() and 0b00001111

            val counts = ByteBuffer.wrap(buf.copyOfRange(4, 12))
            val QDCOUNT = counts.short.toInt()
            val ANCOUNT = counts.short.toInt()
            val NSCOUNT = counts.short.toInt()
            val ARCOUNT = counts.short.toInt()

            val header = message.header

            header.ID = ID
            header.QR = QR
            header.opcode = opcode
            header.AA = AA
            header.TC = TC
            header.RD = RD
            header.RA = RA
            header.Z = Z
            header.RCODE = RCODE
            header.QDCOUNT = QDCOUNT
            header.ANCOUNT = ANCOUNT
            header.NSCOUNT = NSCOUNT
            header.ARCOUNT = ARCOUNT

            return message
        }

        private fun parseAN(messageInfo: MessageInfo): MessageInfo {
            val message = messageInfo.message
            var newPos = messageInfo.pos
            val buf = messageInfo.buf

            for (i in 0 until message.header.ANCOUNT) {
                val nameAndLength = getName(newPos, buf)
                val name = nameAndLength.name
                newPos += nameAndLength.length
                val type = BytesNumber.getShort(buf.copyOfRange(newPos, newPos + 2)).toInt()
                val `class` = (BytesNumber.getShort(buf.copyOfRange(newPos + 2, newPos + 4))).toInt()
                val ttl = BytesNumber.getInt(buf.copyOfRange(newPos + 4, newPos + 8))
                val rdlength = BytesNumber.getShort(buf.copyOfRange(newPos + 8, newPos + 10)).toInt()

                newPos += 2 + 2 + 4 + 2

                val answer = when (type) {
                    5 -> {
                        val cname = getName(newPos, buf).name
                        CNAMERecord(name, `class`, ttl, cname)
                    }

                    1 -> {
                        val address = InetAddress.getByAddress(buf.copyOfRange(newPos, newPos + 4))
                        ARecord(name, `class`, ttl, address)
                    }

                    28 -> {
                        val address = InetAddress.getByAddress(buf.copyOfRange(newPos, newPos + 16))
                        AAAARecord(name, `class`, ttl, address)
                    }

                    else -> TODO("other not implement answer type: $type")
                }

                newPos += rdlength

                message.addAnswer(answer)
            }
            return MessageInfo(buf, newPos, message)
        }

        private fun parseAR(messageInfo: MessageInfo): MessageInfo {
            val message = messageInfo.message
            var pos = messageInfo.pos
            val noHeaderBuf = messageInfo.buf

            loop@ for (i in 0 until message.header.ARCOUNT) {
                /**  NAME | domain name | MUST be 0 (root domain) **/
                pos++

                val type = (BytesNumber.getShort(noHeaderBuf.copyOfRange(pos, pos + 2))).toInt()
                pos += 2

                when (type) {
                    41 -> {
                        val CLASS = (BytesNumber.getShort(noHeaderBuf.copyOfRange(pos, pos + 2))).toInt()
                        pos += 2

                        val EXTENDED_RCODE = noHeaderBuf[pos++].toUInt()

                        val EDNS_VERSION = noHeaderBuf[pos++].toUInt()
                        val Z = noHeaderBuf.copyOfRange(pos, pos + 2)
                        pos += 2

                        val RDLENGTH = (BytesNumber.getShort(noHeaderBuf.copyOfRange(pos, pos + 2))).toInt()
                        pos += 2

                        val edns = EDNSRecord(CLASS, EXTENDED_RCODE, EDNS_VERSION, Z)

                        if (RDLENGTH == 0) {
                            message.addAdditional(edns)
                            continue@loop
                        }

                        val RDATA = noHeaderBuf.copyOfRange(pos, pos + RDLENGTH)

                        var ePos = 0
                        /** EDNS pos **/

                        while (ePos < RDLENGTH - 1) {
                            val OPTION_CODE = BytesNumber.getShort(RDATA.copyOfRange(ePos, ePos + 2)).toInt()
                            ePos += 2

                            val OPTION_LENGTH = (BytesNumber.getShort(RDATA.copyOfRange(ePos, ePos + 2))).toInt()
                            ePos += 2

                            when (OPTION_CODE) {
                            /** 8 for ECS **/
                                8 -> {
                                    val FAMILY = (BytesNumber.getShort(RDATA.copyOfRange(ePos, ePos + 2))).toInt()
                                    ePos += 2

                                    val sourceNetMask = RDATA[ePos++].toUInt()
                                    val scpoeNetMask = RDATA[ePos++].toUInt()

                                    val ipByteArray =
                                            RDATA.copyOfRange(ePos, ePos + OPTION_LENGTH - 4).run {
                                                when (FAMILY) {
                                                    1 -> {
                                                        if (this.size < 4) return@run this + ByteArray(4 - this.size) { 0 }
                                                        else return@run this
                                                    }

                                                    2 -> {
                                                        if (this.size < 16) return@run this + ByteArray(16 - this.size) { 0 }
                                                        else return@run this
                                                    }

                                                    else -> throw KtdnsException("error FAMILY: $FAMILY")
                                                }
                                            }

                                    ePos += OPTION_LENGTH - 4

                                    val ecsData = EDNSRecord.ECS_DATA(
                                            InetAddress.getByAddress(ipByteArray),
                                            sourceNetMask,
                                            scpoeNetMask
                                    )
                                    edns.addOptionData(ecsData)
                                }

                                else -> {
                                    val data = RDATA.copyOfRange(ePos, ePos + OPTION_LENGTH)
                                    ePos += OPTION_LENGTH

                                    val unknownData = EDNSRecord.UnknownData(OPTION_CODE, data)
                                    edns.addOptionData(unknownData)
                                }
                            }
                        }
                        message.addAdditional(edns)
                    }
                }
            }
            return MessageInfo(noHeaderBuf, pos, message)
        }
    }
}