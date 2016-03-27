package eu.the5zig.mod.server.mcpvp;

import java.util.concurrent.TimeUnit;

public class Feast
{
  private int x;
  private int z;
  private long millisStarted;
  private int feastTime;
  
  public Feast(int x, int z)
  {
    this.x = x;
    this.z = z;
    this.millisStarted = System.currentTimeMillis();
    this.feastTime = 300000;
    this.feastTime += 2000;
  }
  
  public Feast(int x, int z, int feastTime)
  {
    this.x = x;
    this.z = z;
    this.millisStarted = System.currentTimeMillis();
    this.feastTime = feastTime;
    this.feastTime += 2000;
  }
  
  public String getRemainingTime()
  {
    long millis = this.millisStarted + this.feastTime - System.currentTimeMillis();
    if (millis > 999L) {
      return String.format("%02d:%02d", new Object[] { Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(millis)), 
        Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))) });
    }
    return null;
  }
  
  public String getCoordinates()
  {
    return this.x + ", " + this.z;
  }
}
