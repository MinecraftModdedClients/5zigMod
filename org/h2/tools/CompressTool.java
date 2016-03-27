package org.h2.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.h2.compress.CompressDeflate;
import org.h2.compress.CompressLZF;
import org.h2.compress.CompressNo;
import org.h2.compress.Compressor;
import org.h2.compress.LZFInputStream;
import org.h2.compress.LZFOutputStream;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.util.StringUtils;

public class CompressTool
{
  private static final int MAX_BUFFER_SIZE = 393216;
  private byte[] cachedBuffer;
  
  private byte[] getBuffer(int paramInt)
  {
    if (paramInt > 393216) {
      return DataUtils.newBytes(paramInt);
    }
    if ((this.cachedBuffer == null) || (this.cachedBuffer.length < paramInt)) {
      this.cachedBuffer = DataUtils.newBytes(paramInt);
    }
    return this.cachedBuffer;
  }
  
  public static CompressTool getInstance()
  {
    return new CompressTool();
  }
  
  public byte[] compress(byte[] paramArrayOfByte, String paramString)
  {
    int i = paramArrayOfByte.length;
    if (paramArrayOfByte.length < 5) {
      paramString = "NO";
    }
    Compressor localCompressor = getCompressor(paramString);
    byte[] arrayOfByte1 = getBuffer((i < 100 ? i + 100 : i) * 2);
    int j = compress(paramArrayOfByte, paramArrayOfByte.length, localCompressor, arrayOfByte1);
    byte[] arrayOfByte2 = DataUtils.newBytes(j);
    System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, j);
    return arrayOfByte2;
  }
  
  private static int compress(byte[] paramArrayOfByte1, int paramInt, Compressor paramCompressor, byte[] paramArrayOfByte2)
  {
    int i = 0;
    paramArrayOfByte2[0] = ((byte)paramCompressor.getAlgorithm());
    int j = 1 + writeVariableInt(paramArrayOfByte2, 1, paramInt);
    i = paramCompressor.compress(paramArrayOfByte1, paramInt, paramArrayOfByte2, j);
    if ((i > paramInt + j) || (i <= 0))
    {
      paramArrayOfByte2[0] = 0;
      System.arraycopy(paramArrayOfByte1, 0, paramArrayOfByte2, j, paramInt);
      i = paramInt + j;
    }
    return i;
  }
  
  public byte[] expand(byte[] paramArrayOfByte)
  {
    int i = paramArrayOfByte[0];
    Compressor localCompressor = getCompressor(i);
    try
    {
      int j = readVariableInt(paramArrayOfByte, 1);
      int k = 1 + getVariableIntLength(j);
      byte[] arrayOfByte = DataUtils.newBytes(j);
      localCompressor.expand(paramArrayOfByte, k, paramArrayOfByte.length - k, arrayOfByte, 0, j);
      return arrayOfByte;
    }
    catch (Exception localException)
    {
      throw DbException.get(90104, localException, new String[0]);
    }
  }
  
  public static void expand(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt)
  {
    int i = paramArrayOfByte1[0];
    Compressor localCompressor = getCompressor(i);
    try
    {
      int j = readVariableInt(paramArrayOfByte1, 1);
      int k = 1 + getVariableIntLength(j);
      localCompressor.expand(paramArrayOfByte1, k, paramArrayOfByte1.length - k, paramArrayOfByte2, paramInt, j);
    }
    catch (Exception localException)
    {
      throw DbException.get(90104, localException, new String[0]);
    }
  }
  
  public static int readVariableInt(byte[] paramArrayOfByte, int paramInt)
  {
    int i = paramArrayOfByte[(paramInt++)] & 0xFF;
    if (i < 128) {
      return i;
    }
    if (i < 192) {
      return ((i & 0x3F) << 8) + (paramArrayOfByte[paramInt] & 0xFF);
    }
    if (i < 224) {
      return ((i & 0x1F) << 16) + ((paramArrayOfByte[(paramInt++)] & 0xFF) << 8) + (paramArrayOfByte[paramInt] & 0xFF);
    }
    if (i < 240) {
      return ((i & 0xF) << 24) + ((paramArrayOfByte[(paramInt++)] & 0xFF) << 16) + ((paramArrayOfByte[(paramInt++)] & 0xFF) << 8) + (paramArrayOfByte[paramInt] & 0xFF);
    }
    return ((paramArrayOfByte[(paramInt++)] & 0xFF) << 24) + ((paramArrayOfByte[(paramInt++)] & 0xFF) << 16) + ((paramArrayOfByte[(paramInt++)] & 0xFF) << 8) + (paramArrayOfByte[paramInt] & 0xFF);
  }
  
  public static int writeVariableInt(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if (paramInt2 < 0)
    {
      paramArrayOfByte[(paramInt1++)] = -16;
      paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 24));
      paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 16));
      paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 8));
      paramArrayOfByte[paramInt1] = ((byte)paramInt2);
      return 5;
    }
    if (paramInt2 < 128)
    {
      paramArrayOfByte[paramInt1] = ((byte)paramInt2);
      return 1;
    }
    if (paramInt2 < 16384)
    {
      paramArrayOfByte[(paramInt1++)] = ((byte)(0x80 | paramInt2 >> 8));
      paramArrayOfByte[paramInt1] = ((byte)paramInt2);
      return 2;
    }
    if (paramInt2 < 2097152)
    {
      paramArrayOfByte[(paramInt1++)] = ((byte)(0xC0 | paramInt2 >> 16));
      paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 8));
      paramArrayOfByte[paramInt1] = ((byte)paramInt2);
      return 3;
    }
    if (paramInt2 < 268435456)
    {
      paramArrayOfByte[(paramInt1++)] = ((byte)(0xE0 | paramInt2 >> 24));
      paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 16));
      paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 8));
      paramArrayOfByte[paramInt1] = ((byte)paramInt2);
      return 4;
    }
    paramArrayOfByte[(paramInt1++)] = -16;
    paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 24));
    paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 16));
    paramArrayOfByte[(paramInt1++)] = ((byte)(paramInt2 >> 8));
    paramArrayOfByte[paramInt1] = ((byte)paramInt2);
    return 5;
  }
  
  public static int getVariableIntLength(int paramInt)
  {
    if (paramInt < 0) {
      return 5;
    }
    if (paramInt < 128) {
      return 1;
    }
    if (paramInt < 16384) {
      return 2;
    }
    if (paramInt < 2097152) {
      return 3;
    }
    if (paramInt < 268435456) {
      return 4;
    }
    return 5;
  }
  
  private static Compressor getCompressor(String paramString)
  {
    if (paramString == null) {
      paramString = "LZF";
    }
    int i = paramString.indexOf(' ');
    String str = null;
    if (i > 0)
    {
      str = paramString.substring(i + 1);
      paramString = paramString.substring(0, i);
    }
    int j = getCompressAlgorithm(paramString);
    Compressor localCompressor = getCompressor(j);
    localCompressor.setOptions(str);
    return localCompressor;
  }
  
  public static int getCompressAlgorithm(String paramString)
  {
    paramString = StringUtils.toUpperEnglish(paramString);
    if ("NO".equals(paramString)) {
      return 0;
    }
    if ("LZF".equals(paramString)) {
      return 1;
    }
    if ("DEFLATE".equals(paramString)) {
      return 2;
    }
    throw DbException.get(90103, paramString);
  }
  
  private static Compressor getCompressor(int paramInt)
  {
    switch (paramInt)
    {
    case 0: 
      return new CompressNo();
    case 1: 
      return new CompressLZF();
    case 2: 
      return new CompressDeflate();
    }
    throw DbException.get(90103, "" + paramInt);
  }
  
  public static OutputStream wrapOutputStream(OutputStream paramOutputStream, String paramString1, String paramString2)
  {
    try
    {
      if ("GZIP".equals(paramString1))
      {
        paramOutputStream = new GZIPOutputStream(paramOutputStream);
      }
      else if ("ZIP".equals(paramString1))
      {
        ZipOutputStream localZipOutputStream = new ZipOutputStream(paramOutputStream);
        localZipOutputStream.putNextEntry(new ZipEntry(paramString2));
        paramOutputStream = localZipOutputStream;
      }
      else if ("DEFLATE".equals(paramString1))
      {
        paramOutputStream = new DeflaterOutputStream(paramOutputStream);
      }
      else if ("LZF".equals(paramString1))
      {
        paramOutputStream = new LZFOutputStream(paramOutputStream);
      }
      else if (paramString1 != null)
      {
        throw DbException.get(90103, paramString1);
      }
      return paramOutputStream;
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
  
  public static InputStream wrapInputStream(InputStream paramInputStream, String paramString1, String paramString2)
  {
    try
    {
      if ("GZIP".equals(paramString1))
      {
        paramInputStream = new GZIPInputStream(paramInputStream);
      }
      else if ("ZIP".equals(paramString1))
      {
        ZipInputStream localZipInputStream = new ZipInputStream(paramInputStream);
        for (;;)
        {
          ZipEntry localZipEntry = localZipInputStream.getNextEntry();
          if (localZipEntry == null) {
            return null;
          }
          if (paramString2.equals(localZipEntry.getName())) {
            break;
          }
        }
        paramInputStream = localZipInputStream;
      }
      else if ("DEFLATE".equals(paramString1))
      {
        paramInputStream = new InflaterInputStream(paramInputStream);
      }
      else if ("LZF".equals(paramString1))
      {
        paramInputStream = new LZFInputStream(paramInputStream);
      }
      else if (paramString1 != null)
      {
        throw DbException.get(90103, paramString1);
      }
      return paramInputStream;
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
  }
}
