package org.h2.security;

import org.h2.util.Utils;

public class Fog
  implements BlockCipher
{
  private int key;
  
  public void encrypt(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    for (int i = paramInt1; i < paramInt1 + paramInt2; i += 16) {
      encryptBlock(paramArrayOfByte, paramArrayOfByte, i);
    }
  }
  
  public void decrypt(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    for (int i = paramInt1; i < paramInt1 + paramInt2; i += 16) {
      decryptBlock(paramArrayOfByte, paramArrayOfByte, i);
    }
  }
  
  private void encryptBlock(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt)
  {
    int i = paramArrayOfByte1[paramInt] << 24 | (paramArrayOfByte1[(paramInt + 1)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 2)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 3)] & 0xFF;
    
    int j = paramArrayOfByte1[(paramInt + 4)] << 24 | (paramArrayOfByte1[(paramInt + 5)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 6)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 7)] & 0xFF;
    
    int k = paramArrayOfByte1[(paramInt + 8)] << 24 | (paramArrayOfByte1[(paramInt + 9)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 10)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 11)] & 0xFF;
    
    int m = paramArrayOfByte1[(paramInt + 12)] << 24 | (paramArrayOfByte1[(paramInt + 13)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 14)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 15)] & 0xFF;
    
    int n = this.key;
    int i1 = j & 0x1F;
    i ^= n;
    i = i << i1 | i >>> 32 - i1;
    k ^= n;
    k = k << i1 | k >>> 32 - i1;
    i1 = i & 0x1F;
    j ^= n;
    j = j << i1 | j >>> 32 - i1;
    m ^= n;
    m = m << i1 | m >>> 32 - i1;
    paramArrayOfByte2[paramInt] = ((byte)(i >> 24));paramArrayOfByte2[(paramInt + 1)] = ((byte)(i >> 16));
    paramArrayOfByte2[(paramInt + 2)] = ((byte)(i >> 8));paramArrayOfByte2[(paramInt + 3)] = ((byte)i);
    paramArrayOfByte2[(paramInt + 4)] = ((byte)(j >> 24));paramArrayOfByte2[(paramInt + 5)] = ((byte)(j >> 16));
    paramArrayOfByte2[(paramInt + 6)] = ((byte)(j >> 8));paramArrayOfByte2[(paramInt + 7)] = ((byte)j);
    paramArrayOfByte2[(paramInt + 8)] = ((byte)(k >> 24));paramArrayOfByte2[(paramInt + 9)] = ((byte)(k >> 16));
    paramArrayOfByte2[(paramInt + 10)] = ((byte)(k >> 8));paramArrayOfByte2[(paramInt + 11)] = ((byte)k);
    paramArrayOfByte2[(paramInt + 12)] = ((byte)(m >> 24));paramArrayOfByte2[(paramInt + 13)] = ((byte)(m >> 16));
    paramArrayOfByte2[(paramInt + 14)] = ((byte)(m >> 8));paramArrayOfByte2[(paramInt + 15)] = ((byte)m);
  }
  
  private void decryptBlock(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt)
  {
    int i = paramArrayOfByte1[paramInt] << 24 | (paramArrayOfByte1[(paramInt + 1)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 2)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 3)] & 0xFF;
    
    int j = paramArrayOfByte1[(paramInt + 4)] << 24 | (paramArrayOfByte1[(paramInt + 5)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 6)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 7)] & 0xFF;
    
    int k = paramArrayOfByte1[(paramInt + 8)] << 24 | (paramArrayOfByte1[(paramInt + 9)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 10)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 11)] & 0xFF;
    
    int m = paramArrayOfByte1[(paramInt + 12)] << 24 | (paramArrayOfByte1[(paramInt + 13)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 14)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 15)] & 0xFF;
    
    int n = this.key;
    int i1 = 32 - (i & 0x1F);
    j = j << i1 | j >>> 32 - i1;
    j ^= n;
    m = m << i1 | m >>> 32 - i1;
    m ^= n;
    i1 = 32 - (j & 0x1F);
    i = i << i1 | i >>> 32 - i1;
    i ^= n;
    k = k << i1 | k >>> 32 - i1;
    k ^= n;
    paramArrayOfByte2[paramInt] = ((byte)(i >> 24));paramArrayOfByte2[(paramInt + 1)] = ((byte)(i >> 16));
    paramArrayOfByte2[(paramInt + 2)] = ((byte)(i >> 8));paramArrayOfByte2[(paramInt + 3)] = ((byte)i);
    paramArrayOfByte2[(paramInt + 4)] = ((byte)(j >> 24));paramArrayOfByte2[(paramInt + 5)] = ((byte)(j >> 16));
    paramArrayOfByte2[(paramInt + 6)] = ((byte)(j >> 8));paramArrayOfByte2[(paramInt + 7)] = ((byte)j);
    paramArrayOfByte2[(paramInt + 8)] = ((byte)(k >> 24));paramArrayOfByte2[(paramInt + 9)] = ((byte)(k >> 16));
    paramArrayOfByte2[(paramInt + 10)] = ((byte)(k >> 8));paramArrayOfByte2[(paramInt + 11)] = ((byte)k);
    paramArrayOfByte2[(paramInt + 12)] = ((byte)(m >> 24));paramArrayOfByte2[(paramInt + 13)] = ((byte)(m >> 16));
    paramArrayOfByte2[(paramInt + 14)] = ((byte)(m >> 8));paramArrayOfByte2[(paramInt + 15)] = ((byte)m);
  }
  
  public int getKeyLength()
  {
    return 16;
  }
  
  public void setKey(byte[] paramArrayOfByte)
  {
    this.key = ((int)Utils.readLong(paramArrayOfByte, 0));
  }
}
