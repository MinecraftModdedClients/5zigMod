package eu.the5zig.mod.server.hypixel.api;

import com.google.gson.JsonObject;
import eu.the5zig.mod.server.hypixel.HypixelGameType;
import java.util.Arrays;
import java.util.List;

public class HypixelStatQuake
  extends HypixelStatCategory
{
  public HypixelStatQuake()
  {
    super(HypixelGameType.QUAKECRAFT);
  }
  
  public List<String> getStats(JsonObject object)
  {
    return Arrays.asList(new String[] { parseInt(object, "coins"), parseInt(object, "kills"), parseInt(object, "deaths"), parseInt(object, "killstreaks"), parseInt(object, "wins") });
  }
}
