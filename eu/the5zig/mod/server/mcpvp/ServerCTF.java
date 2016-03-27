package eu.the5zig.mod.server.mcpvp;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.LastServer;
import eu.the5zig.mod.config.LastServerConfiguration;
import eu.the5zig.util.minecraft.ChatColor;

public class ServerCTF
  extends ServerMCPVP
{
  private String kit;
  private int kills;
  private int killstreak;
  private int deaths;
  private int steals;
  private int captures;
  private int recovers;
  private EnumCTFTeam team;
  private CTFTeam red;
  private CTFTeam blue;
  
  public ServerCTF(String host, int port)
  {
    super(host, port);
    this.kit = "Heavy";
    this.kills = 0;
    this.killstreak = 0;
    this.deaths = 0;
    this.steals = 0;
    this.captures = 0;
    this.recovers = 0;
    this.red = new CTFTeam(EnumCTFTeam.RED);
    this.blue = new CTFTeam(EnumCTFTeam.BLUE);
    ((LastServer)The5zigMod.getLastServerConfig().getConfigInstance()).setLastServer(this);
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public String getKit()
  {
    return this.kit;
  }
  
  public void setKit(String kit)
  {
    this.kit = kit;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public int getKills()
  {
    return this.kills;
  }
  
  public void setKills(int kills)
  {
    this.kills = kills;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public int getKillstreak()
  {
    return this.killstreak;
  }
  
  public void setKillstreak(int killstreak)
  {
    this.killstreak = killstreak;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public int getDeaths()
  {
    return this.deaths;
  }
  
  public void setDeaths(int deaths)
  {
    this.deaths = deaths;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public int getSteals()
  {
    return this.steals;
  }
  
  public void setSteals(int steals)
  {
    this.steals = steals;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public int getCaptures()
  {
    return this.captures;
  }
  
  public void setCaptures(int captures)
  {
    this.captures = captures;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public int getRecovers()
  {
    return this.recovers;
  }
  
  public void setRecovers(int recovers)
  {
    this.recovers = recovers;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public EnumCTFTeam getTeam()
  {
    return this.team;
  }
  
  public void setTeam(EnumCTFTeam team)
  {
    this.team = team;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public CTFTeam getRedTeam()
  {
    return this.red;
  }
  
  public void setRedTeam(CTFTeam red)
  {
    this.red = red;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public CTFTeam getBlueTeam()
  {
    return this.blue;
  }
  
  public void setBlueTeam(CTFTeam blue)
  {
    this.blue = blue;
    The5zigMod.getLastServerConfig().saveConfig();
  }
  
  public CTFTeam getPlayerTeam()
  {
    return this.team == EnumCTFTeam.RED ? getRedTeam() : getBlueTeam();
  }
  
  public CTFTeam getOtherTeam()
  {
    return this.team == EnumCTFTeam.BLUE ? getRedTeam() : getBlueTeam();
  }
  
  public static enum EnumCTFTeam
  {
    RED(ChatColor.RED + "Red"),  BLUE(ChatColor.BLUE + "Blue");
    
    private String name;
    
    private EnumCTFTeam(String name)
    {
      this.name = name;
    }
    
    public String getName()
    {
      return this.name;
    }
  }
  
  public class CTFTeam
  {
    private final ServerCTF.EnumCTFTeam team;
    private int captures;
    private int maxcaptures;
    private String flag;
    private int players;
    
    public CTFTeam(ServerCTF.EnumCTFTeam team)
    {
      this.team = team;
    }
    
    public CTFTeam(ServerCTF.EnumCTFTeam team, int captures, int maxcaptures, String flag, int players)
    {
      this.team = team;
      this.captures = captures;
      this.maxcaptures = maxcaptures;
      this.flag = flag;
      this.players = players;
      The5zigMod.getLastServerConfig().saveConfig();
    }
    
    public ServerCTF.EnumCTFTeam getTeam()
    {
      return this.team;
    }
    
    public int getCaptures()
    {
      return this.captures;
    }
    
    public void setCaptures(int captures)
    {
      this.captures = captures;
      The5zigMod.getLastServerConfig().saveConfig();
    }
    
    public int getMaxCaptures()
    {
      return this.maxcaptures;
    }
    
    public void setMaxCaptures(int maxcaptures)
    {
      this.maxcaptures = maxcaptures;
      The5zigMod.getLastServerConfig().saveConfig();
    }
    
    public String getFlag()
    {
      return this.flag;
    }
    
    public void setFlag(String flag)
    {
      this.flag = flag;
      The5zigMod.getLastServerConfig().saveConfig();
    }
    
    public int getPlayers()
    {
      return this.players;
    }
    
    public void setPlayers(int players)
    {
      this.players = players;
      The5zigMod.getLastServerConfig().saveConfig();
    }
  }
}
