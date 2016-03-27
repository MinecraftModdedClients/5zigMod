package eu.the5zig.mod.listener;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.items.BoolItem;
import eu.the5zig.mod.gui.GuiCoordinatesClipboard;
import eu.the5zig.mod.gui.GuiHypixelStats;
import eu.the5zig.mod.gui.GuiRaidCalculator;
import eu.the5zig.mod.manager.KeybindingManager;
import eu.the5zig.mod.util.IKeybinding;
import eu.the5zig.mod.util.IVariables;

public class KeybindingListener
  extends Listener
{
  private int lastPressed = 0;
  
  public void onTick()
  {
    KeybindingManager keybindingManager = The5zigMod.getKeybindingManager();
    if (keybindingManager.toggleMod.isPressed())
    {
      ((BoolItem)The5zigMod.getConfig().get("showMod", BoolItem.class)).next();
      The5zigMod.getConfig().save();
    }
    if (keybindingManager.saveCoords.isPressed()) {
      The5zigMod.getVars().displayScreen(new GuiCoordinatesClipboard(null));
    }
    if (keybindingManager.raidTracker.isPressed()) {
      The5zigMod.getVars().displayScreen(new GuiRaidCalculator(null));
    }
    if (this.lastPressed > 0) {
      this.lastPressed -= 1;
    }
  }
  
  public void onKeyPress(int code)
  {
    if ((code == The5zigMod.getKeybindingManager().hypixel.getKeyCode()) && (this.lastPressed++ == 0)) {
      The5zigMod.getVars().displayScreen(new GuiHypixelStats(The5zigMod.getVars().createWrappedGui(The5zigMod.getVars().getMinecraftScreen())));
    }
  }
}
