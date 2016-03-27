package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.items.INonConfigItem;
import eu.the5zig.mod.config.items.Item;
import eu.the5zig.mod.config.items.SelectColorItem;
import eu.the5zig.mod.gui.elements.ButtonRow;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.render.DisplayRenderer;
import eu.the5zig.mod.util.ColorSelectorCallback;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;

public class GuiCustomLocations
  extends Gui
{
  private static List<Item> items = ;
  private static final int HOVER_WIDTH = 15;
  private static final int BOX_WIDTH = 180;
  private IGuiList<Row> guiList;
  private OpeningState state = OpeningState.CLOSED;
  private long lastDelta;
  private float value;
  private int visibleTitleTicks = 0;
  
  static
  {
    for (Item item : The5zigMod.getConfig().getItems("custom_display")) {
      items.add(item);
    }
  }
  
  public GuiCustomLocations(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    The5zigMod.getVars().updateScaledResolution();
    
    this.visibleTitleTicks = 0;
    
    this.state = OpeningState.CLOSED;
    List<Row> rows = Lists.newArrayList();
    this.guiList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 0, getHeight(), getWidth(), getWidth(), rows);
    this.guiList.setScrollX(getWidth());
    
    int i = 0;
    for (int locationItemsSize = items.size(); i < locationItemsSize; i++)
    {
      final Item item = (Item)items.get(i);
      if (!(item instanceof INonConfigItem))
      {
        IButton button;
        IButton button;
        if ((item instanceof SelectColorItem)) {
          button = The5zigMod.getVars().createColorSelector(i, getWidth(), 0, 150, 20, The5zigMod.getVars().shortenToWidth(item.translate(), 155), new ColorSelectorCallback()
          {
            public ChatColor getColor()
            {
              return (ChatColor)((SelectColorItem)item).get();
            }
            
            public void setColor(ChatColor color)
            {
              SelectColorItem colorItem = (SelectColorItem)item;
              colorItem.set(color);
            }
          });
        } else {
          button = The5zigMod.getVars().createButton(i, getWidth(), 0, 150, 20, The5zigMod.getVars().shortenToWidth(item.translate(), 135));
        }
        rows.add(new ButtonRow(button, null));
      }
    }
    rows.add(new ButtonRow(The5zigMod.getVars().createButton(50, getWidth(), 0, 150, 20, I18n.translate("config.custom_display.reset")), null));
  }
  
  protected void onEscapeType()
  {
    The5zigMod.getVars().displayScreen(this.lastScreen);
  }
  
  protected void actionPerformed(IButton button)
  {
    Item item;
    if (button.getId() < items.size())
    {
      item = (Item)items.get(button.getId());
      item.next();
      item.action();
      button.setLabel(item.translate());
      if (item.hasChanged()) {
        The5zigMod.getConfig().save();
      }
    }
    else if (button.getId() == 50)
    {
      for (Item item : items) {
        item.reset();
      }
      The5zigMod.getConfig().save();
    }
  }
  
  protected void tick()
  {
    this.visibleTitleTicks += 1;
  }
  
  protected void mouseClicked(int x, int y, int button)
  {
    this.guiList.mouseClicked(x, y);
  }
  
  protected void mouseReleased(int x, int y, int state)
  {
    this.guiList.mouseReleased(x, y, state);
  }
  
  protected void handleMouseInput()
  {
    this.guiList.handleMouseInput();
  }
  
  public void drawScreen0(int mouseX, int mouseY, float partialTicks)
  {
    if (The5zigMod.getVars().isPlayerNull())
    {
      drawMenuBackground();
      The5zigMod.getRenderer().drawScreen();
    }
    this.guiList.drawScreen(mouseX, mouseY, partialTicks);
    if (this.state == OpeningState.CLOSED)
    {
      drawRect(getWidth() - 15, 0, getWidth(), getHeight(), -1728053248);
      drawCenteredString("...", getWidth() - 7, getHeight() / 2);
    }
    if (((this.state == OpeningState.CLOSED) || (this.state == OpeningState.CLOSING)) && (mouseX >= getWidth() - 15))
    {
      this.state = OpeningState.OPENING;
    }
    else if (((this.state == OpeningState.OPENED) || (this.state == OpeningState.OPENING)) && (mouseX < getWidth() - 180))
    {
      this.state = OpeningState.CLOSING;
      
      this.guiList.setLeft(getWidth());
      this.guiList.setScrollX(getWidth());
      for (Row row : this.guiList.getRows()) {
        ((ButtonRow)row).button1.setX(getWidth());
      }
    }
    updateTimer();
    for (Row row : this.guiList.getRows())
    {
      IButton button = ((ButtonRow)row).button1;
      if (button.getId() < items.size())
      {
        Item item = (Item)items.get(button.getId());
        button.setLabel(item.translate());
      }
    }
    if (this.visibleTitleTicks <= 75)
    {
      GLUtil.enableBlend();
      GLUtil.pushMatrix();
      GLUtil.translate(getWidth() / 2, getHeight() / 2 - 10, 1.0F);
      float scale = 2.5F;
      GLUtil.scale(scale, scale, scale);
      GLUtil.tryBlendFuncSeparate(770, 771, 0, 1);
      int frequencyTotal = 30;
      int currentCountTotal = this.visibleTitleTicks % frequencyTotal;
      
      int alpha = (currentCountTotal > frequencyTotal / 2 ? frequencyTotal / 2 - currentCountTotal : currentCountTotal + frequencyTotal / 2) * 255 / frequencyTotal;
      MinecraftFactory.getVars().drawCenteredString(I18n.translate("config.custom_display.escape"), 0, 0, 0xFFFFFF | alpha << 24);
      GLUtil.popMatrix();
      GLUtil.disableBlend();
    }
  }
  
  private void updateTimer()
  {
    long systemTime = The5zigMod.getVars().getSystemTime();
    float delta = (float)(systemTime - this.lastDelta) / 100.0F;
    if (this.state == OpeningState.OPENING)
    {
      float add = (1.0F - this.value) * delta;
      if (add < 0.001D) {
        add = 0.001F;
      }
      this.value += add;
      if (this.value >= 1.0F)
      {
        this.value = 1.0F;
        this.state = OpeningState.OPENED;
      }
      this.guiList.setLeft(getWidth() - (int)(180.0F * this.value));
      for (Row row : this.guiList.getRows()) {
        ((ButtonRow)row).button1.setX(getWidth() - (int)(170.0F * this.value));
      }
      this.guiList.setScrollX(getWidth() - (int)(180.0F * this.value) + 180 - 12);
    }
    else if (this.state == OpeningState.CLOSING)
    {
      this.value = 0.0F;
      this.state = OpeningState.CLOSED;
    }
    this.lastDelta = systemTime;
  }
  
  public boolean isOpened()
  {
    return this.state != OpeningState.CLOSED;
  }
  
  protected void guiClosed() {}
  
  private static enum OpeningState
  {
    OPENED,  OPENING,  CLOSED,  CLOSING;
    
    private OpeningState() {}
  }
}
