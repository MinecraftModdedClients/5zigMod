package org.objectweb.asm.xml;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.objectweb.asm.ClassReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLReaderFactory;

public class Processor
{
  public static final int BYTECODE = 1;
  public static final int MULTI_XML = 2;
  public static final int SINGLE_XML = 3;
  private static final String SINGLE_XML_NAME = "classes.xml";
  private final int inRepresentation;
  private final int outRepresentation;
  private final InputStream input;
  private final OutputStream output;
  private final Source xslt;
  private int n = 0;
  
  public Processor(int paramInt1, int paramInt2, InputStream paramInputStream, OutputStream paramOutputStream, Source paramSource)
  {
    this.inRepresentation = paramInt1;
    this.outRepresentation = paramInt2;
    this.input = paramInputStream;
    this.output = paramOutputStream;
    this.xslt = paramSource;
  }
  
  public int process()
    throws TransformerException, IOException, SAXException
  {
    ZipInputStream localZipInputStream = new ZipInputStream(this.input);
    ZipOutputStream localZipOutputStream = new ZipOutputStream(this.output);
    OutputStreamWriter localOutputStreamWriter = new OutputStreamWriter(localZipOutputStream);
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    TransformerFactory localTransformerFactory = TransformerFactory.newInstance();
    if ((!localTransformerFactory.getFeature("http://javax.xml.transform.sax.SAXSource/feature")) || (!localTransformerFactory.getFeature("http://javax.xml.transform.sax.SAXResult/feature"))) {
      return 0;
    }
    SAXTransformerFactory localSAXTransformerFactory = (SAXTransformerFactory)localTransformerFactory;
    Templates localTemplates = null;
    if (this.xslt != null) {
      localTemplates = localSAXTransformerFactory.newTemplates(this.xslt);
    }
    Processor.EntryElement localEntryElement = getEntryElement(localZipOutputStream);
    Object localObject1 = null;
    Object localObject2;
    switch (this.outRepresentation)
    {
    case 1: 
      localObject1 = new Processor.OutputSlicingHandler(new Processor.ASMContentHandlerFactory(localZipOutputStream), localEntryElement, false);
      break;
    case 2: 
      localObject1 = new Processor.OutputSlicingHandler(new Processor.SAXWriterFactory(localOutputStreamWriter, true), localEntryElement, true);
      break;
    case 3: 
      localObject2 = new ZipEntry("classes.xml");
      localZipOutputStream.putNextEntry((ZipEntry)localObject2);
      localObject1 = new Processor.SAXWriter(localOutputStreamWriter, false);
    }
    if (localTemplates == null) {
      localObject2 = localObject1;
    } else {
      localObject2 = new Processor.InputSlicingHandler("class", (ContentHandler)localObject1, new Processor.TransformerHandlerFactory(localSAXTransformerFactory, localTemplates, (ContentHandler)localObject1));
    }
    Processor.SubdocumentHandlerFactory localSubdocumentHandlerFactory = new Processor.SubdocumentHandlerFactory((ContentHandler)localObject2);
    if ((localObject2 != null) && (this.inRepresentation != 3))
    {
      ((ContentHandler)localObject2).startDocument();
      ((ContentHandler)localObject2).startElement("", "classes", "classes", new AttributesImpl());
    }
    ZipEntry localZipEntry;
    for (int i = 0; (localZipEntry = localZipInputStream.getNextEntry()) != null; i++)
    {
      update(localZipEntry.getName(), this.n++);
      if (isClassEntry(localZipEntry))
      {
        processEntry(localZipInputStream, localZipEntry, localSubdocumentHandlerFactory);
      }
      else
      {
        OutputStream localOutputStream = localEntryElement.openEntry(getName(localZipEntry));
        copyEntry(localZipInputStream, localOutputStream);
        localEntryElement.closeEntry();
      }
    }
    if ((localObject2 != null) && (this.inRepresentation != 3))
    {
      ((ContentHandler)localObject2).endElement("", "classes", "classes");
      ((ContentHandler)localObject2).endDocument();
    }
    if (this.outRepresentation == 3) {
      localZipOutputStream.closeEntry();
    }
    localZipOutputStream.flush();
    localZipOutputStream.close();
    return i;
  }
  
  private void copyEntry(InputStream paramInputStream, OutputStream paramOutputStream)
    throws IOException
  {
    if (this.outRepresentation == 3) {
      return;
    }
    byte[] arrayOfByte = new byte['ࠀ'];
    int i;
    while ((i = paramInputStream.read(arrayOfByte)) != -1) {
      paramOutputStream.write(arrayOfByte, 0, i);
    }
  }
  
  private boolean isClassEntry(ZipEntry paramZipEntry)
  {
    String str = paramZipEntry.getName();
    return ((this.inRepresentation == 3) && (str.equals("classes.xml"))) || (str.endsWith(".class")) || (str.endsWith(".class.xml"));
  }
  
