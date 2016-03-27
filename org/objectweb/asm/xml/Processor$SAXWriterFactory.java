package org.objectweb.asm.xml;

import java.io.Writer;
import org.xml.sax.ContentHandler;

final class Processor$SAXWriterFactory
  implements Processor.ContentHandlerFactory
{
  private final Writer w;
  private final boolean optimizeEmptyElements;
  
  Processor$SAXWriterFactory(Writer paramWriter, boolean paramBoolean)
  {
    this.w = paramWriter;
    this.optimizeEmptyElements = paramBoolean;
  }
  
  public final ContentHandler createContentHandler()
  {
    return new Processor.SAXWriter(this.w, this.optimizeEmptyElements);
  }
}
