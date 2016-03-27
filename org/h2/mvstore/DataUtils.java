package org.h2.mvstore;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.h2.util.New;

public class DataUtils
{
  public static final int ERROR_READING_FAILED = 1;
  public static final int ERROR_WRITING_FAILED = 2;
  public static final int ERROR_INTERNAL = 3;
  public static final int ERROR_CLOSED = 4;
  public static final int ERROR_UNSUPPORTED_FORMAT = 5;
  public static final int ERROR_FILE_CORRUPT = 6;
  public static final int ERROR_FILE_LOCKED = 7;
  public static final int ERROR_SERIALIZATION = 8;
  public static final int ERROR_CHUNK_NOT_FOUND = 9;
  public static final int ERROR_TRANSACTION_CORRUPT = 100;
  public static final int ERROR_TRANSACTION_LOCKED = 101;
  public static final int ERROR_TRANSACTION_STILL_OPEN = 102;
  public static final int ERROR_TRANSACTION_ILLEGAL_STATE = 103;
  public static final int PAGE_TYPE_LEAF = 0;
  public static final int PAGE_TYPE_NODE = 1;
  public static final int PAGE_COMPRESSED = 2;
  public static final int PAGE_COMPRESSED_HIGH = 6;
  public static final int MAX_VAR_INT_LEN = 5;
  public static final int MAX_VAR_LONG_LEN = 10;
  public static final int COMPRESSED_VAR_INT_MAX = 2097151;
  public static final long COMPRESSED_VAR_LONG_MAX = 562949953421311L;
  public static final int PAGE_MEMORY = 128;
  public static final int PAGE_MEMORY_CHILD = 16;
  public static final int PAGE_LARGE = 2097152;
  public static final Charset UTF8 = Charset.forName("UTF-8");
  public static final Charset LATIN = Charset.forName("ISO-8859-1");
  private static final byte[] EMPTY_BYTES = new byte[0];
  private static final int MAX_GROW = 16777216;
  
  public static int getVarIntLen(int paramInt)
  {
    if ((paramInt & 0xFFFFFF80) == 0) {
      return 1;
    }
    if ((paramInt & 0xC000) == 0) {
      return 2;
    }
    if ((paramInt & 0xFFE00000) == 0) {
      return 3;
    }
    if ((paramInt & 0xF0000000) == 0) {
      return 4;
    }
    return 5;
  }
  
  public static int getVarLongLen(long paramLong)
  {
    int i = 1;
    for (;;)
    {
      paramLong >>>= 7;
      if (paramLong == 0L) {
        return i;
      }
      i++;
    }
  }
  
  public static int readVarInt(ByteBuffer paramByteBuffer)
  {
    int i = paramByteBuffer.get();
    if (i >= 0) {
      return i;
    }
    return readVarIntRest(paramByteBuffer, i);
  }
  
  private static int readVarIntRest(ByteBuffer paramByteBuffer, int paramInt)
  {
    int i = paramInt & 0x7F;
    paramInt = paramByteBuffer.get();
    if (paramInt >= 0) {
      return i | paramInt << 7;
    }
    i |= (paramInt & 0x7F) << 7;
    paramInt = paramByteBuffer.get();
    if (paramInt >= 0) {
      return i | paramInt << 14;
    }
    i |= (paramInt & 0x7F) << 14;
    paramInt = paramByteBuffer.get();
    if (paramInt >= 0) {
      return i | paramInt << 21;
    }
    i |= (paramInt & 0x7F) << 21 | paramByteBuffer.get() << 28;
    return i;
  }
  
  public static long readVarLong(ByteBuffer paramByteBuffer)
  {
    long l1 = paramByteBuffer.get();
    if (l1 >= 0L) {
      return l1;
    }
    l1 &= 0x7F;
    for (int i = 7; i < 64; i += 7)
    {
      long l2 = paramByteBuffer.get();
      l1 |= (l2 & 0x7F) << i;
      if (l2 >= 0L) {
        break;
      }
    }
    return l1;
  }
  
