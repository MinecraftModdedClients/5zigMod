package eu.the5zig.mod.config.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class ColorFormattingItem
  extends Item<ChatColor>
{
  private static final List<ChatColor> formattings = Arrays.asList(new ChatColor[] { ChatColor.RESET, ChatColor.BOLD, ChatColor.ITALIC, ChatColor.UNDERLINE });
  
  public ColorFormattingItem(String key, String category, ChatColor defaultValue)
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
  
  public void next()
  {
    ChatColor current = !formattings.contains(get()) ? ChatColor.RESET : (ChatColor)get();
    set(formattings.get((formattings.indexOf(current) + 1) % formattings.size()));
  }
  
  public String translateValue()
  {
    return StringUtils.capitalize(((ChatColor)get()).getName().replace("_", ""));
  }
}
