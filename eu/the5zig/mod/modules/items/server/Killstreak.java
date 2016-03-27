package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.render.LargeTextRenderer;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.GameState;

public class Killstreak
  extends ServerItem
{
  public Killstreak()
  {
    super(new GameState[0]);
  }
  
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    super.render(x, y, renderLocation, dummy);
    if ((getServer() == null) || (getServer().getGameMode() == null)) {
      return;
    }
    int killstreak = getServer().getGameMode().getKillStreak();
    if ((killstreak > 1) && (The5zigMod.getConfig().getBool("showLargeKillstreaks")))
    {
      String text = null;
      if (killstreak == 2) {
        text = I18n.translate("ingame.killstreak.double");
      } else if (killstreak == 3) {
        text = I18n.translate("ingame.killstreak.triple");
      } else if (killstreak == 4) {
        text = I18n.translate("ingame.killstreak.quadruple");
      } else if (killstreak >= 5) {
        text = I18n.translate("ingame.killstreak.multi");
      }
      if (text == null) {
        return;
      }
      DisplayRenderer.largeTextRenderer.render(The5zigMod.getRenderer().getPrefix() + text);
    }
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(1);
    }
    return (getServer().getGameMode() != null) && (getServer().getGameMode().getKillStreak() > 0) ? Integer.valueOf(getServer().getGameMode().getKillStreak()) : null;
  }
  
  public String getTranslation()
  {
    return "ingame.killstreak";
  }
}
