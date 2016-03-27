package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ChatBackgroundManager;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.FriendManager;
import eu.the5zig.mod.chat.GroupChatManager;
import eu.the5zig.mod.chat.GroupMember;
import eu.the5zig.mod.chat.entity.Conversation;
import eu.the5zig.mod.chat.entity.ConversationAnnouncements;
import eu.the5zig.mod.chat.entity.ConversationChat;
import eu.the5zig.mod.chat.entity.ConversationGroupChat;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.entity.Friend.OnlineStatus;
import eu.the5zig.mod.chat.entity.Group;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.chat.entity.Message.MessageType;
import eu.the5zig.mod.chat.gui.AudioChatLine;
import eu.the5zig.mod.chat.gui.ChatLine;
import eu.the5zig.mod.chat.gui.ViewMoreRow;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.config.ConfigNew.BackgroundType;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.manager.ChatTypingManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.SearchEntry;
import eu.the5zig.mod.manager.SearchManager;
import eu.the5zig.mod.manager.SkinManager;
import eu.the5zig.mod.render.Base64Renderer;
import eu.the5zig.mod.util.AudioCallback;
import eu.the5zig.mod.util.FileSelectorCallback;
import eu.the5zig.mod.util.GuiListChatCallback;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class TabConversations
  extends Tab
  implements Clickable<Conversation>
{
  private static int lastSelected = 0;
  private static float currentScroll = -1.0F;
  private final Base64Renderer base64Renderer = new Base64Renderer();
  private final GuiChat guiChat;
  public final List<ChatLine> chatLines = Collections.synchronizedList(new ArrayList());
  protected IGuiList<Conversation> conversationList;
  public IGuiList<? extends Row> chatList;
  private int chatboxWidth = 100;
  private AudioCallback audioCallback;
  
  public TabConversations(GuiChat guiChat)
  {
    this.guiChat = guiChat;
    this.audioCallback = new AudioCallback()
    {
      public void done(File audioFile)
      {
        Conversation selectedConversation = TabConversations.this.getSelectedConversation();
        if ((selectedConversation == null) || (!(selectedConversation instanceof ConversationChat))) {
          return;
        }
        if (!The5zigMod.getNetworkManager().isConnected()) {
          return;
        }
        The5zigMod.getConversationManager().sendAudio(((ConversationChat)selectedConversation).getFriendUUID(), audioFile);
      }
    };
  }
  
  public void initGui()
  {
    List<Conversation> conversations = The5zigMod.getConversationManager().getConversations();
    addButton(The5zigMod.getVars().createButton(1, getWidth() - 80 - 10, getHeight() - 26, 80, 20, I18n.translate("chat.send")));
    addButton(The5zigMod.getVars().createButton(20, 2, getHeight() - 16 - 20 - 10 - 2, 98, 20, I18n.translate("group.new")));
    addButton(The5zigMod.getVars().createButton(21, 2, getHeight() - 16 - 10, 98, 20, I18n.translate("chat.conversation_settings")));
    addTextField(The5zigMod.getVars().createTextfield(300, 110, getHeight() - 26, getWidth() - 110 - 80 - 10 - 5, 20, 100));
    
    this.conversationList = The5zigMod.getVars().createGuiList(this, 110, getHeight(), 76, getHeight() - 16 - 36, 0, 100, conversations);
    this.conversationList.setLeftbound(true);
    this.conversationList.setScrollX(95);
    this.chatList = The5zigMod.getVars().createGuiListChat(getWidth(), getHeight(), 60, getHeight() - 26 - 12, 110, getWidth() - 10, getWidth() - 15, this.chatLines, new GuiListChatCallback()
    {
      public boolean drawDefaultBackground()
      {
        return (getResourceLocation() == null) && ((The5zigMod.getConfig().getEnum("chatBackgroundType", ConfigNew.BackgroundType.class) != ConfigNew.BackgroundType.TRANSPARENT) || (The5zigMod.getVars().isPlayerNull()));
      }
      
      public Object getResourceLocation()
      {
        return The5zigMod.getDataManager().getChatBackgroundManager().getChatBackground();
      }
      
      public int getImageWidth()
      {
        return The5zigMod.getDataManager().getChatBackgroundManager().getImageWidth();
      }
      
      public int getImageHeight()
      {
        return The5zigMod.getDataManager().getChatBackgroundManager().getImageWidth();
      }
      
      public void chatLineClicked(Row row, int mouseX, int y, int minY, int left)
      {
        ChatLine chatLine = (ChatLine)row;
        List<String> lines = The5zigMod.getVars().splitStringToWidth(chatLine.getMessage().toString(), chatLine.getMaxMessageWidth());
        int yy = 0;
        int i1 = 0;
        for (int linesSize = lines.size(); i1 < linesSize; i1++)
        {
          String line = (String)lines.get(i1);
          int minChatLineX = chatLine.getMessage().getMessageType() == Message.MessageType.LEFT ? left + 4 : TabConversations.this.getWidth() - 22 - The5zigMod.getVars().getStringWidth(line);
          if (i1 == linesSize - 1)
          {
            String time = ChatColor.GRAY + Utils.convertToTimeWithMinutes(chatLine.getMessage().getTime());
            chatLine.getClass();int timeWidth = (int)(The5zigMod.getVars().getStringWidth(time) * 0.6F);
            if (chatLine.getMessage().getMessageType() == Message.MessageType.RIGHT) {
              minChatLineX -= timeWidth + 6;
            }
          }
          int maxChatLineX = minChatLineX + The5zigMod.getVars().getStringWidth(line);
          int minChatLineY = minY + yy;
          int maxChatLineY = minChatLineY + 9;
          if ((mouseX >= minChatLineX) && (mouseX <= maxChatLineX) && (y > minChatLineY) && (y <= maxChatLineY))
          {
            String[] words = line.split(" ");
            StringBuilder builder = new StringBuilder();
            for (String word : words)
            {
              builder.append(word);
              int wordX = The5zigMod.getVars().getStringWidth(builder.toString()) + minChatLineX;
              if ((wordX >= mouseX) && (wordX <= mouseX + The5zigMod.getVars().getStringWidth(word)))
              {
                for (String url : Utils.matchURL(chatLine.getMessage().toString())) {
                  if (url.contains(ChatColor.stripColor(word)))
                  {
                    Utils.openURL(url);
                    return;
                  }
                }
                Utils.openURLIfFound(ChatColor.stripColor(word));
                break;
              }
              builder.append(" ");
            }
            break;
          }
          chatLine.getClass();yy += 12;
        }
      }
    });
    this.chatboxWidth = (getWidth() - 10 - 110);
    scrollToBottom();
    
    this.conversationList.setSelectedId(lastSelected);
    this.conversationList.onSelect(this.conversationList.getRows().indexOf(getSelectedConversation()), getSelectedConversation(), false);
    
    ITextfield textfield = The5zigMod.getVars().createTextfield(I18n.translate("gui.search"), 9991, 2, 56, 96, 16);
    Comparator comparator = new Comparator()
    {
      public int compare(Conversation o1, Conversation o2)
      {
        return (int)(o2.getLastUsed() - o1.getLastUsed());
      }
    };
    SearchEntry searchEntry = new SearchEntry(textfield, conversations, comparator)
    {
      public boolean filter(String text, Object o)
      {
        Conversation conversation = (Conversation)o;
        if ((conversation instanceof ConversationChat)) {
          return ((ConversationChat)conversation).getFriendName().toLowerCase().contains(text.toLowerCase());
        }
        if ((conversation instanceof ConversationGroupChat)) {
          return ((ConversationGroupChat)conversation).getName().toLowerCase().contains(text.toLowerCase());
        }
        if ((conversation instanceof ConversationAnnouncements)) {
          return I18n.translate("announcement.short_desc").toLowerCase().contains(text.toLowerCase());
        }
        return false;
      }
    };
    searchEntry.setAlwaysVisible(true);
    The5zigMod.getDataManager().getSearchManager().addSearch(searchEntry, new SearchEntry[0]);
    if (currentScroll > -1.0F) {
      this.chatList.scrollTo(currentScroll);
    }
  }
  
  public void onSelect(int id, Conversation row, boolean doubleClick)
  {
    synchronized (this.chatLines)
    {
      this.chatLines.clear();
    }
    if (row == null) {
      return;
    }
    lastSelected = this.conversationList.getRows().indexOf(row);
    getTextfieldById(300).setText(row.getCurrentMessage());
    getButtonById(1).setEnabled(false);
    List<Message> messages = row.getMessages();
    if (messages.size() > row.getMaxMessages()) {
      synchronized (this.chatLines)
      {
        this.chatLines.add(new ViewMoreRow(100 + getChatBoxWidth() / 2, new Callback()
        {
          public void call(IButton button)
          {
            Conversation conversation = TabConversations.this.getSelectedConversation();
            IGuiList guiList = TabConversations.this.chatList;
            
            int remaining = conversation.getMessages().size() - guiList.getRows().size();
            if (remaining < 0) {
              remaining = 0;
            }
            conversation.getClass();
            if (remaining > 50)
            {
              conversation.getClass();remaining = 50;
            }
            float currentScroll = guiList.getCurrentScroll();
            
            conversation.getClass();conversation.setMaxMessages(conversation.getMaxMessages() + 50);
            
            TabConversations.this.conversationList.onSelect(TabConversations.this.conversationList.getRows().indexOf(TabConversations.this.getSelectedConversation()), TabConversations.this.getSelectedConversation(), false);
            guiList.calculateHeightMap();
            currentScroll += guiList.getHeight(remaining);
            guiList.scrollTo(currentScroll);
          }
        }));
      }
    }
    boolean overMax = messages.size() > row.getMaxMessages();
    int start = overMax ? messages.size() - row.getMaxMessages() : 0;
    int end = messages.size();
    synchronized (this.chatLines)
    {
      for (int i1 = start; i1 < end; i1++)
      {
        Message message = (Message)messages.get(i1);
        this.chatLines.add(ChatLine.fromMessage(message));
      }
    }
    scrollToBottom();
    The5zigMod.getConversationManager().setConversationRead(row, true);
  }
  
  protected void mouseClicked(int x, int y, int button)
  {
    this.conversationList.mouseClicked(x, y);
    IGuiList guiList = this.chatList;
    guiList.mouseClicked(x, y);
    The5zigMod.getDataManager().getSearchManager().mouseClicked(x, y, button);
    super.mouseClicked(x, y, button);
  }
  
  public void handleMouseInput()
  {
    this.conversationList.handleMouseInput();
    IGuiList guiList = this.chatList;
    guiList.handleMouseInput();
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    this.conversationList.drawScreen(mouseX, mouseY, partialTicks);
    IGuiList guiList = this.chatList;
    guiList.drawScreen(mouseX, mouseY, partialTicks);
    if ((getSelectedConversation() != null) && ((getSelectedConversation() instanceof ConversationChat))) {
      drawAvatar();
    }
    int x = (getSelectedConversation() == null) || (!(getSelectedConversation() instanceof ConversationChat)) ? 110 : 135;
    The5zigMod.getVars().drawString(getConversationName(), x, 36);
    The5zigMod.getVars().drawString(getConversationDescription(), x, 48);
    The5zigMod.getDataManager().getSearchManager().draw();
  }
  
  private void drawAvatar()
  {
    if ((getSelectedConversation() == null) || (!(getSelectedConversation() instanceof ConversationChat))) {
      return;
    }
    ConversationChat conversationChat = (ConversationChat)getSelectedConversation();
    String base64EncodedSkin = The5zigMod.getSkinManager().getBase64EncodedSkin(conversationChat.getFriendUUID());
    if ((this.base64Renderer.getBase64String() != null) && (base64EncodedSkin == null)) {
      this.base64Renderer.reset();
    } else if ((base64EncodedSkin != null) && (!base64EncodedSkin.equals(this.base64Renderer.getBase64String()))) {
      this.base64Renderer.setBase64String(base64EncodedSkin, "player_skin/" + conversationChat.getFriendUUID());
    }
    int width = 20;int height = 20;
    this.base64Renderer.renderImage(110, 36, width, height);
  }
  
  private String getConversationName()
  {
    Conversation selectedConversation = getSelectedConversation();
    if (selectedConversation == null) {
      return I18n.translate("chat.no_conversations");
    }
    if ((selectedConversation instanceof ConversationChat))
    {
      String name = ((ConversationChat)selectedConversation).getFriendName();
      UUID uuid = ((ConversationChat)selectedConversation).getFriendUUID();
      if (!The5zigMod.getFriendManager().isFriend(uuid)) {
        return name;
      }
      String displayName = The5zigMod.getFriendManager().getFriend(uuid).getDisplayName();
      if (The5zigMod.getDataManager().getChatTypingManager().isTyping(uuid)) {
        displayName = displayName + " " + ChatColor.GRAY + I18n.translate("friend.typing");
      }
      return displayName;
    }
    if ((selectedConversation instanceof ConversationGroupChat))
    {
      ConversationGroupChat conversationGroupChat = (ConversationGroupChat)selectedConversation;
      Group group = The5zigMod.getGroupChatManager().getGroup(conversationGroupChat.getGroupId());
      if (group != null) {
        return group.getName();
      }
      return conversationGroupChat.getName();
    }
    if ((selectedConversation instanceof ConversationAnnouncements)) {
      return I18n.translate("announcement.short_desc");
    }
    return I18n.translate("error");
  }
  
  private String getConversationDescription()
  {
    Conversation selectedConversation = getSelectedConversation();
    if (selectedConversation == null) {
      return "";
    }
    if ((selectedConversation instanceof ConversationChat))
    {
      UUID uuid = ((ConversationChat)selectedConversation).getFriendUUID();
      if (!The5zigMod.getFriendManager().isFriend(uuid)) {
        return ChatColor.RED + I18n.translate("connection.offline");
      }
      Friend friend = The5zigMod.getFriendManager().getFriend(uuid);
      String status;
      String status;
      if (friend.getStatus() == Friend.OnlineStatus.AWAY)
      {
        status = "8340212f-d91d-4875-98a2-7a3a16e0c6e5".equals(uuid.toString()) ? "Fappen" : friend.getStatus().getDisplayName();
      }
      else
      {
        String status;
        if (friend.getStatus() == Friend.OnlineStatus.OFFLINE) {
          status = ChatColor.GRAY + ChatColor.ITALIC.toString() + I18n.translate("friend.info.last_seen", new Object[] { friend.getLastOnline() });
        } else {
          status = friend.getStatus().getDisplayName();
        }
      }
      return status;
    }
    if ((selectedConversation instanceof ConversationGroupChat))
    {
      ConversationGroupChat conversationGroupChat = (ConversationGroupChat)selectedConversation;
      Group group = The5zigMod.getGroupChatManager().getGroup(conversationGroupChat.getGroupId());
      if (group == null) {
        return ChatColor.DARK_GRAY + I18n.translate("group.unknown");
      }
      String result = ChatColor.GRAY + ChatColor.ITALIC.toString();
      int maxWidth = getWidth() - 130;
      List<GroupMember> members = group.getMembers();
      int i = 0;
      for (int membersSize = members.size(); i < membersSize; i++)
      {
        GroupMember member = (GroupMember)members.get(i);
        String str = " " + I18n.translate("group.more_members", new Object[] { Integer.valueOf(membersSize - i) });
        if (The5zigMod.getVars().getStringWidth(result + member.getUsername() + str) > maxWidth)
        {
          result = result + str;
          break;
        }
        if (i > 0) {
          result = result + ", ";
        }
        result = result + member.getUsername();
      }
      return result;
    }
    if ((selectedConversation instanceof ConversationAnnouncements)) {
      return "";
    }
    return I18n.translate("error");
  }
  
  public int getChatBoxWidth()
  {
    return this.chatboxWidth;
  }
  
  protected void onKeyType(char character, int key)
  {
    The5zigMod.getDataManager().getSearchManager().keyTyped(character, key);
    if (getSelectedConversation() == null) {
      return;
    }
    ITextfield textfield = getTextfieldById(300);
    if (key == 200) {
      textfield.setText(getSelectedConversation().getPreviousSentMessage());
    }
    if (key == 208) {
      textfield.setText(getSelectedConversation().getNextSentMessage());
    }
  }
  
  protected void tick()
  {
    if (getSelectedConversation() != null) {
      getSelectedConversation().setCurrentMessage(getTextfieldById(300).getText());
    }
    enableDisableButtons();
    doGroupChatStuff();
    currentScroll = this.chatList.getCurrentScroll();
  }
  
  private void doGroupChatStuff()
  {
    Conversation conversation = getSelectedConversation();
    IButton info = getButtonById(50);
    if ((conversation == null) || (!(conversation instanceof ConversationGroupChat)) || (The5zigMod.getGroupChatManager().getGroup(((ConversationGroupChat)conversation).getGroupId()) == null))
    {
      if (info != null) {
        removeButton(info);
      }
    }
    else
    {
      int strWidth = The5zigMod.getVars().getStringWidth(getConversationName());
      if (info == null) {
        addButton(The5zigMod.getVars().createStringButton(50, 110 + strWidth + 4, 35, The5zigMod.getVars().getStringWidth(String.format("[%s]", new Object[] { I18n.translate("group.info") })), 10, 
          String.format("[%s]", new Object[] {I18n.translate("group.info") })));
      } else {
        getButtonById(50).setX(110 + strWidth + 4);
      }
    }
    IButton image = getButtonById(70);
    IButton audio = getButtonById(71);
    if ((conversation != null) && ((conversation instanceof ConversationChat)) && (getTextfieldById(300).getText().isEmpty()))
    {
      if ((image == null) && (audio == null))
      {
        addButton(The5zigMod.getVars().createButton(70, getWidth() - 80 - 10, getHeight() - 26, 20, 20, "+"));
        addButton(The5zigMod.getVars().createAudioButton(71, getWidth() - 80 - 10 + 22, getHeight() - 26, this.audioCallback));
        getButtonById(1).setWidth(36);
        getButtonById(1).setX(getWidth() - 80 - 10 + 44);
      }
    }
    else if ((image != null) && (audio != null))
    {
      removeButton(image);
      removeButton(audio);
      getButtonById(1).setWidth(80);
      getButtonById(1).setX(getWidth() - 80 - 10);
    }
  }
  
  private void enableDisableButtons()
  {
    ITextfield textfield = getTextfieldById(300);
    IButton sendButton = getButtonById(1);
    sendButton.setEnabled((The5zigMod.getNetworkManager().isConnected()) && (textfield.getText().length() > 0) && 
      (getSelectedConversation() != null) && (!(getSelectedConversation() instanceof ConversationAnnouncements)));
    getButtonById(21).setEnabled(getSelectedConversation() != null);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      if (!The5zigMod.getNetworkManager().isConnected()) {
        return;
      }
      Conversation conversation = getSelectedConversation();
      if (conversation == null) {
        return;
      }
      ITextfield textfield = getTextfieldById(300);
      String text = textfield.getText();
      text = StringUtils.normalizeSpace(text);
      if ((text == null) || (text.isEmpty())) {
        return;
      }
      if ((conversation instanceof ConversationChat))
      {
        ConversationChat conversationChat = (ConversationChat)conversation;
        UUID friendUUID = conversationChat.getFriendUUID();
        if (The5zigMod.getFriendManager().isBlocked(friendUUID))
        {
          The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.RED + I18n.translate("conn.block.blocked", new Object[] { conversationChat.getFriendName() }));
          textfield.setText("");
          getButtonById(1).setEnabled(false);
          return;
        }
        if (!The5zigMod.getFriendManager().isFriend(friendUUID))
        {
          The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.RED + I18n.translate("conn.user.not_friend_anymore", new Object[] { conversationChat.getFriendName() }));
          textfield.setText("");
          getButtonById(1).setEnabled(false);
          return;
        }
        The5zigMod.getConversationManager().sendConversationMessage(conversationChat.getFriendUUID(), text);
        conversation.addLastSentMessage(text);
        textfield.setText("");
        getButtonById(1).setEnabled(false);
        if (The5zigMod.getConfig().getBool("playMessageSounds")) {
          The5zigMod.getVars().playSound("the5zigmod", "chat.message.send", 1.0F);
        }
      }
      if ((conversation instanceof ConversationGroupChat))
      {
        ConversationGroupChat conversationGroupChat = (ConversationGroupChat)conversation;
        Group group = The5zigMod.getGroupChatManager().getGroup(conversationGroupChat.getGroupId());
        if (group == null)
        {
          textfield.setText("");
          return;
        }
        The5zigMod.getConversationManager().sendGroupMessage(group, text);
        conversation.addLastSentMessage(text);
        textfield.setText("");
        getButtonById(1).setEnabled(false);
        if (The5zigMod.getConfig().getBool("playMessageSounds")) {
          The5zigMod.getVars().playSound("the5zigmod", "chat.message.send", 1.0F);
        }
      }
    }
    if (button.getId() == 20) {
      The5zigMod.getVars().displayScreen(new GuiCreateGroupChat(this.guiChat));
    }
    if (button.getId() == 21)
    {
      Conversation selected = getSelectedConversation();
      if (selected == null) {
        return;
      }
      The5zigMod.getVars().displayScreen(new GuiConversationSettings(this.guiChat, selected));
    }
    if (button.getId() == 50)
    {
      if (!(getSelectedConversation() instanceof ConversationGroupChat)) {
        return;
      }
      ConversationGroupChat conversation = (ConversationGroupChat)getSelectedConversation();
      Group group = The5zigMod.getGroupChatManager().getGroup(conversation.getGroupId());
      if (group == null) {
        return;
      }
      The5zigMod.getVars().displayScreen(new GuiGroupChatInfo(this.guiChat, group));
    }
    if (button.getId() == 70)
    {
      if (!(getSelectedConversation() instanceof ConversationChat)) {
        return;
      }
      final UUID uuid = ((ConversationChat)getSelectedConversation()).getFriendUUID();
      The5zigMod.getVars().displayScreen(new GuiFileSelector(this.guiChat, new FileSelectorCallback()
      
        new File
        {
          public void onDone(File file)
          {
            if (!The5zigMod.getNetworkManager().isConnected()) {
              return;
            }
            The5zigMod.getConversationManager().sendImage(uuid, file);
          }
          
          public String getTitle()
          {
            return "The 5zig Mod - " + I18n.translate("chat.select_image");
          }
        }, new File("screenshots"), new String[] { "png", "jpg" }));
    }
  }
  
  protected void guiClosed()
  {
    for (Row row : this.chatList.getRows()) {
      if ((row instanceof AudioChatLine)) {
        ((AudioChatLine)row).close();
      }
    }
  }
  
  public void scrollToBottom()
  {
    IGuiList guiList = this.chatList;
    guiList.scrollToBottom();
  }
  
  public void setCurrentConversation(Conversation conversation)
  {
    List rows = this.conversationList.getRows();
    this.conversationList.setSelectedId(rows.indexOf(conversation));
    lastSelected = rows.indexOf(conversation);
  }
  
  public Conversation getSelectedConversation()
  {
    return (Conversation)this.conversationList.getSelectedRow();
  }
  
  public String getTitleName()
  {
    return "";
  }
  
  public static void resetScroll()
  {
    currentScroll = -1.0F;
  }
}
