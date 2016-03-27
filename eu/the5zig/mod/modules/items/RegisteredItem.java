package eu.the5zig.mod.modules.items;

public class RegisteredItem
{
  private final String key;
  private final Class<? extends Item> clazz;
  private final Category category;
  
  RegisteredItem(String key, Class<? extends Item> clazz, Category category)
  {
    this.key = key;
    this.clazz = clazz;
    this.category = category;
  }
  
  RegisteredItem(String key, Class<? extends Item> clazz)
  {
    this(key, clazz, Category.OTHER);
  }
  
  public String getKey()
  {
    return this.key;
  }
  
  public Class<? extends Item> getClazz()
  {
    return this.clazz;
  }
  
  public Category getCategory()
  {
    return this.category;
  }
}
