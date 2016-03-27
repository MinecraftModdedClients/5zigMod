package eu.the5zig.mod.modules.items.player;

import eu.the5zig.mod.modules.items.StringItem;
import java.text.DateFormat;
import java.util.Date;

public class Time
  extends StringItem
{
  private static final DateFormat dateFormat = DateFormat.getTimeInstance(2);
  
  protected Object getValue(boolean dummy)
  {
    return dateFormat.format(new Date());
  }
  
  public String getTranslation()
  {
    return "ingame.time";
  }
}