  private void processEntry(ZipInputStream paramZipInputStream, ZipEntry paramZipEntry, Processor.ContentHandlerFactory paramContentHandlerFactory)
  {
    ContentHandler localContentHandler = paramContentHandlerFactory.createContentHandler();
    try
    {
      boolean bool = this.inRepresentation == 3;
      Object localObject;
      if (this.inRepresentation == 1)
      {
        localObject = new ClassReader(readEntry(paramZipInputStream, paramZipEntry));
        ((ClassReader)localObject).accept(new SAXClassAdapter(localContentHandler, bool), 0);
      }
      else
      {
        localObject = XMLReaderFactory.createXMLReader();
        ((XMLReader)localObject).setContentHandler(localContentHandler);
        ((XMLReader)localObject).parse(new InputSource(bool ? new Processor.ProtectedInputStream(paramZipInputStream) : new ByteArrayInputStream(readEntry(paramZipInputStream, paramZipEntry))));
      }
    }
    catch (Exception localException)
    {
      update(paramZipEntry.getName(), 0);
      update(localException, 0);
    }
  }
  
  private Processor.EntryElement getEntryElement(ZipOutputStream paramZipOutputStream)
  {
    if (this.outRepresentation == 3) {
      return new Processor.SingleDocElement(paramZipOutputStream);
    }
    return new Processor.ZipEntryElement(paramZipOutputStream);
  }
  
  private String getName(ZipEntry paramZipEntry)
  {
    String str = paramZipEntry.getName();
    if (isClassEntry(paramZipEntry)) {
      if ((this.inRepresentation != 1) && (this.outRepresentation == 1)) {
        str = str.substring(0, str.length() - 4);
      } else if ((this.inRepresentation == 1) && (this.outRepresentation != 1)) {
        str = str + ".xml";
      }
    }
    return str;
  }
  
  private static byte[] readEntry(InputStream paramInputStream, ZipEntry paramZipEntry)
    throws IOException
  {
    long l = paramZipEntry.getSize();
    int j;
    if (l > -1L)
    {
      localObject = new byte[(int)l];
      int i = 0;
      while ((j = paramInputStream.read((byte[])localObject, i, localObject.length - i)) > 0) {
        i += j;
      }
      return (byte[])localObject;
    }
    Object localObject = new ByteArrayOutputStream();
    byte[] arrayOfByte = new byte['က'];
    while ((j = paramInputStream.read(arrayOfByte)) != -1) {
      ((ByteArrayOutputStream)localObject).write(arrayOfByte, 0, j);
    }
    return ((ByteArrayOutputStream)localObject).toByteArray();
  }
  
  protected void update(Object paramObject, int paramInt)
  {
    if ((paramObject instanceof Throwable)) {
      ((Throwable)paramObject).printStackTrace();
    } else if (paramInt % 100 == 0) {
      System.err.println(paramInt + " " + paramObject);
    }
  }
  
  public static void main(String[] paramArrayOfString)
    throws Exception
  {
    if (paramArrayOfString.length < 2)
    {
      showUsage();
      return;
    }
    int i = getRepresentation(paramArrayOfString[0]);
    int j = getRepresentation(paramArrayOfString[1]);
    Object localObject = System.in;
    BufferedOutputStream localBufferedOutputStream = new BufferedOutputStream(System.out);
    StreamSource localStreamSource = null;
    for (int k = 2; k < paramArrayOfString.length; k++) {
      if ("-in".equals(paramArrayOfString[k]))
      {
        localObject = new FileInputStream(paramArrayOfString[(++k)]);
      }
      else if ("-out".equals(paramArrayOfString[k]))
      {
        localBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(paramArrayOfString[(++k)]));
      }
      else if ("-xslt".equals(paramArrayOfString[k]))
      {
        localStreamSource = new StreamSource(new FileInputStream(paramArrayOfString[(++k)]));
      }
      else
      {
        showUsage();
        return;
      }
    }
    if ((i == 0) || (j == 0))
    {
      showUsage();
      return;
    }
    Processor localProcessor = new Processor(i, j, (InputStream)localObject, localBufferedOutputStream, localStreamSource);
    long l1 = System.currentTimeMillis();
    int m = localProcessor.process();
    long l2 = System.currentTimeMillis();
    System.err.println(m);
    System.err.println(l2 - l1 + "ms  " + 1000.0F * m / (float)(l2 - l1) + " resources/sec");
  }
  
  private static int getRepresentation(String paramString)
  {
    if ("code".equals(paramString)) {
      return 1;
    }
    if ("xml".equals(paramString)) {
      return 2;
    }
    if ("singlexml".equals(paramString)) {
      return 3;
    }
    return 0;
  }
  
  private static void showUsage()
  {
    System.err.println("Usage: Main <in format> <out format> [-in <input jar>] [-out <output jar>] [-xslt <xslt fiel>]");
    System.err.println("  when -in or -out is omitted sysin and sysout would be used");
    System.err.println("  <in format> and <out format> - code | xml | singlexml");
  }
}
