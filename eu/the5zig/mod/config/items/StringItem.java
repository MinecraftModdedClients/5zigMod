package eu.the5zig.mod.config.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class StringItem
  extends Item<String>
{
  public StringItem(String key, String category, String defaultValue)
  {
    super(key, category, defaultValue);
  }
  
  public void deserialize(JsonObject object)
  {
    if (object.get(getKey()) == null) {
      return;
    }
    set(object.get(getKey()).getAsString());
  }
  
  public void serialize(JsonObject object)
  {
    if (get() == null) {
      object.remove(getKey());
    } else {
      object.addProperty(getKey(), (String)get());
    }
  }
  
  public void next() {}
}
