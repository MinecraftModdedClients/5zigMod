package eu.the5zig.mod.chat.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.AudioMessage;
import eu.the5zig.mod.chat.entity.AudioMessage.AudioData;
import eu.the5zig.mod.chat.entity.ConversationChat;
import eu.the5zig.mod.chat.entity.FileMessage.Status;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import java.io.File;
import java.util.UUID;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line.Info;

public class AudioChatLine
  extends FileChatLine
{
  private Clip clip;
  private boolean clipLoaded = false;
  private int lastFrame;
  private boolean hoverPlay = false;
  private boolean hoverSlider = false;
  private int sliderX;
  
  public AudioChatLine(Message message)
  {
    super(message);
  }
  
  private AudioMessage getAudioMessage()
  {
    return (AudioMessage)getMessage();
  }
  
  private AudioMessage.AudioData getAudioData()
  {
    return (AudioMessage.AudioData)getAudioMessage().getFileData();
  }
  
  protected String getName()
  {
    return I18n.translate("chat.audio.name");
  }
  
  protected int getWidth()
  {
    return 100;
  }
  
  protected int getHeight()
  {
    return getLineHeight() - 28;
  }
  
  protected boolean drawOverlay()
  {
    return false;
  }
  
  protected void preDraw(int x, int y, int width, int height, int mouseX, int mouseY)
  {
    if ((!this.clipLoaded) && ((getAudioData().getStatus() == FileMessage.Status.UPLOADED) || (getAudioData().getStatus() == FileMessage.Status.DOWNLOADED))) {
      loadClip();
    }
  }
  
  protected void postDraw(int x, int y, int width, int height, int mouseX, int mouseY)
  {
    Gui.drawRect(x, y, x + width, y + height, -5592406);
    Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, -14540254);
    this.hoverPlay = false;
    if ((this.clipLoaded) && (this.clip == null))
    {
      drawStatus(I18n.translate("chat.audio.not_found"), x, y, width, height, 0.8F);
      return;
    }
    this.hoverPlay = ((mouseX >= x + 8) && (mouseX <= x + 15) && (mouseY >= y + 11) && (mouseY <= y + 17));
    int frame = this.lastFrame;
    if (!this.clip.isRunning())
    {
      The5zigMod.getVars().drawString("|>", x + 8, y + 11, this.hoverPlay ? 4473924 : 16777215);
    }
    else
    {
      The5zigMod.getVars().drawString("||", x + 8, y + 11, this.hoverPlay ? 4473924 : 16777215);
      frame = this.clip.getFramePosition();
    }
    double totalSeconds = this.clip.getMicrosecondLength() / 1000000.0D;
    double currentSecond = frame / this.clip.getFrameLength() * totalSeconds;
    The5zigMod.getVars().drawString(Utils.getShortenedDouble(currentSecond, 1) + "/" + Utils.getShortenedDouble(totalSeconds, 1) + " sec", x + 8, y + 22);
    Gui.drawRect(x + 21, y + 14, x + 91, y + 18, -15658735);
    Gui.drawRect(x + 20, y + 13, x + 90, y + 17, -5592406);
    this.hoverSlider = ((mouseX >= x + 20) && (mouseX <= x + 90) && (mouseY >= y + 14) && (mouseY <= y + 18));
    this.sliderX = (x + 20);
    int left = frame == 0 ? x + 20 : x + 20 + (int)(frame / this.clip.getFrameLength() * 70.0D);
    Gui.drawRect(left, y + 10, left + 1, y + 20, -3355444);
  }
  
  public IButton mousePressed(int mouseX, int mouseY)
  {
    IButton button = super.mousePressed(mouseX, mouseY);
    if (this.clip == null) {
      return button;
    }
    if (this.hoverPlay)
    {
      if (this.clip.isRunning())
      {
        this.lastFrame = this.clip.getFramePosition();
        this.clip.stop();
      }
      else
      {
        if (this.lastFrame < this.clip.getFrameLength()) {
          this.clip.setFramePosition(this.lastFrame);
        } else {
          this.clip.setFramePosition(0);
        }
        this.lastFrame = 0;
        this.clip.start();
      }
    }
    else if (this.hoverSlider)
    {
      int sliderPos = mouseX - this.sliderX;
      int maxSliderPos = 70;
      int maxFrame = this.clip.getFrameLength();
      int framePosition = (int)Math.ceil(sliderPos / maxSliderPos * maxFrame);
      if (this.clip.isRunning())
      {
        this.clip.stop();
        this.clip.setFramePosition(framePosition);
        this.clip.start();
      }
      else
      {
        this.lastFrame = framePosition;
      }
    }
    return button;
  }
  
  private void loadClip()
  {
    this.clipLoaded = true;
    try
    {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("the5zigmod/media/" + The5zigMod.getDataManager().getUniqueId().toString() + "/" + 
        ((ConversationChat)getMessage().getConversation()).getFriendUUID().toString() + "/" + 
        getAudioData().getHash()));
      this.clip = ((Clip)AudioSystem.getLine(new Line.Info(Clip.class)));
      this.clip.open(audioInputStream);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public void close()
  {
    if (this.clip == null) {
      return;
    }
    if (this.clip.isRunning()) {
      this.clip.stop();
    }
    this.clip.close();
  }
  
  public int getLineHeight()
  {
    return 70;
  }
}
