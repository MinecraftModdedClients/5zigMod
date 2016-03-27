package org.h2.compress;

import java.nio.ByteBuffer;

public final class CompressLZF
  implements Compressor
{
  private static final int HASH_SIZE = 16384;
  private static final int MAX_LITERAL = 32;
  private static final int MAX_OFF = 8192;
  private static final int MAX_REF = 264;
  private int[] cachedHashTable;
  
  public void setOptions(String paramString) {}
  
  private static int first(byte[] paramArrayOfByte, int paramInt)
  {
    return paramArrayOfByte[paramInt] << 8 | paramArrayOfByte[(paramInt + 1)] & 0xFF;
  }
  
  private static int first(ByteBuffer paramByteBuffer, int paramInt)
  {
    return paramByteBuffer.get(paramInt) << 8 | paramByteBuffer.get(paramInt + 1) & 0xFF;
  }
  
  private static int next(int paramInt1, byte[] paramArrayOfByte, int paramInt2)
  {
    return paramInt1 << 8 | paramArrayOfByte[(paramInt2 + 2)] & 0xFF;
  }
  
  private static int next(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2)
  {
    return paramInt1 << 8 | paramByteBuffer.get(paramInt2 + 2) & 0xFF;
  }
  
  private static int hash(int paramInt)
  {
    return paramInt * 2777 >> 9 & 0x3FFF;
  }
  
  public int compress(byte[] paramArrayOfByte1, int paramInt1, byte[] paramArrayOfByte2, int paramInt2)
  {
    int i = 0;
    if (this.cachedHashTable == null) {
      this.cachedHashTable = new int['䀀'];
    }
    int[] arrayOfInt = this.cachedHashTable;
    int j = 0;
    paramInt2++;
    int k = first(paramArrayOfByte1, 0);
    while (i < paramInt1 - 4)
    {
      int m = paramArrayOfByte1[(i + 2)];
      
      k = (k << 8) + (m & 0xFF);
      int n = hash(k);
      int i1 = arrayOfInt[n];
      arrayOfInt[n] = i;
      if ((i1 < i) && (i1 > 0) && ((n = i - i1 - 1) < 8192) && (paramArrayOfByte1[(i1 + 2)] == m) && (paramArrayOfByte1[(i1 + 1)] == (byte)(k >> 8)) && (paramArrayOfByte1[i1] == (byte)(k >> 16)))
      {
        int i2 = paramInt1 - i - 2;
        if (i2 > 264) {
          i2 = 264;
        }
        if (j == 0)
        {
          paramInt2--;
        }
        else
        {
          paramArrayOfByte2[(paramInt2 - j - 1)] = ((byte)(j - 1));
          j = 0;
        }
        int i3 = 3;
        while ((i3 < i2) && (paramArrayOfByte1[(i1 + i3)] == paramArrayOfByte1[(i + i3)])) {
          i3++;
        }
        i3 -= 2;
        if (i3 < 7)
        {
          paramArrayOfByte2[(paramInt2++)] = ((byte)((n >> 8) + (i3 << 5)));
        }
        else
        {
          paramArrayOfByte2[(paramInt2++)] = ((byte)((n >> 8) + 224));
          paramArrayOfByte2[(paramInt2++)] = ((byte)(i3 - 7));
        }
        paramArrayOfByte2[(paramInt2++)] = ((byte)n);
        
        paramInt2++;
        i += i3;
        
        k = first(paramArrayOfByte1, i);
        k = next(k, paramArrayOfByte1, i);
        arrayOfInt[hash(k)] = (i++);
        k = next(k, paramArrayOfByte1, i);
        arrayOfInt[hash(k)] = (i++);
      }
      else
      {
        paramArrayOfByte2[(paramInt2++)] = paramArrayOfByte1[(i++)];
        j++;
        if (j == 32)
        {
          paramArrayOfByte2[(paramInt2 - j - 1)] = ((byte)(j - 1));
          j = 0;
          
          paramInt2++;
        }
      }
    }
    while (i < paramInt1)
    {
      paramArrayOfByte2[(paramInt2++)] = paramArrayOfByte1[(i++)];
      j++;
      if (j == 32)
      {
        paramArrayOfByte2[(paramInt2 - j - 1)] = ((byte)(j - 1));
        j = 0;
        paramInt2++;
      }
    }
    paramArrayOfByte2[(paramInt2 - j - 1)] = ((byte)(j - 1));
    if (j == 0) {
      paramInt2--;
    }
    return paramInt2;
  }
  
  public int compress(ByteBuffer paramByteBuffer, byte[] paramArrayOfByte, int paramInt)
  {
    int i = paramByteBuffer.position();
    int j = paramByteBuffer.capacity() - i;
    if (this.cachedHashTable == null) {
      this.cachedHashTable = new int['䀀'];
    }
    int[] arrayOfInt = this.cachedHashTable;
    int k = 0;
    paramInt++;
    int m = first(paramByteBuffer, 0);
    while (i < j - 4)
    {
      int n = paramByteBuffer.get(i + 2);
      
      m = (m << 8) + (n & 0xFF);
      int i1 = hash(m);
      int i2 = arrayOfInt[i1];
      arrayOfInt[i1] = i;
      if ((i2 < i) && (i2 > 0) && ((i1 = i - i2 - 1) < 8192) && (paramByteBuffer.get(i2 + 2) == n) && (paramByteBuffer.get(i2 + 1) == (byte)(m >> 8)) && (paramByteBuffer.get(i2) == (byte)(m >> 16)))
      {
        int i3 = j - i - 2;
        if (i3 > 264) {
          i3 = 264;
        }
        if (k == 0)
        {
          paramInt--;
        }
        else
        {
          paramArrayOfByte[(paramInt - k - 1)] = ((byte)(k - 1));
          k = 0;
        }
        int i4 = 3;
        while ((i4 < i3) && (paramByteBuffer.get(i2 + i4) == paramByteBuffer.get(i + i4))) {
          i4++;
        }
        i4 -= 2;
        if (i4 < 7)
        {
          paramArrayOfByte[(paramInt++)] = ((byte)((i1 >> 8) + (i4 << 5)));
        }
        else
        {
          paramArrayOfByte[(paramInt++)] = ((byte)((i1 >> 8) + 224));
          paramArrayOfByte[(paramInt++)] = ((byte)(i4 - 7));
        }
        paramArrayOfByte[(paramInt++)] = ((byte)i1);
        
        paramInt++;
        i += i4;
        
        m = first(paramByteBuffer, i);
        m = next(m, paramByteBuffer, i);
        arrayOfInt[hash(m)] = (i++);
        m = next(m, paramByteBuffer, i);
        arrayOfInt[hash(m)] = (i++);
      }
      else
      {
        paramArrayOfByte[(paramInt++)] = paramByteBuffer.get(i++);
        k++;
        if (k == 32)
        {
          paramArrayOfByte[(paramInt - k - 1)] = ((byte)(k - 1));
          k = 0;
          
          paramInt++;
        }
      }
    }
    while (i < j)
    {
      paramArrayOfByte[(paramInt++)] = paramByteBuffer.get(i++);
      k++;
      if (k == 32)
      {
        paramArrayOfByte[(paramInt - k - 1)] = ((byte)(k - 1));
        k = 0;
        paramInt++;
      }
    }
    paramArrayOfByte[(paramInt - k - 1)] = ((byte)(k - 1));
    if (k == 0) {
      paramInt--;
    }
    return paramInt;
  }
  
  public void expand(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, int paramInt3, int paramInt4)
  {
    if ((paramInt1 < 0) || (paramInt3 < 0) || (paramInt4 < 0)) {
      throw new IllegalArgumentException();
    }
    do
    {
      int i = paramArrayOfByte1[(paramInt1++)] & 0xFF;
      if (i < 32)
      {
        i++;
        
        System.arraycopy(paramArrayOfByte1, paramInt1, paramArrayOfByte2, paramInt3, i);
        paramInt3 += i;
        paramInt1 += i;
      }
      else
      {
        int j = i >> 5;
        if (j == 7) {
          j += (paramArrayOfByte1[(paramInt1++)] & 0xFF);
        }
        j += 2;
        
        i = -((i & 0x1F) << 8) - 1;
        
        i -= (paramArrayOfByte1[(paramInt1++)] & 0xFF);
        
        i += paramInt3;
        if (paramInt3 + j >= paramArrayOfByte2.length) {
          throw new ArrayIndexOutOfBoundsException();
        }
        for (int k = 0; k < j; k++) {
          paramArrayOfByte2[(paramInt3++)] = paramArrayOfByte2[(i++)];
        }
      }
    } while (paramInt3 < paramInt4);
  }
  
  public static void expand(ByteBuffer paramByteBuffer1, ByteBuffer paramByteBuffer2)
  {
    do
    {
      int i = paramByteBuffer1.get() & 0xFF;
      int j;
      if (i < 32)
      {
        i++;
        for (j = 0; j < i; j++) {
          paramByteBuffer2.put(paramByteBuffer1.get());
        }
      }
      else
      {
        j = i >> 5;
        if (j == 7) {
          j += (paramByteBuffer1.get() & 0xFF);
        }
        j += 2;
        
        i = -((i & 0x1F) << 8) - 1;
        
        i -= (paramByteBuffer1.get() & 0xFF);
        
        i += paramByteBuffer2.position();
        for (int k = 0; k < j; k++) {
          paramByteBuffer2.put(paramByteBuffer2.get(i++));
        }
      }
    } while (paramByteBuffer2.position() < paramByteBuffer2.capacity());
  }
  
  public int getAlgorithm()
  {
    return 1;
  }
}
