package eu.the5zig.mod.config.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.mod.I18n;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import org.apache.commons.lang3.StringUtils;

public class SelectColorItem
  extends Item<ChatColor>
{
  public SelectColorItem(String key, String category, ChatColor defaultValue)
  {
    super(key, category, defaultValue);
  }
  
  public void deserialize(JsonObject object)
  {
    set(ChatColor.valueOf(object.get(getKey()).getAsString()));
  }
  
  public void serialize(JsonObject object)
  {
    object.addProperty(getKey(), ((ChatColor)get()).name());
  }
  
  public void next() {}
  
  public String translate()
  {
    return I18n.translate(new StringBuilder().append(getTranslationPrefix()).append(".").append(getCategory()).append(".").append(Utils.upperToDash(getKey())).toString()) + ":";
  }
  
  public String translateValue()
  {
    return StringUtils.capitalize(((ChatColor)get()).name().toLowerCase().replace("_", ""));
  }
}
