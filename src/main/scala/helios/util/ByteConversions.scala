package helios.util

import akka.util.ByteString

object ByteConversions {

  /**
   * used to convert pretty print byte arrays in binary or hexadecimal
   * @param a the array to print
   */
  implicit class barrImpls(val a: Array[Byte]) {
    /**
     *
     * @return the hex representation of the byte array
     */
    def byteArrayToHex: String = {
      BigInt(1, a).toString(16)
    }

    /**
     *
     * @return the binary representation of the byte array
     */
    def byteArrayToBinary: String = {
      BigInt(1, a).toString(2)
    }
  }

  /**
   * formats a ByteString
   * @param data the ByteString to format
   * @return the formatted ByteString
   */
  def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + new String(data.toArray, "UTF-8")
}
