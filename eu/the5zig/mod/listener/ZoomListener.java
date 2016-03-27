package eu.the5zig.mod.listener;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.manager.KeybindingManager;
import eu.the5zig.mod.util.IKeybinding;
import eu.the5zig.mod.util.IVariables;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ZoomListener
  extends Listener
{
  private boolean zoomed = false;
  private float previousFOV;
  private boolean previousSmoothCamera;
  
  public void onTick()
  {
    if ((The5zigMod.getVars().getMinecraftScreen() == null) && (isKeyDown(The5zigMod.getKeybindingManager().zoom)))
    {
      if (!this.zoomed)
      {
        this.zoomed = true;
        this.previousFOV = The5zigMod.getVars().getFOV();
        this.previousSmoothCamera = The5zigMod.getVars().isSmoothCamera();
        
        The5zigMod.getVars().setFOV(this.previousFOV / The5zigMod.getConfig().getFloat("zoomFactor"));
        The5zigMod.getVars().setSmoothCamera(true);
      }
    }
    else if (this.zoomed)
    {
      this.zoomed = false;
      
      The5zigMod.getVars().setFOV(this.previousFOV);
      The5zigMod.getVars().setSmoothCamera(this.previousSmoothCamera);
    }
  }
  
  private boolean isKeyDown(IKeybinding keybinding)
  {
    return (keybinding.getKeyCode() != 0) && (keybinding.getKeyCode() < 0 ? Mouse.isButtonDown(keybinding.getKeyCode() + 100) : Keyboard.isKeyDown(keybinding.getKeyCode()));
  }
}
