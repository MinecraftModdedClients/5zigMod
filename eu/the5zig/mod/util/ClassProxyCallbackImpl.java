package eu.the5zig.mod.util;

import com.mojang.authlib.GameProfile;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.LastServer;
import eu.the5zig.mod.config.LastServerConfiguration;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.crashreport.CrashHopper;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.GuiSettings;
import eu.the5zig.mod.manager.AutoReconnectManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.PingManager;
import eu.the5zig.mod.manager.SearchEntry;
import eu.the5zig.mod.manager.SearchManager;
import eu.the5zig.mod.render.SnowRenderer;
import eu.the5zig.mod.server.Server;
import java.io.File;
import org.apache.logging.log4j.Logger;

public class ClassProxyCallbackImpl
  implements ClassProxyCallback
{
  public Logger getLogger()
  {
    return The5zigMod.logger;
  }
  
  public String getLastServer()
  {
    Server server = ((LastServer)The5zigMod.getLastServerConfig().getConfigInstance()).getLastServer();
    if (server == null) {
      return null;
    }
    return server.getHost() + ":" + server.getPort();
  }
  
  public String translate(String key, Object... format)
  {
    return I18n.translate(key, format);
  }
  
  public void displayGuiSettings(Gui lastScreen)
  {
    The5zigMod.getVars().displayScreen(new GuiSettings(lastScreen, "main"));
  }
  
  public void handlePlayerInfo(boolean updatePing, int ping, GameProfile gameProfile)
  {
    The5zigMod.getDataManager().getPingManager().handlePlayerInfo(updatePing, ping, gameProfile);
  }
  
  public boolean isShowLastServer()
  {
    return The5zigMod.getConfig().getBool("showLastServer");
  }
  
  public boolean is2ndChatTextLeftbound()
  {
    return The5zigMod.getConfig().getBool("2ndChatTextLeftbound");
  }
  
  public boolean is2ndChatVisible()
  {
    return The5zigMod.getConfig().getBool("2ndChatVisible");
  }
  
  public float get2ndChatOpacity()
  {
    return The5zigMod.getConfig().getFloat("2ndChatOpacity");
  }
  
  public float get2ndChatWidth()
  {
    return The5zigMod.getConfig().getFloat("2ndChatWidth");
  }
  
  public float get2ndChatHeightFocused()
  {
    return The5zigMod.getConfig().getFloat("2ndChatHeightFocused");
  }
  
  public float get2ndChatHeightUnfocused()
  {
    return The5zigMod.getConfig().getFloat("2ndChatHeightUnfocused");
  }
  
  public float get2ndChatScale()
  {
    return The5zigMod.getConfig().getFloat("2ndChatScale");
  }
  
  public void resetServer()
  {
    The5zigMod.getDataManager().resetServer();
  }
  
  public boolean isRenderCustomModels()
  {
    return The5zigMod.getConfig().getBool("showCustomModels");
  }
  
  public void checkAutoreconnectCountdown(int width, int height)
  {
    The5zigMod.getDataManager().getAutoReconnectManager().checkCountdown(width, height);
  }
  
  public void setAutoreconnectServerData(Object serverData)
  {
    The5zigMod.getDataManager().getAutoReconnectManager().setServerData(serverData);
  }
  
  public void launchCrashHopper(Throwable cause, File crashFile)
  {
    CrashHopper.launch(cause, crashFile);
  }
  
  public void addSearch(SearchEntry searchEntry, SearchEntry... searchEntries)
  {
    The5zigMod.getDataManager().getSearchManager().addSearch(searchEntry, searchEntries);
  }
  
  public void renderSnow(int width, int height)
  {
    The5zigMod.getDataManager().getSnowRenderer().render(width, height);
  }
  
  public void disableTray()
  {
    ((BoolItem)The5zigMod.getConfig().get("showTrayNotifications", BoolItem.class)).set(Boolean.valueOf(false));
    The5zigMod.getConfig().save();
  }
  
  public boolean isTrayEnabled()
  {
    return The5zigMod.getConfig().getBool("showTrayNotifications");
  }
}
