package org.h2.security;

import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.util.MathUtils;

public class SecureFileStore
  extends FileStore
{
  private byte[] key;
  private final BlockCipher cipher;
  private final BlockCipher cipherForInitVector;
  private byte[] buffer = new byte[4];
  private long pos;
  private final byte[] bufferForInitVector;
  private final int keyIterations;
  
  public SecureFileStore(DataHandler paramDataHandler, String paramString1, String paramString2, String paramString3, byte[] paramArrayOfByte, int paramInt)
  {
    super(paramDataHandler, paramString1, paramString2);
    this.key = paramArrayOfByte;
    this.cipher = CipherFactory.getBlockCipher(paramString3);
    this.cipherForInitVector = CipherFactory.getBlockCipher(paramString3);
    this.keyIterations = paramInt;
    this.bufferForInitVector = new byte[16];
  }
  
  protected byte[] generateSalt()
  {
    return MathUtils.secureRandomBytes(16);
  }
  
  protected void initKey(byte[] paramArrayOfByte)
  {
    this.key = SHA256.getHashWithSalt(this.key, paramArrayOfByte);
    for (int i = 0; i < this.keyIterations; i++) {
      this.key = SHA256.getHash(this.key, true);
    }
    this.cipher.setKey(this.key);
    this.key = SHA256.getHash(this.key, true);
    this.cipherForInitVector.setKey(this.key);
  }
  
  protected void writeDirect(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    super.write(paramArrayOfByte, paramInt1, paramInt2);
    this.pos += paramInt2;
  }
  
  public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if (this.buffer.length < paramArrayOfByte.length) {
      this.buffer = new byte[paramInt2];
    }
    System.arraycopy(paramArrayOfByte, paramInt1, this.buffer, 0, paramInt2);
    xorInitVector(this.buffer, 0, paramInt2, this.pos);
    this.cipher.encrypt(this.buffer, 0, paramInt2);
    super.write(this.buffer, 0, paramInt2);
    this.pos += paramInt2;
  }
  
  protected void readFullyDirect(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    super.readFully(paramArrayOfByte, paramInt1, paramInt2);
    this.pos += paramInt2;
  }
  
  public void readFully(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    super.readFully(paramArrayOfByte, paramInt1, paramInt2);
    for (int i = 0; i < paramInt2; i++) {
      if (paramArrayOfByte[i] != 0)
      {
        this.cipher.decrypt(paramArrayOfByte, paramInt1, paramInt2);
        xorInitVector(paramArrayOfByte, paramInt1, paramInt2, this.pos);
        break;
      }
    }
    this.pos += paramInt2;
  }
  
  public void seek(long paramLong)
  {
    this.pos = paramLong;
    super.seek(paramLong);
  }
  
  private void xorInitVector(byte[] paramArrayOfByte, int paramInt1, int paramInt2, long paramLong)
  {
    byte[] arrayOfByte = this.bufferForInitVector;
    while (paramInt2 > 0)
    {
      for (int i = 0; i < 16; i += 8)
      {
        long l = paramLong + i >>> 3;
        arrayOfByte[i] = ((byte)(int)(l >> 56));
        arrayOfByte[(i + 1)] = ((byte)(int)(l >> 48));
        arrayOfByte[(i + 2)] = ((byte)(int)(l >> 40));
        arrayOfByte[(i + 3)] = ((byte)(int)(l >> 32));
        arrayOfByte[(i + 4)] = ((byte)(int)(l >> 24));
        arrayOfByte[(i + 5)] = ((byte)(int)(l >> 16));
        arrayOfByte[(i + 6)] = ((byte)(int)(l >> 8));
        arrayOfByte[(i + 7)] = ((byte)(int)l);
      }
      this.cipherForInitVector.encrypt(arrayOfByte, 0, 16);
      for (i = 0; i < 16; i++)
      {
        int tmp174_173 = (paramInt1 + i); byte[] tmp174_169 = paramArrayOfByte;tmp174_169[tmp174_173] = ((byte)(tmp174_169[tmp174_173] ^ arrayOfByte[i]));
      }
      paramLong += 16L;
      paramInt1 += 16;
      paramInt2 -= 16;
    }
  }
}
