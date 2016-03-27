package org.h2.mvstore;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.h2.message.DbException;
import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.StringDataType;
import org.h2.store.fs.FilePath;
import org.h2.store.fs.FileUtils;

public class MVStoreTool
{
  public static void main(String... paramVarArgs)
  {
    for (int i = 0; i < paramVarArgs.length; i++)
    {
      String str;
      if ("-dump".equals(paramVarArgs[i]))
      {
        str = paramVarArgs[(++i)];
        dump(str, new PrintWriter(System.out), true);
      }
      else if ("-info".equals(paramVarArgs[i]))
      {
        str = paramVarArgs[(++i)];
        info(str, new PrintWriter(System.out));
      }
      else if ("-compact".equals(paramVarArgs[i]))
      {
        str = paramVarArgs[(++i)];
        compact(str, false);
      }
      else if ("-compress".equals(paramVarArgs[i]))
      {
        str = paramVarArgs[(++i)];
        compact(str, true);
      }
    }
  }
  
  public static void dump(String paramString, boolean paramBoolean)
  {
    dump(paramString, new PrintWriter(System.out), paramBoolean);
  }
  
  public static void info(String paramString)
  {
    info(paramString, new PrintWriter(System.out));
  }
  
  public static void dump(String paramString, Writer paramWriter, boolean paramBoolean)
  {
    PrintWriter localPrintWriter = new PrintWriter(paramWriter, true);
    if (!FilePath.get(paramString).exists())
    {
      localPrintWriter.println("File not found: " + paramString);
      return;
    }
    long l1 = FileUtils.size(paramString);
    localPrintWriter.printf("File %s, %d bytes, %d MB\n", new Object[] { paramString, Long.valueOf(l1), Long.valueOf(l1 / 1024L / 1024L) });
    FileChannel localFileChannel = null;
    int i = 4096;
    TreeMap localTreeMap1 = new TreeMap();
    
    long l2 = 0L;
    try
    {
      localFileChannel = FilePath.get(paramString).open("r");
      long l3 = localFileChannel.size();
      int j = Long.toHexString(l3).length();
      ByteBuffer localByteBuffer1 = ByteBuffer.allocate(4096);
      long l4 = 0L;
      for (long l5 = 0L; l5 < l3;)
      {
        localByteBuffer1.rewind();
        DataUtils.readFully(localFileChannel, l5, localByteBuffer1);
        localByteBuffer1.rewind();
        k = localByteBuffer1.get();
        if (k == 72)
        {
          localPrintWriter.printf("%0" + j + "x fileHeader %s%n", new Object[] { Long.valueOf(l5), new String(localByteBuffer1.array(), DataUtils.LATIN).trim() });
          
          l5 += i;
        }
        else if (k != 99)
        {
          l5 += i;
        }
        else
        {
          localByteBuffer1.position(0);
          Chunk localChunk = null;
          try
          {
            localChunk = Chunk.readChunkHeader(localByteBuffer1, l5);
          }
          catch (IllegalStateException localIllegalStateException)
          {
            l5 += i;
          }
          continue;
          
          int m = localChunk.len * 4096;
          localPrintWriter.printf("%n%0" + j + "x chunkHeader %s%n", new Object[] { Long.valueOf(l5), localChunk.toString() });
          
          ByteBuffer localByteBuffer2 = ByteBuffer.allocate(m);
          DataUtils.readFully(localFileChannel, l5, localByteBuffer2);
          int n = localByteBuffer1.position();
          l5 += m;
          int i1 = localChunk.pageCount;
          l4 += localChunk.pageCount;
          TreeMap localTreeMap2 = new TreeMap();
          
          int i2 = 0;
          int i6;
          while (i1 > 0)
          {
            try
            {
              localByteBuffer2.position(n);
            }
            catch (IllegalArgumentException localIllegalArgumentException1)
            {
              localPrintWriter.printf("ERROR illegal position %d%n", new Object[] { Integer.valueOf(n) });
              break;
            }
            int i3 = localByteBuffer2.getInt();
            
            localByteBuffer2.getShort();
            int i5 = DataUtils.readVarInt(localByteBuffer2);
            i6 = DataUtils.readVarInt(localByteBuffer2);
            int i7 = localByteBuffer2.get();
            int i8 = (i7 & 0x2) != 0 ? 1 : 0;
            int i9 = (i7 & 0x1) != 0 ? 1 : 0;
            if (paramBoolean) {
              localPrintWriter.printf("+%0" + j + "x %s, map %x, %d entries, %d bytes, maxLen %x%n", new Object[] { Integer.valueOf(n), (i9 != 0 ? "node" : "leaf") + (i8 != 0 ? " compressed" : ""), Integer.valueOf(i5), Integer.valueOf(i9 != 0 ? i6 + 1 : i6), Integer.valueOf(i3), Integer.valueOf(DataUtils.getPageMaxLength(DataUtils.getPagePos(0, 0, i3, 0))) });
            }
            n += i3;
            Integer localInteger3 = (Integer)localTreeMap2.get(Integer.valueOf(i5));
            if (localInteger3 == null) {
              localInteger3 = Integer.valueOf(0);
            }
            localTreeMap2.put(Integer.valueOf(i5), Integer.valueOf(localInteger3.intValue() + i3));
            Long localLong = (Long)localTreeMap1.get(Integer.valueOf(i5));
            if (localLong == null) {
              localLong = Long.valueOf(0L);
            }
            localTreeMap1.put(Integer.valueOf(i5), Long.valueOf(localLong.longValue() + i3));
            i2 += i3;
            l2 += i3;
            i1--;
            long[] arrayOfLong1 = null;
            long[] arrayOfLong2 = null;
            if (i9 != 0)
            {
              arrayOfLong1 = new long[i6 + 1];
              for (int i10 = 0; i10 <= i6; i10++) {
                arrayOfLong1[i10] = localByteBuffer2.getLong();
              }
              arrayOfLong2 = new long[i6 + 1];
              for (i10 = 0; i10 <= i6; i10++)
              {
                long l6 = DataUtils.readVarLong(localByteBuffer2);
                arrayOfLong2[i10] = l6;
              }
            }
            String[] arrayOfString1 = new String[i6];
            if ((i5 == 0) && (paramBoolean))
            {
              int i11;
              if (i8 == 0) {
                for (i11 = 0; i11 < i6; i11++)
                {
                  String str1 = StringDataType.INSTANCE.read(localByteBuffer2);
                  arrayOfString1[i11] = str1;
                }
              }
              if (i9 != 0)
              {
                for (i11 = 0; i11 < i6; i11++)
                {
                  long l8 = arrayOfLong1[i11];
                  localPrintWriter.printf("    %d children < %s @ chunk %x +%0" + j + "x%n", new Object[] { Long.valueOf(arrayOfLong2[i11]), arrayOfString1[i11], Integer.valueOf(DataUtils.getPageChunkId(l8)), Integer.valueOf(DataUtils.getPageOffset(l8)) });
                }
                long l7 = arrayOfLong1[i6];
                localPrintWriter.printf("    %d children >= %s @ chunk %x +%0" + j + "x%n", new Object[] { Long.valueOf(arrayOfLong2[i6]), arrayOfString1.length >= i6 ? null : arrayOfString1[i6], Integer.valueOf(DataUtils.getPageChunkId(l7)), Integer.valueOf(DataUtils.getPageOffset(l7)) });
              }
              else if (i8 == 0)
              {
                String[] arrayOfString2 = new String[i6];
                for (int i13 = 0; i13 < i6; i13++)
                {
                  String str2 = StringDataType.INSTANCE.read(localByteBuffer2);
                  arrayOfString2[i13] = str2;
                }
                for (i13 = 0; i13 < i6; i13++) {
                  localPrintWriter.println("    " + arrayOfString1[i13] + " = " + arrayOfString2[i13]);
                }
              }
            }
            else if ((i9 != 0) && (paramBoolean))
            {
              for (int i12 = 0; i12 <= i6; i12++)
              {
                long l9 = arrayOfLong1[i12];
                localPrintWriter.printf("    %d children @ chunk %x +%0" + j + "x%n", new Object[] { Long.valueOf(arrayOfLong2[i12]), Integer.valueOf(DataUtils.getPageChunkId(l9)), Integer.valueOf(DataUtils.getPageOffset(l9)) });
              }
            }
          }
          for (Integer localInteger2 : localTreeMap2.keySet())
          {
            i6 = 100 * ((Integer)localTreeMap2.get(localInteger2)).intValue() / i2;
            localPrintWriter.printf("map %x: %d bytes, %d%%%n", new Object[] { localInteger2, localTreeMap2.get(localInteger2), Integer.valueOf(i6) });
          }
          int i4 = localByteBuffer2.limit() - 128;
          try
          {
            localByteBuffer2.position(i4);
            localPrintWriter.printf("+%0" + j + "x chunkFooter %s%n", new Object[] { Integer.valueOf(i4), new String(localByteBuffer2.array(), localByteBuffer2.position(), 128, DataUtils.LATIN).trim() });
          }
          catch (IllegalArgumentException localIllegalArgumentException2)
          {
            localPrintWriter.printf("ERROR illegal footer position %d%n", new Object[] { Integer.valueOf(i4) });
          }
        }
      }
      int k;
      localPrintWriter.printf("%n%0" + j + "x eof%n", new Object[] { Long.valueOf(l3) });
      localPrintWriter.printf("\n", new Object[0]);
      localPrintWriter.printf("page size total: %d bytes, page count: %d, average page size: %d bytes\n", new Object[] { Long.valueOf(l2), Long.valueOf(l4), Long.valueOf(l2 / l4) });
      for (Integer localInteger1 : localTreeMap1.keySet())
      {
        k = (int)(100L * ((Long)localTreeMap1.get(localInteger1)).longValue() / l2);
        localPrintWriter.printf("map %x: %d bytes, %d%%%n", new Object[] { localInteger1, localTreeMap1.get(localInteger1), Integer.valueOf(k) });
      }
      if (localFileChannel != null) {
        try
        {
          localFileChannel.close();
        }
        catch (IOException localIOException1) {}
      }
      localPrintWriter.flush();
    }
    catch (IOException localIOException2)
    {
      localPrintWriter.println("ERROR: " + localIOException2);
      localIOException2.printStackTrace(localPrintWriter);
    }
    finally
    {
      if (localFileChannel != null) {
        try
        {
          localFileChannel.close();
        }
        catch (IOException localIOException4) {}
      }
    }
  }
  
