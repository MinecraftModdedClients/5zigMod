package eu.the5zig.mod.server;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.listener.Listener;
import eu.the5zig.mod.manager.DataManager;

public abstract class GameListener<T extends GameMode>
  extends Listener
{
  protected final ServerInstance serverInstance;
  protected final Class<T> gameMode;
  
  public GameListener(ServerInstance serverInstance, Class<T> gameMode)
  {
    this.serverInstance = serverInstance;
    this.gameMode = gameMode;
  }
  
  public ServerInstance getServerInstance()
  {
    return this.serverInstance;
  }
  
  public Class<T> getServer()
  {
    return this.gameMode;
  }
  
  protected T getGameMode()
  {
    return (GameMode)this.gameMode.cast(((GameServer)The5zigMod.getDataManager().getServer()).getGameMode());
  }
  
  public void onMatch(String key, PatternResult match) {}
  
  public void onGameModeJoin() {}
  
  public Class<T> getGameModeClass()
  {
    return this.gameMode;
  }
}
