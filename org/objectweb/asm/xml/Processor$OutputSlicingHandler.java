package org.objectweb.asm.xml;

import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

final class Processor$OutputSlicingHandler
  extends DefaultHandler
{
  private final String subdocumentRoot = "class";
  private Processor.ContentHandlerFactory subdocumentHandlerFactory;
  private final Processor.EntryElement entryElement;
  private boolean isXml;
  private boolean subdocument = false;
  private ContentHandler subdocumentHandler;
  
  Processor$OutputSlicingHandler(Processor.ContentHandlerFactory paramContentHandlerFactory, Processor.EntryElement paramEntryElement, boolean paramBoolean)
  {
    this.subdocumentHandlerFactory = paramContentHandlerFactory;
    this.entryElement = paramEntryElement;
    this.isXml = paramBoolean;
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
      String str = paramAttributes.getValue("name");
      if ((str == null) || (str.length() == 0)) {
        throw new SAXException("Class element without name attribute.");
      }
      try
      {
        this.entryElement.openEntry(str + ".class");
      }
      catch (IOException localIOException)
      {
        throw new SAXException(localIOException.toString(), localIOException);
      }
      this.subdocumentHandler = this.subdocumentHandlerFactory.createContentHandler();
      this.subdocumentHandler.startDocument();
      this.subdocumentHandler.startElement(paramString1, paramString2, paramString3, paramAttributes);
      this.subdocument = true;
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
        try
        {
          this.entryElement.closeEntry();
        }
        catch (IOException localIOException)
        {
          throw new SAXException(localIOException.toString(), localIOException);
        }
      }
    }
  }
  
  public final void startDocument()
    throws SAXException
  {}
  
  public final void endDocument()
    throws SAXException
  {}
  
  public final void characters(char[] paramArrayOfChar, int paramInt1, int paramInt2)
    throws SAXException
  {
    if (this.subdocument) {
      this.subdocumentHandler.characters(paramArrayOfChar, paramInt1, paramInt2);
    }
  }
}