  public static void info(String paramString, Writer paramWriter)
  {
    PrintWriter localPrintWriter = new PrintWriter(paramWriter, true);
    if (!FilePath.get(paramString).exists())
    {
      localPrintWriter.println("File not found: " + paramString);
      return;
    }
    long l1 = FileUtils.size(paramString);
    MVStore localMVStore = new MVStore.Builder().fileName(paramString).readOnly().open();
    try
    {
      MVMap localMVMap = localMVStore.getMetaMap();
      Map localMap = localMVStore.getStoreHeader();
      long l2 = DataUtils.readHexLong(localMap, "created", 0L);
      TreeMap localTreeMap = new TreeMap();
      long l3 = 0L;
      long l4 = 0L;
      long l5 = 0L;
      long l6 = 0L;
      for (Iterator localIterator = localMVMap.entrySet().iterator(); localIterator.hasNext();)
      {
        localEntry = (Map.Entry)localIterator.next();
        localObject1 = (String)localEntry.getKey();
        if (((String)localObject1).startsWith("chunk."))
        {
          Chunk localChunk = Chunk.fromString((String)localEntry.getValue());
          localTreeMap.put(Integer.valueOf(localChunk.id), localChunk);
          l3 += localChunk.len * 4096;
          l4 += localChunk.maxLen;
          l5 += localChunk.maxLenLive;
          if (localChunk.maxLenLive > 0L) {
            l6 += localChunk.maxLen;
          }
        }
      }
      Map.Entry localEntry;
      Object localObject1;
      localPrintWriter.printf("Created: %s\n", new Object[] { formatTimestamp(l2, l2) });
      localPrintWriter.printf("Last modified: %s\n", new Object[] { formatTimestamp(FileUtils.lastModified(paramString), l2) });
      
      localPrintWriter.printf("File length: %d\n", new Object[] { Long.valueOf(l1) });
      localPrintWriter.printf("The last chunk is not listed\n", new Object[0]);
      localPrintWriter.printf("Chunk length: %d\n", new Object[] { Long.valueOf(l3) });
      localPrintWriter.printf("Chunk count: %d\n", new Object[] { Integer.valueOf(localTreeMap.size()) });
      localPrintWriter.printf("Used space: %d%%\n", new Object[] { Integer.valueOf(getPercent(l3, l1)) });
      localPrintWriter.printf("Chunk fill rate: %d%%\n", new Object[] { Integer.valueOf(l4 == 0L ? 100 : getPercent(l5, l4)) });
      
      localPrintWriter.printf("Chunk fill rate excluding empty chunks: %d%%\n", new Object[] { Integer.valueOf(l6 == 0L ? 100 : getPercent(l5, l6)) });
      for (localIterator = localTreeMap.entrySet().iterator(); localIterator.hasNext();)
      {
        localEntry = (Map.Entry)localIterator.next();
        localObject1 = (Chunk)localEntry.getValue();
        long l7 = l2 + ((Chunk)localObject1).time;
        localPrintWriter.printf("  Chunk %d: %s, %d%% used, %d blocks", new Object[] { Integer.valueOf(((Chunk)localObject1).id), formatTimestamp(l7, l2), Integer.valueOf(getPercent(((Chunk)localObject1).maxLenLive, ((Chunk)localObject1).maxLen)), Integer.valueOf(((Chunk)localObject1).len) });
        if (((Chunk)localObject1).maxLenLive == 0L) {
          localPrintWriter.printf(", unused: %s", new Object[] { formatTimestamp(l2 + ((Chunk)localObject1).unused, l2) });
        }
        localPrintWriter.printf("\n", new Object[0]);
      }
      localPrintWriter.printf("\n", new Object[0]);
    }
    catch (Exception localException)
    {
      localPrintWriter.println("ERROR: " + localException);
      localException.printStackTrace(localPrintWriter);
    }
    finally
    {
      localMVStore.close();
    }
    localPrintWriter.flush();
  }
  
