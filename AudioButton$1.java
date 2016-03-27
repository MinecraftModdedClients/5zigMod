import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.util.ClassProxyCallback;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

class AudioButton$1
  extends Thread
{
  AudioButton$1(AudioButton this$0, String x0)
  {
    super(x0);
  }
  
  public void run()
  {
    try
    {
      DataLine.Info info = new DataLine.Info(TargetDataLine.class, AudioButton.format);
      if (!AudioSystem.isLineSupported(info))
      {
        AudioButton.access$000(this.this$0, MinecraftFactory.getClassProxyCallback().translate("chat.audio.not_supported", new Object[0]));
        return;
      }
      if (AudioButton.access$100() != null) {
        AudioButton.access$200(this.this$0);
      }
      AudioButton.access$102((TargetDataLine)AudioSystem.getLine(info));
      AudioButton.access$100().open(AudioButton.format);
      AudioButton.access$100().start();
      
      AudioInputStream ais = new AudioInputStream(AudioButton.access$100());
      if (AudioButton.access$300(this.this$0).exists()) {
        FileUtils.deleteQuietly(AudioButton.access$300(this.this$0));
      }
      if (!AudioButton.access$300(this.this$0).createNewFile()) {
        throw new IOException("Could not create Audio File!");
      }
      AudioButton.access$402(this.this$0, true);
      AudioButton.access$502(this.this$0, System.currentTimeMillis());
      
      AudioSystem.write(ais, AudioFileFormat.Type.WAVE, AudioButton.access$300(this.this$0));
    }
    catch (LineUnavailableException e)
    {
      AudioButton.access$000(this.this$0, MinecraftFactory.getClassProxyCallback().translate("chat.audio.unavailable", new Object[0]));
      MinecraftFactory.getClassProxyCallback().getLogger().error("Audioline currently unavailable!", e);
    }
    catch (Exception e)
    {
      MinecraftFactory.getClassProxyCallback().getLogger().error("Could not record Audio!", e);
      AudioButton.access$000(this.this$0, e.getMessage());
    }
  }
}
