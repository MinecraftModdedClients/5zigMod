package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Iterator;
import java.util.List;

public class GuiBanned
  extends Gui
{
  private String reason;
  private long time;
  private List<?> reasonList;
  private int exitCount = 300;
  
  public GuiBanned(String reason, long time)
  {
    this.reason = reason.replace("\r\n", "\n");
    this.time = time;
  }
  
  public void initGui()
  {
    this.reasonList = The5zigMod.getVars().splitStringToWidth(this.reason, getWidth() - 50);
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawMenuBackground();
    int y = 100;
    for (Iterator<?> it = this.reasonList.iterator(); it.hasNext(); y += The5zigMod.getVars().getFontHeight()) {
      drawCenteredString((String)it.next(), getWidth() / 2, y);
    }
    y += 20;
    drawCenteredString("Ban-Time: " + Utils.convertToDate(this.time).replace("Today", I18n.translate("profile.today").replace("Yesterday", I18n.translate("profile.yesterday"))), 
      getWidth() / 2, y);
    drawCenteredString(I18n.translate("banned.exiting", new Object[] { Integer.valueOf(this.exitCount / 20) }), getWidth() / 2, getHeight() - 38);
    drawCenteredString(ChatColor.GRAY.toString() + ChatColor.ITALIC.toString() + I18n.translate("banned.help"), getWidth() / 2, getHeight() - 25);
  }
  
  protected void tick()
  {
    this.exitCount -= 1;
    if (this.exitCount < 0) {
      The5zigMod.getVars().shutdown();
    }
  }
  
  protected void actionPerformed(IButton button) {}
  
  public void keyTyped0(char paramChar, int paramInt) {}
  
  public String getTitleKey()
  {
    return "banned.title";
  }
}
