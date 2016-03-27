package eu.the5zig.mod.modules.items.server;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.render.LargeTextRenderer;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.GameServer;
import eu.the5zig.mod.server.GameState;

public abstract class LargeTextItem<T extends GameMode>
  extends GameModeItem<T>
{
  public LargeTextItem(Class<? extends GameServer> serverClass, Class<? extends T> modeClass, GameState... state)
  {
    super(serverClass, modeClass, state);
  }
  
  public void render(int x, int y, RenderLocation renderLocation, boolean dummy)
  {
    if (dummy) {
      return;
    }
    if (isOnline())
    {
      String text = getText();
      if (text != null) {
        DisplayRenderer.largeTextRenderer.render(The5zigMod.getRenderer().getPrefix() + text);
      }
    }
  }
  
  protected abstract String getText();
  
  protected Object getValue(boolean dummy)
  {
    return null;
  }
  
  public int getWidth(boolean dummy)
  {
    return 0;
  }
  
  public int getHeight(boolean dummy)
  {
    return 0;
  }
}
