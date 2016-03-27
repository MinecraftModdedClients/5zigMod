package eu.the5zig.mod.manager;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.util.Counter;
import eu.the5zig.mod.util.IVariables;

public class SpeedCalculator
{
  private double currentSpeed;
  private SpeedCounter[] timers;
  
  public SpeedCalculator()
  {
    this.currentSpeed = 0.0D;
    this.timers = new SpeedCounter[20];
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < this.timers.length; i++)
    {
      long plus = startTime + i * 1000L / this.timers.length;
      this.timers[i] = new SpeedCounter(1000, plus);
    }
  }
  
  public double getCurrentSpeed()
  {
    return this.currentSpeed;
  }
  
  public void update()
  {
    for (SpeedCounter counter : this.timers) {
      if ((!The5zigMod.getVars().isPlayerNull()) && (!The5zigMod.getVars().isTerrainLoading()))
      {
        if (counter.isOver())
        {
          double x = The5zigMod.getVars().getPlayerPosX();
          double y = The5zigMod.getVars().getPlayerPosY();
          double z = The5zigMod.getVars().getPlayerPosZ();
          if ((counter.lastX != null) && (counter.lastY != null) && (counter.lastZ != null)) {
            this.currentSpeed = Math.sqrt((counter.lastX.doubleValue() - x) * (counter.lastX.doubleValue() - x) + (counter.lastY.doubleValue() - y) * (counter.lastY.doubleValue() - y) + (counter.lastZ.doubleValue() - z) * (counter.lastZ.doubleValue() - z));
          } else {
            this.currentSpeed = 0.0D;
          }
          counter.lastX = Double.valueOf(x);
          counter.lastY = Double.valueOf(y);
          counter.lastZ = Double.valueOf(z);
          
          counter.updateStartTime();
        }
      }
      else
      {
        counter.lastX = null;
        counter.lastY = null;
        counter.lastZ = null;
      }
    }
  }
  
  public class SpeedCounter
    extends Counter
  {
    private Double lastX;
    private Double lastY;
    private Double lastZ;
    
    public SpeedCounter(int MEASURE_INTERVAL, long startTime)
    {
      super(startTime);
    }
  }
}
