package ktdns.core.message.record

import ktdns.extend.BytesNumber

class SOARecord(
        val MNAME: String,
        val RNAME: String,
        val SERIAL: Int,
        val REFRESH: Int,
        val RETRY: Int,
        val EXPIRE: Int,
        val MIN_TTL: Int,
        override val NAME: String,
        override val CLASS: Int,
        override val TTL: Int
) : Record() {

    override val TYPE = RecordType.SOA
    //    override val CLASS = 1
    override val RDATA: ByteArray
        get() {
            val arrayList = ArrayList<Byte>()

            arrayList.addAll(string2RDATA(MNAME).toTypedArray())
            arrayList.addAll(string2RDATA(RNAME).toTypedArray())

            arrayList.addAll(BytesNumber.getIntByteArray(SERIAL).toTypedArray())
            arrayList.addAll(BytesNumber.getIntByteArray(REFRESH).toTypedArray())
            arrayList.addAll(BytesNumber.getIntByteArray(RETRY).toTypedArray())
            arrayList.addAll(BytesNumber.getIntByteArray(EXPIRE).toTypedArray())
            arrayList.addAll(BytesNumber.getIntByteArray(MIN_TTL).toTypedArray())

            return arrayList.toByteArray()
        }
}