package eu.the5zig.mod.config.items;

public class PlaceholderItem
  extends NonConfigItem
{
  private static int id = 0;
  
  public PlaceholderItem(String category)
  {
    super("placeholder__" + id++, category);
  }
  
  public final void action() {}
}
