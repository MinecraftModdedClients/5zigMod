package org.objectweb.asm.xml;

import java.io.IOException;
import java.io.Writer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

final class Processor$SAXWriter
  extends DefaultHandler
  implements LexicalHandler
{
  private static final char[] OFF = "                                                                                                        ".toCharArray();
  private Writer w;
  private final boolean optimizeEmptyElements;
  private boolean openElement = false;
  private int ident = 0;
  
  Processor$SAXWriter(Writer paramWriter, boolean paramBoolean)
  {
    this.w = paramWriter;
    this.optimizeEmptyElements = paramBoolean;
  }
  
  public final void startElement(String paramString1, String paramString2, String paramString3, Attributes paramAttributes)
    throws SAXException
  {
    try
    {
      closeElement();
      writeIdent();
      this.w.write('<' + paramString3);
      if ((paramAttributes != null) && (paramAttributes.getLength() > 0)) {
        writeAttributes(paramAttributes);
      }
      if (this.optimizeEmptyElements) {
        this.openElement = true;
      } else {
        this.w.write(">\n");
      }
      this.ident += 2;
    }
    catch (IOException localIOException)
    {
      throw new SAXException(localIOException);
    }
  }
  
  public final void endElement(String paramString1, String paramString2, String paramString3)
    throws SAXException
  {
    this.ident -= 2;
    try
    {
      if (this.openElement)
      {
        this.w.write("/>\n");
        this.openElement = false;
      }
      else
      {
        writeIdent();
        this.w.write("</" + paramString3 + ">\n");
      }
    }
    catch (IOException localIOException)
    {
      throw new SAXException(localIOException);
    }
  }
  
  public final void endDocument()
    throws SAXException
  {
    try
    {
      this.w.flush();
    }
    catch (IOException localIOException)
    {
      throw new SAXException(localIOException);
    }
  }
  
  public final void comment(char[] paramArrayOfChar, int paramInt1, int paramInt2)
    throws SAXException
  {
    try
    {
      closeElement();
      writeIdent();
      this.w.write("<!-- ");
      this.w.write(paramArrayOfChar, paramInt1, paramInt2);
      this.w.write(" -->\n");
    }
    catch (IOException localIOException)
    {
      throw new SAXException(localIOException);
    }
  }
  
  public final void startDTD(String paramString1, String paramString2, String paramString3)
    throws SAXException
  {}
  
  public final void endDTD()
    throws SAXException
  {}
  
  public final void startEntity(String paramString)
    throws SAXException
  {}
  
  public final void endEntity(String paramString)
    throws SAXException
  {}
  
  public final void startCDATA()
    throws SAXException
  {}
  
  public final void endCDATA()
    throws SAXException
  {}
  
  private final void writeAttributes(Attributes paramAttributes)
    throws IOException
  {
    StringBuffer localStringBuffer = new StringBuffer();
    int i = paramAttributes.getLength();
    for (int j = 0; j < i; j++) {
      localStringBuffer.append(' ').append(paramAttributes.getLocalName(j)).append("=\"").append(esc(paramAttributes.getValue(j))).append('"');
    }
    this.w.write(localStringBuffer.toString());
  }
  
  private static final String esc(String paramString)
  {
    StringBuffer localStringBuffer = new StringBuffer(paramString.length());
    for (int i = 0; i < paramString.length(); i++)
    {
      char c = paramString.charAt(i);
      switch (c)
      {
      case '&': 
        localStringBuffer.append("&amp;");
        break;
      case '<': 
        localStringBuffer.append("&lt;");
        break;
      case '>': 
        localStringBuffer.append("&gt;");
        break;
      case '"': 
        localStringBuffer.append("&quot;");
        break;
      default: 
        if (c > '') {
          localStringBuffer.append("&#").append(Integer.toString(c)).append(';');
        } else {
          localStringBuffer.append(c);
        }
        break;
      }
    }
    return localStringBuffer.toString();
  }
  
  private final void writeIdent()
    throws IOException
  {
    int i = this.ident;
    while (i > 0) {
      if (i > OFF.length)
      {
        this.w.write(OFF);
        i -= OFF.length;
      }
      else
      {
        this.w.write(OFF, 0, i);
        i = 0;
      }
    }
  }
  
  private final void closeElement()
    throws IOException
  {
    if (this.openElement) {
      this.w.write(">\n");
    }
    this.openElement = false;
  }
  
  static {}
  
  static void _clinit_() {}
}
