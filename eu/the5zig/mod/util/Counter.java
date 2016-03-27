package eu.the5zig.mod.util;

public class Counter
{
  private int MEASURE_INTERVAL;
  private long startTime;
  private double count;
  
  public Counter(int MEASURE_INTERVAL, long startTime)
  {
    this.MEASURE_INTERVAL = MEASURE_INTERVAL;
    this.startTime = startTime;
    this.count = 0.0D;
  }
  
  public void updateCount(double add)
  {
    this.count += add;
  }
  
  public double getCount()
  {
    return this.count;
  }
  
  public boolean isOver()
  {
    return System.currentTimeMillis() - this.startTime >= this.MEASURE_INTERVAL;
  }
  
  public void updateStartTime()
  {
    while (isOver()) {
      this.startTime += this.MEASURE_INTERVAL;
    }
    this.count = 0.0D;
  }
}
