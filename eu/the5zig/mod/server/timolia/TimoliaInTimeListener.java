package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;

public class TimoliaInTimeListener
  extends GameListener<ServerTimolia.InTime>
{
  public TimoliaInTimeListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.InTime.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    ServerTimolia.InTime gameMode = (ServerTimolia.InTime)getGameMode();
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("starting.actionbar")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("start")) {
        gameMode.setState(GameState.STARTING);
      }
    }
    if (gameMode.getState() == GameState.STARTING)
    {
      if (key.equals("intime.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("intime.start"))
      {
        gameMode.setState(GameState.GAME);
        gameMode.setTime(System.currentTimeMillis());
      }
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if (key.equals("intime.invincibility"))
      {
        gameMode.setInvincible(false);
        gameMode.setInvincibleTimer(System.currentTimeMillis() + 3000L);
      }
      if (key.equals("intime.loot.min")) {
        gameMode.setLoot(System.currentTimeMillis() + 60000 * Integer.parseInt(match.get(0)));
      }
      if (key.equals("intime.loot.sec")) {
        gameMode.setLoot(System.currentTimeMillis() + 1000 * Integer.parseInt(match.get(0)));
      }
      if (key.equals("intime.loot.spawned"))
      {
        gameMode.setLoot(-1L);
        gameMode.setLootTimer(System.currentTimeMillis() + 3000L);
      }
      if (key.equals("intime.spawn_regeneration"))
      {
        gameMode.setSpawnRegeneration(true);
        gameMode.setSpawnRegenerationTimer(System.currentTimeMillis() + 3000L);
      }
    }
  }
  
  public void onTick()
  {
    ServerTimolia.InTime gameMode = (ServerTimolia.InTime)getGameMode();
    if (gameMode.getState() == GameState.GAME)
    {
      if ((gameMode.getInvincibleTimer() != -1L) && (gameMode.getInvincibleTimer() - System.currentTimeMillis() < 0L)) {
        gameMode.setInvincibleTimer(-1L);
      }
      if ((gameMode.getSpawnRegenerationTimer() != -1L) && (gameMode.getSpawnRegenerationTimer() - System.currentTimeMillis() < 0L)) {
        gameMode.setSpawnRegenerationTimer(-1L);
      }
      if ((gameMode.getLootTimer() != -1L) && (gameMode.getLootTimer() - System.currentTimeMillis() < 0L)) {
        gameMode.setLootTimer(-1L);
      }
    }
  }
}
