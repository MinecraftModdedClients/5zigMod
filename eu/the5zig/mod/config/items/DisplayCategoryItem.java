package eu.the5zig.mod.config.items;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.GuiSettings;
import eu.the5zig.mod.util.IVariables;

public class DisplayCategoryItem
  extends NonConfigItem
{
  private String displayCategory;
  
  public DisplayCategoryItem(String key, String category, String displayCategory)
  {
    super(key, category);
    this.displayCategory = displayCategory;
  }
  
  public void action()
  {
    The5zigMod.getVars().displayScreen(new GuiSettings(The5zigMod.getVars().getCurrentScreen(), this.displayCategory));
  }
}
