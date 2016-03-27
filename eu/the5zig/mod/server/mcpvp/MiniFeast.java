package eu.the5zig.mod.server.mcpvp;

public class MiniFeast
{
  private long timeStarted;
  private int xmin;
  private int xmax;
  private int zmin;
  private int zmax;
  
  public MiniFeast(int xmin, int xmax, int zmin, int zmax)
  {
    this.xmin = xmin;
    this.xmax = xmax;
    this.zmin = zmin;
    this.zmax = zmax;
    this.timeStarted = System.currentTimeMillis();
  }
  
  public boolean isOver()
  {
    return System.currentTimeMillis() - this.timeStarted > 90000L;
  }
  
  public String getCoordinates()
  {
    return this.xmin + " - " + this.xmax + ", " + this.zmin + " - " + this.zmax;
  }
  
  public boolean equals(Object obj)
  {
    return ((obj instanceof MiniFeast)) && (getCoordinates().equals(((MiniFeast)obj).getCoordinates()));
  }
}
