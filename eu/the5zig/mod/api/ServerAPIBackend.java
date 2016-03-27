package eu.the5zig.mod.api;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerAPIBackend
{
  private String displayName = "Unknown Server";
  private final LinkedHashMap<String, String> stats = Maps.newLinkedHashMap();
  private HashMap<Integer, String> imageCache = Maps.newHashMap();
  private String base64;
  private String largeText;
  private long countdownTime;
  private String countdownName;
  
  public void updateStat(String name, String score)
  {
    this.stats.put(name, score);
  }
  
  public void resetStat(String stat)
  {
    this.stats.remove(stat);
  }
  
  public void reset()
  {
    this.stats.clear();
    this.imageCache.clear();
    this.displayName = "Unknown Server";
    this.largeText = null;
    this.base64 = null;
    resetCountdown();
  }
  
  public Map<String, String> getStats()
  {
    return this.stats;
  }
  
  public void setDisplayName(String displayName)
  {
    this.displayName = displayName;
  }
  
  public String getDisplayName()
  {
    return this.displayName;
  }
  
  public void setImage(String base64, int id)
  {
    this.base64 = base64;
    this.imageCache.put(Integer.valueOf(id), base64);
  }
  
  public void setImage(int id)
  {
    this.base64 = ((String)this.imageCache.get(Integer.valueOf(id)));
  }
  
  public void resetImage()
  {
    this.base64 = null;
  }
  
  public String getBase64()
  {
    return this.base64;
  }
  
  public void setLargeText(String largeText)
  {
    this.largeText = largeText;
  }
  
  public String getLargeText()
  {
    return this.largeText;
  }
  
  public void startCountdown(String name, long time)
  {
    this.countdownName = name;
    this.countdownTime = (System.currentTimeMillis() + time);
  }
  
  public void resetCountdown()
  {
    this.countdownTime = -1L;
    this.countdownName = null;
  }
  
  public long getCountdownTime()
  {
    if (this.countdownTime - System.currentTimeMillis() < 0L) {
      resetCountdown();
    }
    return this.countdownTime;
  }
  
  public String getCountdownName()
  {
    return this.countdownName;
  }
}
