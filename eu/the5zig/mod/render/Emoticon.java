package eu.the5zig.mod.render;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.IVariables;
import java.awt.image.BufferedImage;

public class Emoticon
{
  private final String unicodeCharacter;
  private final BufferedImage bufferedImage;
  private final Object resourceLocation;
  
  public Emoticon(String unicodeCharacter, BufferedImage bufferedImage)
  {
    this.unicodeCharacter = unicodeCharacter;
    this.bufferedImage = bufferedImage;
    this.resourceLocation = The5zigMod.getVars().bindDynamicImage("emoticon", bufferedImage);
  }
  
  public String getUnicodeCharacter()
  {
    return this.unicodeCharacter;
  }
  
  public Object getResourceLocation()
  {
    return this.resourceLocation;
  }
  
  public void render()
  {
    Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, this.bufferedImage.getWidth(), this.bufferedImage.getHeight(), this.bufferedImage.getWidth(), this.bufferedImage.getHeight());
  }
}