  private static String formatTimestamp(long paramLong1, long paramLong2)
  {
    String str1 = new Timestamp(paramLong1).toString();
    String str2 = str1.substring(0, 19);
    str2 = str2 + " (+" + (paramLong1 - paramLong2) / 1000L + " s)";
    return str2;
  }
  
  private static int getPercent(long paramLong1, long paramLong2)
  {
    if (paramLong1 == 0L) {
      return 0;
    }
    if (paramLong1 == paramLong2) {
      return 100;
    }
    return (int)(1L + 98L * paramLong1 / paramLong2);
  }
  
  public static void compact(String paramString, boolean paramBoolean)
  {
    String str1 = paramString + ".tempFile";
    FileUtils.delete(str1);
    compact(paramString, str1, paramBoolean);
    try
    {
      FileUtils.moveAtomicReplace(str1, paramString);
    }
    catch (DbException localDbException)
    {
      String str2 = paramString + ".newFile";
      FileUtils.delete(str2);
      FileUtils.move(str1, str2);
      FileUtils.delete(paramString);
      FileUtils.move(str2, paramString);
    }
  }
  
  public static void compactCleanUp(String paramString)
  {
    String str1 = paramString + ".tempFile";
    if (FileUtils.exists(str1)) {
      FileUtils.delete(str1);
    }
    String str2 = paramString + ".newFile";
    if (FileUtils.exists(str2)) {
      if (FileUtils.exists(paramString)) {
        FileUtils.delete(str2);
      } else {
        FileUtils.move(str2, paramString);
      }
    }
  }
  
