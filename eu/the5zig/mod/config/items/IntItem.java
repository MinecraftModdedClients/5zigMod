package eu.the5zig.mod.config.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class IntItem
  extends Item<Integer>
{
  public IntItem(String key, String category, Integer defaultValue)
  {
    super(key, category, defaultValue);
  }
  
  public void deserialize(JsonObject object)
  {
    set(Integer.valueOf(object.get(getKey()).getAsInt()));
  }
  
  public void serialize(JsonObject object)
  {
    object.addProperty(getKey(), (Number)get());
  }
  
  public void next()
  {
    set(Integer.valueOf(((Integer)get()).intValue() + 1));
  }
}
