package eu.the5zig.mod.config.items;

public class ActionItem
  extends NonConfigItem
{
  private final Runnable runnable;
  
  public ActionItem(String key, String category, Runnable runnable)
  {
    super(key, category);
    this.runnable = runnable;
  }
  
  public void action()
  {
    this.runnable.run();
  }
}
