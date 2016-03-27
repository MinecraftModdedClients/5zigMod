package eu.the5zig.mod.chat.network.packets;

import eu.the5zig.mod.chat.GroupMember;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Group;
import eu.the5zig.mod.chat.entity.Rank;
import eu.the5zig.mod.chat.entity.User;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.LocaleUtils;

public class PacketBuffer
  extends ByteBuf
{
  private ByteBuf buf;
  
  public PacketBuffer(ByteBuf buf)
  {
    this.buf = buf;
  }
  
  public static int getVarIntSize(int input)
  {
    for (int var1 = 1; var1 < 5; var1++) {
      if ((input & -1 << var1 * 7) == 0) {
        return var1;
      }
    }
    return 5;
  }
  
  public int readVarIntFromBuffer()
  {
    int var1 = 0;
    int var2 = 0;
    byte var3;
    do
    {
      var3 = readByte();
      var1 |= (var3 & 0x7F) << var2++ * 7;
      if (var2 > 5) {
        throw new RuntimeException("VarInt too big");
      }
    } while ((var3 & 0x80) == 128);
    return var1;
  }
  
  public void writeVarIntToBuffer(int input)
  {
    while ((input & 0xFFFFFF80) != 0)
    {
      writeByte(input & 0x7F | 0x80);
      input >>>= 7;
    }
    writeByte(input);
  }
  
  public void writeUUID(UUID uuid)
  {
    writeLong(uuid.getMostSignificantBits());
    writeLong(uuid.getLeastSignificantBits());
  }
  
  public UUID readUUID()
  {
    return new UUID(readLong(), readLong());
  }
  
  public void writeEnum(Enum e)
  {
    writeVarIntToBuffer(e.ordinal());
  }
  
  public <T extends Enum> T readEnum(Class<T> classOfT)
  {
    return ((Enum[])classOfT.getEnumConstants())[readVarIntFromBuffer()];
  }
  
  public void writeUser(User user)
  {
    writeString(user.getUsername());
    writeUUID(user.getUniqueId());
  }
  
  public User readUser()
  {
    return new User(readString(), readUUID());
  }
  
  public Rank readRank()
  {
    return Rank.values()[readVarIntFromBuffer()];
  }
  
  public Friend readFriend()
  {
    String username = readString();
    UUID uuid = readUUID();
    String status = readString();
    int onlineOrdinal = readVarIntFromBuffer();
    if ((onlineOrdinal < 0) || (onlineOrdinal >= Friend.OnlineStatus.values().length)) {
      throw new IllegalArgumentException("Received Integer is out of enum range.");
    }
    Friend.OnlineStatus online = Friend.OnlineStatus.values()[onlineOrdinal];
    long lastOnline = 0L;
    if (online == Friend.OnlineStatus.OFFLINE) {
      lastOnline = readLong();
    }
    Rank rank = readRank();
    long firstOnline = readLong();
    boolean favorite = readBoolean();
    String modVersion = readString();
    String locale = readString();
    
    Friend friend = new Friend(username, uuid);
    friend.setStatusMessage(status);
    friend.setStatus(online);
    if (online == Friend.OnlineStatus.OFFLINE) {
      friend.setLastOnline(lastOnline);
    }
    friend.setRank(rank);
    friend.setFirstOnline(firstOnline);
    friend.setModVersion(modVersion);
    friend.setLocale(locale.isEmpty() ? null : LocaleUtils.toLocale(locale));
    friend.setFavorite(favorite);
    return friend;
  }
  
  public Group readGroup()
  {
    int id = readVarIntFromBuffer();
    String name = readString();
    User owner = null;
    int size = readVarIntFromBuffer();
    List<GroupMember> members = new ArrayList(size);
    for (int i = 0; i < size; i++)
    {
      User member = readUser();
      int type = readVarIntFromBuffer();
      if (type == 2) {
        owner = member;
      }
      members.add(new GroupMember(member, type));
    }
    if (owner == null) {
      owner = new User("unknown", UUID.randomUUID());
    }
    return new Group(id, name, owner, members);
  }
  
  public void writeString(String string)
  {
    byte[] bytes = string.getBytes(Charsets.UTF_8);
    writeInt(bytes.length);
    writeBytes(bytes);
  }
  
  public String readString()
  {
    int length = readInt();
    return new String(readBytes(length).array(), Charsets.UTF_8);
  }
  
  public int capacity()
  {
    return this.buf.capacity();
  }
  
  public ByteBuf capacity(int i)
  {
    return this.buf.capacity(i);
  }
  
  public int maxCapacity()
  {
    return this.buf.maxCapacity();
  }
  
  public ByteBufAllocator alloc()
  {
    return this.buf.alloc();
  }
  
  public ByteOrder order()
  {
    return this.buf.order();
  }
  
  public ByteBuf order(ByteOrder byteOrder)
  {
    return this.buf.order(byteOrder);
  }
  
  public ByteBuf unwrap()
  {
    return this.buf.unwrap();
  }
  
  public boolean isDirect()
  {
    return this.buf.isDirect();
  }
  
  public int readerIndex()
  {
    return this.buf.readerIndex();
  }
  
  public ByteBuf readerIndex(int i)
  {
    return this.buf.readerIndex(i);
  }
  
  public int writerIndex()
  {
    return this.buf.writerIndex();
  }
  
  public ByteBuf writerIndex(int i)
  {
    return this.buf.writerIndex(i);
  }
  
  public ByteBuf setIndex(int i, int i2)
  {
    return this.buf.setInt(i, i2);
  }
  
  public int readableBytes()
  {
    return this.buf.readableBytes();
  }
  
  public int writableBytes()
  {
    return this.buf.writableBytes();
  }
  
  public int maxWritableBytes()
  {
    return this.buf.maxWritableBytes();
  }
  
  public boolean isReadable()
  {
    return this.buf.isReadable();
  }
  
  public boolean isReadable(int i)
  {
    return this.buf.isReadable(i);
  }
  
  public boolean isWritable()
  {
    return this.buf.isWritable();
  }
  
  public boolean isWritable(int i)
  {
    return this.buf.isWritable(i);
  }
  
  public ByteBuf clear()
  {
    return this.buf.clear();
  }
  
  public ByteBuf markReaderIndex()
  {
    return this.buf.markReaderIndex();
  }
  
  public ByteBuf resetReaderIndex()
  {
    return this.buf.resetReaderIndex();
  }
  
  public ByteBuf markWriterIndex()
  {
    return this.buf.markWriterIndex();
  }
  
  public ByteBuf resetWriterIndex()
  {
    return this.buf.resetWriterIndex();
  }
  
  public ByteBuf discardReadBytes()
  {
    return this.buf.discardReadBytes();
  }
  
  public ByteBuf discardSomeReadBytes()
  {
    return this.buf.discardSomeReadBytes();
  }
  
  public ByteBuf ensureWritable(int i)
  {
    return this.buf.ensureWritable(i);
  }
  
  public int ensureWritable(int i, boolean b)
  {
    return this.buf.ensureWritable(i, b);
  }
  
  public boolean getBoolean(int i)
  {
    return this.buf.getBoolean(i);
  }
  
  public byte getByte(int i)
  {
    return this.buf.getByte(i);
  }
  
  public short getUnsignedByte(int i)
  {
    return this.buf.getUnsignedByte(i);
  }
  
  public short getShort(int i)
  {
    return this.buf.getShort(i);
  }
  
  public int getUnsignedShort(int i)
  {
    return this.buf.getUnsignedShort(i);
  }
  
  public int getMedium(int i)
  {
    return this.buf.getMedium(i);
  }
  
  public int getUnsignedMedium(int i)
  {
    return this.buf.getUnsignedMedium(i);
  }
  
  public int getInt(int i)
  {
    return this.buf.getInt(i);
  }
  
  public long getUnsignedInt(int i)
  {
    return this.buf.getUnsignedInt(i);
  }
  
  public long getLong(int i)
  {
    return this.buf.getLong(i);
  }
  
  public char getChar(int i)
  {
    return this.buf.getChar(i);
  }
  
  public float getFloat(int i)
  {
    return this.buf.getFloat(i);
  }
  
  public double getDouble(int i)
  {
    return this.buf.getDouble(i);
  }
  
  public ByteBuf getBytes(int i, ByteBuf byteBuf)
  {
    return this.buf.getBytes(i, byteBuf);
  }
  
  public ByteBuf getBytes(int i, ByteBuf byteBuf, int i2)
  {
    return this.buf.getBytes(i, byteBuf, i2);
  }
  
  public ByteBuf getBytes(int i, ByteBuf byteBuf, int i2, int i3)
  {
    return this.buf.getBytes(i, byteBuf, i2, i3);
  }
  
  public ByteBuf getBytes(int i, byte[] bytes)
  {
    return this.buf.getBytes(i, bytes);
  }
  
  public ByteBuf getBytes(int i, byte[] bytes, int i2, int i3)
  {
    return this.buf.getBytes(i, bytes, i2, i3);
  }
  
  public ByteBuf getBytes(int i, ByteBuffer byteBuffer)
  {
    return this.buf.getBytes(i, byteBuffer);
  }
  
  public ByteBuf getBytes(int i, OutputStream outputStream, int i2)
    throws IOException
  {
    return this.buf.getBytes(i, outputStream, i2);
  }
  
  public int getBytes(int i, GatheringByteChannel gatheringByteChannel, int i2)
    throws IOException
  {
    return this.buf.getBytes(i, gatheringByteChannel, i2);
  }
  
  public ByteBuf setBoolean(int i, boolean b)
  {
    return this.buf.setBoolean(i, b);
  }
  
  public ByteBuf setByte(int i, int i2)
  {
    return this.buf.setByte(i, i2);
  }
  
  public ByteBuf setShort(int i, int i2)
  {
    return this.buf.setShort(i, i2);
  }
  
  public ByteBuf setMedium(int i, int i2)
  {
    return this.buf.setMedium(i, i2);
  }
  
  public ByteBuf setInt(int i, int i2)
  {
    return this.buf.setInt(i, i2);
  }
  
  public ByteBuf setLong(int i, long l)
  {
    return this.buf.setLong(i, l);
  }
  
  public ByteBuf setChar(int i, int i2)
  {
    return this.buf.setChar(i, i2);
  }
  
  public ByteBuf setFloat(int i, float v)
  {
    return this.buf.setFloat(i, v);
  }
  
  public ByteBuf setDouble(int i, double v)
  {
    return this.buf.setDouble(i, v);
  }
  
  public ByteBuf setBytes(int i, ByteBuf byteBuf)
  {
    return this.buf.setBytes(i, byteBuf);
  }
  
  public ByteBuf setBytes(int i, ByteBuf byteBuf, int i2)
  {
    return this.buf.setBytes(i, byteBuf, i2);
  }
  
  public ByteBuf setBytes(int i, ByteBuf byteBuf, int i2, int i3)
  {
    return this.buf.setBytes(i, byteBuf, i2, i3);
  }
  
  public ByteBuf setBytes(int i, byte[] bytes)
  {
    return this.buf.setBytes(i, bytes);
  }
  
  public ByteBuf setBytes(int i, byte[] bytes, int i2, int i3)
  {
    return this.buf.setBytes(i, bytes, i2, i3);
  }
  
  public ByteBuf setBytes(int i, ByteBuffer byteBuffer)
  {
    return this.buf.setBytes(i, byteBuffer);
  }
  
  public int setBytes(int i, InputStream inputStream, int i2)
    throws IOException
  {
    return this.buf.setBytes(i, inputStream, i2);
  }
  
  public int setBytes(int i, ScatteringByteChannel scatteringByteChannel, int i2)
    throws IOException
  {
    return this.buf.setBytes(i, scatteringByteChannel, i2);
  }
  
  public ByteBuf setZero(int i, int i2)
  {
    return this.buf.setZero(i, i2);
  }
  
  public boolean readBoolean()
  {
    return this.buf.readBoolean();
  }
  
  public byte readByte()
  {
    return this.buf.readByte();
  }
  
  public short readUnsignedByte()
  {
    return this.buf.readUnsignedByte();
  }
  
  public short readShort()
  {
    return this.buf.readShort();
  }
  
  public int readUnsignedShort()
  {
    return this.buf.readUnsignedShort();
  }
  
  public int readMedium()
  {
    return this.buf.readMedium();
  }
  
  public int readUnsignedMedium()
  {
    return this.buf.readUnsignedMedium();
  }
  
  public int readInt()
  {
    return this.buf.readInt();
  }
  
  public long readUnsignedInt()
  {
    return this.buf.readUnsignedInt();
  }
  
  public long readLong()
  {
    return this.buf.readLong();
  }
  
  public char readChar()
  {
    return this.buf.readChar();
  }
  
  public float readFloat()
  {
    return this.buf.readFloat();
  }
  
  public double readDouble()
  {
    return this.buf.readDouble();
  }
  
  public ByteBuf readBytes(int i)
  {
    return this.buf.readBytes(i);
  }
  
  public ByteBuf readSlice(int i)
  {
    return this.buf.readSlice(i);
  }
  
  public ByteBuf readBytes(ByteBuf byteBuf)
  {
    return this.buf.readBytes(byteBuf);
  }
  
  public ByteBuf readBytes(ByteBuf byteBuf, int i)
  {
    return this.buf.readBytes(byteBuf, i);
  }
  
  public ByteBuf readBytes(ByteBuf byteBuf, int i, int i2)
  {
    return this.buf.readBytes(byteBuf, i, i2);
  }
  
  public ByteBuf readBytes(byte[] bytes)
  {
    return this.buf.readBytes(bytes);
  }
  
  public ByteBuf readBytes(byte[] bytes, int i, int i2)
  {
    return this.buf.readBytes(bytes, i, i2);
  }
  
  public ByteBuf readBytes(ByteBuffer byteBuffer)
  {
    return this.buf.readBytes(byteBuffer);
  }
  
  public ByteBuf readBytes(OutputStream outputStream, int i)
    throws IOException
  {
    return this.buf.readBytes(outputStream, i);
  }
  
  public int readBytes(GatheringByteChannel gatheringByteChannel, int i)
    throws IOException
  {
    return this.buf.readBytes(gatheringByteChannel, i);
  }
  
  public ByteBuf skipBytes(int i)
  {
    return this.buf.skipBytes(i);
  }
  
  public ByteBuf writeBoolean(boolean b)
  {
    return this.buf.writeBoolean(b);
  }
  
  public ByteBuf writeByte(int i)
  {
    return this.buf.writeByte(i);
  }
  
  public ByteBuf writeShort(int i)
  {
    return this.buf.writeShort(i);
  }
  
  public ByteBuf writeMedium(int i)
  {
    return this.buf.writeMedium(i);
  }
  
  public ByteBuf writeInt(int i)
  {
    return this.buf.writeInt(i);
  }
  
  public ByteBuf writeLong(long l)
  {
    return this.buf.writeLong(l);
  }
  
  public ByteBuf writeChar(int i)
  {
    return this.buf.writeChar(i);
  }
  
  public ByteBuf writeFloat(float v)
  {
    return this.buf.writeFloat(v);
  }
  
  public ByteBuf writeDouble(double v)
  {
    return this.buf.writeDouble(v);
  }
  
  public ByteBuf writeBytes(ByteBuf byteBuf)
  {
    return this.buf.writeBytes(byteBuf);
  }
  
  public ByteBuf writeBytes(ByteBuf byteBuf, int i)
  {
    return this.buf.writeBytes(byteBuf, i);
  }
  
  public ByteBuf writeBytes(ByteBuf byteBuf, int i, int i2)
  {
    return this.buf.writeBytes(byteBuf, i, i2);
  }
  
  public ByteBuf writeBytes(byte[] bytes)
  {
    return this.buf.writeBytes(bytes);
  }
  
  public ByteBuf writeBytes(byte[] bytes, int i, int i2)
  {
    return this.buf.writeBytes(bytes, i, i2);
  }
  
  public ByteBuf writeBytes(ByteBuffer byteBuffer)
  {
    return this.buf.writeBytes(byteBuffer);
  }
  
  public int writeBytes(InputStream inputStream, int i)
    throws IOException
  {
    return this.buf.writeBytes(inputStream, i);
  }
  
  public int writeBytes(ScatteringByteChannel scatteringByteChannel, int i)
    throws IOException
  {
    return this.buf.writeBytes(scatteringByteChannel, i);
  }
  
  public ByteBuf writeZero(int i)
  {
    return this.buf.writeZero(i);
  }
  
  public int indexOf(int i, int i2, byte b)
  {
    return this.buf.indexOf(i, i2, b);
  }
  
  public int bytesBefore(byte b)
  {
    return this.buf.bytesBefore(b);
  }
  
  public int bytesBefore(int i, byte b)
  {
    return this.buf.bytesBefore(i, b);
  }
  
  public int bytesBefore(int i, int i2, byte b)
  {
    return this.buf.bytesBefore(i, i2, b);
  }
  
  public int forEachByte(ByteBufProcessor byteBufProcessor)
  {
    return this.buf.forEachByte(byteBufProcessor);
  }
  
  public int forEachByte(int i, int i2, ByteBufProcessor byteBufProcessor)
  {
    return this.buf.forEachByte(i, i2, byteBufProcessor);
  }
  
  public int forEachByteDesc(ByteBufProcessor byteBufProcessor)
  {
    return this.buf.forEachByteDesc(byteBufProcessor);
  }
  
  public int forEachByteDesc(int i, int i2, ByteBufProcessor byteBufProcessor)
  {
    return this.buf.forEachByteDesc(i, i2, byteBufProcessor);
  }
  
  public ByteBuf copy()
  {
    return this.buf.copy();
  }
  
  public ByteBuf copy(int i, int i2)
  {
    return this.buf.copy(i, i2);
  }
  
  public ByteBuf slice()
  {
    return this.buf.slice();
  }
  
  public ByteBuf slice(int i, int i2)
  {
    return this.buf.slice(i, i2);
  }
  
  public ByteBuf duplicate()
  {
    return this.buf.duplicate();
  }
  
  public int nioBufferCount()
  {
    return this.buf.nioBufferCount();
  }
  
  public ByteBuffer nioBuffer()
  {
    return this.buf.nioBuffer();
  }
  
  public ByteBuffer nioBuffer(int i, int i2)
  {
    return this.buf.nioBuffer(i, i2);
  }
  
  public ByteBuffer internalNioBuffer(int i, int i2)
  {
    return this.buf.internalNioBuffer(i, i2);
  }
  
  public ByteBuffer[] nioBuffers()
  {
    return this.buf.nioBuffers();
  }
  
  public ByteBuffer[] nioBuffers(int i, int i2)
  {
    return this.buf.nioBuffers(i, i2);
  }
  
  public boolean hasArray()
  {
    return this.buf.hasArray();
  }
  
  public byte[] array()
  {
    return this.buf.array();
  }
  
  public int arrayOffset()
  {
    return this.buf.arrayOffset();
  }
  
  public boolean hasMemoryAddress()
  {
    return this.buf.hasMemoryAddress();
  }
  
  public long memoryAddress()
  {
    return this.buf.memoryAddress();
  }
  
  public String toString(Charset charset)
  {
    return this.buf.toString(charset);
  }
  
  public String toString(int i, int i2, Charset charset)
  {
    return this.buf.toString(i, i2, charset);
  }
  
  public int hashCode()
  {
    return this.buf.hashCode();
  }
  
  public boolean equals(Object o)
  {
    return this.buf.equals(o);
  }
  
  public int compareTo(ByteBuf byteBuf)
  {
    return this.buf.compareTo(byteBuf);
  }
  
  public String toString()
  {
    return this.buf.toString();
  }
  
  public ByteBuf retain(int i)
  {
    return this.buf.retain(i);
  }
  
  public ByteBuf retain()
  {
    return this.buf.retain();
  }
  
  public int refCnt()
  {
    return this.buf.refCnt();
  }
  
  public boolean release()
  {
    return this.buf.release();
  }
  
  public boolean release(int i)
  {
    return this.buf.release(i);
  }
}
