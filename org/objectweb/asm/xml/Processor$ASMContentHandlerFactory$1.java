package org.objectweb.asm.xml;

import java.io.IOException;
import java.io.OutputStream;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.xml.sax.SAXException;

class Processor$ASMContentHandlerFactory$1
  extends ASMContentHandler
{
  final Processor.ASMContentHandlerFactory this$0;
  
  Processor$ASMContentHandlerFactory$1(Processor.ASMContentHandlerFactory paramASMContentHandlerFactory, ClassVisitor paramClassVisitor, ClassWriter paramClassWriter)
  {
    super(paramClassVisitor);
  }
  
  public void endDocument()
    throws SAXException
  {
    try
    {
      this.this$0.os.write(this.val$cw.toByteArray());
    }
    catch (IOException localIOException)
    {
      throw new SAXException(localIOException);
    }
  }
}
