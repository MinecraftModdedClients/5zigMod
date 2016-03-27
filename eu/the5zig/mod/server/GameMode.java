package eu.the5zig.mod.server;

public abstract class GameMode
{
  private long time;
  private GameState state;
  private int kills;
  private int killStreak;
  private long killStreakTime;
  private int deaths;
  private String winner;
  private boolean respawnable;
  
  public GameMode()
  {
    this.time = -1L;
    this.killStreakTime = -1L;
    this.state = GameState.LOBBY;
    this.respawnable = false;
  }
  
  public long getTime()
  {
    return this.time;
  }
  
  public void setTime(long time)
  {
    this.time = time;
  }
  
  public GameState getState()
  {
    return this.state;
  }
  
  public void setState(GameState state)
  {
    this.state = state;
    if (state == GameState.FINISHED) {
      this.time = (System.currentTimeMillis() - this.time);
    } else {
      this.time = -1L;
    }
  }
  
  public int getKills()
  {
    return this.kills;
  }
  
  public void setKills(int kills)
  {
    this.kills = kills;
  }
  
  public int getKillStreak()
  {
    if ((this.killStreakTime != -1L) && (System.currentTimeMillis() - this.killStreakTime > 0L))
    {
      this.killStreakTime = -1L;
      this.killStreak = 0;
    }
    return this.killStreak;
  }
  
  public void setKillStreak(int killStreak)
  {
    this.killStreak = killStreak;
    this.killStreakTime = (System.currentTimeMillis() + 20000L);
  }
  
  public int getDeaths()
  {
    return this.deaths;
  }
  
  public void setDeaths(int deaths)
  {
    this.deaths = deaths;
  }
  
  public String getWinner()
  {
    return this.winner;
  }
  
  public void setWinner(String winner)
  {
    this.winner = winner;
  }
  
  public boolean isRespawnable()
  {
    return this.respawnable;
  }
  
  public void setRespawnable(boolean canRespawn)
  {
    this.respawnable = canRespawn;
  }
  
  public abstract String getName();
}
