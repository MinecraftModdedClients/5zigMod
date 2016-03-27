package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.config.items.NonConfigItem;
import eu.the5zig.mod.config.items.PlaceholderItem;
import eu.the5zig.mod.config.items.SelectColorItem;
import eu.the5zig.mod.config.items.SliderItem;
import eu.the5zig.mod.gui.elements.ButtonRow;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.modules.RenderLocation;
import eu.the5zig.mod.modules.items.RegisteredItem;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.input.Keyboard;

public class GuiModuleItems
  extends Gui
  implements Clickable<eu.the5zig.mod.modules.items.Item>
{
  private static final int START_INDEX = 500;
  private final Module module;
  private IGuiList<eu.the5zig.mod.modules.items.Item> guiListItems;
  private int itemIndex;
  private List<ButtonRow> buttons = Lists.newArrayList();
  private HashMap<IButton, eu.the5zig.mod.config.items.Item> buttonMap = Maps.newHashMap();
  private long lastMouseMoved;
  private int lastMouseX;
  private int lastMouseY;
  
  public GuiModuleItems(Gui lastScreen, Module module)
  {
    super(lastScreen);
    this.module = module;
  }
  
  public void initGui()
  {
    addBottomDoneButton();
    this.guiListItems = The5zigMod.getVars().createGuiList(this, getWidth(), getHeight(), 32, getHeight() - 48 - 22, getWidth() / 2 - 180, getWidth() / 2 - 10, this.module.getItems());
    this.guiListItems.setRowWidth(160);
    this.guiListItems.setLeftbound(true);
    this.guiListItems.setDrawSelection(true);
    this.guiListItems.setScrollX(getWidth() / 2 - 15);
    this.guiListItems.setHeaderPadding(The5zigMod.getVars().getFontHeight());
    this.guiListItems.setHeader(I18n.translate("modules.settings.item.list.items"));
    
    IGuiList guiListSettings = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 32, getHeight() - 50 - 90, getWidth() / 2 + 10, getWidth() / 2 + 180, this.buttons);
    guiListSettings.setDrawSelection(false);
    guiListSettings.setLeftbound(true);
    guiListSettings.setRowWidth(160);
    guiListSettings.setScrollX(getWidth() / 2 + 175);
    guiListSettings.setHeaderPadding(The5zigMod.getVars().getFontHeight());
    guiListSettings.setHeader(I18n.translate("modules.settings.item.list.settings"));
    addGuiList(guiListSettings);
    addGuiList(this.guiListItems);
    
    this.guiListItems.onSelect(this.itemIndex, this.guiListItems.getRows().isEmpty() ? null : (eu.the5zig.mod.modules.items.Item)this.guiListItems.getRows().get(this.itemIndex), true);
    
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 180, getHeight() - 48 - 20, 145, 20, I18n.translate("modules.settings.item.add")));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 - 30, getHeight() - 48 - 20, 20, 20, "-"));
    
    addButton(The5zigMod.getVars().createButton(10, getWidth() / 2 + 10, getHeight() - 48 - 90, 170, 20, I18n.translate("modules.settings.item.color") + ": " + (
      (this.guiListItems.getSelectedRow() == null) || (((eu.the5zig.mod.modules.items.Item)this.guiListItems.getSelectedRow()).getColor() == null) ? I18n.translate("modules.settings.default") : 
      I18n.translate("modules.settings.custom"))));
    addButton(The5zigMod.getVars().createButton(11, getWidth() / 2 + 10, getHeight() - 48 - 68, 83, 20, I18n.translate("modules.move_up")));
    addButton(The5zigMod.getVars().createButton(12, getWidth() / 2 + 97, getHeight() - 48 - 68, 83, 20, I18n.translate("modules.move_down")));
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      The5zigMod.getVars().displayScreen(new GuiModuleAddItem(this, this.module));
    }
    else if (button.getId() == 2)
    {
      int currentIndex = this.guiListItems.getSelectedId();
      List<eu.the5zig.mod.modules.items.Item> items = this.module.getItems();
      if (!items.isEmpty())
      {
        items.remove(currentIndex);
        The5zigMod.getModuleMaster().save();
      }
      onSelect(this.guiListItems.getSelectedId(), (eu.the5zig.mod.modules.items.Item)this.guiListItems.getSelectedRow(), false);
    }
    else if (button.getId() == 10)
    {
      if (this.guiListItems.getSelectedRow() != null) {
        The5zigMod.getVars().displayScreen(new GuiModuleItemColor(this, (eu.the5zig.mod.modules.items.Item)this.guiListItems.getSelectedRow()));
      }
    }
    else if (button.getId() == 11)
    {
      if (move(-1)) {
        The5zigMod.getModuleMaster().save();
      }
    }
    else if (button.getId() == 12)
    {
      if (move(1)) {
        The5zigMod.getModuleMaster().save();
      }
    }
    else if (this.buttonMap.containsKey(button))
    {
      eu.the5zig.mod.config.items.Item item = (eu.the5zig.mod.config.items.Item)this.buttonMap.get(button);
      item.next();
      item.action();
      button.setLabel(item.translate());
      if (item.hasChanged()) {
        The5zigMod.getModuleMaster().save();
      }
    }
  }
  
  public void onSelect(int id, eu.the5zig.mod.modules.items.Item item, boolean doubleClick)
  {
    this.itemIndex = id;
    if (item == null) {
      return;
    }
    this.buttons.clear();
    this.buttonMap.clear();
    int index = 500;
    for (eu.the5zig.mod.config.items.Item configItem : item.getSettings())
    {
      IButton button = getButton(configItem, index++);
      this.buttons.add(new ButtonRow(button, null));
      this.buttonMap.put(button, configItem);
    }
  }
  
  private IButton getButton(eu.the5zig.mod.config.items.Item item, int id)
  {
    if ((item instanceof PlaceholderItem)) {
      return null;
    }
    if ((item instanceof SliderItem)) {
      return The5zigMod.getVars().createSlider(id, getWidth() / 2 + 10, 0, GuiSettings.mapSlider((SliderItem)item));
    }
    if ((item instanceof SelectColorItem)) {
      return The5zigMod.getVars().createColorSelector(id, getWidth() / 2 + 10, 0, 160, 20, item.translate(), GuiSettings.mapColor((SelectColorItem)item));
    }
    return The5zigMod.getVars().createButton(id, getWidth() / 2 + 10, 0, 160, 20, item.translate());
  }
  
  protected void tick()
  {
    getButtonById(2).setEnabled(this.guiListItems.getSelectedRow() != null);
    getButtonById(10).setEnabled(this.guiListItems.getSelectedRow() != null);
    getButtonById(11).setEnabled(this.guiListItems.getSelectedId() > 0);
    getButtonById(12).setEnabled(this.guiListItems.getSelectedId() < this.module.getItems().size() - 1);
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    int x = getWidth() / 2 + 10;
    int y = getHeight() - 42 - 50;
    int width = 170;
    int height = 44;
    renderItemPreviewBox(x, y, width, height);
    if (this.guiListItems.getSelectedRow() != null)
    {
      eu.the5zig.mod.modules.items.Item item = (eu.the5zig.mod.modules.items.Item)this.guiListItems.getSelectedRow();
      int itemHeight = item.getHeight(true);
      int itemWidth = item.getWidth(true);
      item.render(x + (width - itemWidth) / 2, y + (height - itemHeight) / 2, RenderLocation.LEFT, true);
    }
  }
  
  public void drawScreen0(int mouseX, int mouseY, float partialTicks)
  {
    super.drawScreen0(mouseX, mouseY, partialTicks);
    eu.the5zig.mod.modules.items.Item hover = (eu.the5zig.mod.modules.items.Item)this.guiListItems.getHoverItem(mouseX, mouseY);
    if (hover != null)
    {
      String key = "modules.item." + eu.the5zig.mod.modules.items.Item.byItem(hover.getClass()).getKey().toLowerCase();
      if (I18n.has(key + ".desc"))
      {
        The5zigMod.getVars().getCurrentScreen().drawHoveringText(The5zigMod.getVars().splitStringToWidth(I18n.translate(key) + "\n" + 
          I18n.translate(new StringBuilder().append(key).append(".desc").toString()), 140), mouseX, mouseY);
        GLUtil.disableLighting();
      }
    }
    if ((this.lastMouseX != mouseX) || (this.lastMouseY != mouseY))
    {
      this.lastMouseX = mouseX;
      this.lastMouseY = mouseY;
      this.lastMouseMoved = System.currentTimeMillis();
    }
    if (System.currentTimeMillis() - this.lastMouseMoved > 700L)
    {
      IButton hovered = getHoveredButton(mouseX, mouseY);
      if (hovered != null)
      {
        eu.the5zig.mod.config.items.Item item = (eu.the5zig.mod.config.items.Item)this.buttonMap.get(hovered);
        if (item != null)
        {
          String hoverText = item.getHoverText();
          
          List<String> lines = Lists.newArrayList();
          lines.add(hovered.getLabel());
          lines.addAll(The5zigMod.getVars().splitStringToWidth(hoverText, 150));
          if ((!(item instanceof NonConfigItem)) && (Keyboard.isKeyDown(56))) {
            lines.add(ChatColor.DARK_GRAY.toString() + ChatColor.ITALIC + I18n.translate("config.click_to_reset"));
          }
          drawHoveringText(lines, mouseX, mouseY);
          GLUtil.disableLighting();
        }
      }
    }
  }
  
  protected void mouseClicked(int x, int y, int button)
  {
    super.mouseClicked(x, y, button);
    tryReset();
  }
  
  protected void mouseReleased(int x, int y, int state)
  {
    super.mouseReleased(x, y, state);
    tryReset();
  }
  
  private void tryReset()
  {
    if (Keyboard.isKeyDown(56))
    {
      IButton hovered = getHoveredButton(this.lastMouseX, this.lastMouseY);
      if ((hovered != null) && (!(hovered instanceof NonConfigItem)))
      {
        eu.the5zig.mod.config.items.Item item = (eu.the5zig.mod.config.items.Item)this.buttonMap.get(hovered);
        item.reset();
        hovered.setLabel(item.translate());
        if (item.hasChanged()) {
          The5zigMod.getConfig().save();
        }
      }
    }
  }
  
  private IButton getHoveredButton(int mouseX, int mouseY)
  {
    for (ButtonRow button : this.buttons)
    {
      if ((button.button1 != null) && (isHovered(button.button1, mouseX, mouseY))) {
        return button.button1;
      }
      if ((button.button2 != null) && (isHovered(button.button2, mouseX, mouseY))) {
        return button.button2;
      }
    }
    return null;
  }
  
  private boolean isHovered(IButton button, int lastMouseX, int lastMouseY)
  {
    return (lastMouseX >= button.getX()) && (lastMouseX <= button.getX() + button.getWidth()) && (lastMouseY >= button.getY()) && (lastMouseY <= button.getY() + button.getHeight());
  }
  
  private void renderItemPreviewBox(int x, int y, int width, int height)
  {
    Gui.drawRect(x, y, x + width, y + height, -2011028958);
    GLUtil.pushMatrix();
    float scale = 1.6F;
    GLUtil.translate(x + width / 2, y + height / 2 - The5zigMod.getVars().getFontHeight() * scale / 2.0F, 1.0F);
    GLUtil.scale(scale, scale, scale);
    GLUtil.enableBlend();
    MinecraftFactory.getVars().drawCenteredString(I18n.translate("modules.preview"), 0, 0, 584965597);
    GLUtil.disableBlend();
    GLUtil.popMatrix();
    
    Gui.drawRectOutline(x, y, x + width, y + height, -16777216);
  }
  
  public String getTitleKey()
  {
    return "modules.settings.title";
  }
  
  private boolean move(int pos)
  {
    eu.the5zig.mod.modules.items.Item item = (eu.the5zig.mod.modules.items.Item)this.guiListItems.getSelectedRow();
    if (item == null) {
      return false;
    }
    List<eu.the5zig.mod.modules.items.Item> items = this.module.getItems();
    
    int currentIndex = items.indexOf(item);
    int nextIndex = currentIndex + pos;
    if ((nextIndex >= 0) && (nextIndex < items.size()))
    {
      Collections.swap(items, currentIndex, nextIndex);
      this.guiListItems.setSelectedId(nextIndex);
      return true;
    }
    return false;
  }
}
