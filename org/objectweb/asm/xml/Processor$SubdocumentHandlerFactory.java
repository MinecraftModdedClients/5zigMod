package org.objectweb.asm.xml;

import org.xml.sax.ContentHandler;

final class Processor$SubdocumentHandlerFactory
  implements Processor.ContentHandlerFactory
{
  private final ContentHandler subdocumentHandler;
  
  Processor$SubdocumentHandlerFactory(ContentHandler paramContentHandler)
  {
    this.subdocumentHandler = paramContentHandler;
  }
  
  public final ContentHandler createContentHandler()
  {
    return this.subdocumentHandler;
  }
}