  public static void writeVarInt(OutputStream paramOutputStream, int paramInt)
    throws IOException
  {
    while ((paramInt & 0xFFFFFF80) != 0)
    {
      paramOutputStream.write((byte)(0x80 | paramInt & 0x7F));
      paramInt >>>= 7;
    }
    paramOutputStream.write((byte)paramInt);
  }
  
  public static void writeVarInt(ByteBuffer paramByteBuffer, int paramInt)
  {
    while ((paramInt & 0xFFFFFF80) != 0)
    {
      paramByteBuffer.put((byte)(0x80 | paramInt & 0x7F));
      paramInt >>>= 7;
    }
    paramByteBuffer.put((byte)paramInt);
  }
  
  public static ByteBuffer writeStringData(ByteBuffer paramByteBuffer, String paramString, int paramInt)
  {
    paramByteBuffer = ensureCapacity(paramByteBuffer, 3 * paramInt);
    for (int i = 0; i < paramInt; i++)
    {
      int j = paramString.charAt(i);
      if (j < 128)
      {
        paramByteBuffer.put((byte)j);
      }
      else if (j >= 2048)
      {
        paramByteBuffer.put((byte)(0xE0 | j >> 12));
        paramByteBuffer.put((byte)(j >> 6 & 0x3F));
        paramByteBuffer.put((byte)(j & 0x3F));
      }
      else
      {
        paramByteBuffer.put((byte)(0xC0 | j >> 6));
        paramByteBuffer.put((byte)(j & 0x3F));
      }
    }
    return paramByteBuffer;
  }
  
  public static String readString(ByteBuffer paramByteBuffer, int paramInt)
  {
    char[] arrayOfChar = new char[paramInt];
    for (int i = 0; i < paramInt; i++)
    {
      int j = paramByteBuffer.get() & 0xFF;
      if (j < 128) {
        arrayOfChar[i] = ((char)j);
      } else if (j >= 224) {
        arrayOfChar[i] = ((char)(((j & 0xF) << 12) + ((paramByteBuffer.get() & 0x3F) << 6) + (paramByteBuffer.get() & 0x3F)));
      } else {
        arrayOfChar[i] = ((char)(((j & 0x1F) << 6) + (paramByteBuffer.get() & 0x3F)));
      }
    }
    return new String(arrayOfChar);
  }
  
  public static void writeVarLong(ByteBuffer paramByteBuffer, long paramLong)
  {
    while ((paramLong & 0xFFFFFFFFFFFFFF80) != 0L)
    {
      paramByteBuffer.put((byte)(int)(0x80 | paramLong & 0x7F));
      paramLong >>>= 7;
    }
    paramByteBuffer.put((byte)(int)paramLong);
  }
  
  public static void writeVarLong(OutputStream paramOutputStream, long paramLong)
    throws IOException
  {
    while ((paramLong & 0xFFFFFFFFFFFFFF80) != 0L)
    {
      paramOutputStream.write((byte)(int)(0x80 | paramLong & 0x7F));
      paramLong >>>= 7;
    }
    paramOutputStream.write((byte)(int)paramLong);
  }
  
  public static void copyWithGap(Object paramObject1, Object paramObject2, int paramInt1, int paramInt2)
  {
    if (paramInt2 > 0) {
      System.arraycopy(paramObject1, 0, paramObject2, 0, paramInt2);
    }
    if (paramInt2 < paramInt1) {
      System.arraycopy(paramObject1, paramInt2, paramObject2, paramInt2 + 1, paramInt1 - paramInt2);
    }
  }
  
  public static void copyExcept(Object paramObject1, Object paramObject2, int paramInt1, int paramInt2)
  {
    if ((paramInt2 > 0) && (paramInt1 > 0)) {
      System.arraycopy(paramObject1, 0, paramObject2, 0, paramInt2);
    }
    if (paramInt2 < paramInt1) {
      System.arraycopy(paramObject1, paramInt2 + 1, paramObject2, paramInt2, paramInt1 - paramInt2 - 1);
    }
  }
  