  public static void compact(String paramString1, String paramString2, boolean paramBoolean)
  {
    MVStore localMVStore1 = new MVStore.Builder().fileName(paramString1).readOnly().open();
    
    FileUtils.delete(paramString2);
    MVStore.Builder localBuilder = new MVStore.Builder().fileName(paramString2);
    if (paramBoolean) {
      localBuilder.compress();
    }
    MVStore localMVStore2 = localBuilder.open();
    compact(localMVStore1, localMVStore2);
    localMVStore2.close();
    localMVStore1.close();
  }
  
  public static void compact(MVStore paramMVStore1, MVStore paramMVStore2)
  {
    MVMap localMVMap1 = paramMVStore1.getMetaMap();
    MVMap localMVMap2 = paramMVStore2.getMetaMap();
    for (Iterator localIterator = localMVMap1.entrySet().iterator(); localIterator.hasNext();)
    {
      localObject1 = (Map.Entry)localIterator.next();
      localObject2 = (String)((Map.Entry)localObject1).getKey();
      if (!((String)localObject2).startsWith("chunk.")) {
        if (!((String)localObject2).startsWith("map.")) {
          if (!((String)localObject2).startsWith("name.")) {
            if (!((String)localObject2).startsWith("root.")) {
              localMVMap2.put(localObject2, ((Map.Entry)localObject1).getValue());
            }
          }
        }
      }
    }
    Object localObject1;
    Object localObject2;
    for (localIterator = paramMVStore1.getMapNames().iterator(); localIterator.hasNext();)
    {
      localObject1 = (String)localIterator.next();
      localObject2 = new MVMap.Builder().keyType(new GenericDataType()).valueType(new GenericDataType());
      
      MVMap localMVMap3 = paramMVStore1.openMap((String)localObject1, (MVMap.MapBuilder)localObject2);
      MVMap localMVMap4 = paramMVStore2.openMap((String)localObject1, (MVMap.MapBuilder)localObject2);
      localMVMap4.copyFrom(localMVMap3);
    }
  }
  
  static class GenericDataType
    implements DataType
  {
    public int compare(Object paramObject1, Object paramObject2)
    {
      throw DataUtils.newUnsupportedOperationException("Can not compare");
    }
    
    public int getMemory(Object paramObject)
    {
      return paramObject == null ? 0 : ((byte[])paramObject).length * 8;
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object paramObject)
    {
      if (paramObject != null) {
        paramWriteBuffer.put((byte[])paramObject);
      }
    }
    
    public void write(WriteBuffer paramWriteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
    {
      for (Object localObject : paramArrayOfObject) {
        write(paramWriteBuffer, localObject);
      }
    }
    
    public Object read(ByteBuffer paramByteBuffer)
    {
      int i = paramByteBuffer.remaining();
      if (i == 0) {
        return null;
      }
      byte[] arrayOfByte = new byte[i];
      paramByteBuffer.get(arrayOfByte);
      return arrayOfByte;
    }
    
    public void read(ByteBuffer paramByteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
    {
      for (int i = 0; i < paramArrayOfObject.length; i++) {
        paramArrayOfObject[i] = read(paramByteBuffer);
      }
    }
  }
}
