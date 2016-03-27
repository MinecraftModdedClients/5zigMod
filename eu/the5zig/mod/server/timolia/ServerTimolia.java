package eu.the5zig.mod.server.timolia;

import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import org.lwjgl.util.vector.Vector3f;

public class ServerTimolia
  extends GameServer
{
  public ServerTimolia() {}
  
  public ServerTimolia(String host, int port)
  {
    super(host, port);
    setRenderSaturation(false);
  }
  
  public String getLobbyString()
  {
    return getGameMode().getName() + "/" + getLobby();
  }
  
  public class Splun
    extends GameMode
  {
    public Splun() {}
    
    public String getName()
    {
      return "Splun";
    }
  }
  
  public class DNA
    extends GameMode
  {
    private double height;
    
    public DNA() {}
    
    public double getHeight()
    {
      return this.height;
    }
    
    public void setHeight(double height)
    {
      this.height = height;
    }
    
    public String getName()
    {
      return "DNA";
    }
  }
  
  public class TSpiele
    extends GameMode
  {
    public TSpiele() {}
    
    public String getName()
    {
      return "TSpiele";
    }
  }
  
  public class Arena
    extends GameMode
  {
    private int round;
    
    public Arena() {}
    
    public int getRound()
    {
      return this.round;
    }
    
    public void setRound(int round)
    {
      this.round = round;
    }
    
    public String getName()
    {
      return "4rena";
    }
  }
  
  public class BrainBow
    extends GameMode
  {
    private String team;
    private int score;
    
    public BrainBow() {}
    
    public String getTeam()
    {
      return this.team;
    }
    
    public void setTeam(String team)
    {
      this.team = team;
    }
    
    public int getScore()
    {
      return this.score;
    }
    
    public void setScore(int score)
    {
      this.score = score;
    }
    
    public String getName()
    {
      return "BrainBow";
    }
  }
  
  public class PvP
    extends GameMode
  {
    private long winTime;
    private String opponent;
    private ServerTimolia.OpponentStats opponentStats;
    private int winStreak;
    private String winMessage;
    private ServerTimolia.PvPTournament tournament;
    
    public PvP() {}
    
    public long getWinTime()
    {
      return this.winTime;
    }
    
    public void setWinTime(long winTime)
    {
      this.winTime = winTime;
    }
    
    public String getOpponent()
    {
      return this.opponent;
    }
    
    public void setOpponent(String opponent)
    {
      this.opponent = opponent;
    }
    
    public ServerTimolia.OpponentStats getOpponentStats()
    {
      return this.opponentStats;
    }
    
    public void setOpponentStats(ServerTimolia.OpponentStats opponentStats)
    {
      this.opponentStats = opponentStats;
    }
    
    public int getWinStreak()
    {
      return this.winStreak;
    }
    
    public void setWinStreak(int winStreak)
    {
      this.winStreak = winStreak;
    }
    
    public String getWinMessage()
    {
      return this.winMessage;
    }
    
    public void setWinMessage(String winMessage)
    {
      this.winMessage = winMessage;
    }
    
    public ServerTimolia.PvPTournament getTournament()
    {
      return this.tournament;
    }
    
    public void setTournament(ServerTimolia.PvPTournament tournament)
    {
      this.tournament = tournament;
    }
    
    public String getName()
    {
      return "1vs1";
    }
  }
  
  public class OpponentStats
  {
    private int gamesTotal;
    private int gamesWon;
    private double killDeathRatio;
    
    public OpponentStats(int gamesTotal, int gamesWon, double killDeathRatio)
    {
      this.gamesTotal = gamesTotal;
      this.gamesWon = gamesWon;
      this.killDeathRatio = killDeathRatio;
    }
    
    public int getGamesTotal()
    {
      return this.gamesTotal;
    }
    
    public void setGamesTotal(int gamesTotal)
    {
      this.gamesTotal = gamesTotal;
    }
    
    public int getGamesWon()
    {
      return this.gamesWon;
    }
    
    public void setGamesWon(int gamesWon)
    {
      this.gamesWon = gamesWon;
    }
    
    public double getKillDeathRatio()
    {
      return this.killDeathRatio;
    }
    
    public void setKillDeathRatio(double killDeathRatio)
    {
      this.killDeathRatio = killDeathRatio;
    }
  }
  
  public class PvPTournament
  {
    private String host;
    private int participants;
    private int qualificationRounds;
    private int bestOf;
    private int currentRound;
    private int kills;
    private int deaths;
    private String kit;
    private long roundTime;
    private boolean inventoryRequested = false;
    
    public PvPTournament() {}
    
    public String getHost()
    {
      return this.host;
    }
    
    public void setHost(String host)
    {
      this.host = host;
    }
    
    public int getParticipants()
    {
      return this.participants;
    }
    
    public void setParticipants(int participants)
    {
      this.participants = participants;
    }
    
    public int getQualificationRounds()
    {
      return this.qualificationRounds;
    }
    
    public void setQualificationRounds(int qualificationRounds)
    {
      this.qualificationRounds = qualificationRounds;
    }
    
    public int getBestOf()
    {
      return this.bestOf;
    }
    
    public void setBestOf(int bestOf)
    {
      this.bestOf = bestOf;
    }
    
    public int getCurrentRound()
    {
      return this.currentRound;
    }
    
    public void setCurrentRound(int currentRound)
    {
      this.currentRound = currentRound;
    }
    
    public int getKills()
    {
      return this.kills;
    }
    
    public void setKills(int kills)
    {
      this.kills = kills;
    }
    
    public int getDeaths()
    {
      return this.deaths;
    }
    
    public void setDeaths(int deaths)
    {
      this.deaths = deaths;
    }
    
    public String getKit()
    {
      return this.kit;
    }
    
    public void setKit(String kit)
    {
      this.kit = kit;
    }
    
    public long getRoundTime()
    {
      return this.roundTime;
    }
    
    public void setRoundTime(long roundTime)
    {
      this.roundTime = roundTime;
    }
    
    public boolean isInventoryRequested()
    {
      return this.inventoryRequested;
    }
    
    public void setInventoryRequested(boolean inventoryRequested)
    {
      this.inventoryRequested = inventoryRequested;
    }
  }
  
  public class InTime
    extends GameMode
  {
    private boolean invincible;
    private long invincibleTimer;
    private long loot;
    private long lootTimer;
    private boolean spawnRegeneration;
    private long spawnRegenerationTimer;
    
    public InTime()
    {
      this.invincible = true;
      this.invincibleTimer = -1L;
      this.loot = -1L;
      this.lootTimer = -1L;
      this.spawnRegeneration = false;
      this.spawnRegenerationTimer = -1L;
    }
    
    public boolean isInvincible()
    {
      return this.invincible;
    }
    
    public void setInvincible(boolean invincible)
    {
      this.invincible = invincible;
    }
    
    public long getInvincibleTimer()
    {
      return this.invincibleTimer;
    }
    
    public void setInvincibleTimer(long invincibleTimer)
    {
      this.invincibleTimer = invincibleTimer;
    }
    
    public long getLoot()
    {
      return this.loot;
    }
    
    public void setLoot(long loot)
    {
      this.loot = loot;
    }
    
    public long getLootTimer()
    {
      return this.lootTimer;
    }
    
    public void setLootTimer(long lootTimer)
    {
      this.lootTimer = lootTimer;
    }
    
    public boolean isSpawnRegeneration()
    {
      return this.spawnRegeneration;
    }
    
    public void setSpawnRegeneration(boolean spawnRegeneration)
    {
      this.spawnRegeneration = spawnRegeneration;
    }
    
    public long getSpawnRegenerationTimer()
    {
      return this.spawnRegenerationTimer;
    }
    
    public void setSpawnRegenerationTimer(long spawnRegenerationTimer)
    {
      this.spawnRegenerationTimer = spawnRegenerationTimer;
    }
    
    public String getName()
    {
      return "InTime";
    }
  }
  
  public class Arcade
    extends GameMode
  {
    private String currentMiniGame;
    private String nextMiniGame;
    
    public Arcade() {}
    
    public String getCurrentMiniGame()
    {
      return this.currentMiniGame;
    }
    
    public void setCurrentMiniGame(String currentMiniGame)
    {
      this.currentMiniGame = currentMiniGame;
    }
    
    public String getNextMiniGame()
    {
      return this.nextMiniGame;
    }
    
    public void setNextMiniGame(String nextMiniGame)
    {
      this.nextMiniGame = nextMiniGame;
    }
    
    public String getName()
    {
      return "Arcade";
    }
  }
  
  public class Advent
    extends GameMode
  {
    private String parkourName;
    private int currentCheckpoint;
    private long timeGold;
    private long timeSilver;
    private long timeBronze;
    
    public Advent() {}
    
    public String getParkourName()
    {
      return this.parkourName;
    }
    
    public void setParkourName(String parkourName)
    {
      this.parkourName = parkourName;
    }
    
    public void setCurrentCheckpoint(int currentCheckpoint)
    {
      this.currentCheckpoint = currentCheckpoint;
    }
    
    public int getCurrentCheckpoint()
    {
      return this.currentCheckpoint;
    }
    
    public long getTimeGold()
    {
      return this.timeGold;
    }
    
    public void setTimeGold(long timeGold)
    {
      this.timeGold = timeGold;
    }
    
    public long getTimeSilver()
    {
      return this.timeSilver;
    }
    
    public void setTimeSilver(long timeSilver)
    {
      this.timeSilver = timeSilver;
    }
    
    public long getTimeBronze()
    {
      return this.timeBronze;
    }
    
    public void setTimeBronze(long timeBronze)
    {
      this.timeBronze = timeBronze;
    }
    
    public String getName()
    {
      return "Adventskalender";
    }
  }
  
  public class JumpWorld
    extends GameMode
  {
    private int checkpoints;
    private Vector3f lastCheckpoint;
    private int fails;
    
    public JumpWorld() {}
    
    public int getCheckpoints()
    {
      return this.checkpoints;
    }
    
    public void setCheckpoints(int checkpoints)
    {
      this.checkpoints = checkpoints;
    }
    
    public Vector3f getLastCheckpoint()
    {
      return this.lastCheckpoint;
    }
    
    public void setLastCheckpoint(Vector3f lastCheckpoint)
    {
      this.lastCheckpoint = lastCheckpoint;
    }
    
    public int getFails()
    {
      return this.fails;
    }
    
    public void setFails(int fails)
    {
      this.fails = fails;
    }
    
    public String getName()
    {
      return "JumpWorld";
    }
  }
}
