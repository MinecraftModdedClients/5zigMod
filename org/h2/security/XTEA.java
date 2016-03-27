package org.h2.security;

import org.h2.engine.SysProperties;
import org.h2.message.DbException;

public class XTEA
  implements BlockCipher
{
  private static final int DELTA = -1640531527;
  private int k0;
  private int k1;
  private int k2;
  private int k3;
  private int k4;
  private int k5;
  private int k6;
  private int k7;
  private int k8;
  private int k9;
  private int k10;
  private int k11;
  private int k12;
  private int k13;
  private int k14;
  private int k15;
  private int k16;
  private int k17;
  private int k18;
  private int k19;
  private int k20;
  private int k21;
  private int k22;
  private int k23;
  private int k24;
  private int k25;
  private int k26;
  private int k27;
  private int k28;
  private int k29;
  private int k30;
  private int k31;
  
  public void setKey(byte[] paramArrayOfByte)
  {
    int[] arrayOfInt1 = new int[4];
    for (int i = 0; i < 16;) {
      arrayOfInt1[(i / 4)] = ((paramArrayOfByte[(i++)] << 24) + ((paramArrayOfByte[(i++)] & 0xFF) << 16) + ((paramArrayOfByte[(i++)] & 0xFF) << 8) + (paramArrayOfByte[(i++)] & 0xFF));
    }
    int[] arrayOfInt2 = new int[32];
    int j = 0;
    for (int k = 0; j < 32;)
    {
      arrayOfInt2[(j++)] = (k + arrayOfInt1[(k & 0x3)]);
      k -= 1640531527;
      arrayOfInt2[(j++)] = (k + arrayOfInt1[(k >>> 11 & 0x3)]);
    }
    this.k0 = arrayOfInt2[0];this.k1 = arrayOfInt2[1];this.k2 = arrayOfInt2[2];this.k3 = arrayOfInt2[3];
    this.k4 = arrayOfInt2[4];this.k5 = arrayOfInt2[5];this.k6 = arrayOfInt2[6];this.k7 = arrayOfInt2[7];
    this.k8 = arrayOfInt2[8];this.k9 = arrayOfInt2[9];this.k10 = arrayOfInt2[10];this.k11 = arrayOfInt2[11];
    this.k12 = arrayOfInt2[12];this.k13 = arrayOfInt2[13];this.k14 = arrayOfInt2[14];this.k15 = arrayOfInt2[15];
    this.k16 = arrayOfInt2[16];this.k17 = arrayOfInt2[17];this.k18 = arrayOfInt2[18];this.k19 = arrayOfInt2[19];
    this.k20 = arrayOfInt2[20];this.k21 = arrayOfInt2[21];this.k22 = arrayOfInt2[22];this.k23 = arrayOfInt2[23];
    this.k24 = arrayOfInt2[24];this.k25 = arrayOfInt2[25];this.k26 = arrayOfInt2[26];this.k27 = arrayOfInt2[27];
    this.k28 = arrayOfInt2[28];this.k29 = arrayOfInt2[29];this.k30 = arrayOfInt2[30];this.k31 = arrayOfInt2[31];
  }
  
  public void encrypt(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if ((SysProperties.CHECK) && 
      (paramInt2 % 16 != 0)) {
      DbException.throwInternalError("unaligned len " + paramInt2);
    }
    for (int i = paramInt1; i < paramInt1 + paramInt2; i += 8) {
      encryptBlock(paramArrayOfByte, paramArrayOfByte, i);
    }
  }
  
  public void decrypt(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if ((SysProperties.CHECK) && 
      (paramInt2 % 16 != 0)) {
      DbException.throwInternalError("unaligned len " + paramInt2);
    }
    for (int i = paramInt1; i < paramInt1 + paramInt2; i += 8) {
      decryptBlock(paramArrayOfByte, paramArrayOfByte, i);
    }
  }
  
  private void encryptBlock(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt)
  {
    int i = paramArrayOfByte1[paramInt] << 24 | (paramArrayOfByte1[(paramInt + 1)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 2)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 3)] & 0xFF;
    
    int j = paramArrayOfByte1[(paramInt + 4)] << 24 | (paramArrayOfByte1[(paramInt + 5)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 6)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 7)] & 0xFF;
    
    i += ((j << 4 ^ j >>> 5) + j ^ this.k0);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k1);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k2);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k3);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k4);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k5);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k6);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k7);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k8);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k9);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k10);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k11);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k12);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k13);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k14);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k15);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k16);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k17);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k18);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k19);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k20);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k21);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k22);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k23);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k24);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k25);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k26);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k27);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k28);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k29);
    i += ((j << 4 ^ j >>> 5) + j ^ this.k30);
    j += ((i >>> 5 ^ i << 4) + i ^ this.k31);
    paramArrayOfByte2[paramInt] = ((byte)(i >> 24));
    paramArrayOfByte2[(paramInt + 1)] = ((byte)(i >> 16));
    paramArrayOfByte2[(paramInt + 2)] = ((byte)(i >> 8));
    paramArrayOfByte2[(paramInt + 3)] = ((byte)i);
    paramArrayOfByte2[(paramInt + 4)] = ((byte)(j >> 24));
    paramArrayOfByte2[(paramInt + 5)] = ((byte)(j >> 16));
    paramArrayOfByte2[(paramInt + 6)] = ((byte)(j >> 8));
    paramArrayOfByte2[(paramInt + 7)] = ((byte)j);
  }
  
  private void decryptBlock(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt)
  {
    int i = paramArrayOfByte1[paramInt] << 24 | (paramArrayOfByte1[(paramInt + 1)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 2)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 3)] & 0xFF;
    
    int j = paramArrayOfByte1[(paramInt + 4)] << 24 | (paramArrayOfByte1[(paramInt + 5)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 6)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 7)] & 0xFF;
    
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k31);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k30);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k29);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k28);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k27);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k26);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k25);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k24);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k23);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k22);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k21);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k20);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k19);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k18);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k17);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k16);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k15);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k14);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k13);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k12);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k11);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k10);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k9);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k8);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k7);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k6);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k5);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k4);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k3);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k2);
    j -= ((i >>> 5 ^ i << 4) + i ^ this.k1);
    i -= ((j << 4 ^ j >>> 5) + j ^ this.k0);
    paramArrayOfByte2[paramInt] = ((byte)(i >> 24));
    paramArrayOfByte2[(paramInt + 1)] = ((byte)(i >> 16));
    paramArrayOfByte2[(paramInt + 2)] = ((byte)(i >> 8));
    paramArrayOfByte2[(paramInt + 3)] = ((byte)i);
    paramArrayOfByte2[(paramInt + 4)] = ((byte)(j >> 24));
    paramArrayOfByte2[(paramInt + 5)] = ((byte)(j >> 16));
    paramArrayOfByte2[(paramInt + 6)] = ((byte)(j >> 8));
    paramArrayOfByte2[(paramInt + 7)] = ((byte)j);
  }
  
  public int getKeyLength()
  {
    return 16;
  }
}
