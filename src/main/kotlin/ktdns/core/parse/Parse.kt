package ktdns.core.parse

import ktdns.core.message.Message
import ktdns.extend.toUInt
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class Parse {
    fun parseQuery(buf: ByteArray, socket: DatagramSocket, sourceAddress: InetAddress, sourcePort: Int): Message {
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
        val QR = FLAGS[0].toUInt() and 0b10000000
        val opcode = FLAGS[0].toUInt() and 0b01111000
        val AA = FLAGS[0].toUInt() and 0b00000100
        val TC = FLAGS[0].toUInt() and 0b00000010
        val RD = FLAGS[0].toUInt() and 0b00000001
        val RA = FLAGS[1].toUInt() and 0b10000000
        val Z = FLAGS[1].toUInt() and 0b01110000
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
        val QNAMEBuilder = StringBuilder()

        var pos = 0
        /*var length = noHeaderBuf[pos].toUInt()
        var QNAMELength = 1 + length
        do {
//            length = noHeaderBuf[pos].toUInt()
            pos++
            QNAMEBuilder.append(String(noHeaderBuf.copyOfRange(pos, pos + length)))
            QNAMEBuilder.append('.')

            pos += length
            length = noHeaderBuf[pos].toUInt()

            QNAMELength += 1 + length
        } while (length != 0)

        val QNAME = QNAMEBuilder.toString()
        val QTYPE = ByteBuffer.wrap(noHeaderBuf.copyOfRange(QNAMELength, QNAMELength + 2)).short.toInt()
        val QCLASS = ByteBuffer.wrap(noHeaderBuf.copyOfRange(QNAMELength + 2, QNAMELength + 4)).short.toInt()

        val question = Message.Question()
        question.QNAME = QNAME
        question.QTYPE = QTYPE
        question.QCLASS = QCLASS

        message.questions.add(question)*/

        for (i in 0 until QDCOUNT) {
            var length = noHeaderBuf[pos].toUInt()
            var QNAMELength = 1 + length
            do {
//            length = noHeaderBuf[pos].toUInt()
                pos++
                QNAMEBuilder.append(String(noHeaderBuf.copyOfRange(pos, pos + length)))
                QNAMEBuilder.append('.')

                pos += length
                length = noHeaderBuf[pos].toUInt()

                QNAMELength += 1 + length
            } while (length != 0)

            val QNAME = QNAMEBuilder.toString()
            val QTYPE = ByteBuffer.wrap(noHeaderBuf.copyOfRange(QNAMELength, QNAMELength + 2)).short.toInt()
            val QCLASS = ByteBuffer.wrap(noHeaderBuf.copyOfRange(QNAMELength + 2, QNAMELength + 4)).short.toInt()

            val question = Message.Question()
            question.QNAME = QNAME
            question.QTYPE = QTYPE
            question.QCLASS = QCLASS

            message.questions.add(question)
            pos = QNAMELength + 4
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

        message.socket = socket
        message.souceAddress = sourceAddress
        message.sourcePort = sourcePort

        return message
    }
}