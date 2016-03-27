package eu.the5zig.mod.server.playminity;

import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;

public class ServerPlayMinity
  extends GameServer
{
  public ServerPlayMinity() {}
  
  public ServerPlayMinity(String host, int port)
  {
    super(host, port);
    setRenderEntityHealth(true);
  }
  
  public class JumpLeague
    extends GameMode
  {
    private int checkPoint;
    private int maxCheckPoints;
    private int fails;
    private int lives;
    
    public JumpLeague()
    {
      this.maxCheckPoints = 10;
      this.lives = 3;
    }
    
    public int getCheckPoint()
    {
      return this.checkPoint;
    }
    
    public void setCheckPoint(int checkPoint)
    {
      this.checkPoint = checkPoint;
    }
    
    public int getMaxCheckPoints()
    {
      return this.maxCheckPoints;
    }
    
    public void setMaxCheckPoints(int maxCheckPoints)
    {
      this.maxCheckPoints = maxCheckPoints;
    }
    
    public int getFails()
    {
      return this.fails;
    }
    
    public void setFails(int fails)
    {
      this.fails = fails;
    }
    
    public int getLives()
    {
      return this.lives;
    }
    
    public void setLives(int lives)
    {
      this.lives = lives;
    }
    
    public String getName()
    {
      return "JumpLeague";
    }
  }
}
