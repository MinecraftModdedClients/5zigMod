package org.h2.security;

import java.util.Arrays;

public class SHA256
{
  private static final int[] K = { 1116352408, 1899447441, -1245643825, -373957723, 961987163, 1508970993, -1841331548, -1424204075, -670586216, 310598401, 607225278, 1426881987, 1925078388, -2132889090, -1680079193, -1046744716, -459576895, -272742522, 264347078, 604807628, 770255983, 1249150122, 1555081692, 1996064986, -1740746414, -1473132947, -1341970488, -1084653625, -958395405, -710438585, 113926993, 338241895, 666307205, 773529912, 1294757372, 1396182291, 1695183700, 1986661051, -2117940946, -1838011259, -1564481375, -1474664885, -1035236496, -949202525, -778901479, -694614492, -200395387, 275423344, 430227734, 506948616, 659060556, 883997877, 958139571, 1322822218, 1537002063, 1747873779, 1955562222, 2024104815, -2067236844, -1933114872, -1866530822, -1538233109, -1090935817, -965641998 };
  private static final int[] HH = { 1779033703, -1150833019, 1013904242, -1521486534, 1359893119, -1694144372, 528734635, 1541459225 };
  private final byte[] result = new byte[32];
  private final int[] w = new int[64];
  private final int[] hh = new int[8];
  
  public static byte[] getHashWithSalt(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    byte[] arrayOfByte = new byte[paramArrayOfByte1.length + paramArrayOfByte2.length];
    System.arraycopy(paramArrayOfByte1, 0, arrayOfByte, 0, paramArrayOfByte1.length);
    System.arraycopy(paramArrayOfByte2, 0, arrayOfByte, paramArrayOfByte1.length, paramArrayOfByte2.length);
    return getHash(arrayOfByte, true);
  }
  
  public static byte[] getKeyPasswordHash(String paramString, char[] paramArrayOfChar)
  {
    String str = paramString + "@";
    byte[] arrayOfByte = new byte[2 * (str.length() + paramArrayOfChar.length)];
    int i = 0;
    int j = 0;
    int m;
    for (int k = str.length(); j < k; j++)
    {
      m = str.charAt(j);
      arrayOfByte[(i++)] = ((byte)(m >> 8));
      arrayOfByte[(i++)] = ((byte)m);
    }
    for (int n : paramArrayOfChar)
    {
      arrayOfByte[(i++)] = ((byte)(n >> 8));
      arrayOfByte[(i++)] = ((byte)n);
    }
    Arrays.fill(paramArrayOfChar, '\000');
    return getHash(arrayOfByte, true);
  }
  
  public static byte[] getHMAC(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    paramArrayOfByte1 = normalizeKeyForHMAC(paramArrayOfByte1);
    int i = paramArrayOfByte2.length;
    int j = 64 + Math.max(32, i);
    int k = getIntCount(j);
    byte[] arrayOfByte1 = new byte[k * 4];
    int[] arrayOfInt = new int[k];
    SHA256 localSHA256 = new SHA256();
    byte[] arrayOfByte2 = new byte[64 + i];
    byte[] arrayOfByte3 = new byte[96];
    localSHA256.calculateHMAC(paramArrayOfByte1, paramArrayOfByte2, i, arrayOfByte2, arrayOfByte3, arrayOfByte1, arrayOfInt);
    return localSHA256.result;
  }
  
  private void calculateHMAC(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4, byte[] paramArrayOfByte5, int[] paramArrayOfInt)
  {
    Arrays.fill(paramArrayOfByte3, 0, 64, (byte)54);
    xor(paramArrayOfByte3, paramArrayOfByte1, 64);
    System.arraycopy(paramArrayOfByte2, 0, paramArrayOfByte3, 64, paramInt);
    calculateHash(paramArrayOfByte3, 64 + paramInt, paramArrayOfByte5, paramArrayOfInt);
    Arrays.fill(paramArrayOfByte4, 0, 64, (byte)92);
    xor(paramArrayOfByte4, paramArrayOfByte1, 64);
    System.arraycopy(this.result, 0, paramArrayOfByte4, 64, 32);
    calculateHash(paramArrayOfByte4, 96, paramArrayOfByte5, paramArrayOfInt);
  }
  
