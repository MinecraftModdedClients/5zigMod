package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;

public class GuiRaidCalculator
  extends GuiOptions
{
  private String result = "";
  
  public GuiRaidCalculator(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    addDoneButton();
    
    addTextField(The5zigMod.getVars().createTextfield(1, getWidth() / 2 - 140, 60, 80, 20));
    addTextField(The5zigMod.getVars().createTextfield(2, getWidth() / 2 - 40, 60, 80, 20));
    addTextField(The5zigMod.getVars().createTextfield(3, getWidth() / 2 + 60, 60, 80, 20));
    addButton(The5zigMod.getVars().createButton(12, getWidth() / 2 - 102, 90, 100, 20, I18n.translate("raid.track", new Object[] { I18n.translate("raid.east").toUpperCase() })));
    addButton(The5zigMod.getVars().createButton(13, getWidth() / 2 + 2, 90, 100, 20, I18n.translate("raid.track", new Object[] { I18n.translate("raid.west").toUpperCase() })));
    
    addTextField(The5zigMod.getVars().createTextfield(4, getWidth() / 2 - 140, 130, 80, 20));
    addTextField(The5zigMod.getVars().createTextfield(5, getWidth() / 2 - 40, 130, 80, 20));
    addTextField(The5zigMod.getVars().createTextfield(6, getWidth() / 2 + 60, 130, 80, 20));
    addButton(The5zigMod.getVars().createButton(10, getWidth() / 2 - 102, 160, 100, 20, I18n.translate("raid.track", new Object[] { I18n.translate("raid.north") }).toUpperCase()));
    addButton(The5zigMod.getVars().createButton(11, getWidth() / 2 + 2, 160, 100, 20, I18n.translate("raid.track", new Object[] { I18n.translate("raid.south").toUpperCase() })));
    for (int i = 1; i < getButtonList().size(); i++) {
      ((IButton)getButtonList().get(i)).setEnabled((((ITextfield)this.textfields.get(0)).getText().length() > 0) && (((ITextfield)this.textfields.get(1)).getText().length() > 0) && (((ITextfield)this.textfields.get(2)).getText().length() > 0) && 
        (isInt(((ITextfield)this.textfields.get(0)).getText())) && (isInt(((ITextfield)this.textfields.get(1)).getText())) && (isInt(((ITextfield)this.textfields.get(2)).getText())));
    }
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 10)
    {
      int z = Integer.parseInt(getTextfieldById(4).getText());
      int trackedZ = Integer.parseInt(getTextfieldById(5).getText());
      int narrowZ = Integer.parseInt(getTextfieldById(6).getText());
      
      trackedZ = -trackedZ;
      
      int resZ = z + trackedZ - narrowZ / 2;
      
      this.result = (ChatColor.RED + I18n.translate("raid.tracked", new Object[] { String.valueOf(trackedZ), String.valueOf(z), "§6" + resZ + ChatColor.RED, "§4" + String.valueOf(resZ / 8.0D) + ChatColor.RED, 
        I18n.translate("raid.north") }));
    }
    if (button.getId() == 11)
    {
      int z = Integer.parseInt(getTextfieldById(4).getText());
      int trackedZ = Integer.parseInt(getTextfieldById(5).getText());
      int narrowZ = Integer.parseInt(getTextfieldById(6).getText());
      
      int resZ = z + trackedZ - narrowZ / 2;
      
      this.result = (ChatColor.RED + I18n.translate("raid.tracked", new Object[] { String.valueOf(trackedZ), String.valueOf(z), "§6" + resZ + ChatColor.RED, "§4" + String.valueOf(resZ / 8.0D) + ChatColor.RED, 
        I18n.translate("raid.south") }));
    }
    if (button.getId() == 12)
    {
      int x = Integer.parseInt(getTextfieldById(1).getText());
      int trackedX = Integer.parseInt(getTextfieldById(2).getText());
      int narrowX = Integer.parseInt(getTextfieldById(3).getText());
      
      trackedX = -trackedX;
      
      int resX = x + trackedX - narrowX / 2;
      
      this.result = (ChatColor.RED + I18n.translate("raid.tracked", new Object[] { String.valueOf(trackedX), String.valueOf(x), "§6" + resX + ChatColor.RED, "§4" + String.valueOf(resX / 8.0D) + ChatColor.RED, 
        I18n.translate("raid.east") }));
    }
    if (button.getId() == 13)
    {
      int x = Integer.parseInt(getTextfieldById(1).getText());
      int trackedX = Integer.parseInt(getTextfieldById(2).getText());
      int narrowX = Integer.parseInt(getTextfieldById(3).getText());
      
      int resX = x + trackedX - narrowX / 2;
      this.result = (ChatColor.RED + I18n.translate("raid.tracked", new Object[] { String.valueOf(trackedX), String.valueOf(x), "§6" + resX + ChatColor.RED, "§4" + String.valueOf(resX / 8.0D) + ChatColor.RED, 
        I18n.translate("raid.west") }));
    }
  }
  
  protected void onKeyType(char character, int key)
  {
    for (int i = 1; i < 3; i++) {
      ((IButton)getButtonList().get(i)).setEnabled((((ITextfield)this.textfields.get(0)).getText().length() > 0) && (((ITextfield)this.textfields.get(1)).getText().length() > 0) && (((ITextfield)this.textfields.get(2)).getText().length() > 0) && 
        (isInt(((ITextfield)this.textfields.get(0)).getText())) && (isInt(((ITextfield)this.textfields.get(1)).getText())) && (isInt(((ITextfield)this.textfields.get(2)).getText())));
    }
    for (int i = 3; i < 5; i++) {
      ((IButton)getButtonList().get(i)).setEnabled((((ITextfield)this.textfields.get(3)).getText().length() > 0) && (((ITextfield)this.textfields.get(4)).getText().length() > 0) && (((ITextfield)this.textfields.get(5)).getText().length() > 0) && 
        (isInt(((ITextfield)this.textfields.get(3)).getText())) && (isInt(((ITextfield)this.textfields.get(4)).getText())) && (isInt(((ITextfield)this.textfields.get(5)).getText())));
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawCenteredString(ChatColor.RED + this.result, getWidth() / 2, 190);
    
    The5zigMod.getVars().drawString("X: ", getWidth() / 2 - 155, 69);
    drawCenteredString(I18n.translate("raid.tracker_coords"), getWidth() / 2 - 100, 40);
    drawCenteredString(I18n.translate("raid.tracked_blocks"), getWidth() / 2, 40);
    drawCenteredString(I18n.translate("raid.narrow"), getWidth() / 2 + 100, 40);
    
    The5zigMod.getVars().drawString("Z: ", getWidth() / 2 - 155, 139);
  }
  
  public String getTitleKey()
  {
    return "raid.title";
  }
}
