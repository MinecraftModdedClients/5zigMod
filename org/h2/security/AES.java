package org.h2.security;

public class AES
  implements BlockCipher
{
  private static final int[] RCON = new int[10];
  private static final int[] FS = new int['Ā'];
  private static final int[] FT0 = new int['Ā'];
  private static final int[] FT1 = new int['Ā'];
  private static final int[] FT2 = new int['Ā'];
  private static final int[] FT3 = new int['Ā'];
  private static final int[] RS = new int['Ā'];
  private static final int[] RT0 = new int['Ā'];
  private static final int[] RT1 = new int['Ā'];
  private static final int[] RT2 = new int['Ā'];
  private static final int[] RT3 = new int['Ā'];
  private final int[] encKey = new int[44];
  private final int[] decKey = new int[44];
  
  private static int rot8(int paramInt)
  {
    return paramInt >>> 8 | paramInt << 24;
  }
  
  private static int xtime(int paramInt)
  {
    return (paramInt << 1 ^ ((paramInt & 0x80) != 0 ? 27 : 0)) & 0xFF;
  }
  
  private static int mul(int[] paramArrayOfInt1, int[] paramArrayOfInt2, int paramInt1, int paramInt2)
  {
    return (paramInt1 != 0) && (paramInt2 != 0) ? paramArrayOfInt1[((paramArrayOfInt2[paramInt1] + paramArrayOfInt2[paramInt2]) % 255)] : 0;
  }
  
  static
  {
    int[] arrayOfInt1 = new int['Ā'];
    int[] arrayOfInt2 = new int['Ā'];
    int i = 0;
    for (int j = 1; i < 256; j ^= xtime(j))
    {
      arrayOfInt1[i] = j;
      arrayOfInt2[j] = i;i++;
    }
    i = 0;
    for (j = 1; i < 10; j = xtime(j))
    {
      RCON[i] = (j << 24);i++;
    }
    FS[0] = 99;
    RS[99] = 0;
    int k;
    for (i = 1; i < 256; i++)
    {
      j = arrayOfInt1[(255 - arrayOfInt2[i])];k = j;
      k = (k << 1 | k >> 7) & 0xFF;
      j ^= k;
      k = (k << 1 | k >> 7) & 0xFF;
      j ^= k;
      k = (k << 1 | k >> 7) & 0xFF;
      j ^= k;
      k = (k << 1 | k >> 7) & 0xFF;
      j ^= k ^ 0x63;
      FS[i] = (j & 0xFF);
      RS[j] = (i & 0xFF);
    }
    for (i = 0; i < 256; i++)
    {
      j = FS[i];k = xtime(j);
      FT0[i] = (j ^ k ^ j << 8 ^ j << 16 ^ k << 24);
      FT1[i] = rot8(FT0[i]);
      FT2[i] = rot8(FT1[i]);
      FT3[i] = rot8(FT2[i]);
      k = RS[i];
      RT0[i] = (mul(arrayOfInt1, arrayOfInt2, 11, k) ^ mul(arrayOfInt1, arrayOfInt2, 13, k) << 8 ^ mul(arrayOfInt1, arrayOfInt2, 9, k) << 16 ^ mul(arrayOfInt1, arrayOfInt2, 14, k) << 24);
      
      RT1[i] = rot8(RT0[i]);
      RT2[i] = rot8(RT1[i]);
      RT3[i] = rot8(RT2[i]);
    }
  }
  
  private static int getDec(int paramInt)
  {
    return RT0[FS[(paramInt >> 24 & 0xFF)]] ^ RT1[FS[(paramInt >> 16 & 0xFF)]] ^ RT2[FS[(paramInt >> 8 & 0xFF)]] ^ RT3[FS[(paramInt & 0xFF)]];
  }
  
  public void setKey(byte[] paramArrayOfByte)
  {
    int i = 0;
    for (int j = 0; i < 4; i++) {
      this.encKey[i] = (this.decKey[i] = (paramArrayOfByte[(j++)] & 0xFF) << 24 | (paramArrayOfByte[(j++)] & 0xFF) << 16 | (paramArrayOfByte[(j++)] & 0xFF) << 8 | paramArrayOfByte[(j++)] & 0xFF);
    }
    i = 0;
    for (j = 0; j < 10; i += 4)
    {
      this.encKey[(i + 4)] = (this.encKey[i] ^ RCON[j] ^ FS[(this.encKey[(i + 3)] >> 16 & 0xFF)] << 24 ^ FS[(this.encKey[(i + 3)] >> 8 & 0xFF)] << 16 ^ FS[(this.encKey[(i + 3)] & 0xFF)] << 8 ^ FS[(this.encKey[(i + 3)] >> 24 & 0xFF)]);
      
      this.encKey[(i + 5)] = (this.encKey[(i + 1)] ^ this.encKey[(i + 4)]);
      this.encKey[(i + 6)] = (this.encKey[(i + 2)] ^ this.encKey[(i + 5)]);
      this.encKey[(i + 7)] = (this.encKey[(i + 3)] ^ this.encKey[(i + 6)]);j++;
    }
    j = 0;
    this.decKey[(j++)] = this.encKey[(i++)];
    this.decKey[(j++)] = this.encKey[(i++)];
    this.decKey[(j++)] = this.encKey[(i++)];
    this.decKey[(j++)] = this.encKey[(i++)];
    for (int k = 1; k < 10; k++)
    {
      i -= 8;
      this.decKey[(j++)] = getDec(this.encKey[(i++)]);
      this.decKey[(j++)] = getDec(this.encKey[(i++)]);
      this.decKey[(j++)] = getDec(this.encKey[(i++)]);
      this.decKey[(j++)] = getDec(this.encKey[(i++)]);
    }
    i -= 8;
    this.decKey[(j++)] = this.encKey[(i++)];
    this.decKey[(j++)] = this.encKey[(i++)];
    this.decKey[(j++)] = this.encKey[(i++)];
    this.decKey[j] = this.encKey[i];
  }
  
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
    int[] arrayOfInt = this.encKey;
    int i = (paramArrayOfByte1[paramInt] << 24 | (paramArrayOfByte1[(paramInt + 1)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 2)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 3)] & 0xFF) ^ arrayOfInt[0];
    
    int j = (paramArrayOfByte1[(paramInt + 4)] << 24 | (paramArrayOfByte1[(paramInt + 5)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 6)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 7)] & 0xFF) ^ arrayOfInt[1];
    
    int k = (paramArrayOfByte1[(paramInt + 8)] << 24 | (paramArrayOfByte1[(paramInt + 9)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 10)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 11)] & 0xFF) ^ arrayOfInt[2];
    
    int m = (paramArrayOfByte1[(paramInt + 12)] << 24 | (paramArrayOfByte1[(paramInt + 13)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 14)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 15)] & 0xFF) ^ arrayOfInt[3];
    
    int n = FT0[(i >> 24 & 0xFF)] ^ FT1[(j >> 16 & 0xFF)] ^ FT2[(k >> 8 & 0xFF)] ^ FT3[(m & 0xFF)] ^ arrayOfInt[4];
    
    int i1 = FT0[(j >> 24 & 0xFF)] ^ FT1[(k >> 16 & 0xFF)] ^ FT2[(m >> 8 & 0xFF)] ^ FT3[(i & 0xFF)] ^ arrayOfInt[5];
    
    int i2 = FT0[(k >> 24 & 0xFF)] ^ FT1[(m >> 16 & 0xFF)] ^ FT2[(i >> 8 & 0xFF)] ^ FT3[(j & 0xFF)] ^ arrayOfInt[6];
    
    int i3 = FT0[(m >> 24 & 0xFF)] ^ FT1[(i >> 16 & 0xFF)] ^ FT2[(j >> 8 & 0xFF)] ^ FT3[(k & 0xFF)] ^ arrayOfInt[7];
    
    i = FT0[(n >> 24 & 0xFF)] ^ FT1[(i1 >> 16 & 0xFF)] ^ FT2[(i2 >> 8 & 0xFF)] ^ FT3[(i3 & 0xFF)] ^ arrayOfInt[8];
    
    j = FT0[(i1 >> 24 & 0xFF)] ^ FT1[(i2 >> 16 & 0xFF)] ^ FT2[(i3 >> 8 & 0xFF)] ^ FT3[(n & 0xFF)] ^ arrayOfInt[9];
    
    k = FT0[(i2 >> 24 & 0xFF)] ^ FT1[(i3 >> 16 & 0xFF)] ^ FT2[(n >> 8 & 0xFF)] ^ FT3[(i1 & 0xFF)] ^ arrayOfInt[10];
    
    m = FT0[(i3 >> 24 & 0xFF)] ^ FT1[(n >> 16 & 0xFF)] ^ FT2[(i1 >> 8 & 0xFF)] ^ FT3[(i2 & 0xFF)] ^ arrayOfInt[11];
    
    n = FT0[(i >> 24 & 0xFF)] ^ FT1[(j >> 16 & 0xFF)] ^ FT2[(k >> 8 & 0xFF)] ^ FT3[(m & 0xFF)] ^ arrayOfInt[12];
    
    i1 = FT0[(j >> 24 & 0xFF)] ^ FT1[(k >> 16 & 0xFF)] ^ FT2[(m >> 8 & 0xFF)] ^ FT3[(i & 0xFF)] ^ arrayOfInt[13];
    
    i2 = FT0[(k >> 24 & 0xFF)] ^ FT1[(m >> 16 & 0xFF)] ^ FT2[(i >> 8 & 0xFF)] ^ FT3[(j & 0xFF)] ^ arrayOfInt[14];
    
    i3 = FT0[(m >> 24 & 0xFF)] ^ FT1[(i >> 16 & 0xFF)] ^ FT2[(j >> 8 & 0xFF)] ^ FT3[(k & 0xFF)] ^ arrayOfInt[15];
    
    i = FT0[(n >> 24 & 0xFF)] ^ FT1[(i1 >> 16 & 0xFF)] ^ FT2[(i2 >> 8 & 0xFF)] ^ FT3[(i3 & 0xFF)] ^ arrayOfInt[16];
    
    j = FT0[(i1 >> 24 & 0xFF)] ^ FT1[(i2 >> 16 & 0xFF)] ^ FT2[(i3 >> 8 & 0xFF)] ^ FT3[(n & 0xFF)] ^ arrayOfInt[17];
    
    k = FT0[(i2 >> 24 & 0xFF)] ^ FT1[(i3 >> 16 & 0xFF)] ^ FT2[(n >> 8 & 0xFF)] ^ FT3[(i1 & 0xFF)] ^ arrayOfInt[18];
    
    m = FT0[(i3 >> 24 & 0xFF)] ^ FT1[(n >> 16 & 0xFF)] ^ FT2[(i1 >> 8 & 0xFF)] ^ FT3[(i2 & 0xFF)] ^ arrayOfInt[19];
    
    n = FT0[(i >> 24 & 0xFF)] ^ FT1[(j >> 16 & 0xFF)] ^ FT2[(k >> 8 & 0xFF)] ^ FT3[(m & 0xFF)] ^ arrayOfInt[20];
    
    i1 = FT0[(j >> 24 & 0xFF)] ^ FT1[(k >> 16 & 0xFF)] ^ FT2[(m >> 8 & 0xFF)] ^ FT3[(i & 0xFF)] ^ arrayOfInt[21];
    
    i2 = FT0[(k >> 24 & 0xFF)] ^ FT1[(m >> 16 & 0xFF)] ^ FT2[(i >> 8 & 0xFF)] ^ FT3[(j & 0xFF)] ^ arrayOfInt[22];
    
    i3 = FT0[(m >> 24 & 0xFF)] ^ FT1[(i >> 16 & 0xFF)] ^ FT2[(j >> 8 & 0xFF)] ^ FT3[(k & 0xFF)] ^ arrayOfInt[23];
    
    i = FT0[(n >> 24 & 0xFF)] ^ FT1[(i1 >> 16 & 0xFF)] ^ FT2[(i2 >> 8 & 0xFF)] ^ FT3[(i3 & 0xFF)] ^ arrayOfInt[24];
    
    j = FT0[(i1 >> 24 & 0xFF)] ^ FT1[(i2 >> 16 & 0xFF)] ^ FT2[(i3 >> 8 & 0xFF)] ^ FT3[(n & 0xFF)] ^ arrayOfInt[25];
    
    k = FT0[(i2 >> 24 & 0xFF)] ^ FT1[(i3 >> 16 & 0xFF)] ^ FT2[(n >> 8 & 0xFF)] ^ FT3[(i1 & 0xFF)] ^ arrayOfInt[26];
    
    m = FT0[(i3 >> 24 & 0xFF)] ^ FT1[(n >> 16 & 0xFF)] ^ FT2[(i1 >> 8 & 0xFF)] ^ FT3[(i2 & 0xFF)] ^ arrayOfInt[27];
    
    n = FT0[(i >> 24 & 0xFF)] ^ FT1[(j >> 16 & 0xFF)] ^ FT2[(k >> 8 & 0xFF)] ^ FT3[(m & 0xFF)] ^ arrayOfInt[28];
    
    i1 = FT0[(j >> 24 & 0xFF)] ^ FT1[(k >> 16 & 0xFF)] ^ FT2[(m >> 8 & 0xFF)] ^ FT3[(i & 0xFF)] ^ arrayOfInt[29];
    
    i2 = FT0[(k >> 24 & 0xFF)] ^ FT1[(m >> 16 & 0xFF)] ^ FT2[(i >> 8 & 0xFF)] ^ FT3[(j & 0xFF)] ^ arrayOfInt[30];
    
    i3 = FT0[(m >> 24 & 0xFF)] ^ FT1[(i >> 16 & 0xFF)] ^ FT2[(j >> 8 & 0xFF)] ^ FT3[(k & 0xFF)] ^ arrayOfInt[31];
    
    i = FT0[(n >> 24 & 0xFF)] ^ FT1[(i1 >> 16 & 0xFF)] ^ FT2[(i2 >> 8 & 0xFF)] ^ FT3[(i3 & 0xFF)] ^ arrayOfInt[32];
    
    j = FT0[(i1 >> 24 & 0xFF)] ^ FT1[(i2 >> 16 & 0xFF)] ^ FT2[(i3 >> 8 & 0xFF)] ^ FT3[(n & 0xFF)] ^ arrayOfInt[33];
    
    k = FT0[(i2 >> 24 & 0xFF)] ^ FT1[(i3 >> 16 & 0xFF)] ^ FT2[(n >> 8 & 0xFF)] ^ FT3[(i1 & 0xFF)] ^ arrayOfInt[34];
    
    m = FT0[(i3 >> 24 & 0xFF)] ^ FT1[(n >> 16 & 0xFF)] ^ FT2[(i1 >> 8 & 0xFF)] ^ FT3[(i2 & 0xFF)] ^ arrayOfInt[35];
    
    n = FT0[(i >> 24 & 0xFF)] ^ FT1[(j >> 16 & 0xFF)] ^ FT2[(k >> 8 & 0xFF)] ^ FT3[(m & 0xFF)] ^ arrayOfInt[36];
    
    i1 = FT0[(j >> 24 & 0xFF)] ^ FT1[(k >> 16 & 0xFF)] ^ FT2[(m >> 8 & 0xFF)] ^ FT3[(i & 0xFF)] ^ arrayOfInt[37];
    
    i2 = FT0[(k >> 24 & 0xFF)] ^ FT1[(m >> 16 & 0xFF)] ^ FT2[(i >> 8 & 0xFF)] ^ FT3[(j & 0xFF)] ^ arrayOfInt[38];
    
    i3 = FT0[(m >> 24 & 0xFF)] ^ FT1[(i >> 16 & 0xFF)] ^ FT2[(j >> 8 & 0xFF)] ^ FT3[(k & 0xFF)] ^ arrayOfInt[39];
    
    i = (FS[(n >> 24 & 0xFF)] << 24 | FS[(i1 >> 16 & 0xFF)] << 16 | FS[(i2 >> 8 & 0xFF)] << 8 | FS[(i3 & 0xFF)]) ^ arrayOfInt[40];
    
    j = (FS[(i1 >> 24 & 0xFF)] << 24 | FS[(i2 >> 16 & 0xFF)] << 16 | FS[(i3 >> 8 & 0xFF)] << 8 | FS[(n & 0xFF)]) ^ arrayOfInt[41];
    
    k = (FS[(i2 >> 24 & 0xFF)] << 24 | FS[(i3 >> 16 & 0xFF)] << 16 | FS[(n >> 8 & 0xFF)] << 8 | FS[(i1 & 0xFF)]) ^ arrayOfInt[42];
    
    m = (FS[(i3 >> 24 & 0xFF)] << 24 | FS[(n >> 16 & 0xFF)] << 16 | FS[(i1 >> 8 & 0xFF)] << 8 | FS[(i2 & 0xFF)]) ^ arrayOfInt[43];
    
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
    int[] arrayOfInt = this.decKey;
    int i = (paramArrayOfByte1[paramInt] << 24 | (paramArrayOfByte1[(paramInt + 1)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 2)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 3)] & 0xFF) ^ arrayOfInt[0];
    
    int j = (paramArrayOfByte1[(paramInt + 4)] << 24 | (paramArrayOfByte1[(paramInt + 5)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 6)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 7)] & 0xFF) ^ arrayOfInt[1];
    
    int k = (paramArrayOfByte1[(paramInt + 8)] << 24 | (paramArrayOfByte1[(paramInt + 9)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 10)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 11)] & 0xFF) ^ arrayOfInt[2];
    
    int m = (paramArrayOfByte1[(paramInt + 12)] << 24 | (paramArrayOfByte1[(paramInt + 13)] & 0xFF) << 16 | (paramArrayOfByte1[(paramInt + 14)] & 0xFF) << 8 | paramArrayOfByte1[(paramInt + 15)] & 0xFF) ^ arrayOfInt[3];
    
    int n = RT0[(i >> 24 & 0xFF)] ^ RT1[(m >> 16 & 0xFF)] ^ RT2[(k >> 8 & 0xFF)] ^ RT3[(j & 0xFF)] ^ arrayOfInt[4];
    
    int i1 = RT0[(j >> 24 & 0xFF)] ^ RT1[(i >> 16 & 0xFF)] ^ RT2[(m >> 8 & 0xFF)] ^ RT3[(k & 0xFF)] ^ arrayOfInt[5];
    
    int i2 = RT0[(k >> 24 & 0xFF)] ^ RT1[(j >> 16 & 0xFF)] ^ RT2[(i >> 8 & 0xFF)] ^ RT3[(m & 0xFF)] ^ arrayOfInt[6];
    
    int i3 = RT0[(m >> 24 & 0xFF)] ^ RT1[(k >> 16 & 0xFF)] ^ RT2[(j >> 8 & 0xFF)] ^ RT3[(i & 0xFF)] ^ arrayOfInt[7];
    
    i = RT0[(n >> 24 & 0xFF)] ^ RT1[(i3 >> 16 & 0xFF)] ^ RT2[(i2 >> 8 & 0xFF)] ^ RT3[(i1 & 0xFF)] ^ arrayOfInt[8];
    
    j = RT0[(i1 >> 24 & 0xFF)] ^ RT1[(n >> 16 & 0xFF)] ^ RT2[(i3 >> 8 & 0xFF)] ^ RT3[(i2 & 0xFF)] ^ arrayOfInt[9];
    
    k = RT0[(i2 >> 24 & 0xFF)] ^ RT1[(i1 >> 16 & 0xFF)] ^ RT2[(n >> 8 & 0xFF)] ^ RT3[(i3 & 0xFF)] ^ arrayOfInt[10];
    
    m = RT0[(i3 >> 24 & 0xFF)] ^ RT1[(i2 >> 16 & 0xFF)] ^ RT2[(i1 >> 8 & 0xFF)] ^ RT3[(n & 0xFF)] ^ arrayOfInt[11];
    
    n = RT0[(i >> 24 & 0xFF)] ^ RT1[(m >> 16 & 0xFF)] ^ RT2[(k >> 8 & 0xFF)] ^ RT3[(j & 0xFF)] ^ arrayOfInt[12];
    
    i1 = RT0[(j >> 24 & 0xFF)] ^ RT1[(i >> 16 & 0xFF)] ^ RT2[(m >> 8 & 0xFF)] ^ RT3[(k & 0xFF)] ^ arrayOfInt[13];
    
    i2 = RT0[(k >> 24 & 0xFF)] ^ RT1[(j >> 16 & 0xFF)] ^ RT2[(i >> 8 & 0xFF)] ^ RT3[(m & 0xFF)] ^ arrayOfInt[14];
    
    i3 = RT0[(m >> 24 & 0xFF)] ^ RT1[(k >> 16 & 0xFF)] ^ RT2[(j >> 8 & 0xFF)] ^ RT3[(i & 0xFF)] ^ arrayOfInt[15];
    
    i = RT0[(n >> 24 & 0xFF)] ^ RT1[(i3 >> 16 & 0xFF)] ^ RT2[(i2 >> 8 & 0xFF)] ^ RT3[(i1 & 0xFF)] ^ arrayOfInt[16];
    
    j = RT0[(i1 >> 24 & 0xFF)] ^ RT1[(n >> 16 & 0xFF)] ^ RT2[(i3 >> 8 & 0xFF)] ^ RT3[(i2 & 0xFF)] ^ arrayOfInt[17];
    
    k = RT0[(i2 >> 24 & 0xFF)] ^ RT1[(i1 >> 16 & 0xFF)] ^ RT2[(n >> 8 & 0xFF)] ^ RT3[(i3 & 0xFF)] ^ arrayOfInt[18];
    
    m = RT0[(i3 >> 24 & 0xFF)] ^ RT1[(i2 >> 16 & 0xFF)] ^ RT2[(i1 >> 8 & 0xFF)] ^ RT3[(n & 0xFF)] ^ arrayOfInt[19];
    
    n = RT0[(i >> 24 & 0xFF)] ^ RT1[(m >> 16 & 0xFF)] ^ RT2[(k >> 8 & 0xFF)] ^ RT3[(j & 0xFF)] ^ arrayOfInt[20];
    
    i1 = RT0[(j >> 24 & 0xFF)] ^ RT1[(i >> 16 & 0xFF)] ^ RT2[(m >> 8 & 0xFF)] ^ RT3[(k & 0xFF)] ^ arrayOfInt[21];
    
    i2 = RT0[(k >> 24 & 0xFF)] ^ RT1[(j >> 16 & 0xFF)] ^ RT2[(i >> 8 & 0xFF)] ^ RT3[(m & 0xFF)] ^ arrayOfInt[22];
    
    i3 = RT0[(m >> 24 & 0xFF)] ^ RT1[(k >> 16 & 0xFF)] ^ RT2[(j >> 8 & 0xFF)] ^ RT3[(i & 0xFF)] ^ arrayOfInt[23];
    
    i = RT0[(n >> 24 & 0xFF)] ^ RT1[(i3 >> 16 & 0xFF)] ^ RT2[(i2 >> 8 & 0xFF)] ^ RT3[(i1 & 0xFF)] ^ arrayOfInt[24];
    
    j = RT0[(i1 >> 24 & 0xFF)] ^ RT1[(n >> 16 & 0xFF)] ^ RT2[(i3 >> 8 & 0xFF)] ^ RT3[(i2 & 0xFF)] ^ arrayOfInt[25];
    
    k = RT0[(i2 >> 24 & 0xFF)] ^ RT1[(i1 >> 16 & 0xFF)] ^ RT2[(n >> 8 & 0xFF)] ^ RT3[(i3 & 0xFF)] ^ arrayOfInt[26];
    
    m = RT0[(i3 >> 24 & 0xFF)] ^ RT1[(i2 >> 16 & 0xFF)] ^ RT2[(i1 >> 8 & 0xFF)] ^ RT3[(n & 0xFF)] ^ arrayOfInt[27];
    
    n = RT0[(i >> 24 & 0xFF)] ^ RT1[(m >> 16 & 0xFF)] ^ RT2[(k >> 8 & 0xFF)] ^ RT3[(j & 0xFF)] ^ arrayOfInt[28];
    
    i1 = RT0[(j >> 24 & 0xFF)] ^ RT1[(i >> 16 & 0xFF)] ^ RT2[(m >> 8 & 0xFF)] ^ RT3[(k & 0xFF)] ^ arrayOfInt[29];
    
    i2 = RT0[(k >> 24 & 0xFF)] ^ RT1[(j >> 16 & 0xFF)] ^ RT2[(i >> 8 & 0xFF)] ^ RT3[(m & 0xFF)] ^ arrayOfInt[30];
    
    i3 = RT0[(m >> 24 & 0xFF)] ^ RT1[(k >> 16 & 0xFF)] ^ RT2[(j >> 8 & 0xFF)] ^ RT3[(i & 0xFF)] ^ arrayOfInt[31];
    
    i = RT0[(n >> 24 & 0xFF)] ^ RT1[(i3 >> 16 & 0xFF)] ^ RT2[(i2 >> 8 & 0xFF)] ^ RT3[(i1 & 0xFF)] ^ arrayOfInt[32];
    
    j = RT0[(i1 >> 24 & 0xFF)] ^ RT1[(n >> 16 & 0xFF)] ^ RT2[(i3 >> 8 & 0xFF)] ^ RT3[(i2 & 0xFF)] ^ arrayOfInt[33];
    
    k = RT0[(i2 >> 24 & 0xFF)] ^ RT1[(i1 >> 16 & 0xFF)] ^ RT2[(n >> 8 & 0xFF)] ^ RT3[(i3 & 0xFF)] ^ arrayOfInt[34];
    
    m = RT0[(i3 >> 24 & 0xFF)] ^ RT1[(i2 >> 16 & 0xFF)] ^ RT2[(i1 >> 8 & 0xFF)] ^ RT3[(n & 0xFF)] ^ arrayOfInt[35];
    
    n = RT0[(i >> 24 & 0xFF)] ^ RT1[(m >> 16 & 0xFF)] ^ RT2[(k >> 8 & 0xFF)] ^ RT3[(j & 0xFF)] ^ arrayOfInt[36];
    
    i1 = RT0[(j >> 24 & 0xFF)] ^ RT1[(i >> 16 & 0xFF)] ^ RT2[(m >> 8 & 0xFF)] ^ RT3[(k & 0xFF)] ^ arrayOfInt[37];
    
    i2 = RT0[(k >> 24 & 0xFF)] ^ RT1[(j >> 16 & 0xFF)] ^ RT2[(i >> 8 & 0xFF)] ^ RT3[(m & 0xFF)] ^ arrayOfInt[38];
    
    i3 = RT0[(m >> 24 & 0xFF)] ^ RT1[(k >> 16 & 0xFF)] ^ RT2[(j >> 8 & 0xFF)] ^ RT3[(i & 0xFF)] ^ arrayOfInt[39];
    
    i = (RS[(n >> 24 & 0xFF)] << 24 | RS[(i3 >> 16 & 0xFF)] << 16 | RS[(i2 >> 8 & 0xFF)] << 8 | RS[(i1 & 0xFF)]) ^ arrayOfInt[40];
    
    j = (RS[(i1 >> 24 & 0xFF)] << 24 | RS[(n >> 16 & 0xFF)] << 16 | RS[(i3 >> 8 & 0xFF)] << 8 | RS[(i2 & 0xFF)]) ^ arrayOfInt[41];
    
    k = (RS[(i2 >> 24 & 0xFF)] << 24 | RS[(i1 >> 16 & 0xFF)] << 16 | RS[(n >> 8 & 0xFF)] << 8 | RS[(i3 & 0xFF)]) ^ arrayOfInt[42];
    
    m = (RS[(i3 >> 24 & 0xFF)] << 24 | RS[(i2 >> 16 & 0xFF)] << 16 | RS[(i1 >> 8 & 0xFF)] << 8 | RS[(n & 0xFF)]) ^ arrayOfInt[43];
    
    paramArrayOfByte2[paramInt] = ((byte)(i >> 24));
    paramArrayOfByte2[(paramInt + 1)] = ((byte)(i >> 16));
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
}
