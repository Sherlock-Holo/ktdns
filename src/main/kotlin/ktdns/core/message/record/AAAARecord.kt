package ktdns.core.message.record

import java.net.InetAddress

class AAAARecord(
        override val NAME: String,
        override val CLASS: Int,
        override val TTL: Int,
        val address: InetAddress
) : Record() {

    override val TYPE = RecordType.AAAA
    override val RDLENGTH = 16
    override val RDATA = address.address
}