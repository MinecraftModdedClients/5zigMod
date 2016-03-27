package org.objectweb.asm.xml;

import java.io.IOException;
import java.io.OutputStream;

final class Processor$SingleDocElement
  implements Processor.EntryElement
{
  private final OutputStream os;
  
  Processor$SingleDocElement(OutputStream paramOutputStream)
  {
    this.os = paramOutputStream;
  }
  
  public OutputStream openEntry(String paramString)
    throws IOException
  {
    return this.os;
  }
  
  public void closeEntry()
    throws IOException
  {
    this.os.flush();
  }
}
