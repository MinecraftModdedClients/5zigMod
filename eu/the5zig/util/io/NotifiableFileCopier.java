package eu.the5zig.util.io;

import eu.the5zig.util.Callback;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class NotifiableFileCopier
{
  private final File[] src;
  private final File dest;
  private final Callback<Float> callback;
  
  private NotifiableFileCopier(Callback<Float> callback, File[] src, File dest)
    throws IOException
  {
    this.callback = callback;
    this.src = src;
    this.dest = dest;
    run();
  }
  
  public static NotifiableFileCopier copy(Callback<Float> callback, File[] src, File dest)
    throws IOException
  {
    return new NotifiableFileCopier(callback, src, dest);
  }
  
  public static NotifiableFileCopier copy(File[] src, File dest)
    throws IOException
  {
    return copy(null, src, dest);
  }
  
  public void run()
    throws IOException
  {
    long fileSize = 0L;
    for (File file : this.src) {
      fileSize += file.length();
    }
    for (File file : this.src)
    {
      FileInputStream input = null;
      FileOutputStream output = null;
      try
      {
        input = new FileInputStream(file);
        output = new FileOutputStream(this.dest);
        byte[] buf = new byte[' '];
        int bytesRead;
        while ((bytesRead = input.read(buf)) > 0)
        {
          output.write(buf, 0, bytesRead);
          if (this.callback != null) {
            this.callback.call(Float.valueOf((float)this.dest.length() / (float)fileSize));
          }
        }
      }
      finally
      {
        if (input != null) {
          input.close();
        }
        if (output != null) {
          output.close();
        }
      }
    }
  }
}
