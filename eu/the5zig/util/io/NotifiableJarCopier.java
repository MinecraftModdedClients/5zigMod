package eu.the5zig.util.io;

import eu.the5zig.util.Callback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NotifiableJarCopier
{
  private final File[] jarFiles;
  private final File destinationJar;
  private final Callback<Float> callback;
  private long length;
  
  private NotifiableJarCopier(File[] jarFiles, File destinationJar, Callback<Float> callback)
    throws IOException
  {
    this.jarFiles = jarFiles;
    this.destinationJar = destinationJar;
    this.callback = callback;
    for (File jarFile : jarFiles)
    {
      JarFile file = new JarFile(jarFile);
      Enumeration entries = file.entries();
      while (entries.hasMoreElements()) {
        this.length += ((JarEntry)entries.nextElement()).getSize();
      }
    }
    run();
  }
  
  public static NotifiableJarCopier copy(File[] jarFiles, File destinationJar, Callback<Float> callback)
    throws IOException
  {
    return new NotifiableJarCopier(jarFiles, destinationJar, callback);
  }
  
  public static NotifiableJarCopier copy(File[] jarFiles, File destinationJar)
    throws IOException
  {
    return copy(jarFiles, destinationJar, null);
  }
  
  public void run()
    throws IOException
  {
    CountingJarOutputStream tempJar = null;
    try
    {
      tempJar = new CountingJarOutputStream(new FileOutputStream(this.destinationJar));
      ArrayList<String> entryList = new ArrayList();
      
      int j = this.jarFiles.length;
      for (int i = 0; i < j; i++)
      {
        File jarFile = this.jarFiles[i];
        JarFile jar = null;
        Enumeration<JarEntry> entries;
        try
        {
          jar = new JarFile(jarFile);
          for (entries = jar.entries(); entries.hasMoreElements();)
          {
            JarEntry entry = new JarEntry(((JarEntry)entries.nextElement()).getName());
            if (!entryList.contains(entry.getName()))
            {
              entryList.add(entry.getName());
              if ((i == 0) || (!entry.getName().startsWith("META-INF/")))
              {
                InputStream entryStream = jar.getInputStream(entry);
                
                tempJar.putNextEntry(entry);
                
                byte[] buffer = new byte[' '];
                int bytesRead;
                while ((bytesRead = entryStream.read(buffer)) > 0)
                {
                  tempJar.write(buffer, 0, bytesRead);
                  if (this.callback != null) {
                    this.callback.call(Float.valueOf((float)tempJar.getCount() / (float)this.length));
                  }
                }
                entryStream.close();
                tempJar.flush();
                tempJar.closeEntry();
              }
            }
          }
        }
        finally {}
      }
    }
    finally
    {
      if (tempJar != null) {
        tempJar.close();
      }
    }
  }
}
