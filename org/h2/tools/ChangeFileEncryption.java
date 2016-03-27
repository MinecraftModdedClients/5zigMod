package org.h2.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.h2.message.DbException;
import org.h2.security.SHA256;
import org.h2.store.FileLister;
import org.h2.store.FileStore;
import org.h2.store.fs.FileChannelInputStream;
import org.h2.store.fs.FileChannelOutputStream;
import org.h2.store.fs.FilePath;
import org.h2.store.fs.FilePathEncrypt;
import org.h2.store.fs.FilePathEncrypt.FileEncrypt;
import org.h2.store.fs.FileUtils;
import org.h2.util.Tool;

public class ChangeFileEncryption
  extends Tool
{
  private String directory;
  private String cipherType;
  private byte[] decrypt;
  private byte[] encrypt;
  private byte[] decryptKey;
  private byte[] encryptKey;
  
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new ChangeFileEncryption().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    String str1 = ".";
    String str2 = null;
    char[] arrayOfChar1 = null;
    char[] arrayOfChar2 = null;
    String str3 = null;
    boolean bool = false;
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str4 = paramVarArgs[i];
      if (str4.equals("-dir"))
      {
        str1 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-cipher"))
      {
        str2 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-db"))
      {
        str3 = paramVarArgs[(++i)];
      }
      else if (str4.equals("-decrypt"))
      {
        arrayOfChar1 = paramVarArgs[(++i)].toCharArray();
      }
      else if (str4.equals("-encrypt"))
      {
        arrayOfChar2 = paramVarArgs[(++i)].toCharArray();
      }
      else if (str4.equals("-quiet"))
      {
        bool = true;
      }
      else
      {
        if ((str4.equals("-help")) || (str4.equals("-?")))
        {
          showUsage();
          return;
        }
        showUsageAndThrowUnsupportedOption(str4);
      }
    }
    if (((arrayOfChar2 == null) && (arrayOfChar1 == null)) || (str2 == null))
    {
      showUsage();
      throw new SQLException("Encryption or decryption password not set, or cipher not set");
    }
    try
    {
      process(str1, str3, str2, arrayOfChar1, arrayOfChar2, bool);
    }
    catch (Exception localException)
    {
      throw DbException.toSQLException(localException);
    }
  }
  
  private static byte[] getFileEncryptionKey(char[] paramArrayOfChar)
  {
    if (paramArrayOfChar == null) {
      return null;
    }
    return SHA256.getKeyPasswordHash("file", paramArrayOfChar);
  }
  
  public static void execute(String paramString1, String paramString2, String paramString3, char[] paramArrayOfChar1, char[] paramArrayOfChar2, boolean paramBoolean)
    throws SQLException
  {
    try
    {
      new ChangeFileEncryption().process(paramString1, paramString2, paramString3, paramArrayOfChar1, paramArrayOfChar2, paramBoolean);
    }
    catch (Exception localException)
    {
      throw DbException.toSQLException(localException);
    }
  }
  
  private void process(String paramString1, String paramString2, String paramString3, char[] paramArrayOfChar1, char[] paramArrayOfChar2, boolean paramBoolean)
    throws SQLException
  {
    paramString1 = FileLister.getDir(paramString1);
    ChangeFileEncryption localChangeFileEncryption = new ChangeFileEncryption();
    if (paramArrayOfChar2 != null)
    {
      for (int k : paramArrayOfChar2) {
        if (k == 32) {
          throw new SQLException("The file password may not contain spaces");
        }
      }
      localChangeFileEncryption.encryptKey = FilePathEncrypt.getPasswordBytes(paramArrayOfChar2);
      localChangeFileEncryption.encrypt = getFileEncryptionKey(paramArrayOfChar2);
    }
    if (paramArrayOfChar1 != null)
    {
      localChangeFileEncryption.decryptKey = FilePathEncrypt.getPasswordBytes(paramArrayOfChar1);
      localChangeFileEncryption.decrypt = getFileEncryptionKey(paramArrayOfChar1);
    }
    localChangeFileEncryption.out = this.out;
    localChangeFileEncryption.directory = paramString1;
    localChangeFileEncryption.cipherType = paramString3;
    
    ??? = FileLister.getDatabaseFiles(paramString1, paramString2, true);
    FileLister.tryUnlockDatabase((List)???, "encryption");
    ??? = FileLister.getDatabaseFiles(paramString1, paramString2, false);
    if ((((ArrayList)???).size() == 0) && (!paramBoolean)) {
      printNoDatabaseFilesFound(paramString1, paramString2);
    }
    for (Iterator localIterator = ((ArrayList)???).iterator(); localIterator.hasNext();)
    {
      str1 = (String)localIterator.next();
      String str2 = paramString1 + "/temp.db";
      FileUtils.delete(str2);
      FileUtils.move(str1, str2);
      FileUtils.move(str2, str1);
    }
    String str1;
    for (localIterator = ((ArrayList)???).iterator(); localIterator.hasNext();)
    {
      str1 = (String)localIterator.next();
      if (!FileUtils.isDirectory(str1)) {
        localChangeFileEncryption.process(str1);
      }
    }
  }
  
  private void process(String paramString)
  {
    if (paramString.endsWith(".mv.db"))
    {
      try
      {
        copy(paramString);
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, "Error encrypting / decrypting file " + paramString);
      }
      return;
    }
    FileStore localFileStore;
    if (this.decrypt == null) {
      localFileStore = FileStore.open(null, paramString, "r");
    } else {
      localFileStore = FileStore.open(null, paramString, "r", this.cipherType, this.decrypt);
    }
    try
    {
      localFileStore.init();
      copy(paramString, localFileStore, this.encrypt);
    }
    finally
    {
      localFileStore.closeSilently();
    }
  }
  
  private void copy(String paramString)
    throws IOException
  {
    if (FileUtils.isDirectory(paramString)) {
      return;
    }
    Object localObject1 = FilePath.get(paramString).open("r");
    Object localObject2 = null;
    String str = this.directory + "/temp.db";
    try
    {
      if (this.decryptKey != null) {
        localObject1 = new FilePathEncrypt.FileEncrypt(paramString, this.decryptKey, (FileChannel)localObject1);
      }
      FileChannelInputStream localFileChannelInputStream = new FileChannelInputStream((FileChannel)localObject1, true);
      FileUtils.delete(str);
      localObject2 = FilePath.get(str).open("rw");
      if (this.encryptKey != null) {
        localObject2 = new FilePathEncrypt.FileEncrypt(str, this.encryptKey, (FileChannel)localObject2);
      }
      FileChannelOutputStream localFileChannelOutputStream = new FileChannelOutputStream((FileChannel)localObject2, true);
      byte[] arrayOfByte = new byte['က'];
      long l1 = ((FileChannel)localObject1).size();
      long l2 = l1;
      long l3 = System.currentTimeMillis();
      while (l1 > 0L)
      {
        if (System.currentTimeMillis() - l3 > 1000L)
        {
          this.out.println(paramString + ": " + (100L - 100L * l1 / l2) + "%");
          l3 = System.currentTimeMillis();
        }
        int i = (int)Math.min(arrayOfByte.length, l1);
        i = localFileChannelInputStream.read(arrayOfByte, 0, i);
        localFileChannelOutputStream.write(arrayOfByte, 0, i);
        l1 -= i;
      }
      localFileChannelInputStream.close();
      localFileChannelOutputStream.close();
    }
    finally
    {
      ((FileChannel)localObject1).close();
      if (localObject2 != null) {
        ((FileChannel)localObject2).close();
      }
    }
    FileUtils.delete(paramString);
    FileUtils.move(str, paramString);
  }
  
  private void copy(String paramString, FileStore paramFileStore, byte[] paramArrayOfByte)
  {
    if (FileUtils.isDirectory(paramString)) {
      return;
    }
    String str = this.directory + "/temp.db";
    FileUtils.delete(str);
    FileStore localFileStore;
    if (paramArrayOfByte == null) {
      localFileStore = FileStore.open(null, str, "rw");
    } else {
      localFileStore = FileStore.open(null, str, "rw", this.cipherType, paramArrayOfByte);
    }
    localFileStore.init();
    byte[] arrayOfByte = new byte['က'];
    long l1 = paramFileStore.length() - 48L;
    long l2 = l1;
    paramFileStore.seek(48L);
    localFileStore.seek(48L);
    long l3 = System.currentTimeMillis();
    while (l1 > 0L)
    {
      if (System.currentTimeMillis() - l3 > 1000L)
      {
        this.out.println(paramString + ": " + (100L - 100L * l1 / l2) + "%");
        l3 = System.currentTimeMillis();
      }
      int i = (int)Math.min(arrayOfByte.length, l1);
      paramFileStore.readFully(arrayOfByte, 0, i);
      localFileStore.write(arrayOfByte, 0, i);
      l1 -= i;
    }
    paramFileStore.close();
    localFileStore.close();
    FileUtils.delete(paramString);
    FileUtils.move(str, paramString);
  }
}
