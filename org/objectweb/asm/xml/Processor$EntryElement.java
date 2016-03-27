package org.objectweb.asm.xml;

import java.io.IOException;
import java.io.OutputStream;

abstract interface Processor$EntryElement
{
  public abstract OutputStream openEntry(String paramString)
    throws IOException;
  
  public abstract void closeEntry()
    throws IOException;
}
