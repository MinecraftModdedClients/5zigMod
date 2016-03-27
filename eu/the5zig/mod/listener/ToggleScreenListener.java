package eu.the5zig.mod.listener;

import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.GuiWelcome;
import eu.the5zig.mod.gui.IWrappedGui;
import eu.the5zig.mod.manager.KeybindingManager;
import eu.the5zig.mod.util.IKeybinding;
import eu.the5zig.mod.util.IVariables;

public class ToggleScreenListener
  extends Listener
{
  private int lastPressed = 0;
  private IWrappedGui lastScreen = null;
  
  private void toggleScreen()
  {
    if ((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat))
    {
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
    else
    {
      if (((The5zigMod.getVars().getMinecraftScreen() instanceof Gui)) && (!(The5zigMod.getVars().getCurrentScreen() instanceof GuiWelcome))) {
        this.lastScreen = null;
      } else {
        this.lastScreen = MinecraftFactory.getVars().createWrappedGui(The5zigMod.getVars().getMinecraftScreen());
      }
      The5zigMod.getVars().displayScreen(new GuiChat(this.lastScreen));
    }
  }
  
  public void onTick()
  {
    if (this.lastPressed > 0) {
      this.lastPressed -= 1;
    }
  }
  
  public void onKeyPress(int code)
  {
    if ((code == The5zigMod.getKeybindingManager().toggleChat.getKeyCode()) && (this.lastPressed++ == 0)) {
      toggleScreen();
    }
  }
}