  public static void readFully(FileChannel paramFileChannel, long paramLong, ByteBuffer paramByteBuffer)
  {
    try
    {
      do
      {
        int i = paramFileChannel.read(paramByteBuffer, paramLong);
        if (i < 0) {
          throw new EOFException();
        }
        paramLong += i;
      } while (paramByteBuffer.remaining() > 0);
      paramByteBuffer.rewind();
    }
    catch (IOException localIOException1)
    {
      long l;
      try
      {
        l = paramFileChannel.size();
      }
      catch (IOException localIOException2)
      {
        l = -1L;
      }
      throw newIllegalStateException(1, "Reading from {0} failed; file length {1} read length {2} at {3}", new Object[] { paramFileChannel, Long.valueOf(l), Integer.valueOf(paramByteBuffer.remaining()), Long.valueOf(paramLong), localIOException1 });
    }
  }
  
  public static void writeFully(FileChannel paramFileChannel, long paramLong, ByteBuffer paramByteBuffer)
  {
    try
    {
      int i = 0;
      do
      {
        int j = paramFileChannel.write(paramByteBuffer, paramLong + i);
        i += j;
      } while (paramByteBuffer.remaining() > 0);
    }
    catch (IOException localIOException)
    {
      throw newIllegalStateException(2, "Writing to {0} failed; length {1} at {2}", new Object[] { paramFileChannel, Integer.valueOf(paramByteBuffer.remaining()), Long.valueOf(paramLong), localIOException });
    }
  }
  
  public static int encodeLength(int paramInt)
  {
    if (paramInt <= 32) {
      return 0;
    }
    int i = Integer.numberOfLeadingZeros(paramInt);
    int j = paramInt << i + 1;
    i += i;
    if ((j & 0x80000000) != 0) {
      i--;
    }
    if (j << 1 != 0) {
      i--;
    }
    i = Math.min(31, 52 - i);
    
    return i;
  }
  
  public static int getPageChunkId(long paramLong)
  {
    return (int)(paramLong >>> 38);
  }
  
  public static int getPageMaxLength(long paramLong)
  {
    int i = (int)(paramLong >> 1 & 0x1F);
    if (i == 31) {
      return 2097152;
    }
    return 2 + (i & 0x1) << (i >> 1) + 4;
  }
  
  public static int getPageOffset(long paramLong)
  {
    return (int)(paramLong >> 6);
  }
  
  public static int getPageType(long paramLong)
  {
    return (int)paramLong & 0x1;
  }
  
  public static long getPagePos(int paramInt1, int paramInt2, int paramInt3, int paramInt4)
  {
    long l = paramInt1 << 38;
    l |= paramInt2 << 6;
    l |= encodeLength(paramInt3) << 1;
    l |= paramInt4;
    return l;
  }
  
  public static short getCheckValue(int paramInt)
  {
    return (short)(paramInt >> 16 ^ paramInt);
  }
  
  public static StringBuilder appendMap(StringBuilder paramStringBuilder, HashMap<String, ?> paramHashMap)
  {
    ArrayList localArrayList = New.arrayList(paramHashMap.keySet());
    Collections.sort(localArrayList);
    for (String str : localArrayList) {
      appendMap(paramStringBuilder, str, paramHashMap.get(str));
    }
    return paramStringBuilder;
  }
  
  public static void appendMap(StringBuilder paramStringBuilder, String paramString, Object paramObject)
  {
    if (paramStringBuilder.length() > 0) {
      paramStringBuilder.append(',');
    }
    paramStringBuilder.append(paramString).append(':');
    String str;
    if ((paramObject instanceof Long)) {
      str = Long.toHexString(((Long)paramObject).longValue());
    } else if ((paramObject instanceof Integer)) {
      str = Integer.toHexString(((Integer)paramObject).intValue());
    } else {
      str = paramObject.toString();
    }
    if ((str.indexOf(',') < 0) && (str.indexOf('"') < 0))
    {
      paramStringBuilder.append(str);
    }
    else
    {
      paramStringBuilder.append('"');
      int i = 0;
      for (int j = str.length(); i < j; i++)
      {
        char c = str.charAt(i);
        if (c == '"') {
          paramStringBuilder.append('\\');
        }
        paramStringBuilder.append(c);
      }
      paramStringBuilder.append('"');
    }
  }
  
