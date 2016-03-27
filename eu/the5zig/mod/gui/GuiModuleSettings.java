package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew.Location;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.gui.elements.ButtonRow;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.StaticModulePreviewRow;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Collections;
import java.util.List;

public class GuiModuleSettings
  extends Gui
{
  private final Module module;
  private List<ButtonRow> settings = Lists.newArrayList();
  
  public GuiModuleSettings(Gui lastScreen, Module module)
  {
    super(lastScreen);
    this.module = module;
  }
  
  public void initGui()
  {
    addBottomDoneButton();
    
    IGuiList guiList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 32, getHeight() - 48, getWidth() / 2 - 180, getWidth() / 2 - 10, this.settings);
    guiList.setRowWidth(160);
    guiList.setLeftbound(true);
    guiList.setDrawSelection(false);
    guiList.setScrollX(getWidth() / 2 - 15);
    guiList.setHeaderPadding(The5zigMod.getVars().getFontHeight());
    guiList.setHeader(I18n.translate("modules.settings.list.title"));
    addGuiList(guiList);
    
    this.settings.clear();
    this.settings.add(new ButtonRow(The5zigMod.getVars().createButton(1, getWidth() / 2 - 180, 0, 160, 20, I18n.translate("modules.settings.id") + ": \"" + 
      The5zigMod.getVars().shortenToWidth(this.module.getId(), 145 - The5zigMod.getVars().getStringWidth(new StringBuilder().append(I18n.translate("modules.settings.id")).append(": \"\"").toString())) + "\""), null));
    this.settings.add(new ButtonRow(The5zigMod.getVars().createButton(2, getWidth() / 2 - 180, 0, 160, 20, I18n.translate("modules.settings.name") + ": " + (
      (this.module.getName() == null) || (this.module.getName().isEmpty()) ? I18n.translate("modules.settings.none") : new StringBuilder().append("\"").append(The5zigMod.getVars().shortenToWidth(this.module.getName(), 145 - 
      The5zigMod.getVars().getStringWidth(new StringBuilder().append(I18n.translate("modules.settings.name")).append(": \"\"").toString()))).append("\"").toString())), null));
    
    this.settings.add(new ButtonRow(The5zigMod.getVars().createButton(3, getWidth() / 2 - 180, 0, 160, 20, I18n.translate("modules.settings.translation") + ": " + (
      (this.module.getTranslation() == null) || (this.module.getTranslation().isEmpty()) ? I18n.translate("modules.settings.none") : new StringBuilder().append("\"").append(The5zigMod.getVars().shortenToWidth(this.module
      .getTranslation(), 145 - The5zigMod.getVars().getStringWidth(new StringBuilder().append(I18n.translate("modules.settings.translation")).append(": \"\"").toString()))).append("\"").toString())), null));
    this.settings.add(new ButtonRow(The5zigMod.getVars().createButton(4, getWidth() / 2 - 180, 0, 160, 20, I18n.translate("modules.settings.items", new Object[] { Integer.valueOf(this.module.getItems().size()) })), null));
    this.settings.add(new ButtonRow(
      The5zigMod.getVars().createButton(5, getWidth() / 2 - 180, 0, 160, 20, I18n.translate("modules.settings.show_label") + ": " + The5zigMod.toBoolean(this.module.isShowLabel())), null));
    
    this.settings.add(new ButtonRow(The5zigMod.getVars().createButton(6, getWidth() / 2 - 180, 0, 160, 20, 
      I18n.translate("modules.location") + ": " + I18n.translate(new StringBuilder().append("modules.location.").append(this.module.getLocation().toString().toLowerCase()).toString())), null));
    this.settings.add(new ButtonRow(The5zigMod.getVars().createButton(7, getWidth() / 2 - 180, 0, 160, 20, I18n.translate("modules.settings.server") + ": " + (
      (this.module.getServer() == null) || (this.module.getServer().isEmpty()) ? I18n.translate("modules.settings.none") : ServerInstance.byConfigName(this.module.getServer()).getName())), null));
    this.settings.add(new ButtonRow(The5zigMod.getVars().createButton(8, getWidth() / 2 - 180, 0, 160, 20, 
      I18n.translate("modules.settings.render") + ": " + (this.module.getRenderType() == null ? I18n.translate("modules.settings.none") : this.module.getRenderType())), null));
    
    IGuiList modulePreviewList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 32, getHeight() - 48, getWidth() / 2 + 10, getWidth() / 2 + 180, 
      Collections.singletonList(new StaticModulePreviewRow(this.module, getWidth() / 2 + 10, 33, 170, getHeight() - 48 - 34)));
    modulePreviewList.setLeftbound(true);
    modulePreviewList.setDrawSelection(false);
    modulePreviewList.setScrollX(getWidth() / 2 + 175);
    addGuiList(modulePreviewList);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1) {
      The5zigMod.getVars().displayScreen(new GuiCenteredTextfield(this, new CenteredTextfieldCallback()
      {
        public void onDone(String text)
        {
          if (!text.isEmpty())
          {
            for (Module m : The5zigMod.getModuleMaster().getModules()) {
              if (m.getId().equals(text))
              {
                The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.RED + I18n.translate("modules.settings.id.already_used"));
                return;
              }
            }
            GuiModuleSettings.this.module.setId(text);
            The5zigMod.getModuleMaster().save();
          }
        }
        
        public String title()
        {
          return I18n.translate("modules.settings.id.change");
        }
      }, this.module.getId(), -1, 100));
    }
    if (button.getId() == 2) {
      The5zigMod.getVars().displayScreen(new GuiCenteredTextfield(this, new CenteredTextfieldCallback()
      {
        public void onDone(String text)
        {
          if (text.isEmpty()) {
            GuiModuleSettings.this.module.setName(null);
          } else {
            GuiModuleSettings.this.module.setName(text);
          }
          The5zigMod.getModuleMaster().save();
        }
        
        public String title()
        {
          return I18n.translate("modules.settings.name.change");
        }
      }, this.module.getName() == null ? "" : this.module.getName(), -1, 100));
    }
    if (button.getId() == 3) {
      The5zigMod.getVars().displayScreen(new GuiCenteredTextfield(this, new CenteredTextfieldCallback()
      {
        public void onDone(String text)
        {
          if (text.isEmpty()) {
            GuiModuleSettings.this.module.setTranslation(null);
          } else {
            GuiModuleSettings.this.module.setTranslation(text);
          }
          The5zigMod.getModuleMaster().save();
        }
        
        public String title()
        {
          return I18n.translate("modules.settings.translation.change");
        }
      }, this.module.getTranslation() == null ? "" : this.module.getTranslation(), -1, 100));
    }
    if (button.getId() == 4) {
      The5zigMod.getVars().displayScreen(new GuiModuleItems(this, this.module));
    }
    if (button.getId() == 5)
    {
      this.module.setShowLabel(!this.module.isShowLabel());
      button.setLabel(I18n.translate("modules.settings.show_label") + ": " + The5zigMod.toBoolean(this.module.isShowLabel()));
      The5zigMod.getModuleMaster().save();
    }
    if (button.getId() == 6) {
      The5zigMod.getVars().displayScreen(new GuiModuleLocation(this, this.module));
    }
    if (button.getId() == 7) {
      The5zigMod.getVars().displayScreen(new GuiModuleServer(this, this.module));
    }
    if (button.getId() == 8) {
      The5zigMod.getVars().displayScreen(new GuiModuleRender(this, this.module));
    }
  }
  
  public String getTitleKey()
  {
    return "modules.settings.title";
  }
}
