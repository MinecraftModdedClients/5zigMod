package eu.the5zig.mod.modules.items.server.hypixel;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.hypixel.ServerHypixel;
import eu.the5zig.mod.server.hypixel.ServerHypixel.Paintball;

public class PaintballTeam
  extends GameModeItem<ServerHypixel.Paintball>
{
  public PaintballTeam()
  {
    super(ServerHypixel.class, ServerHypixel.Paintball.class, new GameState[0]);
  }
  
  protected Object getValue(boolean dummy)
  {
    return dummy ? "Red" : ((ServerHypixel.Paintball)getGameMode()).getTeam();
  }
  
  public String getTranslation()
  {
    return "ingame.team";
  }
}
