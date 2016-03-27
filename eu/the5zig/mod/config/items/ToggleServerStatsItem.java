package eu.the5zig.mod.config.items;

import eu.the5zig.mod.I18n;

public class ToggleServerStatsItem
  extends BoolItem
{
  public ToggleServerStatsItem(String key, String category, Boolean defaultValue)
  {
    super(key, category, defaultValue);
  }
  
  public String translate()
  {
    return I18n.translate("config.server_general.show_server_stats") + ": " + translateValue();
  }
}
