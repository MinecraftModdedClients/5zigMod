package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.items.Item;
import eu.the5zig.mod.config.items.NonConfigItem;
import eu.the5zig.mod.config.items.PlaceholderItem;
import eu.the5zig.mod.config.items.SelectColorItem;
import eu.the5zig.mod.config.items.SliderItem;
import eu.the5zig.mod.gui.elements.ButtonRow;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.util.ColorSelectorCallback;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.SliderCallback;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.lwjgl.input.Keyboard;

public class GuiSettings
  extends Gui
{
  private final String category;
  private IGuiList buttonList;
  private List<ButtonRow> buttons = Lists.newArrayList();
  private HashMap<IButton, Item> configItems = Maps.newHashMap();
  private long lastMouseMoved;
  private int lastMouseX;
  private int lastMouseY;
  
  public GuiSettings(Gui lastScreen, String category)
  {
    super(lastScreen);
    this.category = category;
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 - 100, getHeight() - 27, The5zigMod.getVars().translate("gui.done", new Object[0])));
    
    this.buttonList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 32, getHeight() - 32, 0, getWidth(), this.buttons);
    this.buttonList.setDrawSelection(false);
    this.buttonList.setRowWidth(310);
    this.buttonList.setBottomPadding(2);
    this.buttonList.setScrollX(getWidth() / 2 + 160);
    addGuiList(this.buttonList);
    
    this.buttons.clear();
    this.configItems.clear();
    List<Item> items = The5zigMod.getConfig().getItems(this.category);
    int i = 0;
    for (int itemsSize = items.size(); i < itemsSize; i++)
    {
      IButton button1 = getButton((Item)items.get(i), i);
      if (button1 != null)
      {
        button1.setEnabled(!((Item)items.get(i)).isRestricted());
        this.configItems.put(button1, items.get(i));
      }
      i++;
      IButton button2 = i >= itemsSize ? null : getButton((Item)items.get(i), i);
      if (button2 != null)
      {
        button2.setEnabled(!((Item)items.get(i)).isRestricted());
        this.configItems.put(button2, items.get(i));
      }
      this.buttons.add(new ButtonRow(button1, button2));
    }
    if ("main".equals(this.category)) {
      addButton(The5zigMod.getVars().createButton(999, getWidth() - 102, 2, 100, 20, I18n.translate("config.main.credits")));
    }
  }
  
  private IButton getButton(Item item, int id)
  {
    return getButton(item, id, getWidth());
  }
  
  public static IButton getButton(Item item, int id, int width)
  {
    if ((item instanceof PlaceholderItem)) {
      return null;
    }
    if ((item instanceof SliderItem)) {
      return The5zigMod.getVars().createSlider(id, width / 2 + (id % 2 == 0 ? 65381 : 5), 0, mapSlider((SliderItem)item));
    }
    if ((item instanceof SelectColorItem)) {
      return The5zigMod.getVars().createColorSelector(id, width / 2 + (id % 2 == 0 ? 65381 : 5), 0, 150, 20, item.translate(), mapColor((SelectColorItem)item));
    }
    return The5zigMod.getVars().createButton(id, width / 2 + (id % 2 == 0 ? 65381 : 5), 0, 150, 20, item.translate());
  }
  
  public static SliderCallback mapSlider(SliderItem sliderItem)
  {
    new SliderCallback()
    {
      public String translate()
      {
        return this.val$sliderItem.translate();
      }
      
      public float get()
      {
        return ((Float)this.val$sliderItem.get()).floatValue();
      }
      
      public void set(float value)
      {
        this.val$sliderItem.set(Float.valueOf(value));
      }
      
      public float getMinValue()
      {
        return this.val$sliderItem.getMinValue();
      }
      
      public float getMaxValue()
      {
        return this.val$sliderItem.getMaxValue();
      }
      
      public int getSteps()
      {
        return this.val$sliderItem.getSteps();
      }
      
      public String getCustomValue(float value)
      {
        return this.val$sliderItem.getCustomValue(value);
      }
      
      public String getSuffix()
      {
        return this.val$sliderItem.getSuffix();
      }
      
      public void action()
      {
        this.val$sliderItem.setChanged(true);
        this.val$sliderItem.action();
        The5zigMod.getConfig().save();
      }
    };
  }
  
  public static ColorSelectorCallback mapColor(SelectColorItem item)
  {
    new ColorSelectorCallback()
    {
      public ChatColor getColor()
      {
        return (ChatColor)this.val$item.get();
      }
      
      public void setColor(ChatColor color)
      {
        this.val$item.set(color);
      }
    };
  }
  
  public void drawScreen0(int mouseX, int mouseY, float partialTicks)
  {
    super.drawScreen0(mouseX, mouseY, partialTicks);
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
        Item item = (Item)this.configItems.get(hovered);
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
        Item item = (Item)this.configItems.get(hovered);
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
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 999)
    {
      The5zigMod.getVars().displayScreen(new GuiCredits(this));
    }
    else if (this.configItems.containsKey(button))
    {
      Item item = (Item)this.configItems.get(button);
      item.next();
      item.action();
      button.setLabel(item.translate());
      if (item.hasChanged()) {
        The5zigMod.getConfig().save();
      }
    }
  }
  
  protected void tick()
  {
    for (Map.Entry<IButton, Item> entry : this.configItems.entrySet()) {
      ((IButton)entry.getKey()).setEnabled(!((Item)entry.getValue()).isRestricted());
    }
  }
  
  public String getTitleKey()
  {
    return "config." + this.category + ".title";
  }
}
