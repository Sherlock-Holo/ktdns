package ktdns.core.message.record

class CNAMERecord(
        override val NAME: String,
        override val CLASS: Int,
        override val TTL: Int,
        val cname: String
) : Record() {
    override val TYPE = RecordType.CNAME

    override val RDATA get() = string2RDATA(cname)

    override val RDLENGTH get() = RDATA.size
}