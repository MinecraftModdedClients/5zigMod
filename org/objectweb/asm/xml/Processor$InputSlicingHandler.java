package org.objectweb.asm.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

final class Processor$InputSlicingHandler
  extends DefaultHandler
{
  private String subdocumentRoot;
  private final ContentHandler rootHandler;
  private Processor.ContentHandlerFactory subdocumentHandlerFactory;
  private boolean subdocument = false;
  private ContentHandler subdocumentHandler;
  
  Processor$InputSlicingHandler(String paramString, ContentHandler paramContentHandler, Processor.ContentHandlerFactory paramContentHandlerFactory)
  {
    this.subdocumentRoot = paramString;
    this.rootHandler = paramContentHandler;
    this.subdocumentHandlerFactory = paramContentHandlerFactory;
  }
  
  public final void startElement(String paramString1, String paramString2, String paramString3, Attributes paramAttributes)
    throws SAXException
  {
    if (this.subdocument)
    {
      this.subdocumentHandler.startElement(paramString1, paramString2, paramString3, paramAttributes);
    }
    else if (paramString2.equals(this.subdocumentRoot))
    {
      this.subdocumentHandler = this.subdocumentHandlerFactory.createContentHandler();
      this.subdocumentHandler.startDocument();
      this.subdocumentHandler.startElement(paramString1, paramString2, paramString3, paramAttributes);
      this.subdocument = true;
    }
    else if (this.rootHandler != null)
    {
      this.rootHandler.startElement(paramString1, paramString2, paramString3, paramAttributes);
    }
  }
  
  public final void endElement(String paramString1, String paramString2, String paramString3)
    throws SAXException
  {
    if (this.subdocument)
    {
      this.subdocumentHandler.endElement(paramString1, paramString2, paramString3);
      if (paramString2.equals(this.subdocumentRoot))
      {
        this.subdocumentHandler.endDocument();
        this.subdocument = false;
      }
    }
    else if (this.rootHandler != null)
    {
      this.rootHandler.endElement(paramString1, paramString2, paramString3);
    }
  }
  
  public final void startDocument()
    throws SAXException
  {
    if (this.rootHandler != null) {
      this.rootHandler.startDocument();
    }
  }
  
  public final void endDocument()
    throws SAXException
  {
    if (this.rootHandler != null) {
      this.rootHandler.endDocument();
    }
  }
  
  public final void characters(char[] paramArrayOfChar, int paramInt1, int paramInt2)
    throws SAXException
  {
    if (this.subdocument) {
      this.subdocumentHandler.characters(paramArrayOfChar, paramInt1, paramInt2);
    } else if (this.rootHandler != null) {
      this.rootHandler.characters(paramArrayOfChar, paramInt1, paramInt2);
    }
  }
}
