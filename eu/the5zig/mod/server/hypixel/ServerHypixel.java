package eu.the5zig.mod.server.hypixel;

import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;

public class ServerHypixel
  extends GameServer
{
  public ServerHypixel() {}
  
  public ServerHypixel(String host, int port)
  {
    super(host, port);
  }
  
  public class Quake
    extends GameMode
  {
    public Quake()
    {
      setRespawnable(true);
    }
    
    public String getName()
    {
      return "Quakecraft";
    }
  }
  
  public class Blitz
    extends GameMode
  {
    private String kit;
    private long star;
    private long deathmatch;
    
    public Blitz()
    {
      this.star = -1L;
      this.deathmatch = -1L;
    }
    
    public String getKit()
    {
      return this.kit;
    }
    
    public void setKit(String kit)
    {
      this.kit = kit;
    }
    
    public long getStar()
    {
      return this.star;
    }
    
    public void setStar(long star)
    {
      this.star = star;
    }
    
    public long getDeathmatch()
    {
      return this.deathmatch;
    }
    
    public void setDeathmatch(long deathmatch)
    {
      this.deathmatch = deathmatch;
    }
    
    public String getName()
    {
      return "BlitzSG";
    }
  }
  
  public class Paintball
    extends GameMode
  {
    private String team;
    
    public Paintball()
    {
      setRespawnable(true);
    }
    
    public String getTeam()
    {
      return this.team;
    }
    
    public void setTeam(String team)
    {
      this.team = team;
    }
    
    public String getName()
    {
      return "PaintBall";
    }
  }
}