  private static byte[] normalizeKeyForHMAC(byte[] paramArrayOfByte)
  {
    if (paramArrayOfByte.length > 64) {
      paramArrayOfByte = getHash(paramArrayOfByte, false);
    }
    if (paramArrayOfByte.length < 64) {
      paramArrayOfByte = Arrays.copyOf(paramArrayOfByte, 64);
    }
    return paramArrayOfByte;
  }
  
  private static void xor(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt)
  {
    for (int i = 0; i < paramInt; i++)
    {
      int tmp9_8 = i;paramArrayOfByte1[tmp9_8] = ((byte)(paramArrayOfByte1[tmp9_8] ^ paramArrayOfByte2[i]));
    }
  }
  
  public static byte[] getPBKDF2(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, int paramInt2)
  {
    byte[] arrayOfByte1 = new byte[paramInt2];
    byte[] arrayOfByte2 = normalizeKeyForHMAC(paramArrayOfByte1);
    SHA256 localSHA256 = new SHA256();
    int i = 64 + Math.max(32, paramArrayOfByte2.length + 4);
    byte[] arrayOfByte3 = new byte[i];
    int j = getIntCount(i);
    byte[] arrayOfByte4 = new byte[j * 4];
    int[] arrayOfInt = new int[j];
    byte[] arrayOfByte5 = new byte[64 + i];
    byte[] arrayOfByte6 = new byte[96];
    int k = 1;
    for (int m = 0; m < paramInt2; m += 32)
    {
      for (int n = 0; n < paramInt1; n++)
      {
        if (n == 0)
        {
          System.arraycopy(paramArrayOfByte2, 0, arrayOfByte3, 0, paramArrayOfByte2.length);
          writeInt(arrayOfByte3, paramArrayOfByte2.length, k);
          i = paramArrayOfByte2.length + 4;
        }
        else
        {
          System.arraycopy(localSHA256.result, 0, arrayOfByte3, 0, 32);
          i = 32;
        }
        localSHA256.calculateHMAC(arrayOfByte2, arrayOfByte3, i, arrayOfByte5, arrayOfByte6, arrayOfByte4, arrayOfInt);
        for (int i1 = 0; (i1 < 32) && (i1 + m < paramInt2); i1++)
        {
          int tmp193_192 = (i1 + m); byte[] tmp193_186 = arrayOfByte1;tmp193_186[tmp193_192] = ((byte)(tmp193_186[tmp193_192] ^ localSHA256.result[i1]));
        }
      }
      k++;
    }
    Arrays.fill(paramArrayOfByte1, (byte)0);
    Arrays.fill(arrayOfByte2, (byte)0);
    return arrayOfByte1;
  }
  
  public static byte[] getHash(byte[] paramArrayOfByte, boolean paramBoolean)
  {
    int i = paramArrayOfByte.length;
    int j = getIntCount(i);
    byte[] arrayOfByte = new byte[j * 4];
    int[] arrayOfInt = new int[j];
    SHA256 localSHA256 = new SHA256();
    localSHA256.calculateHash(paramArrayOfByte, i, arrayOfByte, arrayOfInt);
    if (paramBoolean)
    {
      localSHA256.fillWithNull();
      Arrays.fill(arrayOfInt, 0);
      Arrays.fill(arrayOfByte, (byte)0);
      Arrays.fill(paramArrayOfByte, (byte)0);
    }
    return localSHA256.result;
  }
  
  private static int getIntCount(int paramInt)
  {
    return (paramInt + 9 + 63) / 64 * 16;
  }
  
  private void fillWithNull()
  {
    Arrays.fill(this.w, 0);
    Arrays.fill(this.hh, 0);
  }
  
