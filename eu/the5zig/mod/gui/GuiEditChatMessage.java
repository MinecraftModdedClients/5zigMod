package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.config.ChatFilter;
import eu.the5zig.mod.config.ChatFilter.Action;
import eu.the5zig.mod.config.ChatFilter.ChatFilterMessage;
import eu.the5zig.mod.config.ChatFilterConfiguration;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.util.IVariables;
import java.util.Arrays;
import java.util.List;

public class GuiEditChatMessage
  extends Gui
{
  private final ChatFilter.ChatFilterMessage chatMessage;
  private final ChatFilter.ChatFilterMessage chatMessageCopy;
  
  public GuiEditChatMessage(Gui lastScreen, ChatFilter.ChatFilterMessage chatMessage)
  {
    super(lastScreen);
    this.chatMessage = chatMessage;
    this.chatMessageCopy = chatMessage.clone();
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 + 5, getHeight() / 6 + 168, 150, 20, The5zigMod.getVars().translate("gui.done", new Object[0])));
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 - 155, getHeight() / 6 + 168, 150, 20, The5zigMod.getVars().translate("gui.cancel", new Object[0])));
    addTextField(The5zigMod.getVars().createTextfield(1, getWidth() / 2 - 150, getHeight() / 6, 165, 20, 155));
    addTextField(The5zigMod.getVars().createTextfield(3, getWidth() / 2 + 20, getHeight() / 6, 130, 20, 512));
    addTextField(The5zigMod.getVars().createTextfield(2, getWidth() / 2 - 150, getHeight() / 6 + 40, 300, 20, 512));
    addButton(The5zigMod.getVars()
      .createButton(1, getWidth() / 2 - 150, getHeight() / 6 + 80, 145, 20, I18n.translate("chat_filter.edit.action") + " " + this.chatMessage.getAction().getName()));
    addButton(The5zigMod.getVars()
      .createButton(2, getWidth() / 2 + 5, getHeight() / 6 + 80, 145, 20, I18n.translate("chat_filter.edit.use_regex") + " " + The5zigMod.toBoolean(this.chatMessage.useRegex())));
    
    getTextfieldById(1).setText(this.chatMessage.getMessage());
    getTextfieldById(3).setText(this.chatMessage.getExcept() == null ? "" : this.chatMessage.getExcept());
    String s = Arrays.toString(this.chatMessage.getServers());
    if (!s.isEmpty()) {
      s = s.substring(1, s.length() - 1);
    }
    getTextfieldById(2).setText(s);
  }
  
  protected void tick()
  {
    getButtonById(200).setEnabled(!getTextfieldById(1).getText().isEmpty());
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 200)
    {
      this.chatMessage.setMessage(getTextfieldById(1).getText());
      this.chatMessage.setExcept(getTextfieldById(3).getText());
      this.chatMessage.setAction(this.chatMessageCopy.getAction());
      this.chatMessage.setUseRegex(this.chatMessageCopy.useRegex());
      this.chatMessage.clearServers();
      for (String server : getTextfieldById(2).getText().replace(" ", "").split(",")) {
        if (!server.isEmpty()) {
          this.chatMessage.addServer(server);
        }
      }
      The5zigMod.getChatFilterConfig().saveConfig();
    }
    if (button.getId() == 100)
    {
      if (this.chatMessage.getMessage().isEmpty())
      {
        ((ChatFilter)The5zigMod.getChatFilterConfig().getConfigInstance()).getChatMessages().remove(this.chatMessage);
        The5zigMod.getChatFilterConfig().saveConfig();
      }
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
    if (button.getId() == 1)
    {
      this.chatMessageCopy.setAction(this.chatMessageCopy.getAction().getNext());
      button.setLabel(I18n.translate("chat_filter.edit.action") + " " + this.chatMessageCopy.getAction().getName());
    }
    if (button.getId() == 2)
    {
      this.chatMessageCopy.setUseRegex(!this.chatMessageCopy.useRegex());
      button.setLabel(I18n.translate("chat_filter.edit.use_regex") + " " + The5zigMod.toBoolean(this.chatMessageCopy.useRegex()));
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    The5zigMod.getVars().drawString(I18n.translate("chat_filter.edit.chat_message"), getWidth() / 2 - 150, getHeight() / 6 - 12);
    The5zigMod.getVars().drawString(I18n.translate("chat_filter.edit.except"), getWidth() / 2 + 20, getHeight() / 6 - 12);
    The5zigMod.getVars().drawString(I18n.translate("chat_filter.edit.servers"), getWidth() / 2 - 150, getHeight() / 6 + 28);
    drawCenteredString(I18n.translate("chat_filter.edit.multiple_ips"), getWidth() / 2, getHeight() / 6 + 65);
    
    List<String> help = The5zigMod.getVars().splitStringToWidth(I18n.translate("chat_filter.edit.help"), getWidth() / 4 * 3);
    int y = getHeight() / 6 + 105;
    for (String s : help)
    {
      drawCenteredString(s, getWidth() / 2, y);
      y += 10;
    }
  }
  
  public String getTitleKey()
  {
    return "chat_filter.edit.title";
  }
}
