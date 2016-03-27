package org.h2.store.fs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class FilePathRec
  extends FilePathWrapper
{
  private static final FilePathRec INSTANCE = new FilePathRec();
  private static Recorder recorder;
  private boolean trace;
  
  public static void register()
  {
    FilePath.register(INSTANCE);
  }
  
  public static void setRecorder(Recorder paramRecorder)
  {
    recorder = paramRecorder;
  }
  
  public boolean createFile()
  {
    log(2, this.name);
    return super.createFile();
  }
  
  public FilePath createTempFile(String paramString, boolean paramBoolean1, boolean paramBoolean2)
    throws IOException
  {
    log(3, unwrap(this.name) + ":" + paramString + ":" + paramBoolean1 + ":" + paramBoolean2);
    
    return super.createTempFile(paramString, paramBoolean1, paramBoolean2);
  }
  
  public void delete()
  {
    log(4, this.name);
    super.delete();
  }
  
  public FileChannel open(String paramString)
    throws IOException
  {
    return new FileRec(this, super.open(paramString), this.name);
  }
  
  public OutputStream newOutputStream(boolean paramBoolean)
    throws IOException
  {
    log(5, this.name);
    return super.newOutputStream(paramBoolean);
  }
  
  public void moveTo(FilePath paramFilePath, boolean paramBoolean)
  {
    log(6, unwrap(this.name) + ":" + unwrap(paramFilePath.name));
    super.moveTo(paramFilePath, paramBoolean);
  }
  
  public boolean isTrace()
  {
    return this.trace;
  }
  
  public void setTrace(boolean paramBoolean)
  {
    this.trace = paramBoolean;
  }
  
  void log(int paramInt, String paramString)
  {
    log(paramInt, paramString, null, 0L);
  }
  
  void log(int paramInt, String paramString, byte[] paramArrayOfByte, long paramLong)
  {
    if (recorder != null) {
      recorder.log(paramInt, paramString, paramArrayOfByte, paramLong);
    }
  }
  
  public String getScheme()
  {
    return "rec";
  }
}
