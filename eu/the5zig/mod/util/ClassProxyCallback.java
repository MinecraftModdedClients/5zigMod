package eu.the5zig.mod.util;

import com.mojang.authlib.GameProfile;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.manager.SearchEntry;
import java.io.File;
import org.apache.logging.log4j.Logger;

public abstract interface ClassProxyCallback
{
  public abstract Logger getLogger();
  
  public abstract String getLastServer();
  
  public abstract String translate(String paramString, Object... paramVarArgs);
  
  public abstract void displayGuiSettings(Gui paramGui);
  
  public abstract void handlePlayerInfo(boolean paramBoolean, int paramInt, GameProfile paramGameProfile);
  
  public abstract boolean isShowLastServer();
  
  public abstract boolean is2ndChatTextLeftbound();
  
  public abstract boolean is2ndChatVisible();
  
  public abstract float get2ndChatOpacity();
  
  public abstract float get2ndChatWidth();
  
  public abstract float get2ndChatHeightFocused();
  
  public abstract float get2ndChatHeightUnfocused();
  
  public abstract float get2ndChatScale();
  
  public abstract void resetServer();
  
  public abstract boolean isRenderCustomModels();
  
  public abstract void checkAutoreconnectCountdown(int paramInt1, int paramInt2);
  
  public abstract void setAutoreconnectServerData(Object paramObject);
  
  public abstract void launchCrashHopper(Throwable paramThrowable, File paramFile);
  
  public abstract void addSearch(SearchEntry paramSearchEntry, SearchEntry... paramVarArgs);
  
  public abstract void renderSnow(int paramInt1, int paramInt2);
  
  public abstract void disableTray();
  
  public abstract boolean isTrayEnabled();
}
