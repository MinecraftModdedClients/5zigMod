package org.h2.store.fs;

import java.io.IOException;
import java.nio.channels.FileChannel;

public class FilePathNioMapped
  extends FilePathNio
{
  public FileChannel open(String paramString)
    throws IOException
  {
    return new FileNioMapped(this.name.substring(getScheme().length() + 1), paramString);
  }
  
  public String getScheme()
  {
    return "nioMapped";
  }
}
