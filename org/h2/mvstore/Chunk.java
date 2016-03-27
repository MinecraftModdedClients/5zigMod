package org.h2.mvstore;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class Chunk
{
  public static final int MAX_ID = 67108863;
  static final int MAX_HEADER_LENGTH = 1024;
  static final int FOOTER_LENGTH = 128;
  public final int id;
  public long block;
  public int len;
  public int pageCount;
  public int pageCountLive;
  public long maxLen;
  public long maxLenLive;
  public int collectPriority;
  public long metaRootPos;
  public long version;
  public long time;
  public long unused;
  public int mapId;
  public long next;
  
  Chunk(int paramInt)
  {
    this.id = paramInt;
  }
  
  static Chunk readChunkHeader(ByteBuffer paramByteBuffer, long paramLong)
  {
    int i = paramByteBuffer.position();
    byte[] arrayOfByte = new byte[Math.min(paramByteBuffer.remaining(), 1024)];
    paramByteBuffer.get(arrayOfByte);
    try
    {
      for (int j = 0; j < arrayOfByte.length; j++) {
        if (arrayOfByte[j] == 10)
        {
          paramByteBuffer.position(i + j + 1);
          String str = new String(arrayOfByte, 0, j, DataUtils.LATIN).trim();
          return fromString(str);
        }
      }
    }
    catch (Exception localException)
    {
      throw DataUtils.newIllegalStateException(6, "File corrupt reading chunk at position {0}", new Object[] { Long.valueOf(paramLong), localException });
    }
    throw DataUtils.newIllegalStateException(6, "File corrupt reading chunk at position {0}", new Object[] { Long.valueOf(paramLong) });
  }
  
  void writeChunkHeader(WriteBuffer paramWriteBuffer, int paramInt)
  {
    long l = paramWriteBuffer.position();
    paramWriteBuffer.put(asString().getBytes(DataUtils.LATIN));
    while (paramWriteBuffer.position() - l < paramInt - 1) {
      paramWriteBuffer.put((byte)32);
    }
    if ((paramInt != 0) && (paramWriteBuffer.position() > paramInt)) {
      throw DataUtils.newIllegalStateException(3, "Chunk metadata too long", new Object[0]);
    }
    paramWriteBuffer.put((byte)10);
  }
  
  static String getMetaKey(int paramInt)
  {
    return "chunk." + Integer.toHexString(paramInt);
  }
  
  public static Chunk fromString(String paramString)
  {
    HashMap localHashMap = DataUtils.parseMap(paramString);
    int i = DataUtils.readHexInt(localHashMap, "chunk", 0);
    Chunk localChunk = new Chunk(i);
    localChunk.block = DataUtils.readHexLong(localHashMap, "block", 0L);
    localChunk.len = DataUtils.readHexInt(localHashMap, "len", 0);
    localChunk.pageCount = DataUtils.readHexInt(localHashMap, "pages", 0);
    localChunk.pageCountLive = DataUtils.readHexInt(localHashMap, "livePages", localChunk.pageCount);
    localChunk.mapId = DataUtils.readHexInt(localHashMap, "map", 0);
    localChunk.maxLen = DataUtils.readHexLong(localHashMap, "max", 0L);
    localChunk.maxLenLive = DataUtils.readHexLong(localHashMap, "liveMax", localChunk.maxLen);
    localChunk.metaRootPos = DataUtils.readHexLong(localHashMap, "root", 0L);
    localChunk.time = DataUtils.readHexLong(localHashMap, "time", 0L);
    localChunk.unused = DataUtils.readHexLong(localHashMap, "unused", 0L);
    localChunk.version = DataUtils.readHexLong(localHashMap, "version", i);
    localChunk.next = DataUtils.readHexLong(localHashMap, "next", 0L);
    return localChunk;
  }
  
  public int getFillRate()
  {
    if (this.maxLenLive <= 0L) {
      return 0;
    }
    if (this.maxLenLive == this.maxLen) {
      return 100;
    }
    return 1 + (int)(98L * this.maxLenLive / this.maxLen);
  }
  
  public int hashCode()
  {
    return this.id;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof Chunk)) && (((Chunk)paramObject).id == this.id);
  }
  
  public String asString()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    DataUtils.appendMap(localStringBuilder, "chunk", Integer.valueOf(this.id));
    DataUtils.appendMap(localStringBuilder, "block", Long.valueOf(this.block));
    DataUtils.appendMap(localStringBuilder, "len", Integer.valueOf(this.len));
    if (this.maxLen != this.maxLenLive) {
      DataUtils.appendMap(localStringBuilder, "liveMax", Long.valueOf(this.maxLenLive));
    }
    if (this.pageCount != this.pageCountLive) {
      DataUtils.appendMap(localStringBuilder, "livePages", Integer.valueOf(this.pageCountLive));
    }
    DataUtils.appendMap(localStringBuilder, "map", Integer.valueOf(this.mapId));
    DataUtils.appendMap(localStringBuilder, "max", Long.valueOf(this.maxLen));
    if (this.next != 0L) {
      DataUtils.appendMap(localStringBuilder, "next", Long.valueOf(this.next));
    }
    DataUtils.appendMap(localStringBuilder, "pages", Integer.valueOf(this.pageCount));
    DataUtils.appendMap(localStringBuilder, "root", Long.valueOf(this.metaRootPos));
    DataUtils.appendMap(localStringBuilder, "time", Long.valueOf(this.time));
    if (this.unused != 0L) {
      DataUtils.appendMap(localStringBuilder, "unused", Long.valueOf(this.unused));
    }
    DataUtils.appendMap(localStringBuilder, "version", Long.valueOf(this.version));
    return localStringBuilder.toString();
  }
  
  byte[] getFooterBytes()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    DataUtils.appendMap(localStringBuilder, "chunk", Integer.valueOf(this.id));
    DataUtils.appendMap(localStringBuilder, "block", Long.valueOf(this.block));
    DataUtils.appendMap(localStringBuilder, "version", Long.valueOf(this.version));
    byte[] arrayOfByte = localStringBuilder.toString().getBytes(DataUtils.LATIN);
    int i = DataUtils.getFletcher32(arrayOfByte, arrayOfByte.length);
    DataUtils.appendMap(localStringBuilder, "fletcher", Integer.valueOf(i));
    while (localStringBuilder.length() < 127) {
      localStringBuilder.append(' ');
    }
    localStringBuilder.append("\n");
    return localStringBuilder.toString().getBytes(DataUtils.LATIN);
  }
  
  public String toString()
  {
    return asString();
  }
}
