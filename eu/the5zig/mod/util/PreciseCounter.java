package eu.the5zig.mod.util;

public class PreciseCounter
{
  protected int MEASURE_INTERVAL = 1000;
  private double currentCount;
  private Counter[] timers;
  
  public PreciseCounter()
  {
    this.currentCount = 0.0D;
    this.timers = new Counter[20];
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < this.timers.length; i++)
    {
      long plus = startTime + i * 1000L / this.timers.length;
      this.timers[i] = new Counter(this.MEASURE_INTERVAL, plus);
    }
  }
  
  public double getCurrentCount()
  {
    return this.currentCount;
  }
  
  public void incrementCount()
  {
    incrementCount(1.0D);
  }
  
  public void incrementCount(double add)
  {
    for (Counter counter : this.timers) {
      counter.updateCount(add);
    }
  }
  
  public void update()
  {
    for (Counter counter : this.timers) {
      if (counter.isOver())
      {
        this.currentCount = (counter.getCount() / (this.MEASURE_INTERVAL / 1000.0D));
        counter.updateStartTime();
      }
    }
  }
}
