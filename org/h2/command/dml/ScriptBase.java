package org.h2.command.dml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.h2.api.JavaObjectSerializer;
import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.security.SHA256;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.store.FileStoreInputStream;
import org.h2.store.FileStoreOutputStream;
import org.h2.store.LobStorageBackend;
import org.h2.store.fs.FileUtils;
import org.h2.tools.CompressTool;
import org.h2.util.IOUtils;
import org.h2.util.SmallLRUCache;
import org.h2.util.TempFileDeleter;
import org.h2.value.Value;

abstract class ScriptBase
  extends Prepared
  implements DataHandler
{
  private static final String SCRIPT_SQL = "script.sql";
  protected OutputStream out;
  protected InputStream in;
  private Expression fileNameExpr;
  private Expression password;
  private String fileName;
  private String cipher;
  private FileStore store;
  private String compressionAlgorithm;
  
  ScriptBase(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setCipher(String paramString)
  {
    this.cipher = paramString;
  }
  
  private boolean isEncrypted()
  {
    return this.cipher != null;
  }
  
  public void setPassword(Expression paramExpression)
  {
    this.password = paramExpression;
  }
  
  public void setFileNameExpr(Expression paramExpression)
  {
    this.fileNameExpr = paramExpression;
  }
  
  protected String getFileName()
  {
    if ((this.fileNameExpr != null) && (this.fileName == null))
    {
      this.fileName = this.fileNameExpr.optimize(this.session).getValue(this.session).getString();
      if ((this.fileName == null) || (this.fileName.trim().length() == 0)) {
        this.fileName = "script.sql";
      }
      this.fileName = (SysProperties.getScriptDirectory() + this.fileName);
    }
    return this.fileName;
  }
  
  public boolean isTransactional()
  {
    return false;
  }
  
  void deleteStore()
  {
    String str = getFileName();
    if (str != null) {
      FileUtils.delete(str);
    }
  }
  
  private void initStore()
  {
    Database localDatabase = this.session.getDatabase();
    byte[] arrayOfByte = null;
    if ((this.cipher != null) && (this.password != null))
    {
      localObject = this.password.optimize(this.session).getValue(this.session).getString().toCharArray();
      
      arrayOfByte = SHA256.getKeyPasswordHash("script", (char[])localObject);
    }
    Object localObject = getFileName();
    this.store = FileStore.open(localDatabase, (String)localObject, "rw", this.cipher, arrayOfByte);
    this.store.setCheckedWriting(false);
    this.store.init();
  }
  
  void openOutput()
  {
    String str = getFileName();
    if (str == null) {
      return;
    }
    if (isEncrypted())
    {
      initStore();
      this.out = new FileStoreOutputStream(this.store, this, this.compressionAlgorithm);
      
      this.out = new BufferedOutputStream(this.out, 131072);
    }
    else
    {
      OutputStream localOutputStream;
      try
      {
        localOutputStream = FileUtils.newOutputStream(str, false);
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, null);
      }
      this.out = new BufferedOutputStream(localOutputStream, 4096);
      this.out = CompressTool.wrapOutputStream(this.out, this.compressionAlgorithm, "script.sql");
    }
  }
  
  void openInput()
  {
    String str = getFileName();
    if (str == null) {
      return;
    }
    if (isEncrypted())
    {
      initStore();
      this.in = new FileStoreInputStream(this.store, this, this.compressionAlgorithm != null, false);
    }
    else
    {
      InputStream localInputStream;
      try
      {
        localInputStream = FileUtils.newInputStream(str);
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, str);
      }
      this.in = new BufferedInputStream(localInputStream, 4096);
      this.in = CompressTool.wrapInputStream(this.in, this.compressionAlgorithm, "script.sql");
      if (this.in == null) {
        throw DbException.get(90124, "script.sql in " + str);
      }
    }
  }
  
  void closeIO()
  {
    IOUtils.closeSilently(this.out);
    this.out = null;
    IOUtils.closeSilently(this.in);
    this.in = null;
    if (this.store != null)
    {
      this.store.closeSilently();
      this.store = null;
    }
  }
  
  public boolean needRecompile()
  {
    return false;
  }
  
  public String getDatabasePath()
  {
    return null;
  }
  
  public FileStore openFile(String paramString1, String paramString2, boolean paramBoolean)
  {
    return null;
  }
  
  public void checkPowerOff()
  {
    this.session.getDatabase().checkPowerOff();
  }
  
  public void checkWritingAllowed()
  {
    this.session.getDatabase().checkWritingAllowed();
  }
  
  public int getMaxLengthInplaceLob()
  {
    return this.session.getDatabase().getMaxLengthInplaceLob();
  }
  
  public TempFileDeleter getTempFileDeleter()
  {
    return this.session.getDatabase().getTempFileDeleter();
  }
  
  public String getLobCompressionAlgorithm(int paramInt)
  {
    return this.session.getDatabase().getLobCompressionAlgorithm(paramInt);
  }
  
  public void setCompressionAlgorithm(String paramString)
  {
    this.compressionAlgorithm = paramString;
  }
  
  public Object getLobSyncObject()
  {
    return this;
  }
  
  public SmallLRUCache<String, String[]> getLobFileListCache()
  {
    return null;
  }
  
  public LobStorageBackend getLobStorage()
  {
    return null;
  }
  
  public int readLob(long paramLong1, byte[] paramArrayOfByte1, long paramLong2, byte[] paramArrayOfByte2, int paramInt1, int paramInt2)
  {
    throw DbException.throwInternalError();
  }
  
  public JavaObjectSerializer getJavaObjectSerializer()
  {
    return this.session.getDataHandler().getJavaObjectSerializer();
  }
}
