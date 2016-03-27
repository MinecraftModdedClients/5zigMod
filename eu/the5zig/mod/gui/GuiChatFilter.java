package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ChatFilter;
import eu.the5zig.mod.config.ChatFilter.Action;
import eu.the5zig.mod.config.ChatFilter.ChatFilterMessage;
import eu.the5zig.mod.config.ChatFilterConfiguration;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;

public class GuiChatFilter
  extends GuiOptions
{
  private IGuiList guiList;
  
  public GuiChatFilter(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 190, getHeight() - 38, 90, 20, I18n.translate("chat_filter.add")));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 - 95, getHeight() - 38, 90, 20, I18n.translate("chat_filter.edit")));
    addButton(The5zigMod.getVars().createButton(3, getWidth() / 2, getHeight() - 38, 90, 20, I18n.translate("chat_filter.delete")));
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 + 95, getHeight() - 38, 95, 20, The5zigMod.getVars().translate("gui.back", new Object[0])));
    
    this.guiList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 64, getHeight() - 50, 0, getWidth(), 
      ((ChatFilter)The5zigMod.getChatFilterConfig().getConfigInstance()).getChatMessages());
    this.guiList.setRowWidth(getWidth() - 30);
    this.guiList.setScrollX(getWidth() / 2 + 150);
    addGuiList(this.guiList);
  }
  
  protected void tick()
  {
    boolean selected = this.guiList.getSelectedRow() != null;
    
    getButtonById(2).setEnabled(selected);
    getButtonById(3).setEnabled(selected);
  }
  
  protected void actionPerformed(IButton button)
  {
    ChatFilter chatMessagesConfig = (ChatFilter)The5zigMod.getChatFilterConfig().getConfigInstance();
    if (button.getId() == 1)
    {
      ChatFilter tmp25_24 = chatMessagesConfig;tmp25_24.getClass();ChatFilter.ChatFilterMessage chatMessage = new ChatFilter.ChatFilterMessage(tmp25_24, "", ChatFilter.Action.IGNORE, new String[0]);
      chatMessagesConfig.getChatMessages().add(chatMessage);
      The5zigMod.getVars().displayScreen(new GuiEditChatMessage(this, chatMessage));
    }
    Row selectedRow = this.guiList.getSelectedRow();
    if (button.getId() == 2)
    {
      if (selectedRow == null) {
        return;
      }
      The5zigMod.getVars().displayScreen(new GuiEditChatMessage(this, (ChatFilter.ChatFilterMessage)selectedRow));
    }
    if (button.getId() == 3)
    {
      if (selectedRow == null) {
        return;
      }
      ChatFilter.ChatFilterMessage selected = (ChatFilter.ChatFilterMessage)selectedRow;
      chatMessagesConfig.getChatMessages().remove(selected);
      The5zigMod.getChatFilterConfig().saveConfig();
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    int y = 0;
    for (String line : The5zigMod.getVars().splitStringToWidth(I18n.translate("chat_filter.help"), getWidth() / 4 * 3))
    {
      drawCenteredString(ChatColor.GRAY + line, getWidth() / 2, 34 + y);
      y += 10;
    }
  }
  
  public String getTitleKey()
  {
    return "chat_filter.title";
  }
}
