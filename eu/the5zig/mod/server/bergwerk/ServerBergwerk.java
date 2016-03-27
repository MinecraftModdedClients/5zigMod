package eu.the5zig.mod.server.bergwerk;

import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;

public class ServerBergwerk
  extends GameServer
{
  public ServerBergwerk() {}
  
  public ServerBergwerk(String host, int port)
  {
    super(host, port);
  }
  
  public class Flash
    extends GameMode
  {
    public Flash() {}
    
    public String getName()
    {
      return "Flash";
    }
  }
  
  public class Duel
    extends GameMode
  {
    private String team;
    private boolean canRespawn;
    private long teleporterTimer;
    
    public Duel()
    {
      setRespawnable(true);
      this.canRespawn = true;
      this.teleporterTimer = -1L;
    }
    
    public String getTeam()
    {
      return this.team;
    }
    
    public void setTeam(String team)
    {
      this.team = team;
    }
    
    public boolean isCanRespawn()
    {
      return this.canRespawn;
    }
    
    public void setCanRespawn(boolean canRespawn)
    {
      this.canRespawn = canRespawn;
    }
    
    public long getTeleporterTimer()
    {
      return this.teleporterTimer;
    }
    
    public void setTeleporterTimer(long teleporterTimer)
    {
      this.teleporterTimer = teleporterTimer;
    }
    
    public String getName()
    {
      return "BedWars Duel";
    }
  }
}
