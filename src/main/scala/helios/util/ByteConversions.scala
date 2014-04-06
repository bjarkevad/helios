package helios.util

import akka.util.ByteString

object ByteConversions {
  implicit class barrImpls(val a: Array[Byte]) {
    def byteArrayToHex: String = {
      BigInt(1, a).toString(16)
    }

    def byteArrayToBinary: String = {
      BigInt(1, a).toString(2)
    }
  }

  def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + new String(data.toArray, "UTF-8")
}
