package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.gui.elements.BasicRow;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.modules.items.Category;
import eu.the5zig.mod.modules.items.Item;
import eu.the5zig.mod.modules.items.RegisteredItem;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callable;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class GuiModuleAddItem
  extends Gui
  implements Clickable<CategoryRow>
{
  private static final HashMap<Category, List<String>> CATEGORIES = ;
  private final Module module;
  
  static
  {
    for (Category category : Category.values())
    {
      List<String> items = Lists.newArrayList();
      for (RegisteredItem item : Item.getRegisteredItems()) {
        if (item.getCategory() == category) {
          items.add(item.getKey());
        }
      }
      CATEGORIES.put(category, items);
    }
  }
  
  private final List<CategoryRow> categoryList = Lists.newArrayList();
  private IGuiList<ItemRow> guiListItems;
  private final List<ItemRow> itemList = Lists.newArrayList();
  private int categoryIndex;
  private int itemIndex;
  
  public GuiModuleAddItem(Gui lastScreen, Module module)
  {
    super(lastScreen);
    this.module = module;
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 - 155, getHeight() - 32, 150, 20, The5zigMod.getVars().translate("gui.cancel", new Object[0])));
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 + 5, getHeight() - 32, 150, 20, The5zigMod.getVars().translate("gui.done", new Object[0])));
    
    IGuiList<CategoryRow> guiListCategory = The5zigMod.getVars().createGuiList(this, getWidth(), getHeight(), 32, getHeight() - 48, getWidth() / 2 - 180, getWidth() / 2 - 10, this.categoryList);
    
    guiListCategory.setLeftbound(true);
    guiListCategory.setRowWidth(160);
    guiListCategory.setScrollX(getWidth() / 2 - 15);
    addGuiList(guiListCategory);
    this.categoryList.clear();
    for (Category category : Category.values()) {
      this.categoryList.add(new CategoryRow(category));
    }
    this.guiListItems = The5zigMod.getVars().createGuiList(new Clickable()
    {
      public void onSelect(int id, GuiModuleAddItem.ItemRow row, boolean doubleClick)
      {
        GuiModuleAddItem.this.itemIndex = id;
        if (doubleClick) {
          GuiModuleAddItem.this.actionPerformed0(GuiModuleAddItem.this.getButtonById(200));
        }
      }
    }, getWidth(), getHeight(), 32, getHeight() - 48, getWidth() / 2 + 10, getWidth() / 2 + 180, this.itemList);
    this.guiListItems.setLeftbound(true);
    this.guiListItems.setRowWidth(160);
    this.guiListItems.setScrollX(getWidth() / 2 + 175);
    addGuiList(this.guiListItems);
    
    guiListCategory.onSelect(this.categoryIndex, (Row)this.categoryList.get(this.categoryIndex), false);
    this.guiListItems.onSelect(this.itemIndex, (Row)this.itemList.get(this.itemIndex), false);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 100) {
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
    if (button.getId() == 200)
    {
      ItemRow row = (ItemRow)this.guiListItems.getSelectedRow();
      if (row != null) {
        try
        {
          this.module.addItem(Item.create(Item.byKey(row.item)));
          The5zigMod.getModuleMaster().save();
        }
        catch (Exception e)
        {
          The5zigMod.logger.error("Could not add item " + row.item + "!", e);
        }
      }
    }
  }
  
  public void onSelect(int id, CategoryRow row, boolean doubleClick)
  {
    this.categoryIndex = id;
    this.itemList.clear();
    if (row != null) {
      for (String key : (List)CATEGORIES.get(row.category)) {
        if ((!"DUMMY".equals(key)) || (The5zigMod.DEBUG)) {
          this.itemList.add(new ItemRow(key));
        }
      }
    }
  }
  
  public void drawScreen0(int mouseX, int mouseY, float partialTicks)
  {
    super.drawScreen0(mouseX, mouseY, partialTicks);
    ItemRow hover = (ItemRow)this.guiListItems.getHoverItem(mouseX, mouseY);
    if ((hover != null) && (hover.item != null))
    {
      String key = "modules.item." + hover.item.toLowerCase();
      if (I18n.has(key + ".desc"))
      {
        The5zigMod.getVars().getCurrentScreen().drawHoveringText(The5zigMod.getVars().splitStringToWidth(I18n.translate(key) + "\n" + 
          I18n.translate(new StringBuilder().append(key).append(".desc").toString()), 160), mouseX, mouseY);
        GLUtil.disableLighting();
      }
    }
  }
  
  public class CategoryRow
    extends BasicRow
  {
    private final Category category;
    
    public CategoryRow(final Category category)
    {
      super(
      {
        public String call()
        {
          return I18n.translate("modules.category." + category.toString().toLowerCase());
        }
      });
      
      this.category = category;
    }
    
    public int getLineHeight()
    {
      return super.getLineHeight() + 4;
    }
  }
  
  public class ItemRow
    implements Row
  {
    private final String item;
    
    public ItemRow(String item)
    {
      this.item = item;
    }
    
    public void draw(int x, int y)
    {
      The5zigMod.getVars().drawString(The5zigMod.getVars().shortenToWidth(I18n.translate("modules.item." + this.item.toLowerCase()), 170), x + 2, y + 2);
    }
    
    public int getLineHeight()
    {
      return 16;
    }
  }
}
