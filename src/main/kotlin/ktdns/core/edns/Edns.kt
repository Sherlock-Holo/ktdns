package ktdns.core.edns

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class Edns {
    companion object {
        var myIp: InetAddress? = null
            get() {
                return if (field == null) {
                    val url = URL("http://myip.ipip.net")
                    val connection = url.openConnection() as HttpURLConnection
                    val response = connection.inputStream.reader().readText()
                    val ip = response.split(' ')[1].run {
                        this.substring(3 until this.length)
                    }

                    field = InetAddress.getByName(ip)
                    field
                } else field
            }
    }
}