package eu.the5zig.mod.config.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.mod.config.ConfigNew.Location;

public class LocationItem
  extends EnumItem<ConfigNew.Location>
{
  private float xOffset;
  private float yOffset;
  private boolean centered = false;
  
  public LocationItem(String key, String category, ConfigNew.Location defaultValue)
  {
    super(key, category, defaultValue, ConfigNew.Location.class);
  }
  
  public void serialize(JsonObject object)
  {
    JsonObject content = new JsonObject();
    content.addProperty("type", ((ConfigNew.Location)get()).toString());
    content.addProperty("x", Float.valueOf(getXOffset()));
    content.addProperty("y", Float.valueOf(getYOffset()));
    content.addProperty("centered", Boolean.valueOf(isCentered()));
    object.add(getKey(), content);
  }
  
  public void deserialize(JsonObject object)
  {
    JsonObject content = object.get(getKey()).getAsJsonObject();
    ConfigNew.Location location = ConfigNew.Location.valueOf(content.get("type").getAsString());
    set(location);
    setXOffset(content.get("x").getAsFloat());
    setYOffset(content.get("y").getAsFloat());
    setCentered(content.get("centered").getAsBoolean());
  }
  
  public float getXOffset()
  {
    return this.xOffset;
  }
  
  public void setXOffset(float xOffset)
  {
    this.xOffset = xOffset;
  }
  
  public float getYOffset()
  {
    return this.yOffset;
  }
  
  public void setYOffset(float yOffset)
  {
    this.yOffset = yOffset;
  }
  
  public boolean isCentered()
  {
    return this.centered;
  }
  
  public void setCentered(boolean centered)
  {
    this.centered = centered;
  }
}
