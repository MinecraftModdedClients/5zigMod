package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameListener;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.MultiPatternResult;
import eu.the5zig.mod.server.PatternResult;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.server.ServerListener;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;

public class TimoliaPvPListener
  extends GameListener<ServerTimolia.PvP>
{
  public TimoliaPvPListener(ServerInstance serverInstance)
  {
    super(serverInstance, ServerTimolia.PvP.class);
  }
  
  public void onMatch(String key, PatternResult match)
  {
    final ServerTimolia.PvP gameMode = (ServerTimolia.PvP)getGameMode();
    if ((gameMode.getState() == GameState.LOBBY) || (gameMode.getState() == GameState.STARTING))
    {
      if (key.equals("pvp.starting"))
      {
        gameMode.setState(GameState.STARTING);
        gameMode.setOpponent(match.get(0));
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(1)) * 1000L);
        
        getServerInstance().getListener().sendAndIgnoreMultiple("/stats " + gameMode.getOpponent(), "░▒▓Stats von " + gameMode.getOpponent() + "▓▒░", "╚═════", new Callback()
        {
          public void call(MultiPatternResult callback)
          {
            PatternResult gamesTotal = callback.parseKey(TimoliaPvPListener.this.getServerInstance().getListener(), "pvp.stats.games.total");
            PatternResult gamesWon = callback.parseKey(TimoliaPvPListener.this.getServerInstance().getListener(), "pvp.stats.games.won");
            PatternResult killDeathRatio = callback.parseKey(TimoliaPvPListener.this.getServerInstance().getListener(), "pvp.stats.kill_death_ratio");
            if (((gameMode.getState() == GameState.STARTING) || (gameMode.getState() == GameState.GAME)) && (gamesTotal != null) && (gamesWon != null) && (killDeathRatio != null))
            {
              ServerTimolia tmp108_105 = ((ServerTimolia)The5zigMod.getDataManager().getServer());tmp108_105.getClass();gameMode.setOpponentStats(new ServerTimolia.OpponentStats(tmp108_105, Integer.parseInt(gamesTotal.get(0)), 
                Integer.parseInt(gamesWon.get(0)), Double.parseDouble(killDeathRatio.get(0))));
            }
          }
        });
      }
      if (key.equals("pvp.start"))
      {
        gameMode.setState(GameState.GAME);
        if (gameMode.getTournament() != null) {
          gameMode.setTime(System.currentTimeMillis() + gameMode.getTournament().getRoundTime());
        } else {
          gameMode.setTime(System.currentTimeMillis());
        }
      }
    }
    if (gameMode.getState() == GameState.GAME)
    {
      if (key.equals("pvp.ending.sec")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("pvp.ending.min")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L * 60L);
      }
      if ((key.equals("pvp.win")) || (key.equals("pvp.lose")))
      {
        if (key.equals("pvp.win"))
        {
          gameMode.setWinner(The5zigMod.getDataManager().getUsername());
          gameMode.setWinStreak(gameMode.getWinStreak() + 1);
          if (gameMode.getTournament() != null) {
            gameMode.getTournament().setKills(gameMode.getTournament().getKills() + 1);
          }
        }
        if (key.equals("pvp.lose"))
        {
          gameMode.setWinner(gameMode.getOpponent());
          gameMode.setWinStreak(0);
          if (gameMode.getTournament() != null) {
            gameMode.getTournament().setDeaths(gameMode.getTournament().getDeaths() + 1);
          }
        }
        gameMode.setTime(-1L);
        gameMode.setWinTime(System.currentTimeMillis() + 2900L);
      }
      if ((key.equals("pvp.team.win")) || (key.equals("pvp.team.lose")))
      {
        if (key.equals("pvp.team.win"))
        {
          gameMode.setWinStreak(gameMode.getWinStreak() + 1);
          gameMode.setWinMessage(I18n.translate("ingame.win.team.self"));
        }
        if (key.equals("pvp.team.lose"))
        {
          gameMode.setWinStreak(0);
          gameMode.setWinMessage(I18n.translate("ingame.win.team.other"));
        }
        gameMode.setTime(-1L);
        gameMode.setWinTime(System.currentTimeMillis() + 2900L);
      }
      if (key.equals("pvp.team.kill"))
      {
        if ((The5zigMod.getDataManager().getUsername().equals(match.get(0))) && 
          (gameMode.getTournament() != null)) {
          gameMode.getTournament().setDeaths(gameMode.getTournament().getDeaths() + 1);
        }
        if (The5zigMod.getDataManager().getUsername().equals(match.get(1))) {
          if (gameMode.getTournament() != null) {
            gameMode.getTournament().setKills(gameMode.getTournament().getKills() + 1);
          } else {
            gameMode.setKills(gameMode.getKills() + 1);
          }
        }
      }
    }
    if (gameMode.getState() == GameState.LOBBY)
    {
      if (key.equals("pvp.tournament.no_opponent")) {
        gameMode.setTime(-1L);
      }
      if (key.equals("pvp.tournament.starting")) {
        gameMode.setTime(System.currentTimeMillis() + Integer.parseInt(match.get(0)) * 1000L);
      }
      if (key.equals("pvp.tournament.start"))
      {
        ServerTimolia tmp666_663 = ((ServerTimolia)The5zigMod.getDataManager().getServer());tmp666_663.getClass();ServerTimolia.PvPTournament tournament = new ServerTimolia.PvPTournament(tmp666_663);
        gameMode.setTournament(tournament);
        tournament.setInventoryRequested(true);
        The5zigMod.getVars().sendMessage("/t");
      }
    }
    if (gameMode.getTournament() != null)
    {
      if (gameMode.getState() == GameState.LOBBY)
      {
        if (key.equals("pvp.tournament.round")) {
          gameMode.getTournament().setCurrentRound(Integer.parseInt(match.get(0)));
        }
        if ((key.equals("pvp.tournament.participants")) || (key.equals("pvp.tournament.team.participants"))) {
          gameMode.getTournament().setParticipants(Integer.parseInt(match.get(0)));
        }
      }
      if (key.equals("pvp.tournament.win"))
      {
        gameMode.setWinner(match.get(0));
        gameMode.setWinTime(System.currentTimeMillis() + 5000L);
        gameMode.setTournament(null);
      }
    }
    if (((key.equals("pvp.tournament.eliminated")) || (!key.equals("pvp.tournament.team.eliminated"))) || (
    
      (key.equals("pvp.tournament.leave")) || (key.equals("pvp.team.leave")) || (key.equals("pvp.team.leave2"))))
    {
      gameMode.setTournament(null);
      gameMode.setTime(-1L);
    }
  }
  
  public void onTick()
  {
    ServerTimolia.PvP gameMode = (ServerTimolia.PvP)((ServerTimolia)The5zigMod.getDataManager().getServer()).getGameMode();
    if ((gameMode.getWinTime() != -1L) && (gameMode.getWinTime() - System.currentTimeMillis() < 0L))
    {
      gameMode.setWinTime(-1L);
      gameMode.setKills(0);
      gameMode.setDeaths(0);
      gameMode.setWinner(null);
      gameMode.setTime(-1L);
      gameMode.setOpponent(null);
      gameMode.setOpponentStats(null);
      gameMode.setWinMessage(null);
      gameMode.setState(GameState.LOBBY);
    }
  }
}
