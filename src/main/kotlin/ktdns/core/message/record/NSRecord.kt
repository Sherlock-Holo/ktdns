package ktdns.core.message.record

class NSRecord(
        override val NAME: String,
        override val CLASS: Int,
        override val TTL: Int,
        val NSDNAME: String
) : Record() {
    override val TYPE = RecordType.NS

    override val RDATA get() = string2RDATA(NSDNAME)
}