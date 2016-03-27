package eu.the5zig.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarOutputStream;

public class CountingJarOutputStream
  extends JarOutputStream
{
  private long count;
  
  public CountingJarOutputStream(OutputStream out)
    throws IOException
  {
    super(out);
  }
  
  public long getCount()
  {
    return this.count;
  }
  
  public void write(int b)
    throws IOException
  {
    super.write(b);
    this.count += 1L;
  }
  
  public void write(byte[] b)
    throws IOException
  {
    super.write(b);
    this.count += b.length;
  }
  
  public void write(byte[] b, int off, int len)
    throws IOException
  {
    super.write(b, off, len);
    this.count += len;
  }
}
