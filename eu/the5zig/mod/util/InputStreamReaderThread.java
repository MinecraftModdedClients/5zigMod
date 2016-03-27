package eu.the5zig.mod.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class InputStreamReaderThread
  extends Thread
{
  private final InputStream inputStream;
  
  public InputStreamReaderThread(String name, InputStream inputStream)
  {
    super(name);
    this.inputStream = inputStream;
  }
  
  public void run()
  {
    BufferedReader reader = null;
    try
    {
      reader = new BufferedReader(new InputStreamReader(this.inputStream));
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }
      return;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (reader != null) {
        try
        {
          reader.close();
        }
        catch (IOException localIOException4) {}
      }
      if (this.inputStream != null) {
        try
        {
          this.inputStream.close();
        }
        catch (IOException localIOException5) {}
      }
    }
  }
}
