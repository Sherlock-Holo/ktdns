package ktdns.message

class Message {
    var header = Header()

    var question = Question()

    class Header {
        lateinit var ID: ByteArray
        lateinit var FLAGS: ByteArray
        var QR: Int? = null
        var opcode: Int? = null
        var AA: Int? = null
        var TC: Int? = null
        var RD: Int? = null
        var RA: Int? = null
        var Z: Int? = null
        var RCODE: Int? = null
    }

    class Question {
        lateinit var QNAME: String
        var QTYPE: Int? = null
        var QCLASS: Int? = null
        val QNAMEByteArray: ByteArray
            get() {
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
    }
}