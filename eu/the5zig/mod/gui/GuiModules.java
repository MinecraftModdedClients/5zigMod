package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew.Location;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.StaticModulePreviewRow;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class GuiModules
  extends Gui
  implements Clickable<Module>
{
  private IGuiList<Module> guiList;
  private int lastSelected;
  private StaticModulePreviewRow previewRow;
  private boolean displayHelp;
  private IButton closeHelpButton;
  
  public GuiModules(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    addBottomDoneButton();
    
    this.guiList = The5zigMod.getVars().createGuiList(this, getWidth(), getHeight(), 50, getHeight() - 32 - 48, getWidth() / 2 - 180, getWidth() / 2 - 10, 
      The5zigMod.getModuleMaster().getModules());
    this.guiList.setRowWidth(160);
    this.guiList.setLeftbound(true);
    this.guiList.setScrollX(getWidth() / 2 - 15);
    this.guiList.setSelectedId(this.lastSelected);
    this.guiList.setHeaderPadding(The5zigMod.getVars().getFontHeight());
    this.guiList.setHeader(I18n.translate("modules.list.title"));
    addGuiList(this.guiList);
    
    IGuiList modulePreviewList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 50, getHeight() - 50 - 48, getWidth() / 2 + 10, getWidth() / 2 + 180, 
      Collections.singletonList(this.previewRow = new StaticModulePreviewRow((Module)this.guiList.getSelectedRow(), getWidth() / 2 + 10, 51, 170, getHeight() - 150)));
    modulePreviewList.setLeftbound(true);
    modulePreviewList.setDrawSelection(false);
    modulePreviewList.setScrollX(getWidth() / 2 + 175);
    addGuiList(modulePreviewList);
    
    String helpText = I18n.translate("modules.help");
    addButton(The5zigMod.getVars().createStringButton(99, getWidth() / 2 - 180, 30, 
      The5zigMod.getVars().getStringWidth(ChatColor.ITALIC.toString() + ChatColor.UNDERLINE.toString() + helpText), 10, ChatColor.ITALIC
      .toString() + ChatColor.UNDERLINE.toString() + helpText));
    String resetText = I18n.translate("modules.reset");
    addButton(The5zigMod.getVars()
      .createStringButton(98, getWidth() / 2 + 180 - The5zigMod.getVars().getStringWidth(resetText), 30, The5zigMod.getVars().getStringWidth(resetText), 10, resetText));
    
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 180, getHeight() - 48 - 20, 145, 20, I18n.translate("modules.add")));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 - 30, getHeight() - 48 - 20, 20, 20, "-"));
    addButton(The5zigMod.getVars().createButton(5, getWidth() / 2 + 10, getHeight() - 48 - 44, 170, 20, I18n.translate("modules.settings")));
    addButton(The5zigMod.getVars().createButton(10, getWidth() / 2 + 10, getHeight() - 48 - 20, 80, 20, I18n.translate("modules.move_up")));
    addButton(The5zigMod.getVars().createButton(11, getWidth() / 2 + 100, getHeight() - 48 - 20, 80, 20, I18n.translate("modules.move_down")));
    
    this.closeHelpButton = The5zigMod.getVars().createButton(50, getWidth() / 2 - 75, (getHeight() - 200) / 2 + 135, 150, 20, The5zigMod.getVars().translate("gui.done", new Object[0]));
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      int currentIndex = this.guiList.getSelectedId();
      String moduleId = "new-module";
      boolean noMatch = true;
      for (;;)
      {
        for (Module module : The5zigMod.getModuleMaster().getModules()) {
          if (moduleId.equals(module.getId()))
          {
            moduleId = moduleId + "-";
            noMatch = false;
            break;
          }
        }
        if (noMatch) {
          break;
        }
        noMatch = true;
      }
      Module module = new Module(moduleId, null, null, null, true, ConfigNew.Location.TOP_LEFT, 0.0F, 0.0F);
      The5zigMod.getModuleMaster().getModules().add(currentIndex, module);
      The5zigMod.getModuleMaster().save();
      onSelect(currentIndex, module, false);
    }
    if (button.getId() == 2)
    {
      int currentIndex = this.guiList.getSelectedId();
      List<Module> modules = The5zigMod.getModuleMaster().getModules();
      if (!modules.isEmpty())
      {
        modules.remove(currentIndex);
        The5zigMod.getModuleMaster().save();
      }
      onSelect(this.guiList.getSelectedId(), (Module)this.guiList.getSelectedRow(), false);
    }
    if (button.getId() == 5)
    {
      Module module = (Module)this.guiList.getSelectedRow();
      if (module != null) {
        The5zigMod.getVars().displayScreen(new GuiModuleSettings(this, module));
      }
    }
    if ((button.getId() == 10) && 
      (move(-1))) {
      The5zigMod.getModuleMaster().save();
    }
    if ((button.getId() == 11) && 
      (move(1))) {
      The5zigMod.getModuleMaster().save();
    }
    if (button.getId() == 98) {
      The5zigMod.getVars().displayScreen(new GuiYesNo(this, new YesNoCallback()
      {
        public void onDone(boolean yes)
        {
          if (yes) {
            try
            {
              The5zigMod.getModuleMaster().createDefault();
              The5zigMod.getModuleMaster().save();
            }
            catch (Exception e)
            {
              The5zigMod.logger.error("Could not reset modules!", e);
            }
          }
        }
        
        public String title()
        {
          return I18n.translate("modules.reset.title");
        }
      }));
    }
    if (button.getId() == 99) {
      this.displayHelp = true;
    }
  }
  
  public void handleMouseInput0()
  {
    if (!this.displayHelp) {
      super.handleMouseInput0();
    }
  }
  
  public void mouseClicked0(int x, int y, int button)
  {
    if (this.displayHelp)
    {
      if (this.closeHelpButton.mouseClicked(x, y))
      {
        this.closeHelpButton.playClickSound();
        this.displayHelp = false;
      }
      return;
    }
    super.mouseClicked0(x, y, button);
  }
  
  protected void mouseReleased(int x, int y, int state)
  {
    if (this.displayHelp) {
      this.closeHelpButton.mouseReleased(x, y);
    }
  }
  
  protected void tick()
  {
    getButtonById(2).setEnabled(this.guiList.getSelectedRow() != null);
    getButtonById(10).setEnabled(this.guiList.getSelectedId() > 0);
    getButtonById(11).setEnabled(this.guiList.getSelectedId() < The5zigMod.getModuleMaster().getModules().size() - 1);
    if (this.displayHelp) {
      this.closeHelpButton.tick();
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks) {}
  
  public void drawScreen0(int mouseX, int mouseY, float partialTicks)
  {
    super.drawScreen0(mouseX, mouseY, partialTicks);
    if (this.displayHelp)
    {
      GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
      The5zigMod.getVars().bindTexture(The5zigMod.DEMO_BACKGROUND);
      drawTexturedModalRect((getWidth() - 247) / 2, (getHeight() - 200) / 2, 0, 0, 256, 256);
      The5zigMod.getVars().drawCenteredString(ChatColor.BOLD + I18n.translate("modules.help"), getWidth() / 2, (getHeight() - 200) / 2 + 10);
      int y = 0;
      for (String line : The5zigMod.getVars().splitStringToWidth(I18n.translate("modules.help.display"), 236))
      {
        drawCenteredString(ChatColor.WHITE + line, getWidth() / 2, (getHeight() - 200) / 2 + 30 + y);
        y += 10;
      }
      this.closeHelpButton.draw(mouseX, mouseY);
    }
  }
  
  public void onSelect(int id, Module module, boolean doubleClick)
  {
    this.lastSelected = id;
    this.previewRow.setModule(module);
  }
  
  public String getTitleKey()
  {
    return "modules.title";
  }
  
  private boolean move(int pos)
  {
    Module module = (Module)this.guiList.getSelectedRow();
    if (module == null) {
      return false;
    }
    List<Module> modules = The5zigMod.getModuleMaster().getModules();
    
    int currentIndex = modules.indexOf(module);
    int nextIndex = currentIndex + pos;
    if ((nextIndex >= 0) && (nextIndex < modules.size()))
    {
      Collections.swap(modules, currentIndex, nextIndex);
      this.guiList.setSelectedId(nextIndex);
      return true;
    }
    return false;
  }
}
