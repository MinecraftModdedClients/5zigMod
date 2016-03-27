package eu.the5zig.mod.server;

public class GameServer
  extends Server
{
  protected String lobby;
  protected transient GameMode gameMode;
  
  public GameServer() {}
  
  public GameServer(String host, int port)
  {
    super(host, port);
  }
  
  public String getLobby()
  {
    return this.lobby;
  }
  
  public void setLobby(String lobby)
  {
    this.lobby = lobby;
  }
  
  public GameMode getGameMode()
  {
    return this.gameMode;
  }
  
  public void setGameMode(GameMode gameMode)
  {
    this.gameMode = gameMode;
  }
  
  public String getLobbyString()
  {
    return getGameMode().getName() + "/" + getLobby();
  }
}
