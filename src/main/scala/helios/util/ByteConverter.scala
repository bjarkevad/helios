package helios.util

object ByteConverter {
  implicit class Converter(val a: Array[Byte]) {
    def byteArrayToHex: String = {
      BigInt(1, a).toString(16)
    }

    def byteArrayToBinary: String = {
      BigInt(1, a).toString(2)
    }
  }
}
