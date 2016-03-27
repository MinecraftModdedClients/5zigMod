package eu.the5zig.mod.modules.items.server.timolia;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.items.server.GameModeItem;
import eu.the5zig.mod.server.GameState;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.JumpWorld;
import eu.the5zig.mod.util.IVariables;
import org.lwjgl.util.vector.Vector3f;

public class JumpWorldLastCheckpoint
  extends GameModeItem<ServerTimolia.JumpWorld>
{
  public JumpWorldLastCheckpoint()
  {
    super(ServerTimolia.class, ServerTimolia.JumpWorld.class, new GameState[] { GameState.GAME });
  }
  
  protected Object getValue(boolean dummy)
  {
    if (dummy) {
      return shorten(10.0D) + " m";
    }
    Vector3f lastCheckpoint = ((ServerTimolia.JumpWorld)getGameMode()).getLastCheckpoint();
    if (lastCheckpoint == null) {
      return null;
    }
    Vector3f currentPos = new Vector3f((float)The5zigMod.getVars().getPlayerPosX(), (float)The5zigMod.getVars().getPlayerPosY(), (float)The5zigMod.getVars().getPlayerPosZ());
    Vector3f direction = new Vector3f();
    Vector3f.sub(lastCheckpoint, currentPos, direction);
    return shorten(direction.length()) + " m";
  }
  
  public String getTranslation()
  {
    return "ingame.last_checkpoint";
  }
}
