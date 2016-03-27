package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.GameState;

public abstract class GameModeItem<T extends GameMode>
  extends StringItem
{
  private Class<? extends GameServer> serverClass;
  private Class<? extends T> modeClass;
  private GameState[] state;
  
  public GameModeItem(Class<? extends GameServer> serverClass, Class<? extends T> modeClass, GameState... state)
  {
    this.serverClass = serverClass;
    this.modeClass = modeClass;
    this.state = state;
  }
  
  public boolean shouldRender(boolean dummy)
  {
    if (dummy) {
      return true;
    }
    if (isOnline())
    {
      if ((this.state != null) && (this.state.length != 0))
      {
        for (GameState gameState : this.state) {
          if (getGameMode().getState() == gameState) {
            return getValue(false) != null;
          }
        }
        return false;
      }
      return getValue(false) != null;
    }
    return false;
  }
  
  protected boolean isOnline()
  {
    return (The5zigMod.getDataManager().getServer() != null) && (this.serverClass.isAssignableFrom(The5zigMod.getDataManager().getServer().getClass())) && (getServer().getGameMode() != null) && (this.modeClass.isAssignableFrom(getServer().getGameMode().getClass()));
  }
  
  protected GameServer getServer()
  {
    return (GameServer)The5zigMod.getDataManager().getServer();
  }
  
  protected T getGameMode()
  {
    return (GameMode)this.modeClass.cast(getServer().getGameMode());
  }
}
