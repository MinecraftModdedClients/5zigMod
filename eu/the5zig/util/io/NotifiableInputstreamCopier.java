package eu.the5zig.util.io;

import eu.the5zig.util.Callback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NotifiableInputstreamCopier
  implements Runnable
{
  private final InputStream[] src;
  private final File dest;
  private final Callback<Float> callback;
  
  private NotifiableInputstreamCopier(Callback<Float> callback, InputStream[] src, File dest)
  {
    this.callback = callback;
    this.src = src;
    this.dest = dest;
    run();
  }
  
  public void run()
  {
    long fileSize = 0L;
    for (InputStream is : this.src) {
      try
      {
        fileSize += is.available();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    ??? = this.src;??? = ???.length;
    for (??? = 0; ??? < ???;)
    {
      InputStream is = ???[???];
      FileOutputStream output = null;
      try
      {
        output = new FileOutputStream(this.dest);
        byte[] buf = new byte[' '];
        int bytesRead;
        while ((bytesRead = is.read(buf)) > 0)
        {
          output.write(buf, 0, bytesRead);
          if (this.callback != null) {
            this.callback.call(Float.valueOf((float)this.dest.length() / (float)fileSize));
          }
        }
        if (is != null) {
          try
          {
            is.close();
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
        }
        if (output != null) {
          try
          {
            output.close();
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
        }
        ???++;
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      finally
      {
        if (is != null) {
          try
          {
            is.close();
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
        }
        if (output != null) {
          try
          {
            output.close();
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
        }
      }
    }
  }
  
  public static NotifiableInputstreamCopier copy(Callback<Float> callback, InputStream[] src, File dest)
  {
    return new NotifiableInputstreamCopier(callback, src, dest);
  }
}
