package org.h2.store.fs;

class FilePathNioMemLZF
  extends FilePathNioMem
{
  boolean compressed()
  {
    return true;
  }
  
  public String getScheme()
  {
    return "nioMemLZF";
  }
}
