package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Container;
import eu.the5zig.util.minecraft.ChatColor;

public class StatisticRow
  implements Row
{
  private String translation;
  private Container[] containers;
  private int xOff;
  private int yOff;
  
  public StatisticRow(String translation, Container... containers)
  {
    this(translation, 0, 0, containers);
  }
  
  public StatisticRow(String translation, int xOff, int yOff, Container... containers)
  {
    this.translation = translation;
    this.containers = containers;
    this.xOff = xOff;
    this.yOff = yOff;
  }
  
  public int getLineHeight()
  {
    return 14;
  }
  
  public void draw(int x, int y)
  {
    if (!The5zigMod.getNetworkManager().isConnected())
    {
      The5zigMod.getVars().drawString(ChatColor.RED + I18n.translate("connection.offline"), x + this.xOff + 2, y + this.yOff + 2);
      return;
    }
    The5zigMod.getVars().drawString(I18n.translate(this.translation, this.containers), x + this.xOff + 2, y + this.yOff + 2);
  }
}
