import com.mojang.authlib.GameProfile;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.AudioCallback;
import eu.the5zig.mod.util.ClassProxyCallback;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IResourceLocation;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import org.apache.logging.log4j.Logger;

public class AudioButton
  extends IconButton
{
  public static final AudioFormat format = new AudioFormat(16000.0F, 16, 2, true, true);
  private static final IResourceLocation ITEMS = MinecraftFactory.getVars().createResourceLocation("the5zigmod", "textures/items.png");
  private static TargetDataLine line;
  private final AudioCallback callback;
  private File tmpFile;
  private boolean recording;
  private long recordingStarted;
  private String errorMessage;
  private int errorTicks;
  
  public AudioButton(int id, int x, int y, AudioCallback callback)
  {
    super(ITEMS, 80, 0, id, x, y);
    this.callback = callback;
    try
    {
      File dir = eu.the5zig.util.io.FileUtils.createDir(new File("the5zigmod/media/" + MinecraftFactory.getVars().getGameProfile().getId().toString()));
      this.tmpFile = eu.the5zig.util.io.FileUtils.createFile(new File(dir, "audio.wav.tmp"));
    }
    catch (IOException e1)
    {
      e1.printStackTrace();
    }
  }
  
  public void tick()
  {
    super.tick();
    if (this.errorTicks > 0)
    {
      this.errorTicks -= 1;
      if (this.errorTicks == 0) {
        this.errorMessage = null;
      }
    }
    if (System.currentTimeMillis() - this.recordingStarted > 60000L) {
      stopRecording();
    }
  }
  
  public void draw(int mouseX, int mouseY)
  {
    super.draw(mouseX, mouseY);
    if (this.errorMessage != null) {
      MinecraftFactory.getVars().getCurrentScreen().drawHoveringText(Collections.singletonList(this.errorMessage), (int)(getX() + MinecraftFactory.getVars().getStringWidth(this.errorMessage) * 0.8D), 
        getY() - 10);
    }
    if (this.recording)
    {
      String line1 = MinecraftFactory.getClassProxyCallback().translate("chat.audio.recording", new Object[] { Utils.getShortenedDouble(
        (System.currentTimeMillis() - this.recordingStarted) / 1000.0D, 1) });
      String line2 = MinecraftFactory.getClassProxyCallback().translate("chat.audio.abort", new Object[0]);
      int stringWidth = Math.max(MinecraftFactory.getVars().getStringWidth(line1), MinecraftFactory.getVars().getStringWidth(line2));
      MinecraftFactory.getVars().getCurrentScreen().drawHoveringText(Arrays.asList(new String[] { line1, line2 }), (int)(getX() + stringWidth * 0.8D), getY() - 15);
    }
    GLUtil.disableLighting();
    if ((this.recording) && ((mouseX <= getX()) || (mouseX >= getX() + getWidth()) || (mouseY <= getY()) || (mouseY >= getY() + getWidth())))
    {
      this.recordingStarted = System.currentTimeMillis();
      this.recording = false;
      closeLine();
    }
  }
  
  public boolean mouseClicked(int mouseX, int mouseY)
  {
    boolean mouseClicked = super.mouseClicked(mouseX, mouseY);
    if (mouseClicked) {
      startRecording();
    }
    return mouseClicked;
  }
  
  public void mouseReleased(int mouseX, int mouseY)
  {
    super.mouseReleased(mouseX, mouseY);
    stopRecording();
  }
  
  private void error(String message)
  {
    this.errorMessage = message;
    this.errorTicks = 50;
  }
  
  private void startRecording()
  {
    if ((this.recording) || (this.recordingStarted != 0L)) {
      return;
    }
    if (this.errorTicks > 0) {
      this.errorTicks = 1;
    }
    new Thread("Audio Record Thread")
    {
      public void run()
      {
        try
        {
          DataLine.Info info = new DataLine.Info(TargetDataLine.class, AudioButton.format);
          if (!AudioSystem.isLineSupported(info))
          {
            AudioButton.this.error(MinecraftFactory.getClassProxyCallback().translate("chat.audio.not_supported", new Object[0]));
            return;
          }
          if (AudioButton.line != null) {
            AudioButton.this.closeLine();
          }
          AudioButton.access$102((TargetDataLine)AudioSystem.getLine(info));
          AudioButton.line.open(AudioButton.format);
          AudioButton.line.start();
          
          AudioInputStream ais = new AudioInputStream(AudioButton.line);
          if (AudioButton.this.tmpFile.exists()) {
            org.apache.commons.io.FileUtils.deleteQuietly(AudioButton.this.tmpFile);
          }
          if (!AudioButton.this.tmpFile.createNewFile()) {
            throw new IOException("Could not create Audio File!");
          }
          AudioButton.this.recording = true;
          AudioButton.this.recordingStarted = System.currentTimeMillis();
          
          AudioSystem.write(ais, AudioFileFormat.Type.WAVE, AudioButton.this.tmpFile);
        }
        catch (LineUnavailableException e)
        {
          AudioButton.this.error(MinecraftFactory.getClassProxyCallback().translate("chat.audio.unavailable", new Object[0]));
          MinecraftFactory.getClassProxyCallback().getLogger().error("Audioline currently unavailable!", e);
        }
        catch (Exception e)
        {
          MinecraftFactory.getClassProxyCallback().getLogger().error("Could not record Audio!", e);
          AudioButton.this.error(e.getMessage());
        }
      }
    }.start();
  }
  
  private void closeLine()
  {
    if (line == null) {
      return;
    }
    line.stop();
    line.close();
    if (System.currentTimeMillis() - this.recordingStarted > 250L) {
      this.callback.done(this.tmpFile);
    }
    this.recordingStarted = 0L;
    
    line = null;
  }
  
  private void stopRecording()
  {
    if (!this.recording) {
      return;
    }
    this.recording = false;
    if (System.currentTimeMillis() - this.recordingStarted <= 250L) {
      error(MinecraftFactory.getClassProxyCallback().translate("chat.audio.hint", new Object[0]));
    }
    closeLine();
  }
  
  public void guiClosed()
  {
    closeLine();
  }
}
