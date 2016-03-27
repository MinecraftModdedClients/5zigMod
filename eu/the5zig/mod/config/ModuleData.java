package eu.the5zig.mod.config;

import java.util.List;

public class ModuleData
{
  private final List<String> defaultModules;
  private final List<String> availableItems;
  
  public ModuleData(List<String> defaultModules, List<String> availableItems)
  {
    this.defaultModules = defaultModules;
    this.availableItems = availableItems;
  }
  
  public List<String> getDefaultModules()
  {
    return this.defaultModules;
  }
  
  public List<String> getAvailableItems()
  {
    return this.availableItems;
  }
}
