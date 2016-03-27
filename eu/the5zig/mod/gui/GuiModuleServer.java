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
import eu.the5zig.mod.server.ServerInstance;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callable;
import java.util.Collections;
import java.util.List;

public class GuiModuleServer
  extends Gui
{
  private final Module module;
  private IGuiList<ServerRow> guiList;
  private List<ServerRow> servers = Lists.newArrayList();
  
  public GuiModuleServer(Gui lastScreen, Module module)
  {
    super(lastScreen);
    this.module = module;
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 - 155, getHeight() - 32, 150, 20, The5zigMod.getVars().translate("gui.cancel", new Object[0])));
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 + 5, getHeight() - 32, 150, 20, The5zigMod.getVars().translate("gui.done", new Object[0])));
    
    this.guiList = The5zigMod.getVars().createGuiList(new Clickable()
    {
      public void onSelect(int id, GuiModuleServer.ServerRow row, boolean doubleClick)
      {
        if (doubleClick) {
          GuiModuleServer.this.actionPerformed0(GuiModuleServer.this.getButtonById(200));
        }
      }
    }, getWidth(), getHeight(), 32, getHeight() - 48, 0, getWidth(), this.servers);
    this.guiList.setRowWidth(200);
    addGuiList(this.guiList);
    
    this.servers.clear();
    this.servers.add(new ServerRow(null));
    List<String> serverNames = ServerInstance.getServerNames();
    Collections.sort(serverNames);
    for (String server : serverNames) {
      this.servers.add(new ServerRow(ServerInstance.byConfigName(server)));
    }
    if (this.module.getServer() != null) {
      for (int i = 1; i < this.servers.size(); i++) {
        if (this.module.getServer().equals(((ServerRow)this.servers.get(i)).serverInstance.getConfigName()))
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
      ServerRow selected = (ServerRow)this.guiList.getSelectedRow();
      this.module.setServer((selected == null) || (selected.serverInstance == null) ? null : selected.serverInstance.getConfigName());
      The5zigMod.getModuleMaster().save();
    }
    if (button.getId() == 100) {
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
  }
  
  public String getTitleKey()
  {
    return "modules.settings.title";
  }
  
  private class ServerRow
    extends BasicRow
  {
    private ServerInstance serverInstance;
    
    public ServerRow(final ServerInstance serverInstance)
    {
      super(
      {
        public String call()
        {
          return serverInstance == null ? "(" + I18n.translate("modules.settings.none") + ")" : serverInstance.getName();
        }
      });
      
      this.serverInstance = serverInstance;
    }
    
    public int getLineHeight()
    {
      return 18;
    }
  }
}
