package ktdns.core.message.record

class NSRecord(
        override val NAME: String,
        override val CLASS: Int,
        override val TTL: Int,
        val server: String
) : Record() {
    override val TYPE = RecordType.NS

    override val RDATA get() = super.string2RDATA(server)
}