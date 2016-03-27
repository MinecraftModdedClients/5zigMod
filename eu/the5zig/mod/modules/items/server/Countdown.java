package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.render.LargeTextRenderer;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.util.Utils;

public class Countdown
  extends ServerItem
{
  public Countdown()
  {
    super(new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Utils.convertToClock(90L);
    }
    GameMode gameMode = getServer().getGameMode();
    if ((gameMode == null) || (gameMode.getTime() == -1L)) {
      return null;
    }
    String time;
    if ((gameMode.getState() == GameState.LOBBY) || (gameMode.getState() == GameState.STARTING) || (gameMode.getState() == GameState.PREGAME))
    {
      time = shorten((gameMode.getTime() - System.currentTimeMillis()) / 1000.0D);
    }
    else
    {
      String time;
      if ((gameMode.getState() == GameState.GAME) || (gameMode.getState() == GameState.ENDGAME))
      {
        if (System.currentTimeMillis() - gameMode.getTime() > 0L) {
          return Utils.convertToClock(System.currentTimeMillis() - gameMode.getTime());
        }
        time = shorten((gameMode.getTime() - System.currentTimeMillis()) / 1000.0D);
      }
      else
      {
        String time;
        if (gameMode.getState() == GameState.FINISHED) {
          time = Utils.convertToClock(gameMode.getTime());
        } else {
          throw new AssertionError();
        }
      }
    }
    String time;
    renderLarge(gameMode.getTime(), time);
    return time;
  }
  
  private void renderLarge(long time, String timeString)
  {
    long l = System.currentTimeMillis();
    if ((time - l <= 15000L) && (time - l > 0L) && (The5zigMod.getConfig().getBool("showLargeStartCountdown")))
    {
      GameState state = getServer().getGameMode().getState();
      String translationKey;
      if ((state == GameState.LOBBY) || (state == GameState.STARTING))
      {
        translationKey = "ingame.starting_in";
      }
      else
      {
        String translationKey;
        if (state == GameState.PREGAME)
        {
          translationKey = "ingame.invincibility_wears_off";
        }
        else
        {
          String translationKey;
          if ((state == GameState.GAME) || (state == GameState.ENDGAME) || (state == GameState.FINISHED)) {
            translationKey = "ingame.ending_in";
          } else {
            throw new AssertionError();
          }
        }
      }
      String translationKey;
      DisplayRenderer.largeTextRenderer.render(The5zigMod.getRenderer().getPrefix() + I18n.translate(translationKey, new Object[] { timeString }));
    }
  }
  
  public String getTranslation()
  {
    if ((getServer() == null) || (getServer().getGameMode() == null)) {
      return "ingame.time";
    }
    GameState state = getServer().getGameMode().getState();
    if ((state == GameState.LOBBY) || (state == GameState.STARTING)) {
      return "ingame.starting";
    }
    if (state == GameState.PREGAME) {
      return "ingame.invincibility";
    }
    if ((state == GameState.GAME) || (state == GameState.ENDGAME) || (state == GameState.FINISHED)) {
      return "ingame.time";
    }
    return super.getTranslation();
  }
}
