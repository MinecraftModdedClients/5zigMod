package org.objectweb.asm.xml;

import java.io.OutputStream;
import org.objectweb.asm.ClassWriter;
import org.xml.sax.ContentHandler;

final class Processor$ASMContentHandlerFactory
  implements Processor.ContentHandlerFactory
{
  final OutputStream os;
  
  Processor$ASMContentHandlerFactory(OutputStream paramOutputStream)
  {
    this.os = paramOutputStream;
  }
  
  public final ContentHandler createContentHandler()
  {
    ClassWriter localClassWriter = new ClassWriter(1);
    return new Processor.ASMContentHandlerFactory.1(this, localClassWriter, localClassWriter);
  }
}
