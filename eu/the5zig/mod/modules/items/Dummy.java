package eu.the5zig.mod.modules.items;

import eu.the5zig.mod.config.items.Item;

public class Dummy
  extends StringItem
{
  public Dummy()
  {
    addSetting(new eu.the5zig.mod.config.items.StringItem("dummy", "", "dummy-text"));
  }
  
  protected Object getValue(boolean dummy)
  {
    return getSetting("dummy").get();
  }
  
  public boolean shouldRender(boolean dummy)
  {
    return (getValue(dummy) != null) && (!dummy);
  }
  
  public String getName()
  {
    return "Dummy";
  }
}
