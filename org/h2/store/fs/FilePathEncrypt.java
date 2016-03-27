package org.h2.store.fs;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import org.h2.engine.Constants;
import org.h2.mvstore.DataUtils;
import org.h2.security.AES;
import org.h2.security.BlockCipher;
import org.h2.security.SHA256;
import org.h2.util.MathUtils;

public class FilePathEncrypt
  extends FilePathWrapper
{
  private static final String SCHEME = "encrypt";
  
  public static void register()
  {
    FilePath.register(new FilePathEncrypt());
  }
  
  public FileChannel open(String paramString)
    throws IOException
  {
    String[] arrayOfString = parse(this.name);
    FileChannel localFileChannel = FileUtils.open(arrayOfString[1], paramString);
    byte[] arrayOfByte = arrayOfString[0].getBytes(Constants.UTF8);
    return new FileEncrypt(this.name, arrayOfByte, localFileChannel);
  }
  
  public String getScheme()
  {
    return "encrypt";
  }
  
  protected String getPrefix()
  {
    String[] arrayOfString = parse(this.name);
    return getScheme() + ":" + arrayOfString[0] + ":";
  }
  
  public FilePath unwrap(String paramString)
  {
    return FilePath.get(parse(paramString)[1]);
  }
  
  public long size()
  {
    long l = getBase().size() - 4096L;
    l = Math.max(0L, l);
    if ((l & 0xFFF) != 0L) {
      l -= 4096L;
    }
    return l;
  }
  
  public OutputStream newOutputStream(boolean paramBoolean)
    throws IOException
  {
    return new FileChannelOutputStream(open("rw"), paramBoolean);
  }
  
  public InputStream newInputStream()
    throws IOException
  {
    return new FileChannelInputStream(open("r"), true);
  }
  
  private String[] parse(String paramString)
  {
    if (!paramString.startsWith(getScheme())) {
      throw new IllegalArgumentException(paramString + " doesn't start with " + getScheme());
    }
    paramString = paramString.substring(getScheme().length() + 1);
    int i = paramString.indexOf(':');
    if (i < 0) {
      throw new IllegalArgumentException(paramString + " doesn't contain encryption algorithm and password");
    }
    String str = paramString.substring(0, i);
    paramString = paramString.substring(i + 1);
    return new String[] { str, paramString };
  }
  
  public static byte[] getPasswordBytes(char[] paramArrayOfChar)
  {
    int i = paramArrayOfChar.length;
    byte[] arrayOfByte = new byte[i * 2];
    for (int j = 0; j < i; j++)
    {
      int k = paramArrayOfChar[j];
      arrayOfByte[(j + j)] = ((byte)(k >>> 8));
      arrayOfByte[(j + j + 1)] = ((byte)k);
    }
    return arrayOfByte;
  }
  
  public static class FileEncrypt
    extends FileBase
  {
    static final int BLOCK_SIZE = 4096;
    static final int BLOCK_SIZE_MASK = 4095;
    static final int HEADER_LENGTH = 4096;
    private static final byte[] HEADER = "H2encrypt\n".getBytes();
    private static final int SALT_POS = HEADER.length;
    private static final int SALT_LENGTH = 8;
    private static final int HASH_ITERATIONS = 10;
    private final FileChannel base;
    private long pos;
    private long size;
    private final String name;
    private FilePathEncrypt.XTS xts;
    private byte[] encryptionKey;
    
    public FileEncrypt(String paramString, byte[] paramArrayOfByte, FileChannel paramFileChannel)
    {
      this.name = paramString;
      this.base = paramFileChannel;
      this.encryptionKey = paramArrayOfByte;
    }
    
    private void init()
      throws IOException
    {
      if (this.xts != null) {
        return;
      }
      this.size = (this.base.size() - 4096L);
      int i = this.size < 0L ? 1 : 0;
      byte[] arrayOfByte;
      if (i != 0)
      {
        localObject = Arrays.copyOf(HEADER, 4096);
        arrayOfByte = MathUtils.secureRandomBytes(8);
        System.arraycopy(arrayOfByte, 0, localObject, SALT_POS, arrayOfByte.length);
        DataUtils.writeFully(this.base, 0L, ByteBuffer.wrap((byte[])localObject));
        this.size = 0L;
      }
      else
      {
        arrayOfByte = new byte[8];
        DataUtils.readFully(this.base, SALT_POS, ByteBuffer.wrap(arrayOfByte));
        if ((this.size & 0xFFF) != 0L) {
          this.size -= 4096L;
        }
      }
      Object localObject = new AES();
      ((AES)localObject).setKey(SHA256.getPBKDF2(this.encryptionKey, arrayOfByte, 10, 16));
      
      this.encryptionKey = null;
      this.xts = new FilePathEncrypt.XTS((BlockCipher)localObject);
    }
    
    protected void implCloseChannel()
      throws IOException
    {
      this.base.close();
    }
    
    public FileChannel position(long paramLong)
      throws IOException
    {
      this.pos = paramLong;
      return this;
    }
    
    public long position()
      throws IOException
    {
      return this.pos;
    }
    
    public int read(ByteBuffer paramByteBuffer)
      throws IOException
    {
      int i = read(paramByteBuffer, this.pos);
      if (i > 0) {
        this.pos += i;
      }
      return i;
    }
    
    public int read(ByteBuffer paramByteBuffer, long paramLong)
      throws IOException
    {
      int i = paramByteBuffer.remaining();
      if (i == 0) {
        return 0;
      }
      init();
      i = (int)Math.min(i, this.size - paramLong);
      if (paramLong >= this.size) {
        return -1;
      }
      if (paramLong < 0L) {
        throw new IllegalArgumentException("pos: " + paramLong);
      }
      if (((paramLong & 0xFFF) != 0L) || ((i & 0xFFF) != 0))
      {
        long l = paramLong / 4096L * 4096L;
        int j = (int)(paramLong - l);
        int k = (i + j + 4096 - 1) / 4096 * 4096;
        ByteBuffer localByteBuffer = ByteBuffer.allocate(k);
        readInternal(localByteBuffer, l, k);
        localByteBuffer.flip();
        localByteBuffer.limit(j + i);
        localByteBuffer.position(j);
        paramByteBuffer.put(localByteBuffer);
        return i;
      }
      readInternal(paramByteBuffer, paramLong, i);
      return i;
    }
    
    private void readInternal(ByteBuffer paramByteBuffer, long paramLong, int paramInt)
      throws IOException
    {
      int i = paramByteBuffer.position();
      readFully(this.base, paramLong + 4096L, paramByteBuffer);
      long l = paramLong / 4096L;
      while (paramInt > 0)
      {
        this.xts.decrypt(l++, 4096, paramByteBuffer.array(), paramByteBuffer.arrayOffset() + i);
        i += 4096;
        paramInt -= 4096;
      }
    }
    
    private static void readFully(FileChannel paramFileChannel, long paramLong, ByteBuffer paramByteBuffer)
      throws IOException
    {
      do
      {
        int i = paramFileChannel.read(paramByteBuffer, paramLong);
        if (i < 0) {
          throw new EOFException();
        }
        paramLong += i;
      } while (paramByteBuffer.remaining() > 0);
    }
    
    public int write(ByteBuffer paramByteBuffer, long paramLong)
      throws IOException
    {
      init();
      int i = paramByteBuffer.remaining();
      if (((paramLong & 0xFFF) != 0L) || ((i & 0xFFF) != 0))
      {
        l1 = paramLong / 4096L * 4096L;
        int j = (int)(paramLong - l1);
        int k = (i + j + 4096 - 1) / 4096 * 4096;
        ByteBuffer localByteBuffer = ByteBuffer.allocate(k);
        int m = (int)(this.size - l1 + 4096L - 1L) / 4096 * 4096;
        int n = Math.min(k, m);
        if (n > 0)
        {
          readInternal(localByteBuffer, l1, n);
          localByteBuffer.rewind();
        }
        localByteBuffer.limit(j + i);
        localByteBuffer.position(j);
        localByteBuffer.put(paramByteBuffer);
        localByteBuffer.limit(k);
        localByteBuffer.rewind();
        writeInternal(localByteBuffer, l1, k);
        long l2 = paramLong + i;
        this.size = Math.max(this.size, l2);
        int i1 = (int)(this.size & 0xFFF);
        if (i1 > 0)
        {
          localByteBuffer = ByteBuffer.allocate(i1);
          DataUtils.writeFully(this.base, l1 + 4096L + k, localByteBuffer);
        }
        return i;
      }
      writeInternal(paramByteBuffer, paramLong, i);
      long l1 = paramLong + i;
      this.size = Math.max(this.size, l1);
      return i;
    }
    
    private void writeInternal(ByteBuffer paramByteBuffer, long paramLong, int paramInt)
      throws IOException
    {
      ByteBuffer localByteBuffer = ByteBuffer.allocate(paramInt);
      localByteBuffer.put(paramByteBuffer);
      localByteBuffer.flip();
      long l = paramLong / 4096L;
      int i = 0;int j = paramInt;
      while (j > 0)
      {
        this.xts.encrypt(l++, 4096, localByteBuffer.array(), localByteBuffer.arrayOffset() + i);
        i += 4096;
        j -= 4096;
      }
      writeFully(this.base, paramLong + 4096L, localByteBuffer);
    }
    
    private static void writeFully(FileChannel paramFileChannel, long paramLong, ByteBuffer paramByteBuffer)
      throws IOException
    {
      int i = 0;
      do
      {
        int j = paramFileChannel.write(paramByteBuffer, paramLong + i);
        i += j;
      } while (paramByteBuffer.remaining() > 0);
    }
    
    public int write(ByteBuffer paramByteBuffer)
      throws IOException
    {
      int i = write(paramByteBuffer, this.pos);
      if (i > 0) {
        this.pos += i;
      }
      return i;
    }
    
    public long size()
      throws IOException
    {
      init();
      return this.size;
    }
    
    public FileChannel truncate(long paramLong)
      throws IOException
    {
      init();
      if (paramLong > this.size) {
        return this;
      }
      if (paramLong < 0L) {
        throw new IllegalArgumentException("newSize: " + paramLong);
      }
      int i = (int)(paramLong & 0xFFF);
      if (i > 0) {
        this.base.truncate(paramLong + 4096L + 4096L);
      } else {
        this.base.truncate(paramLong + 4096L);
      }
      this.size = paramLong;
      this.pos = Math.min(this.pos, this.size);
      return this;
    }
    
    public void force(boolean paramBoolean)
      throws IOException
    {
      this.base.force(paramBoolean);
    }
    
    public FileLock tryLock(long paramLong1, long paramLong2, boolean paramBoolean)
      throws IOException
    {
      return this.base.tryLock(paramLong1, paramLong2, paramBoolean);
    }
    
    public String toString()
    {
      return this.name;
    }
  }
  
  static class XTS
  {
    private static final int GF_128_FEEDBACK = 135;
    private static final int CIPHER_BLOCK_SIZE = 16;
    private final BlockCipher cipher;
    
    XTS(BlockCipher paramBlockCipher)
    {
      this.cipher = paramBlockCipher;
    }
    
    void encrypt(long paramLong, int paramInt1, byte[] paramArrayOfByte, int paramInt2)
    {
      byte[] arrayOfByte = initTweak(paramLong);
      for (int i = 0; i + 16 <= paramInt1; i += 16)
      {
        if (i > 0) {
          updateTweak(arrayOfByte);
        }
        xorTweak(paramArrayOfByte, i + paramInt2, arrayOfByte);
        this.cipher.encrypt(paramArrayOfByte, i + paramInt2, 16);
        xorTweak(paramArrayOfByte, i + paramInt2, arrayOfByte);
      }
      if (i < paramInt1)
      {
        updateTweak(arrayOfByte);
        swap(paramArrayOfByte, i + paramInt2, i - 16 + paramInt2, paramInt1 - i);
        xorTweak(paramArrayOfByte, i - 16 + paramInt2, arrayOfByte);
        this.cipher.encrypt(paramArrayOfByte, i - 16 + paramInt2, 16);
        xorTweak(paramArrayOfByte, i - 16 + paramInt2, arrayOfByte);
      }
    }
    
    void decrypt(long paramLong, int paramInt1, byte[] paramArrayOfByte, int paramInt2)
    {
      byte[] arrayOfByte1 = initTweak(paramLong);byte[] arrayOfByte2 = arrayOfByte1;
      for (int i = 0; i + 16 <= paramInt1; i += 16)
      {
        if (i > 0)
        {
          updateTweak(arrayOfByte1);
          if ((i + 16 + 16 > paramInt1) && (i + 16 < paramInt1))
          {
            arrayOfByte2 = Arrays.copyOf(arrayOfByte1, 16);
            updateTweak(arrayOfByte1);
          }
        }
        xorTweak(paramArrayOfByte, i + paramInt2, arrayOfByte1);
        this.cipher.decrypt(paramArrayOfByte, i + paramInt2, 16);
        xorTweak(paramArrayOfByte, i + paramInt2, arrayOfByte1);
      }
      if (i < paramInt1)
      {
        swap(paramArrayOfByte, i, i - 16 + paramInt2, paramInt1 - i + paramInt2);
        xorTweak(paramArrayOfByte, i - 16 + paramInt2, arrayOfByte2);
        this.cipher.decrypt(paramArrayOfByte, i - 16 + paramInt2, 16);
        xorTweak(paramArrayOfByte, i - 16 + paramInt2, arrayOfByte2);
      }
    }
    
    private byte[] initTweak(long paramLong)
    {
      byte[] arrayOfByte = new byte[16];
      for (int i = 0; i < 16; paramLong >>>= 8)
      {
        arrayOfByte[i] = ((byte)(int)(paramLong & 0xFF));i++;
      }
      this.cipher.encrypt(arrayOfByte, 0, 16);
      return arrayOfByte;
    }
    
    private static void xorTweak(byte[] paramArrayOfByte1, int paramInt, byte[] paramArrayOfByte2)
    {
      for (int i = 0; i < 16; i++)
      {
        int tmp12_11 = (paramInt + i);paramArrayOfByte1[tmp12_11] = ((byte)(paramArrayOfByte1[tmp12_11] ^ paramArrayOfByte2[i]));
      }
    }
    
    private static void updateTweak(byte[] paramArrayOfByte)
    {
      int i = 0;int j = 0;
      for (int k = 0; k < 16; k++)
      {
        j = (byte)(paramArrayOfByte[k] >> 7 & 0x1);
        paramArrayOfByte[k] = ((byte)((paramArrayOfByte[k] << 1) + i & 0xFF));
        i = j;
      }
      if (j != 0)
      {
        int tmp51_50 = 0;paramArrayOfByte[tmp51_50] = ((byte)(paramArrayOfByte[tmp51_50] ^ 0x87));
      }
    }
    
    private static void swap(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3)
    {
      for (int i = 0; i < paramInt3; i++)
      {
        int j = paramArrayOfByte[(paramInt1 + i)];
        paramArrayOfByte[(paramInt1 + i)] = paramArrayOfByte[(paramInt2 + i)];
        paramArrayOfByte[(paramInt2 + i)] = j;
      }
    }
  }
}
