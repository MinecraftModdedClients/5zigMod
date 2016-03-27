package eu.the5zig.mod.config.items;

import com.google.gson.JsonObject;
import eu.the5zig.mod.I18n;
import eu.the5zig.util.Utils;

public abstract class NonConfigItem
  extends Item<Object>
  implements INonConfigItem
{
  public NonConfigItem(String key, String category)
  {
    super(key, category, null);
  }
  
  public final void deserialize(JsonObject object) {}
  
  public final void serialize(JsonObject object) {}
  
  public final void next() {}
  
  public abstract void action();
  
  public String translate()
  {
    return I18n.translate(getTranslationPrefix() + "." + getCategory() + "." + Utils.upperToDash(getKey()));
  }
}
