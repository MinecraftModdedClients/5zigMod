package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew.Location;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.util.IVariables;
import org.lwjgl.input.Mouse;

public class GuiModuleLocation
  extends Gui
{
  private static final float CENTER_THRESHOLD = 0.03F;
  private final Module module;
  private boolean pressed = false;
  private int xOff;
  private int yOff;
  
  public GuiModuleLocation(Gui lastScreen, Module module)
  {
    super(lastScreen);
    this.module = module;
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 - 100, getHeight() - 20, 100, 20, The5zigMod.getVars().translate("gui.done", new Object[0])));
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2, getHeight() - 20, 100, 20, I18n.translate("modules.location." + this.module.getLocation().toString().toLowerCase())));
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 200) {
      The5zigMod.getModuleMaster().save();
    }
    if (button.getId() == 100)
    {
      this.module.setLocation(this.module.getLocation().getNext());
      this.module.setLocationX(0.0F);
      this.module.setLocationY(0.0F);
      button.setLabel(I18n.translate("modules.location." + this.module.getLocation().toString().toLowerCase()));
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawMenuBackground();
    
    The5zigMod.getVars().updateScaledResolution();
    
    int width = this.module.getMaxWidth(true);
    int height = this.module.getTotalHeight(true) - 6;
    float maxWidthOff = width / getWidth();
    float totalHeightOff = height / getHeight();
    int y;
    int y;
    int y;
    int y;
    int y;
    int y;
    int y;
    switch (this.module.getLocation())
    {
    case TOP_LEFT: 
      int x = 2;
      y = 2;
      break;
    case TOP_RIGHT: 
      int x = getWidth() - width - 2;
      y = 2;
      break;
    case CENTER_LEFT: 
      int x = 2;
      y = (getHeight() - height) / 2;
      break;
    case CENTER_RIGHT: 
      int x = getWidth() - width - 2;
      y = (getHeight() - height) / 2;
      break;
    case BOTTOM_LEFT: 
      int x = 2;
      y = getHeight() - 2 - height;
      break;
    case BOTTOM_RIGHT: 
      int x = getWidth() - width - 2;
      y = getHeight() - 2 - height;
      break;
    case CUSTOM: 
      int x = (int)(getWidth() * this.module.getLocationX());
      y = (int)(getHeight() * this.module.getLocationY());
      break;
    default: 
      throw new AssertionError();
    }
    int y;
    int x;
    this.module.render(The5zigMod.getRenderer(), x, y, true);
    if (this.module.getLocationX() > 0.5D) {
      x -= width;
    }
    if (this.module.getLocationX() == 0.5D) {
      x -= width / 2;
    }
    int color = -65536;
    if (this.pressed)
    {
      float locationX = (mouseX - this.xOff) / getWidth();
      float locationY = (mouseY - this.yOff) / getHeight();
      if ((locationX + maxWidthOff / 2.0F > 0.47000000067055225D) && (this.module.getLocationX() < 0.5D))
      {
        locationX = 0.5F;
        if (locationX + maxWidthOff / 2.0F > 0.5D) {
          this.xOff -= width;
        }
      }
      else if ((locationX - maxWidthOff / 2.0F < 0.5299999993294477D) && (this.module.getLocationX() > 0.5D))
      {
        locationX = 0.5F;
        if (locationX - maxWidthOff / 2.0F < 0.5D) {
          this.xOff += width;
        }
      }
      else if ((locationX + maxWidthOff / 2.0F > 0.47000000067055225D) && (locationX - maxWidthOff / 2.0F < 0.5299999993294477D))
      {
        locationX = 0.5F;
      }
      if (locationX < 0.0F) {
        locationX = 0.0F;
      }
      if (locationX > 1.0F) {
        locationX = 1.0F;
      }
      if (locationY < 0.0F) {
        locationY = 0.0F;
      }
      if (locationY + 0.08D > 1.0D) {
        locationY = 0.92F;
      }
      this.module.setLocationX(locationX);
      this.module.setLocationY(locationY);
      if (!Mouse.isButtonDown(0)) {
        this.pressed = false;
      }
    }
    if ((this.pressed) || ((mouseX >= x) && (mouseX <= x + width) && (mouseY >= y) && (mouseY <= y + height)))
    {
      color = -6750208;
      if ((Mouse.isButtonDown(0)) && (!this.pressed))
      {
        this.pressed = true;
        this.xOff = (mouseX - x);
        if (this.module.getLocationX() > 0.5D) {
          this.xOff -= width;
        }
        this.yOff = (mouseY - y);
        getButtonById(100).setLabel(ConfigNew.Location.CUSTOM.toString());
        this.module.setLocation(ConfigNew.Location.CUSTOM);
      }
    }
    drawRectOutline(x, y, x + width, y + height, color);
  }
  
  protected void onEscapeType()
  {
    The5zigMod.getVars().displayScreen(this.lastScreen);
  }
  
  public String getTitleName()
  {
    return "";
  }
}
