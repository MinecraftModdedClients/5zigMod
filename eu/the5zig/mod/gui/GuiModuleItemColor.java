package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.gui.elements.ButtonRow;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.modules.items.Item.Color;
import eu.the5zig.mod.util.ColorSelectorCallback;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Arrays;
import java.util.List;

public class GuiModuleItemColor
  extends Gui
{
  private static final List<ChatColor> FORMATTINGS = Arrays.asList(new ChatColor[] { ChatColor.RESET, ChatColor.BOLD, ChatColor.ITALIC, ChatColor.UNDERLINE });
  private final Item item;
  private List<ButtonRow> buttons = Lists.newArrayList();
  private List<IButton> colorButtons = Lists.newArrayList();
  
  public GuiModuleItemColor(Gui lastScreen, Item item)
  {
    super(lastScreen);
    this.item = item;
  }
  
  public void initGui()
  {
    addBottomDoneButton();
    
    IGuiList guiList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 32, getHeight() - 48, 0, getWidth(), this.buttons);
    guiList.setDrawSelection(false);
    addGuiList(guiList);
    
    this.buttons.clear();
    this.buttons.add(new ButtonRow(The5zigMod.getVars().createButton(1, getWidth() / 2 - 75, 0, 150, 20, 
      I18n.translate("modules.settings.item.color") + ": " + (this.item.getColor() == null ? I18n.translate("modules.settings.default") : I18n.translate("modules.settings.custom"))), null));
    
    IButton button1 = The5zigMod.getVars().createButton(10, getWidth() / 2 - 155, 0, 150, 20, I18n.translate("modules.settings.item.prefix_formatting") + ": " + (
      (this.item.getColor() == null) || (this.item.getColor().prefixFormatting == null) ? I18n.translate("modules.settings.default") : this.item.getColor().prefixFormatting.name()));
    IButton button2 = The5zigMod.getVars().createColorSelector(11, getWidth() / 2 + 5, 0, 150, 20, I18n.translate("modules.settings.item.prefix_color"), new ColorSelectorCallback()
    {
      public ChatColor getColor()
      {
        return (GuiModuleItemColor.this.item.getColor() == null) || (GuiModuleItemColor.this.item.getColor().prefixColor == null) ? ChatColor.WHITE : GuiModuleItemColor.this.item.getColor().prefixColor;
      }
      
      public void setColor(ChatColor color)
      {
        GuiModuleItemColor.this.item.getColor().prefixColor = color;
        The5zigMod.getModuleMaster().save();
      }
    });
    this.buttons.add(new ButtonRow(button1, button2));
    this.colorButtons.add(button1);
    this.colorButtons.add(button2);
    IButton button3 = The5zigMod.getVars().createButton(12, getWidth() / 2 - 155, 0, 150, 20, I18n.translate("modules.settings.item.main_formatting") + ": " + (
      (this.item.getColor() == null) || (this.item.getColor().mainFormatting == null) ? I18n.translate("modules.settings.default") : this.item.getColor().mainFormatting.name()));
    IButton button4 = The5zigMod.getVars().createColorSelector(13, getWidth() / 2 + 5, 0, 150, 20, I18n.translate("modules.settings.item.main_color"), new ColorSelectorCallback()
    {
      public ChatColor getColor()
      {
        return (GuiModuleItemColor.this.item.getColor() == null) || (GuiModuleItemColor.this.item.getColor().mainColor == null) ? ChatColor.WHITE : GuiModuleItemColor.this.item.getColor().mainColor;
      }
      
      public void setColor(ChatColor color)
      {
        GuiModuleItemColor.this.item.getColor().mainColor = color;
        The5zigMod.getModuleMaster().save();
      }
    });
    this.buttons.add(new ButtonRow(button3, button4));
    this.colorButtons.add(button3);
    this.colorButtons.add(button4);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      if (this.item.getColor() == null) {
        this.item.setColor(new Item.Color(null, null, null, null));
      } else {
        this.item.setColor(null);
      }
      The5zigMod.getModuleMaster().save();
      button.setLabel(I18n.translate("modules.settings.item.color") + ": " + (this.item
        .getColor() == null ? I18n.translate("modules.settings.default") : I18n.translate("modules.settings.custom")));
    }
    if (button.getId() == 10)
    {
      if (this.item.getColor().prefixFormatting == null) {
        this.item.getColor().prefixFormatting = ((ChatColor)FORMATTINGS.get(0));
      } else if (this.item.getColor().prefixFormatting == FORMATTINGS.get(FORMATTINGS.size() - 1)) {
        this.item.getColor().prefixFormatting = null;
      } else {
        this.item.getColor().prefixFormatting = ((ChatColor)FORMATTINGS.get(FORMATTINGS.indexOf(this.item.getColor().prefixFormatting) + 1));
      }
      The5zigMod.getModuleMaster().save();
      button.setLabel(I18n.translate("modules.settings.item.prefix_formatting") + ": " + (
        (this.item.getColor() == null) || (this.item.getColor().prefixFormatting == null) ? I18n.translate("modules.settings.default") : this.item.getColor().prefixFormatting.name()));
    }
    if (button.getId() == 12)
    {
      if (this.item.getColor().mainFormatting == null) {
        this.item.getColor().mainFormatting = ((ChatColor)FORMATTINGS.get(0));
      } else if (this.item.getColor().mainFormatting == FORMATTINGS.get(FORMATTINGS.size() - 1)) {
        this.item.getColor().mainFormatting = null;
      } else {
        this.item.getColor().mainFormatting = ((ChatColor)FORMATTINGS.get(FORMATTINGS.indexOf(this.item.getColor().mainFormatting) + 1));
      }
      The5zigMod.getModuleMaster().save();
      button.setLabel(I18n.translate("modules.settings.item.main_formatting") + ": " + (
        (this.item.getColor() == null) || (this.item.getColor().mainFormatting == null) ? I18n.translate("modules.settings.default") : this.item.getColor().mainFormatting.name()));
    }
  }
  
  protected void tick()
  {
    for (IButton colorButton : this.colorButtons) {
      colorButton.setEnabled(this.item.getColor() != null);
    }
  }
  
  public String getTitleName()
  {
    return "modules.settings.title";
  }
}