  private void calculateHash(byte[] paramArrayOfByte1, int paramInt, byte[] paramArrayOfByte2, int[] paramArrayOfInt)
  {
    int[] arrayOfInt1 = this.w;
    int[] arrayOfInt2 = this.hh;
    byte[] arrayOfByte = this.result;
    int i = getIntCount(paramInt);
    System.arraycopy(paramArrayOfByte1, 0, paramArrayOfByte2, 0, paramInt);
    paramArrayOfByte2[paramInt] = Byte.MIN_VALUE;
    Arrays.fill(paramArrayOfByte2, paramInt + 1, i * 4, (byte)0);
    int j = 0;
    for (int k = 0; k < i; k++)
    {
      paramArrayOfInt[k] = readInt(paramArrayOfByte2, j);j += 4;
    }
    paramArrayOfInt[(i - 2)] = (paramInt >>> 29);
    paramArrayOfInt[(i - 1)] = (paramInt << 3);
    System.arraycopy(HH, 0, arrayOfInt2, 0, 8);
    for (j = 0; j < i; j += 16)
    {
      for (k = 0; k < 16; k++) {
        arrayOfInt1[k] = paramArrayOfInt[(j + k)];
      }
      for (k = 16; k < 64; k++)
      {
        m = arrayOfInt1[(k - 2)];
        n = rot(m, 17) ^ rot(m, 19) ^ m >>> 10;
        m = arrayOfInt1[(k - 15)];
        i1 = rot(m, 7) ^ rot(m, 18) ^ m >>> 3;
        arrayOfInt1[k] = (n + arrayOfInt1[(k - 7)] + i1 + arrayOfInt1[(k - 16)]);
      }
      k = arrayOfInt2[0];int m = arrayOfInt2[1];int n = arrayOfInt2[2];int i1 = arrayOfInt2[3];
      int i2 = arrayOfInt2[4];int i3 = arrayOfInt2[5];int i4 = arrayOfInt2[6];int i5 = arrayOfInt2[7];
      for (int i6 = 0; i6 < 64; i6++)
      {
        int i7 = i5 + (rot(i2, 6) ^ rot(i2, 11) ^ rot(i2, 25)) + (i2 & i3 ^ (i2 ^ 0xFFFFFFFF) & i4) + K[i6] + arrayOfInt1[i6];
        
        int i8 = (rot(k, 2) ^ rot(k, 13) ^ rot(k, 22)) + (k & m ^ k & n ^ m & n);
        
        i5 = i4;
        i4 = i3;
        i3 = i2;
        i2 = i1 + i7;
        i1 = n;
        n = m;
        m = k;
        k = i7 + i8;
      }
      arrayOfInt2[0] += k;
      arrayOfInt2[1] += m;
      arrayOfInt2[2] += n;
      arrayOfInt2[3] += i1;
      arrayOfInt2[4] += i2;
      arrayOfInt2[5] += i3;
      arrayOfInt2[6] += i4;
      arrayOfInt2[7] += i5;
    }
    for (j = 0; j < 8; j++) {
      writeInt(arrayOfByte, j * 4, arrayOfInt2[j]);
    }
  }
  
  private static int rot(int paramInt1, int paramInt2)
  {
    return Integer.rotateRight(paramInt1, paramInt2);
  }
  
  private static int readInt(byte[] paramArrayOfByte, int paramInt)
  {
    return ((paramArrayOfByte[paramInt] & 0xFF) << 24) + ((paramArrayOfByte[(paramInt + 1)] & 0xFF) << 16) + ((paramArrayOfByte[(paramInt + 2)] & 0xFF) << 8) + (paramArrayOfByte[(paramInt + 3)] & 0xFF);
  }
  
  private static void writeInt(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    paramArrayOfByte[paramInt1] = ((byte)(paramInt2 >> 24));
    paramArrayOfByte[(paramInt1 + 1)] = ((byte)(paramInt2 >> 16));
    paramArrayOfByte[(paramInt1 + 2)] = ((byte)(paramInt2 >> 8));
    paramArrayOfByte[(paramInt1 + 3)] = ((byte)paramInt2);
  }
}
