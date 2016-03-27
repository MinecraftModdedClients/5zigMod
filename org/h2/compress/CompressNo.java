package org.h2.compress;

public class CompressNo
  implements Compressor
{
  public int getAlgorithm()
  {
    return 0;
  }
  
  public void setOptions(String paramString) {}
  
  public int compress(byte[] paramArrayOfByte1, int paramInt1, byte[] paramArrayOfByte2, int paramInt2)
  {
    System.arraycopy(paramArrayOfByte1, 0, paramArrayOfByte2, paramInt2, paramInt1);
    return paramInt2 + paramInt1;
  }
  
  public void expand(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, int paramInt3, int paramInt4)
  {
    System.arraycopy(paramArrayOfByte1, paramInt1, paramArrayOfByte2, paramInt3, paramInt4);
  }
}
