package eu.the5zig.mod.crashreport;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

public class CrashHopper
{
  private static final File HOPPER_JAR_FILE = new File("the5zigmod/crash-hopper.jar");
  private static boolean extracted = false;
  
  public static void init()
  {
    InputStream resourceInputStream = null;
    OutputStream outputStream = null;
    try
    {
      if (HOPPER_JAR_FILE.exists())
      {
        resourceInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("crash-hopper.jar");
        if (resourceInputStream == null) {
          throw new RuntimeException("Could not find original Jar File of Crash Hopper!");
        }
        byte[] validMD5 = DigestUtils.md5(resourceInputStream);
        
        InputStream jarInputStream = null;
        try
        {
          byte[] jarMD5 = DigestUtils.md5(jarInputStream = new FileInputStream(HOPPER_JAR_FILE));
          if (Arrays.equals(validMD5, jarMD5))
          {
            extracted = true;
            return;
          }
        }
        finally {}
      }
      resourceInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("crash-hopper.jar");
      if (resourceInputStream == null) {
        throw new RuntimeException("Could not find original Jar File of Crash Hopper!");
      }
      outputStream = new FileOutputStream(HOPPER_JAR_FILE);
      
      IOUtils.copy(resourceInputStream, outputStream);
      
      The5zigMod.logger.info("Extracted new Crash Hopper!");
      
      extracted = true;
    }
    catch (Exception e)
    {
      The5zigMod.logger.warn("Could not extract Crash Hopper!", e);
    }
    finally
    {
      IOUtils.closeQuietly(resourceInputStream);
      IOUtils.closeQuietly(outputStream);
    }
  }
  
  public static void launch(Throwable cause, File crashFile)
  {
    if (!extracted) {
      return;
    }
    ZipFile zipFile = null;
    try
    {
      URLClassLoader ucl = (URLClassLoader)Thread.currentThread().getContextClassLoader();
      
      URL[] urls = ucl.getURLs();
      for (URL url : urls)
      {
        ZipFile z = getZipFile(url);
        if (z != null)
        {
          zipFile = z;
          break;
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    if (zipFile == null) {
      return;
    }
    boolean containsModFile = false;
    for (Throwable throwable = cause; throwable.getCause() != null; throwable = throwable.getCause()) {
      for (StackTraceElement stackTraceElement : throwable.getStackTrace())
      {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements())
        {
          ZipEntry entry = (ZipEntry)entries.nextElement();
          if (!entry.isDirectory()) {
            if (stackTraceElement.getClassName().equals(entry.getName().replace('/', '.').replace(".class", "")))
            {
              containsModFile = true;
              break;
            }
          }
        }
      }
    }
    if (!containsModFile) {
      return;
    }
    try
    {
      new ProcessBuilder(new String[] { "java", "-jar", HOPPER_JAR_FILE.getAbsolutePath(), String.valueOf((The5zigMod.getConfig() == null) || (The5zigMod.getConfig().getBool("reportCrashes")) ? 1 : false), crashFile.getAbsolutePath() }).start();
    }
    catch (IOException localIOException) {}
  }
  
  private static ZipFile getZipFile(URL url)
  {
    try
    {
      URI uri = url.toURI();
      
      File file = new File(uri);
      
      ZipFile zipFile = new ZipFile(file);
      if (zipFile.getEntry("eu/the5zig/mod/The5zigMod.class") == null)
      {
        zipFile.close();
        return null;
      }
      return zipFile;
    }
    catch (Exception ignored) {}
    return null;
  }
}
