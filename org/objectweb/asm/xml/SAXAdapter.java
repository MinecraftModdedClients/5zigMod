package org.objectweb.asm.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class SAXAdapter
{
  private final ContentHandler h;
  
  protected SAXAdapter(ContentHandler paramContentHandler)
  {
    this.h = paramContentHandler;
  }
  
  protected ContentHandler getContentHandler()
  {
    return this.h;
  }
  
  protected void addDocumentStart()
  {
    try
    {
      this.h.startDocument();
    }
    catch (SAXException localSAXException)
    {
      throw new RuntimeException(localSAXException.getMessage(), localSAXException.getException());
    }
  }
  
  protected void addDocumentEnd()
  {
    try
    {
      this.h.endDocument();
    }
    catch (SAXException localSAXException)
    {
      throw new RuntimeException(localSAXException.getMessage(), localSAXException.getException());
    }
  }
  
  protected final void addStart(String paramString, Attributes paramAttributes)
  {
    try
    {
      this.h.startElement("", paramString, paramString, paramAttributes);
    }
    catch (SAXException localSAXException)
    {
      throw new RuntimeException(localSAXException.getMessage(), localSAXException.getException());
    }
  }
  
  protected final void addEnd(String paramString)
  {
    try
    {
      this.h.endElement("", paramString, paramString);
    }
    catch (SAXException localSAXException)
    {
      throw new RuntimeException(localSAXException.getMessage(), localSAXException.getException());
    }
  }
  
  protected final void addElement(String paramString, Attributes paramAttributes)
  {
    addStart(paramString, paramAttributes);
    addEnd(paramString);
  }
}
