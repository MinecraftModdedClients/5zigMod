package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew.CoordStyle;
import eu.the5zig.mod.config.items.EnumItem;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.util.IVariables;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Coordinates
  extends eu.the5zig.mod.modules.items.Item
{
  public Coordinates()
  {
    addSetting(new EnumItem("coordStyle", "coordinates", ConfigNew.CoordStyle.BELOW_OTHER, ConfigNew.CoordStyle.class));
  }
  
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    List<String> coordinates = getCoordinates(dummy);
    for (int i = 0; i < coordinates.size(); i++) {
      draw((String)coordinates.get(i), x, y + 10 * i, renderLocation == RenderLocation.CENTERED);
    }
  }
  
  private List<String> getCoordinates(boolean dummy)
  {
    ConfigNew.CoordStyle coordStyle = (ConfigNew.CoordStyle)getSetting("coordStyle").get();
    String xPos = shorten(dummy ? 0.0D : The5zigMod.getVars().getPlayerPosX());
    String yPos = shorten(dummy ? 64.0D : The5zigMod.getVars().getPlayerPosY());
    String zPos = shorten(dummy ? 0.0D : The5zigMod.getVars().getPlayerPosZ());
    if (coordStyle == ConfigNew.CoordStyle.BELOW_OTHER)
    {
      String xPre = getPrefix("X") + xPos;
      String yPre = getPrefix("Y") + yPos;
      String zPre = getPrefix("Z") + zPos;
      return Arrays.asList(new String[] { xPre, yPre, zPre });
    }
    String pre = getPrefix("X/Y/Z") + xPos + "/" + yPos + "/" + zPos;
    return Collections.singletonList(pre);
  }
  
  private void draw(String string, int x, int y, boolean centered)
  {
    if (centered) {
      The5zigMod.getVars().drawCenteredString(string, x + The5zigMod.getVars().getStringWidth(string) / 2, y);
    } else {
      The5zigMod.getVars().drawString(string, x, y);
    }
  }
  
  public int getWidth(boolean dummy)
  {
    List<String> coordinates = getCoordinates(dummy);
    int maxWidth = 0;
    for (String coordinate : coordinates)
    {
      int width = The5zigMod.getVars().getStringWidth(coordinate);
      if (width > maxWidth) {
        maxWidth = width;
      }
    }
    return maxWidth;
  }
  
  public int getHeight(boolean dummy)
  {
    return getSetting("coordStyle").get() == ConfigNew.CoordStyle.BELOW_OTHER ? 30 : 10;
  }
}
