package org.h2.compress;

import java.util.StringTokenizer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.h2.message.DbException;

public class CompressDeflate
  implements Compressor
{
  private int level = -1;
  private int strategy = 0;
  
  public void setOptions(String paramString)
  {
    if (paramString == null) {
      return;
    }
    try
    {
      StringTokenizer localStringTokenizer = new StringTokenizer(paramString);
      while (localStringTokenizer.hasMoreElements())
      {
        String str = localStringTokenizer.nextToken();
        if (("level".equals(str)) || ("l".equals(str))) {
          this.level = Integer.parseInt(localStringTokenizer.nextToken());
        } else if (("strategy".equals(str)) || ("s".equals(str))) {
          this.strategy = Integer.parseInt(localStringTokenizer.nextToken());
        }
        Deflater localDeflater = new Deflater(this.level);
        localDeflater.setStrategy(this.strategy);
      }
    }
    catch (Exception localException)
    {
      throw DbException.get(90102, paramString);
    }
  }
  
  public int compress(byte[] paramArrayOfByte1, int paramInt1, byte[] paramArrayOfByte2, int paramInt2)
  {
    Deflater localDeflater = new Deflater(this.level);
    localDeflater.setStrategy(this.strategy);
    localDeflater.setInput(paramArrayOfByte1, 0, paramInt1);
    localDeflater.finish();
    int i = localDeflater.deflate(paramArrayOfByte2, paramInt2, paramArrayOfByte2.length - paramInt2);
    if (i == 0)
    {
      this.strategy = 0;
      this.level = -1;
      return compress(paramArrayOfByte1, paramInt1, paramArrayOfByte2, paramInt2);
    }
    localDeflater.end();
    return paramInt2 + i;
  }
  
  public int getAlgorithm()
  {
    return 2;
  }
  
  public void expand(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, int paramInt3, int paramInt4)
  {
    Inflater localInflater = new Inflater();
    localInflater.setInput(paramArrayOfByte1, paramInt1, paramInt2);
    localInflater.finished();
    try
    {
      int i = localInflater.inflate(paramArrayOfByte2, paramInt3, paramInt4);
      if (i != paramInt4) {
        throw new DataFormatException(i + " " + paramInt4);
      }
    }
    catch (DataFormatException localDataFormatException)
    {
      throw DbException.get(90104, localDataFormatException, new String[0]);
    }
    localInflater.end();
  }
}
