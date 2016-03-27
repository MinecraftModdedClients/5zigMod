package org.objectweb.asm.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class Processor$ZipEntryElement
  implements Processor.EntryElement
{
  private ZipOutputStream zos;
  
  Processor$ZipEntryElement(ZipOutputStream paramZipOutputStream)
  {
    this.zos = paramZipOutputStream;
  }
  
  public OutputStream openEntry(String paramString)
    throws IOException
  {
    ZipEntry localZipEntry = new ZipEntry(paramString);
    this.zos.putNextEntry(localZipEntry);
    return this.zos;
  }
  
  public void closeEntry()
    throws IOException
  {
    this.zos.flush();
    this.zos.closeEntry();
  }
}
