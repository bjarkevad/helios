/**
 * Generated class : msg_battery_status
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
 * Class msg_battery_status
 * Transmitte battery informations for a accu pack.
 **/
public class msg_battery_status extends MAVLinkMessage {
  public static final int MAVLINK_MSG_ID_BATTERY_STATUS = 147;
  private static final long serialVersionUID = MAVLINK_MSG_ID_BATTERY_STATUS;
  public msg_battery_status() {
    this(1,1);
}
  public msg_battery_status(int sysId, int componentId) {
    messageType = MAVLINK_MSG_ID_BATTERY_STATUS;
    this.sysId = sysId;
    this.componentId = componentId;
    length = 24;
}

  /**
   * Consumed charge, in milliampere hours (1 = 1 mAh), -1: autopilot does not provide mAh consumption estimate
   */
  public long current_consumed;
  /**
   * Consumed energy, in 100*Joules (intergrated U*I*dt)  (1 = 100 Joule), -1: autopilot does not provide energy consumption estimate
   */
  public long energy_consumed;
  /**
   * Battery voltage of cell 1, in millivolts (1 = 1 millivolt)
   */
  public int voltage_cell_1;
  /**
   * Battery voltage of cell 2, in millivolts (1 = 1 millivolt), -1: no cell
   */
  public int voltage_cell_2;
  /**
   * Battery voltage of cell 3, in millivolts (1 = 1 millivolt), -1: no cell
   */
  public int voltage_cell_3;
  /**
   * Battery voltage of cell 4, in millivolts (1 = 1 millivolt), -1: no cell
   */
  public int voltage_cell_4;
  /**
   * Battery voltage of cell 5, in millivolts (1 = 1 millivolt), -1: no cell
   */
  public int voltage_cell_5;
  /**
   * Battery voltage of cell 6, in millivolts (1 = 1 millivolt), -1: no cell
   */
  public int voltage_cell_6;
  /**
   * Battery current, in 10*milliamperes (1 = 10 milliampere), -1: autopilot does not measure the current
   */
  public int current_battery;
  /**
   * Accupack ID
   */
  public int accu_id;
  /**
   * Remaining battery energy: (0%: 0, 100%: 100), -1: autopilot does not estimate the remaining battery
   */
  public int battery_remaining;
/**
 * Decode message with raw data
 */
public void decode(LittleEndianDataInputStream dis) throws IOException {
  current_consumed = (int)dis.readInt();
  energy_consumed = (int)dis.readInt();
  voltage_cell_1 = (int)dis.readUnsignedShort()&0x00FFFF;
  voltage_cell_2 = (int)dis.readUnsignedShort()&0x00FFFF;
  voltage_cell_3 = (int)dis.readUnsignedShort()&0x00FFFF;
  voltage_cell_4 = (int)dis.readUnsignedShort()&0x00FFFF;
  voltage_cell_5 = (int)dis.readUnsignedShort()&0x00FFFF;
  voltage_cell_6 = (int)dis.readUnsignedShort()&0x00FFFF;
  current_battery = (int)dis.readShort();
  accu_id = (int)dis.readUnsignedByte()&0x00FF;
  battery_remaining = (int)dis.readByte();
}
/**
 * Encode message with raw data and other informations
 */
public byte[] encode() throws IOException {
  byte[] buffer = new byte[8+24];
   LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(new ByteArrayOutputStream());
  dos.writeByte((byte)0xFE);
  dos.writeByte(length & 0x00FF);
  dos.writeByte(sequence & 0x00FF);
  dos.writeByte(sysId & 0x00FF);
  dos.writeByte(componentId & 0x00FF);
  dos.writeByte(messageType & 0x00FF);
  dos.writeInt((int)(current_consumed&0x00FFFFFFFF));
  dos.writeInt((int)(energy_consumed&0x00FFFFFFFF));
  dos.writeShort(voltage_cell_1&0x00FFFF);
  dos.writeShort(voltage_cell_2&0x00FFFF);
  dos.writeShort(voltage_cell_3&0x00FFFF);
  dos.writeShort(voltage_cell_4&0x00FFFF);
  dos.writeShort(voltage_cell_5&0x00FFFF);
  dos.writeShort(voltage_cell_6&0x00FFFF);
  dos.writeShort(current_battery&0x00FFFF);
  dos.writeByte(accu_id&0x00FF);
  dos.write(battery_remaining&0x00FF);
  dos.flush();
  byte[] tmp = dos.toByteArray();
  for (int b=0; b<tmp.length; b++) buffer[b]=tmp[b];
  int crc = MAVLinkCRC.crc_calculate_encode(buffer, 24);
  crc = MAVLinkCRC.crc_accumulate((byte) IMAVLinkCRC.MAVLINK_MESSAGE_CRCS[messageType], crc);
  byte crcl = (byte) (crc & 0x00FF);
  byte crch = (byte) ((crc >> 8) & 0x00FF);
  buffer[30] = crcl;
  buffer[31] = crch;
  dos.close();
  return buffer;
}
public String toString() {
return "MAVLINK_MSG_ID_BATTERY_STATUS : " +   "  current_consumed="+current_consumed+  "  energy_consumed="+energy_consumed+  "  voltage_cell_1="+voltage_cell_1+  "  voltage_cell_2="+voltage_cell_2+  "  voltage_cell_3="+voltage_cell_3+  "  voltage_cell_4="+voltage_cell_4+  "  voltage_cell_5="+voltage_cell_5+  "  voltage_cell_6="+voltage_cell_6+  "  current_battery="+current_battery+  "  accu_id="+accu_id+  "  battery_remaining="+battery_remaining;}
}
