package eu.the5zig.mod.server.venicraft;

import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;

public class ServerVenicraft
  extends GameServer
{
  public ServerVenicraft() {}
  
  public ServerVenicraft(String host, int port)
  {
    super(host, port);
  }
  
  public class Mineathlon
    extends GameMode
  {
    private String discipline;
    private int round;
    
    public Mineathlon() {}
    
    public String getDiscipline()
    {
      return this.discipline;
    }
    
    public void setDiscipline(String discipline)
    {
      this.discipline = discipline;
    }
    
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
      return "Mineathlon";
    }
  }
  
  public class CrystalDefense
    extends GameMode
  {
    public CrystalDefense() {}
    
    public String getName()
    {
      return "CrystalDefense";
    }
  }
  
  public class SurvivalGames
    extends GameMode
  {
    public SurvivalGames() {}
    
    public String getName()
    {
      return "SurvivalGames";
    }
  }
}
