package eu.the5zig.mod.config.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.mod.The5zigMod;

public class BoolItem
  extends Item<Boolean>
{
  public BoolItem(String key, String category, Boolean defaultValue)
  {
    super(key, category, defaultValue);
  }
  
  public void deserialize(JsonObject object)
  {
    set(Boolean.valueOf(object.get(getKey()).getAsBoolean()));
  }
  
  public void serialize(JsonObject object)
  {
    object.addProperty(getKey(), (Boolean)get());
  }
  
  public void next()
  {
    set(Boolean.valueOf(!((Boolean)get()).booleanValue()));
  }
  
  public String translateValue()
  {
    return The5zigMod.toBoolean(((Boolean)get()).booleanValue());
  }
}