  public static HashMap<String, String> parseMap(String paramString)
  {
    HashMap localHashMap = New.hashMap();
    int i = 0;
    for (int j = paramString.length(); i < j;)
    {
      int k = i;
      i = paramString.indexOf(':', i);
      if (i < 0) {
        throw newIllegalStateException(6, "Not a map: {0}", new Object[] { paramString });
      }
      String str = paramString.substring(k, i++);
      StringBuilder localStringBuilder = new StringBuilder();
      while (i < j)
      {
        char c = paramString.charAt(i++);
        if (c == ',') {
          break;
        }
        if (c == '"') {
          while (i < j)
          {
            c = paramString.charAt(i++);
            if (c == '\\')
            {
              if (i == j) {
                throw newIllegalStateException(6, "Not a map: {0}", new Object[] { paramString });
              }
              c = paramString.charAt(i++);
            }
            else
            {
              if (c == '"') {
                break;
              }
            }
            localStringBuilder.append(c);
          }
        }
        localStringBuilder.append(c);
      }
      localHashMap.put(str, localStringBuilder.toString());
    }
    return localHashMap;
  }
  
  public static int getFletcher32(byte[] paramArrayOfByte, int paramInt)
  {
    int i = 65535;int j = 65535;
    int k = 0;int m = paramInt / 2 * 2;
    int n;
    while (k < m)
    {
      for (n = Math.min(k + 720, m); k < n;)
      {
        int i1 = (paramArrayOfByte[(k++)] & 0xFF) << 8 | paramArrayOfByte[(k++)] & 0xFF;
        j += i += i1;
      }
      i = (i & 0xFFFF) + (i >>> 16);
      j = (j & 0xFFFF) + (j >>> 16);
    }
    if (k < paramInt)
    {
      n = (paramArrayOfByte[k] & 0xFF) << 8;
      j += i += n;
    }
    i = (i & 0xFFFF) + (i >>> 16);
    j = (j & 0xFFFF) + (j >>> 16);
    return j << 16 | i;
  }
  
  public static void checkArgument(boolean paramBoolean, String paramString, Object... paramVarArgs)
  {
    if (!paramBoolean) {
      throw newIllegalArgumentException(paramString, paramVarArgs);
    }
  }
  
  public static IllegalArgumentException newIllegalArgumentException(String paramString, Object... paramVarArgs)
  {
    return (IllegalArgumentException)initCause(new IllegalArgumentException(formatMessage(0, paramString, paramVarArgs)), paramVarArgs);
  }
  
  public static UnsupportedOperationException newUnsupportedOperationException(String paramString)
  {
    return new UnsupportedOperationException(formatMessage(0, paramString, new Object[0]));
  }
  
  public static ConcurrentModificationException newConcurrentModificationException(String paramString)
  {
    return new ConcurrentModificationException(formatMessage(0, paramString, new Object[0]));
  }
  
  public static IllegalStateException newIllegalStateException(int paramInt, String paramString, Object... paramVarArgs)
  {
    return (IllegalStateException)initCause(new IllegalStateException(formatMessage(paramInt, paramString, paramVarArgs)), paramVarArgs);
  }
  
  private static <T extends Exception> T initCause(T paramT, Object... paramVarArgs)
  {
    int i = paramVarArgs.length;
    if (i > 0)
    {
      Object localObject = paramVarArgs[(i - 1)];
      if ((localObject instanceof Exception)) {
        paramT.initCause((Exception)localObject);
      }
    }
    return paramT;
  }
  
  private static String formatMessage(int paramInt, String paramString, Object... paramVarArgs)
  {
    for (int i = 0; i < paramVarArgs.length; i++)
    {
      Object localObject = paramVarArgs[i];
      if (!(localObject instanceof Exception))
      {
        String str = localObject == null ? "null" : localObject.toString();
        if (str.length() > 1000) {
          str = str.substring(0, 1000) + "...";
        }
        paramVarArgs[i] = str;
      }
    }
    return MessageFormat.format(paramString, paramVarArgs) + " [" + 1 + "." + 4 + "." + 183 + "/" + paramInt + "]";
  }
  
