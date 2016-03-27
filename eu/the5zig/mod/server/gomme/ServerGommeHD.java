package eu.the5zig.mod.server.gomme;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.Teamable;

public class ServerGommeHD
  extends GameServer
{
  public ServerGommeHD() {}
  
  public ServerGommeHD(String host, int port)
  {
    super(host, port);
  }
  
  public class SurvivalGames
    extends GameMode
    implements Teamable
  {
    private long deathmatchTime;
    private boolean teamsAllowed;
    
    public SurvivalGames()
    {
      this.deathmatchTime = -1L;
    }
    
    public long getDeathmatchTime()
    {
      return this.deathmatchTime;
    }
    
    public void setDeathmatchTime(long deathmatchTime)
    {
      this.deathmatchTime = deathmatchTime;
    }
    
    public boolean isTeamsAllowed()
    {
      return this.teamsAllowed;
    }
    
    public void setTeamsAllowed(boolean teamsAllowed)
    {
      this.teamsAllowed = teamsAllowed;
    }
    
    public String getName()
    {
      return "SurvivalGames";
    }
  }
  
  public class BedWars
    extends GameMode
  {
    private String team;
    private int beds;
    private boolean canRespawn;
    private boolean teamsAllowed;
    
    public BedWars()
    {
      setRespawnable(true);
      this.canRespawn = true;
      this.teamsAllowed = false;
    }
    
    public String getTeam()
    {
      return this.team == null ? I18n.translate("ingame.kit.none") : this.team;
    }
    
    public void setTeam(String team)
    {
      this.team = team;
    }
    
    public int getBeds()
    {
      return this.beds;
    }
    
    public void setBeds(int beds)
    {
      this.beds = beds;
    }
    
    public boolean isCanRespawn()
    {
      return this.canRespawn;
    }
    
    public void setCanRespawn(boolean canRespawn)
    {
      this.canRespawn = canRespawn;
    }
    
    public boolean isTeamsAllowed()
    {
      return this.teamsAllowed;
    }
    
    public String getName()
    {
      return "BedWars";
    }
  }
  
  public class EnderGames
    extends GameMode
  {
    private String kit;
    private String coins;
    
    public EnderGames() {}
    
    public String getKit()
    {
      return this.kit == null ? I18n.translate("ingame.kit.none") : this.kit;
    }
    
    public void setKit(String kit)
    {
      this.kit = kit;
    }
    
    public void setCoins(String coins)
    {
      this.coins = coins;
    }
    
    public String getCoins()
    {
      return this.coins;
    }
    
    public String getName()
    {
      return "EnderGames";
    }
  }
  
  public class PvP
    extends GameMode
  {
    public PvP() {}
    
    public String getName()
    {
      return "PvP";
    }
  }
  
  public class PvPMatch
    extends GameMode
  {
    public PvPMatch() {}
    
    public String getName()
    {
      return "PvP";
    }
  }
  
  public class FFA
    extends GameMode
  {
    public FFA() {}
    
    public String getName()
    {
      return "FFA";
    }
  }
  
  public class SkyWars
    extends GameMode
  {
    private String kit;
    private int team;
    private String coins;
    
    public SkyWars() {}
    
    public String getKit()
    {
      return this.kit;
    }
    
    public void setKit(String kit)
    {
      this.kit = kit;
    }
    
    public int getTeam()
    {
      return this.team;
    }
    
    public void setTeam(int team)
    {
      this.team = team;
    }
    
    public String getCoins()
    {
      return this.coins;
    }
    
    public void setCoins(String coins)
    {
      this.coins = coins;
    }
    
    public String getName()
    {
      return "SkyWars";
    }
  }
}
