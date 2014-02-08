/**
 * Generated class : msg_file_transfer_start
 * DO NOT MODIFY!
 **/
package org.mavlink.messages.common;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.IMAVLinkCRC;
import org.mavlink.MAVLinkCRC;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.mavlink.io.LittleEndianDataInputStream;
import org.mavlink.io.LittleEndianDataOutputStream;
/**
 * Class msg_file_transfer_start
 * Begin file transfer
 **/
public class msg_file_transfer_start extends MAVLinkMessage {
  public static final int MAVLINK_MSG_ID_FILE_TRANSFER_START = 110;
  private static final long serialVersionUID = MAVLINK_MSG_ID_FILE_TRANSFER_START;
  public msg_file_transfer_start() {
    this(1,1);
}
  public msg_file_transfer_start(int sysId, int componentId) {
    messageType = MAVLINK_MSG_ID_FILE_TRANSFER_START;
    this.sysId = sysId;
    this.componentId = componentId;
    length = 254;
}

  /**
   * Unique transfer ID
   */
  public long transfer_uid;
  /**
   * File size in bytes
   */
  public long file_size;
  /**
   * Destination path
   */
  public char[] dest_path = new char[240];
  public void setDest_path(String tmp) {
    int len = Math.min(tmp.length(), 240);
    for (int i=0; i<len; i++) {
      dest_path[i] = tmp.charAt(i);
    }
    for (int i=len; i<240; i++) {
      dest_path[i] = 0;
    }
  }
  public String getDest_path() {
    String result="";
    for (int i=0; i<240; i++) {
      if (dest_path[i] != 0) result=result+dest_path[i]; else break;
    }
    return result;
  }
  /**
   * Transfer direction: 0: from requester, 1: to requester
   */
  public int direction;
  /**
   * RESERVED
   */
  public int flags;
/**
 * Decode message with raw data
 */
public void decode(LittleEndianDataInputStream dis) throws IOException {
  transfer_uid = (long)dis.readLong();
  file_size = (int)dis.readInt()&0x00FFFFFFFF;
  for (int i=0; i<240; i++) {
    dest_path[i] = (char)dis.readByte();
  }
  direction = (int)dis.readUnsignedByte()&0x00FF;
  flags = (int)dis.readUnsignedByte()&0x00FF;
}
/**
 * Encode message with raw data and other informations
 */
public byte[] encode() throws IOException {
  byte[] buffer = new byte[8+254];
   LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(new ByteArrayOutputStream());
  dos.writeByte((byte)0xFE);
  dos.writeByte(length & 0x00FF);
  dos.writeByte(sequence & 0x00FF);
  dos.writeByte(sysId & 0x00FF);
  dos.writeByte(componentId & 0x00FF);
  dos.writeByte(messageType & 0x00FF);
  dos.writeLong(transfer_uid);
  dos.writeInt((int)(file_size&0x00FFFFFFFF));
  for (int i=0; i<240; i++) {
    dos.writeByte(dest_path[i]);
  }
  dos.writeByte(direction&0x00FF);
  dos.writeByte(flags&0x00FF);
  dos.flush();
  byte[] tmp = dos.toByteArray();
  for (int b=0; b<tmp.length; b++) buffer[b]=tmp[b];
  int crc = MAVLinkCRC.crc_calculate_encode(buffer, 254);
  crc = MAVLinkCRC.crc_accumulate((byte) IMAVLinkCRC.MAVLINK_MESSAGE_CRCS[messageType], crc);
  byte crcl = (byte) (crc & 0x00FF);
  byte crch = (byte) ((crc >> 8) & 0x00FF);
  buffer[260] = crcl;
  buffer[261] = crch;
  dos.close();
  return buffer;
}
public String toString() {
return "MAVLINK_MSG_ID_FILE_TRANSFER_START : " +   "  transfer_uid="+transfer_uid+  "  file_size="+file_size+  "  dest_path="+getDest_path()+  "  direction="+direction+  "  flags="+flags;}
}