  public static int getErrorCode(String paramString)
  {
    if ((paramString != null) && (paramString.endsWith("]")))
    {
      int i = paramString.lastIndexOf('/');
      if (i >= 0)
      {
        String str = paramString.substring(i + 1, paramString.length() - 1);
        try
        {
          return Integer.parseInt(str);
        }
        catch (NumberFormatException localNumberFormatException) {}
      }
    }
    return 0;
  }
  
  public static byte[] newBytes(int paramInt)
  {
    if (paramInt == 0) {
      return EMPTY_BYTES;
    }
    try
    {
      return new byte[paramInt];
    }
    catch (OutOfMemoryError localOutOfMemoryError1)
    {
      OutOfMemoryError localOutOfMemoryError2 = new OutOfMemoryError("Requested memory: " + paramInt);
      localOutOfMemoryError2.initCause(localOutOfMemoryError1);
      throw localOutOfMemoryError2;
    }
  }
  
  public static ByteBuffer ensureCapacity(ByteBuffer paramByteBuffer, int paramInt)
  {
    
    if (paramByteBuffer.remaining() > paramInt) {
      return paramByteBuffer;
    }
    return grow(paramByteBuffer, paramInt);
  }
  
  private static ByteBuffer grow(ByteBuffer paramByteBuffer, int paramInt)
  {
    paramInt = paramByteBuffer.remaining() + paramInt;
    int i = paramByteBuffer.capacity();
    paramInt = Math.max(paramInt, Math.min(i + 16777216, i * 2));
    ByteBuffer localByteBuffer = ByteBuffer.allocate(paramInt);
    paramByteBuffer.flip();
    localByteBuffer.put(paramByteBuffer);
    return localByteBuffer;
  }
  
  public static long readHexLong(Map<String, ? extends Object> paramMap, String paramString, long paramLong)
  {
    Object localObject = paramMap.get(paramString);
    if (localObject == null) {
      return paramLong;
    }
    if ((localObject instanceof Long)) {
      return ((Long)localObject).longValue();
    }
    try
    {
      return parseHexLong((String)localObject);
    }
    catch (NumberFormatException localNumberFormatException)
    {
      throw newIllegalStateException(6, "Error parsing the value {0}", new Object[] { localObject, localNumberFormatException });
    }
  }
  
  public static long parseHexLong(String paramString)
  {
    try
    {
      if (paramString.length() == 16) {
        return Long.parseLong(paramString.substring(0, 8), 16) << 32 | Long.parseLong(paramString.substring(8, 16), 16);
      }
      return Long.parseLong(paramString, 16);
    }
    catch (NumberFormatException localNumberFormatException)
    {
      throw newIllegalStateException(6, "Error parsing the value {0}", new Object[] { paramString, localNumberFormatException });
    }
  }
  
  public static int parseHexInt(String paramString)
  {
    try
    {
      return (int)Long.parseLong(paramString, 16);
    }
    catch (NumberFormatException localNumberFormatException)
    {
      throw newIllegalStateException(6, "Error parsing the value {0}", new Object[] { paramString, localNumberFormatException });
    }
  }
  
  public static int readHexInt(HashMap<String, ? extends Object> paramHashMap, String paramString, int paramInt)
  {
    Object localObject = paramHashMap.get(paramString);
    if (localObject == null) {
      return paramInt;
    }
    if ((localObject instanceof Integer)) {
      return ((Integer)localObject).intValue();
    }
    try
    {
      return (int)Long.parseLong((String)localObject, 16);
    }
    catch (NumberFormatException localNumberFormatException)
    {
      throw newIllegalStateException(6, "Error parsing the value {0}", new Object[] { localObject, localNumberFormatException });
    }
  }
  
  public static class MapEntry<K, V>
    implements Map.Entry<K, V>
  {
    private final K key;
    private V value;
    
    public MapEntry(K paramK, V paramV)
    {
      this.key = paramK;
      this.value = paramV;
    }
    
    public K getKey()
    {
      return (K)this.key;
    }
    
    public V getValue()
    {
      return (V)this.value;
    }
    
    public V setValue(V paramV)
    {
      throw DataUtils.newUnsupportedOperationException("Updating the value is not supported");
    }
  }
}
