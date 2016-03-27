package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.modules.items.StringItem;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.GameState;

public abstract class ServerItem
  extends StringItem
{
  private GameState[] state;
  
  public ServerItem(GameState... state)
  {
    this.state = state;
  }
  
  public boolean shouldRender(boolean dummy)
  {
    if (dummy) {
      return true;
    }
    if ((The5zigMod.getDataManager().getServer() instanceof GameServer))
    {
      if ((this.state != null) && (this.state.length != 0))
      {
        for (GameState gameState : this.state) {
          if (getServer().getGameMode().getState() == gameState) {
            return getValue(false) != null;
          }
        }
        return false;
      }
      return getValue(false) != null;
    }
    return false;
  }
  
  protected GameServer getServer()
  {
    return !(The5zigMod.getDataManager().getServer() instanceof GameServer) ? null : (GameServer)The5zigMod.getDataManager().getServer();
  }
}
