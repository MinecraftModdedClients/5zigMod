package org.objectweb.asm.xml;

import org.xml.sax.ContentHandler;

abstract interface Processor$ContentHandlerFactory
{
  public abstract ContentHandler createContentHandler();
}
