package eu.the5zig.mod.config.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.mod.I18n;

public class EnumItem<E extends Enum>
  extends Item<E>
{
  private final Class<E> e;
  
  public EnumItem(String key, String category, E defaultValue, Class<E> e)
  {
    super(key, category, defaultValue);
    this.e = e;
  }
  
  public void deserialize(JsonObject object)
  {
    set(Enum.valueOf(this.e, object.get(getKey()).getAsString()));
  }
  
  public void serialize(JsonObject object)
  {
    object.addProperty(getKey(), ((Enum)get()).toString());
  }
  
  public void next()
  {
    set(((Enum[])this.e.getEnumConstants())[((((Enum)get()).ordinal() + 1) % ((Enum[])this.e.getEnumConstants()).length)]);
  }
  
  public String translateValue()
  {
    return I18n.translate(getTranslationPrefix() + "." + getCategory() + "." + ((Enum)get()).name().toLowerCase());
  }
}
