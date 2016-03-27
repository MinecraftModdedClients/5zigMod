package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ModuleMaster;
import eu.the5zig.mod.gui.elements.BasicRow;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.modules.Module;
import eu.the5zig.mod.modules.Module.RenderType;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callable;
import java.util.List;

public class GuiModuleRender
  extends Gui
{
  private final Module module;
  private IGuiList<RenderRow> guiList;
  private List<RenderRow> renderTypes = Lists.newArrayList();
  
  public GuiModuleRender(Gui lastScreen, Module module)
  {
    super(lastScreen);
    this.module = module;
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 - 155, getHeight() - 32, 150, 20, The5zigMod.getVars().translate("gui.cancel", new Object[0])));
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 + 5, getHeight() - 32, 150, 20, The5zigMod.getVars().translate("gui.done", new Object[0])));
    
    this.guiList = The5zigMod.getVars().createGuiList(new Clickable()
    {
      public void onSelect(int id, GuiModuleRender.RenderRow row, boolean doubleClick)
      {
        if (doubleClick) {
          GuiModuleRender.this.actionPerformed0(GuiModuleRender.this.getButtonById(200));
        }
      }
    }, getWidth(), getHeight(), 32, getHeight() - 48, 0, getWidth(), this.renderTypes);
    this.guiList.setRowWidth(200);
    addGuiList(this.guiList);
    
    this.renderTypes.clear();
    this.renderTypes.add(new RenderRow(null));
    for (Module.RenderType renderType : Module.RenderType.values()) {
      this.renderTypes.add(new RenderRow(renderType));
    }
    if (this.module.getRenderType() != null) {
      for (int i = 0; i < this.renderTypes.size(); i++) {
        if (this.module.getRenderType() == ((RenderRow)this.renderTypes.get(i)).renderType)
        {
          this.guiList.setSelectedId(i);
          break;
        }
      }
    }
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 200)
    {
      RenderRow selected = (RenderRow)this.guiList.getSelectedRow();
      this.module.setRenderType(selected == null ? null : selected.renderType);
      The5zigMod.getModuleMaster().save();
    }
  }
  
  public String getTitleKey()
  {
    return "modules.settings.title";
  }
  
  private class RenderRow
    extends BasicRow
  {
    private Module.RenderType renderType;
    
    public RenderRow(final Module.RenderType renderType)
    {
      super(
      {
        public String call()
        {
          return renderType == null ? "(" + I18n.translate("modules.settings.none") + ")" : I18n.translate("modules.settings.render." + renderType.toString().toLowerCase());
        }
      });
      
      this.renderType = renderType;
    }
    
    public int getLineHeight()
    {
      return 18;
    }
  }
}
