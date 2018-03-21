package ktdns.core.message.record

import java.net.InetAddress

class ARecord(
        override val NAME: String,
        override val CLASS: Int,
        override val TTL: Int,
        val address: InetAddress
) : Record() {

    override val TYPE = RecordType.A
    override val RDLENGTH = 4
    override val RDATA = address.address
}