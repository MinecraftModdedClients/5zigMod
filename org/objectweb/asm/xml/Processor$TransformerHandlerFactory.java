package org.objectweb.asm.xml;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import org.xml.sax.ContentHandler;

final class Processor$TransformerHandlerFactory
  implements Processor.ContentHandlerFactory
{
  private SAXTransformerFactory saxtf;
  private final Templates templates;
  private ContentHandler outputHandler;
  
  Processor$TransformerHandlerFactory(SAXTransformerFactory paramSAXTransformerFactory, Templates paramTemplates, ContentHandler paramContentHandler)
  {
    this.saxtf = paramSAXTransformerFactory;
    this.templates = paramTemplates;
    this.outputHandler = paramContentHandler;
  }
  
  public final ContentHandler createContentHandler()
  {
    try
    {
      TransformerHandler localTransformerHandler = this.saxtf.newTransformerHandler(this.templates);
      localTransformerHandler.setResult(new SAXResult(this.outputHandler));
      return localTransformerHandler;
    }
    catch (TransformerConfigurationException localTransformerConfigurationException)
    {
      throw new RuntimeException(localTransformerConfigurationException.toString());
    }
  }
}
