package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;

public class TimoliaArcadeListener
  extends GameListener<ServerTimolia.Arcade>
{
  private boolean sentGameRotationRequest = false;
  private boolean receivedGameRotationStart = false;
  
  public TimoliaArcadeListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.Arcade.class);
  }
  
  public void onGameModeJoin()
  {
    sendGameListRequest();
  }
  
  public boolean onServerChat(String message)
  {
    message = ChatColor.stripColor(message);
    if (message.equals("│ Arcade» Spielrotation (kann abweichen):"))
    {
      this.receivedGameRotationStart = true;
      return this.sentGameRotationRequest;
    }
    if (this.receivedGameRotationStart)
    {
      if (message.startsWith("│ Next:")) {
        ((ServerTimolia.Arcade)getGameMode()).setNextMiniGame(message.replace("│ Next: ", ""));
      }
      if (message.startsWith("│     └"))
      {
        boolean sent = this.sentGameRotationRequest;
        this.sentGameRotationRequest = false;
        this.receivedGameRotationStart = false;
        return sent;
      }
      return this.sentGameRotationRequest;
    }
    return false;
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.Arcade gameMode = (ServerTimolia.Arcade)getGameMode();
    if (key.equals("starting.actionbar"))
    {
      gameMode.setWinner(null);
      gameMode.setState(GameState.STARTING);
      gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
    }
    if (key.equals("arcade.end")) {
      gameMode.setState(GameState.ENDGAME);
    }
    if ((key.equals("arcade.win")) && (gameMode.getState() == GameState.ENDGAME)) {
      gameMode.setWinner(match.get(0));
    }
  }
  
  public void onTitle(String title, String subTitle)
  {
    if ((title == null) || (subTitle == null) || (title.isEmpty())) {
      return;
    }
    ServerTimolia.Arcade gameMode = (ServerTimolia.Arcade)getGameMode();
    String miniGame = ChatColor.stripColor(title);
    if ((miniGame.isEmpty()) || (miniGame.equals(gameMode.getCurrentMiniGame()))) {
      return;
    }
    gameMode.setCurrentMiniGame(miniGame);
    gameMode.setNextMiniGame(null);
    
    sendGameListRequest();
  }
  
  public void onTick()
  {
    ServerTimolia.Arcade gameMode = (ServerTimolia.Arcade)getGameMode();
    if ((gameMode.getState() == GameState.STARTING) && 
      (System.currentTimeMillis() - gameMode.getTime() > 0L))
    {
      gameMode.setState(GameState.GAME);
      gameMode.setTime(System.currentTimeMillis());
    }
  }
  
  private void sendGameListRequest()
  {
    this.sentGameRotationRequest = true;
    this.receivedGameRotationStart = false;
    The5zigMod.getVars().sendMessage("/getgamerotation");
  }
}
