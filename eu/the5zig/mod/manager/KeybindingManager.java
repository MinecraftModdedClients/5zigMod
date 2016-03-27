package eu.the5zig.mod.manager;

import com.google.common.collect.Lists;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.util.IKeybinding;
import eu.the5zig.mod.util.IVariables;
import java.util.List;

public class KeybindingManager
{
  public IKeybinding toggleMod;
  public IKeybinding toggleChat;
  public IKeybinding saveCoords;
  public IKeybinding raidTracker;
  public IKeybinding hypixel;
  public IKeybinding zoom;
  private List<IKeybinding> customKeybindings = Lists.newArrayList();
  
  public KeybindingManager()
  {
    this.toggleMod = registerKeybinding("Toggle Mod", 50, "The 5zig Mod");
    this.toggleChat = registerKeybinding("Toggle Chat", 62, "The 5zig Mod");
    this.saveCoords = registerKeybinding("Coord Clipboard", 88, "The 5zig Mod");
    this.raidTracker = registerKeybinding("MCPVP Raid Calculator", 74, "The 5zig Mod");
    this.hypixel = registerKeybinding("Hypixel-Stats", 66, "The 5zig Mod");
    this.zoom = registerKeybinding("Zoom in", 44, "The 5zig Mod");
    
    The5zigMod.getVars().registerKeybindings(this.customKeybindings);
  }
  
  public IKeybinding registerKeybinding(String description, int keyCode, String category)
  {
    IKeybinding keybinding = The5zigMod.getVars().createKeybinding(description, keyCode, category);
    this.customKeybindings.add(keybinding);
    return keybinding;
  }
}
