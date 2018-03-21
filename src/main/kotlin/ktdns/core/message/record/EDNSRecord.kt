package ktdns.core.message.record

import ktdns.KtdnsException
import ktdns.extend.BytesNumber
import ktdns.extend.toUInt
import java.net.InetAddress

class EDNSRecord(
        payloadSize: Int,
        val extended_RCODE: Int,
        val EDNSVersion: Int,
        val Z: ByteArray
) : Record() {

    /**
    +0 (MSB)                            +1 (LSB)
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
    0: |                          OPTION-CODE                       |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
    2: |                         OPTION-LENGTH                      |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
    4: |                            FAMILY                          |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
    6: |     SOURCE PREFIX-LENGTH      |     SCOPE PREFIX-LENGTH    |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
    8: /                           ADDRESS...                       /
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
     */

    val optionDatas = ArrayList<OPTION_DATA>()

    fun addOptionData(option_data: OPTION_DATA): EDNSRecord {
        optionDatas.add(option_data)
        return this
    }

    override val NAME = "0"
    override val TYPE = RecordType.EDNS
    override val CLASS = payloadSize

    override val TTL get() = BytesNumber.getInt(byteArrayOf(extended_RCODE.toByte(), EDNSVersion.toByte()) + Z)

    override val RDATA: ByteArray
        get() {
            val arrayList = ArrayList<Byte>()

            optionDatas.forEach { arrayList.addAll(it.toByteArray().toTypedArray()) }

            return arrayList.toByteArray()
        }

    override fun toByteArray(offset: Int?): ByteArray {
        val arrayList = ArrayList<Byte>()

        arrayList.add(0)
        arrayList.addAll(BytesNumber.getShortByteArray(TYPE.type.toShort()).toTypedArray())
        arrayList.addAll(BytesNumber.getShortByteArray(CLASS.toShort()).toTypedArray())
        arrayList.addAll(BytesNumber.getIntByteArray(TTL).toTypedArray())
        arrayList.addAll(BytesNumber.getShortByteArray(RDLENGTH.toShort()).toTypedArray())
        arrayList.addAll(RDATA.toTypedArray())

        return arrayList.toByteArray()
    }

    abstract class OPTION_DATA {
        abstract val optionCode: Int
        open val optionLength get() = optionData.size
        abstract val optionData: ByteArray

        open fun toByteArray(): ByteArray {
            val arrayList = ArrayList<Byte>()

            arrayList.addAll(BytesNumber.getShortByteArray(optionCode.toShort()).toTypedArray())
            arrayList.addAll(BytesNumber.getShortByteArray(optionLength.toShort()).toTypedArray())
            arrayList.addAll(optionData.toTypedArray())

            return arrayList.toByteArray()
        }
    }

    class ECS_DATA(
            val ip: InetAddress,
            var sourceNetMask: Int,
            var scopeNetMask: Int
    ) : OPTION_DATA() {

        /**
        +0 (MSB)                            +1 (LSB)
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        0: |                          OPTION-CODE                       |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        2: |                         OPTION-LENGTH                      |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        4: |                            FAMILY                          |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        6: |     SOURCE PREFIX-LENGTH      |     SCOPE PREFIX-LENGTH    |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        8: /                           ADDRESS...                       /
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
         */

        override val optionCode = 8

        val ECS_IP: InetAddress
            get() {
                val ipByteArray = ip.address
                when (ipByteArray.size) {
                    4 -> {
                        val sb = StringBuilder()
                        when {
                            (32 - sourceNetMask) <= 8 -> {
                                for (i in 0 until 8) {
                                    if (i < 8 - (32 - sourceNetMask)) sb.append('1')
                                    else sb.append('0')
                                }
                                ipByteArray[3] = (ipByteArray[3].toUInt() and Integer.parseInt(sb.toString(), 2)).toByte()
                            }

                            (32 - sourceNetMask) <= 16 -> {
                                ipByteArray[3] = 0
                                for (i in 0 until 8) {
                                    if (i < 8 - (24 - sourceNetMask)) sb.append('1')
                                    else sb.append('0')
                                }
                                ipByteArray[2] = (ipByteArray[2].toUInt() and Integer.parseInt(sb.toString(), 2)).toByte()
                            }

                            (32 - sourceNetMask) <= 24 -> {
                                ipByteArray[3] = 0
                                ipByteArray[2] = 0
                                for (i in 0 until 8) {
                                    if (i < 8 - (16 - sourceNetMask)) sb.append('1')
                                    else sb.append('0')
                                }
                                ipByteArray[1] = (ipByteArray[1].toUInt() and Integer.parseInt(sb.toString(), 2)).toByte()
                            }

                            else -> {
                                ipByteArray[3] = 0
                                ipByteArray[2] = 0
                                ipByteArray[1] = 0
                                for (i in 0 until 8) {
                                    if (i < 8 - (8 - sourceNetMask)) sb.append('1')
                                    else sb.append('0')
                                }
                                ipByteArray[0] = (ipByteArray[0].toUInt() and Integer.parseInt(sb.toString(), 2)).toByte()
                            }
                        }
                        return InetAddress.getByAddress(ipByteArray)
                    }

                    16 -> TODO("ipv6 mask")

                    else -> throw KtdnsException("error address length: ${ipByteArray.size}")
                }
            }

        override val optionData: ByteArray
            get() {
                val arrayList = ArrayList<Byte>()

                val ECS_IPByteArray = ECS_IP.address
                val family =
                        if (ECS_IPByteArray.size == 4) BytesNumber.getShortByteArray(1)
                        else BytesNumber.getShortByteArray(2)
                arrayList.addAll(family.toTypedArray())

                arrayList.addAll(arrayListOf(sourceNetMask.toByte(), scopeNetMask.toByte()))

                when {
                    sourceNetMask <= 8 -> arrayList.add(ECS_IPByteArray[0])

                    sourceNetMask <= 16 -> arrayList.addAll(ECS_IPByteArray.copyOfRange(0, 2).toTypedArray())

                    sourceNetMask <= 24 -> arrayList.addAll(ECS_IPByteArray.copyOfRange(0, 3).toTypedArray())

                    else -> arrayList.addAll(ECS_IPByteArray.toTypedArray())
                }


                return arrayList.toByteArray()
            }
    }

    class UnknownData(
            override val optionCode: Int,
            override val optionData: ByteArray
    ) : OPTION_DATA()
}