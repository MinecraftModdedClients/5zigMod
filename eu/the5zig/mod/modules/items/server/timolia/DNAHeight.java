package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.DNA;

public class DNAHeight
  extends GameModeItem<ServerTimolia.DNA>
{
  public DNAHeight()
  {
    super(ServerTimolia.class, ServerTimolia.DNA.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return Integer.valueOf(16);
    }
    return shorten(((ServerTimolia.DNA)getGameMode()).getHeight()) + "/" + shorten(32.0D);
  }
  
  public String getTranslation()
  {
    return "ingame.height";
  }
}
