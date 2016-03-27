package eu.the5zig.mod.listener;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.render.EasterRenderer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.SourceDataLine;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

public class EasterListener
  extends Listener
{
  private boolean running = false;
  private BufferedImage bufferedImage;
  private final EasterRenderer easterRenderer = new EasterRenderer(this);
  
  public void onTick()
  {
    if ((!this.running) && 
      (Keyboard.isKeyDown(38)) && (Keyboard.isKeyDown(82)) && (Keyboard.isKeyDown(44)) && (Keyboard.isKeyDown(57))) {
      start();
    }
  }
  
  public boolean isRunning()
  {
    return this.running;
  }
  
  public BufferedImage getBufferedImage()
  {
    return this.bufferedImage;
  }
  
  private void start()
  {
    this.running = true;
    The5zigMod.logger.info("NYANMODE ACTIVATED!!!!!!!!!!!!!");
    new Thread("Easter")
    {
      public void run()
      {
        if (EasterListener.this.bufferedImage == null) {
          try
          {
            BufferedImage bufferedImage = ImageIO.read(new URL("http://5zig.eu/dl/nyan.jpg"));
            if (bufferedImage == null) {
              throw new IOException("Image could not be loaded!");
            }
            EasterListener.this.bufferedImage = bufferedImage;
          }
          catch (IOException e)
          {
            e.printStackTrace();
            EasterListener.this.running = false;
            return;
          }
        }
        AudioInputStream din = null;
        try
        {
          AudioInputStream in = AudioSystem.getAudioInputStream(new URL("http://5zig.eu/dl/nyan.wav"));
          AudioFormat baseFormat = in.getFormat();
          
          AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
          din = AudioSystem.getAudioInputStream(decodedFormat, in);
          DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
          SourceDataLine line = (SourceDataLine)AudioSystem.getLine(info);
          if (line != null)
          {
            line.open(decodedFormat);
            byte[] data = new byte['က'];
            
            line.start();
            int nBytesRead;
            while ((nBytesRead = din.read(data, 0, data.length)) != -1) {
              line.write(data, 0, nBytesRead);
            }
            line.drain();
            line.stop();
            line.close();
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        finally
        {
          if (din != null) {
            try
            {
              din.close();
            }
            catch (IOException localIOException3) {}
          }
          EasterListener.this.running = false;
        }
      }
    }.start();
  }
  
  public EasterRenderer getEasterRenderer()
  {
    return this.easterRenderer;
  }
}
