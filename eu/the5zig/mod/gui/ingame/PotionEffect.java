package eu.the5zig.mod.gui.ingame;

public class PotionEffect
  implements Comparable<PotionEffect>
{
  private final String name;
  private final int time;
  private final String timeString;
  private final int amplifier;
  private final int iconIndex;
  private final boolean good;
  private final boolean hasParticles;
  
  public PotionEffect(String name, int time, String timeString, int amplifier, int iconIndex, boolean good, boolean hasParticles)
  {
    this.name = name;
    this.time = time;
    this.timeString = timeString;
    this.amplifier = amplifier;
    this.iconIndex = iconIndex;
    this.good = good;
    this.hasParticles = hasParticles;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public int getTime()
  {
    return this.time;
  }
  
  public String getTimeString()
  {
    return this.timeString;
  }
  
  public int getAmplifier()
  {
    return this.amplifier;
  }
  
  public int getIconIndex()
  {
    return this.iconIndex;
  }
  
  public boolean isGood()
  {
    return this.good;
  }
  
  public boolean hasParticles()
  {
    return this.hasParticles;
  }
  
  public int compareTo(PotionEffect o)
  {
    return Boolean.valueOf(o.isGood()).compareTo(Boolean.valueOf(isGood()));
  }
}
