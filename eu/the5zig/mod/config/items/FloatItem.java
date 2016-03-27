package eu.the5zig.mod.config.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.util.Utils;

public class FloatItem
  extends Item<Float>
{
  public FloatItem(String key, String category, Float defaultValue)
  {
    super(key, category, defaultValue);
  }
  
  public void deserialize(JsonObject object)
  {
    set(Float.valueOf(object.get(getKey()).getAsFloat()));
  }
  
  public void serialize(JsonObject object)
  {
    object.addProperty(getKey(), (Number)get());
  }
  
  public void next()
  {
    set(Float.valueOf(((Float)get()).floatValue() + 1.0F));
  }
  
  public String translateValue()
  {
    return String.valueOf(Utils.getShortenedFloat(((Float)get()).floatValue(), 1));
  }
}
