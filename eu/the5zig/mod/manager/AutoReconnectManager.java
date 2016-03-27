package eu.the5zig.mod.manager;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;

public class AutoReconnectManager
{
  private static final int COUNTDOWN_TIME = 5000;
  private long countdownStartTime;
  private Object parentScreen;
  private Object lastServerData;
  
  public void setServerData(Object serverData)
  {
    this.lastServerData = serverData;
  }
  
  public void startCountdown(Object parentScreen)
  {
    if (this.lastServerData == null) {
      return;
    }
    The5zigMod.getDataManager().resetServer();
    if (!The5zigMod.getConfig().getBool("autoReconnect"))
    {
      this.countdownStartTime = 0L;
      return;
    }
    this.countdownStartTime = System.currentTimeMillis();
    this.parentScreen = parentScreen;
  }
  
  public void checkCountdown(int guiWidth, int guiHeight)
  {
    if ((this.lastServerData == null) || (this.countdownStartTime == 0L)) {
      return;
    }
    if (System.currentTimeMillis() - this.countdownStartTime > 5000L)
    {
      The5zigMod.getVars().joinServer(this.parentScreen, this.lastServerData);
      this.countdownStartTime = 0L;
    }
    else
    {
      The5zigMod.getVars().drawCenteredString(
        I18n.translate("server.reconnecting", new Object[] {Utils.getShortenedDouble((5000L - (System.currentTimeMillis() - this.countdownStartTime)) / 1000.0D, 1) }), guiWidth / 2, guiHeight - 12);
    }
  }
}
