package eu.the5zig.mod.chat;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.util.IVariables;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ChatBackgroundManager
{
  private Object chatBackgroundLocation;
  private int imageWidth;
  private int imageHeight;
  
  public ChatBackgroundManager()
  {
    reloadBackgroundImage();
  }
  
  public void reloadBackgroundImage()
  {
    String location = The5zigMod.getConfig().getString("chatBackgroundLocation");
    if (location == null)
    {
      resetBackgroundImage();
      return;
    }
    File file = new File(location);
    if (!file.exists())
    {
      resetBackgroundImage();
      return;
    }
    try
    {
      BufferedImage bufferedImage = ImageIO.read(file);
      this.imageWidth = bufferedImage.getWidth();
      this.imageHeight = bufferedImage.getHeight();
      this.chatBackgroundLocation = The5zigMod.getVars().bindDynamicImage("chat_background", bufferedImage);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  public void resetBackgroundImage()
  {
    this.chatBackgroundLocation = null;
    this.imageWidth = 0;
    this.imageHeight = 0;
  }
  
  public Object getChatBackground()
  {
    return this.chatBackgroundLocation;
  }
  
  public int getImageWidth()
  {
    return this.imageWidth;
  }
  
  public int getImageHeight()
  {
    return this.imageHeight;
  }
}
