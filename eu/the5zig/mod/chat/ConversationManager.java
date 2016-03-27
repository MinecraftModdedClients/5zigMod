package eu.the5zig.mod.chat;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.AudioMessage;
import eu.the5zig.mod.chat.entity.AudioMessage.AudioData;
import eu.the5zig.mod.chat.entity.Conversation;
import eu.the5zig.mod.chat.entity.Conversation.Behaviour;
import eu.the5zig.mod.chat.entity.ConversationAnnouncements;
import eu.the5zig.mod.chat.entity.ConversationChat;
import eu.the5zig.mod.chat.entity.ConversationGroupChat;
import eu.the5zig.mod.chat.entity.FileMessage;
import eu.the5zig.mod.chat.entity.FileMessage.FileData;
import eu.the5zig.mod.chat.entity.FileMessage.Status;
import eu.the5zig.mod.chat.entity.Friend;
import eu.the5zig.mod.chat.entity.Group;
import eu.the5zig.mod.chat.entity.ImageMessage;
import eu.the5zig.mod.chat.entity.ImageMessage.ImageData;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.chat.entity.Message.MessageStatus;
import eu.the5zig.mod.chat.entity.Message.MessageType;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.entity.Rank;
import eu.the5zig.mod.chat.gui.ChatLine;
import eu.the5zig.mod.chat.gui.ImageChatLine;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.filetransfer.FileTransferException;
import eu.the5zig.mod.chat.network.filetransfer.FileTransferManager;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferAbort;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferRequest;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferStart.Type;
import eu.the5zig.mod.chat.network.packets.PacketGroupChatMessage;
import eu.the5zig.mod.chat.network.packets.PacketMessageFriend;
import eu.the5zig.mod.chat.network.packets.PacketMessageFriendStatus;
import eu.the5zig.mod.chat.sql.AnnouncementEntity;
import eu.the5zig.mod.chat.sql.ChatEntity;
import eu.the5zig.mod.chat.sql.DatabaseMigration;
import eu.the5zig.mod.chat.sql.GroupChatEntity;
import eu.the5zig.mod.chat.sql.MessageEntity;
import eu.the5zig.mod.config.ConfigNew;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.gui.TabConversations;
import eu.the5zig.mod.manager.AFKManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.TrayManager;
import eu.the5zig.util.AsyncExecutor;
import eu.the5zig.util.Utils;
import eu.the5zig.util.db.Database;
import eu.the5zig.util.db.SQLQuery;
import eu.the5zig.util.db.SQLResult;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public class ConversationManager
{
  public static final String TABLE_CHAT = "conversations_chat";
  public static final String TABLE_CHAT_MESSAGES = "conversation_chat_messages";
  public static final String TABLE_GROUP_CHAT = "conversations_groupchat";
  public static final String TABLE_GROUP_CHAT_MESSAGES = "conversation_groupchat_messages";
  public static final String TABLE_ANNOUNCEMENTS = "announcements";
  public static final String TABLE_ANNOUNCEMENTS_MESSAGES = "announcements_messages";
  private final List<Conversation> conversations = Collections.synchronizedList(new ArrayList());
  private Database sql;
  
  public ConversationManager()
  {
    this.sql = The5zigMod.getConversationDatabase();
    The5zigMod.getDataManager().initNetworkStats();
    if (this.sql == null) {
      return;
    }
    new DatabaseMigration(this.sql).start();
    synchronized (this.conversations)
    {
      try
      {
        loadConversations();
        The5zigMod.logger.info("Loaded {} Chats and Group Chats!", new Object[] { Integer.valueOf(this.conversations.size()) });
      }
      catch (Throwable throwable)
      {
        The5zigMod.logger.error("Could not initialize MySQL!", throwable);
      }
    }
  }
  
  private void loadConversations()
  {
    this.sql.update("CREATE TABLE IF NOT EXISTS conversations_chat (id INT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), friend VARCHAR(16), lastused BIGINT, read BOOLEAN, status INT, behaviour INT)", new Object[0]);
    
    this.sql.update("CREATE TABLE IF NOT EXISTS conversation_chat_messages (id INT AUTO_INCREMENT PRIMARY KEY, conversationid INT, player VARCHAR(20), message VARCHAR(512), time BIGINT, type INT)", new Object[0]);
    
    this.sql.update("CREATE TABLE IF NOT EXISTS announcements (id INT AUTO_INCREMENT PRIMARY KEY, lastused BIGINT, read BOOLEAN, behaviour INT)", new Object[0]);
    this.sql.update("CREATE TABLE IF NOT EXISTS announcements_messages (id INT AUTO_INCREMENT PRIMARY KEY, message VARCHAR(512), type INT, time BIGINT)", new Object[0]);
    
    this.sql.update("CREATE TABLE IF NOT EXISTS conversations_groupchat (id INT AUTO_INCREMENT PRIMARY KEY, groupId INT, name VARCHAR(50), lastused BIGINT, read BOOLEAN, status INT, behaviour INT)", new Object[0]);
    
    this.sql.update("CREATE TABLE IF NOT EXISTS conversation_groupchat_messages (id INT AUTO_INCREMENT PRIMARY KEY, conversationid INT, player VARCHAR(20), message VARCHAR(512), time BIGINT, type INT)", new Object[0]);
    
    loadChatConversations();
    loadGroupChatConversations();
    loadAnnouncements();
    sortConversations();
  }
  
  private void loadChatConversations()
  {
    List<ChatEntity> conversationEntities = this.sql.get(ChatEntity.class).query("SELECT * FROM conversations_chat ORDER BY lastused DESC", new Object[0]).getAll();
    for (ChatEntity entity : conversationEntities)
    {
      ConversationChat conversation = new ConversationChat(entity.getId(), entity.getFriend(), entity.getUuid(), entity.getLastUsed(), entity.isRead(), Message.MessageStatus.values()[entity.getStatus()], Conversation.Behaviour.values()[entity.getBehaviour()]);
      conversation.setMessages(loadMessages(conversation, "conversation_chat_messages"));
      this.conversations.add(conversation);
    }
  }
  
  private void loadGroupChatConversations()
  {
    List<GroupChatEntity> conversationEntities = this.sql.get(GroupChatEntity.class).query("SELECT * FROM conversations_groupchat ORDER BY lastused DESC", new Object[0]).getAll();
    for (GroupChatEntity entity : conversationEntities)
    {
      ConversationGroupChat conversation = new ConversationGroupChat(entity.getId(), entity.getGroupId(), entity.getName(), entity.getLastused(), entity.isRead(), Message.MessageStatus.values()[entity.getStatus()], Conversation.Behaviour.values()[entity.getBehaviour()]);
      conversation.setMessages(loadMessages(conversation, "conversation_groupchat_messages"));
      this.conversations.add(conversation);
    }
  }
  
  private void loadAnnouncements()
  {
    AnnouncementEntity entity = (AnnouncementEntity)this.sql.get(AnnouncementEntity.class).query("SELECT * FROM announcements ORDER BY lastused DESC", new Object[0]).unique();
    if (entity == null) {
      return;
    }
    ConversationAnnouncements conversation = new ConversationAnnouncements(entity.getId(), entity.getLastused(), entity.isRead(), Conversation.Behaviour.values()[entity.getBehaviour()]);
    List<Message> messages = Lists.newArrayList();
    List<MessageEntity> messagesEntities = this.sql.get(MessageEntity.class).query("SELECT * FROM announcements_messages ORDER BY time ASC", new Object[0]).getAll();
    for (MessageEntity messageEntity : messagesEntities) {
      messages.add(new Message(conversation, messageEntity.getId(), "Announcement", messageEntity.getMessage(), messageEntity.getTime(), 
        Message.MessageType.values()[messageEntity.getType()]));
    }
    conversation.setMessages(messages);
    this.conversations.add(conversation);
  }
  
  private List<Message> loadMessages(Conversation conversation, String table)
  {
    List<Message> messages = Lists.newArrayList();
    List<MessageEntity> messagesEntities = this.sql.get(MessageEntity.class).query("SELECT * FROM " + table + " WHERE conversationid=? ORDER BY time ASC", new Object[] { Integer.valueOf(conversation.getId()) }).getAll();
    for (MessageEntity messageEntity : messagesEntities) {
      if (messageEntity.getType() == Message.MessageType.IMAGE.ordinal()) {
        messages.add(new ImageMessage(conversation, messageEntity.getId(), messageEntity.getPlayer(), messageEntity.getMessage(), messageEntity.getTime(), 
          Message.MessageType.values()[messageEntity.getType()]));
      } else if (messageEntity.getType() == Message.MessageType.AUDIO.ordinal()) {
        messages.add(new AudioMessage(conversation, messageEntity.getId(), messageEntity.getPlayer(), messageEntity.getMessage(), messageEntity.getTime(), 
          Message.MessageType.values()[messageEntity.getType()]));
      } else {
        messages.add(new Message(conversation, messageEntity.getId(), messageEntity.getPlayer(), messageEntity.getMessage(), messageEntity.getTime(), 
          Message.MessageType.values()[messageEntity.getType()]));
      }
    }
    return messages;
  }
  
  public List<Conversation> getConversations()
  {
    return this.conversations;
  }
  
  public void updateConversationNames(final Friend friend)
  {
    synchronized (this.conversations)
    {
      for (Conversation conversation : this.conversations) {
        if ((conversation instanceof ConversationChat))
        {
          ConversationChat conversationChat = (ConversationChat)conversation;
          if ((conversationChat.getFriendUUID().equals(friend.getUniqueId())) && (!conversationChat.getFriendName().equals(friend.getName())))
          {
            The5zigMod.logger.info("Friend {} changed its name to {}!", new Object[] { conversationChat.getFriendName(), friend.getName() });
            conversationChat.setFriendName(friend.getName());
            
            The5zigMod.getAsyncExecutor().execute(new Runnable()
            {
              public void run()
              {
                ConversationManager.this.sql.update("UPDATE conversations_chat SET friend=? WHERE uuid=?", new Object[] { friend.getName(), friend.getUniqueId().toString() });
              }
            });
          }
        }
      }
    }
  }
  
  public void setConversationName(final ConversationGroupChat conversation, final String name)
  {
    conversation.setName(name);
    
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        ConversationManager.this.sql.update("UPDATE conversations_groupchat SET name=? WHERE id=?", new Object[] { name, Integer.valueOf(conversation.getId()) });
      }
    });
  }
  
  public ConversationChat newConversation(Friend friend)
  {
    int id = this.sql == null ? -1 : this.sql.updateWithGeneratedKeys("INSERT INTO conversations_chat (uuid, friend, lastused) VALUES (?, ?, ?)", new Object[] { friend.getUniqueId().toString(), friend.getName(), 
      Long.valueOf(System.currentTimeMillis()) });
    ConversationChat conversation = new ConversationChat(id, friend.getName(), friend.getUniqueId(), System.currentTimeMillis(), true, Message.MessageStatus.PENDING, Conversation.Behaviour.DEFAULT);
    synchronized (this.conversations)
    {
      this.conversations.add(conversation);
    }
    return conversation;
  }
  
  public ConversationGroupChat newConversation(Group group)
  {
    int id = this.sql == null ? -1 : this.sql.updateWithGeneratedKeys("INSERT INTO conversations_groupchat (groupId, name, lastused) VALUES (?, ?, ?)", new Object[] { Integer.valueOf(group.getId()), group.getName(), 
      Long.valueOf(System.currentTimeMillis()) });
    ConversationGroupChat conversation = new ConversationGroupChat(id, group.getId(), group.getName(), System.currentTimeMillis(), true, Message.MessageStatus.PENDING, Conversation.Behaviour.DEFAULT);
    synchronized (this.conversations)
    {
      this.conversations.add(conversation);
    }
    return conversation;
  }
  
  public ConversationAnnouncements newConversation()
  {
    int id = this.sql == null ? -1 : this.sql.updateWithGeneratedKeys("INSERT INTO announcements (lastused) VALUES (?)", new Object[] { Long.valueOf(System.currentTimeMillis()) });
    ConversationAnnouncements conversation = new ConversationAnnouncements(id, System.currentTimeMillis(), true, Conversation.Behaviour.DEFAULT);
    synchronized (this.conversations)
    {
      this.conversations.add(conversation);
    }
    return conversation;
  }
  
  public ConversationChat getConversation(Friend friend)
  {
    ConversationChat conversation = null;
    for (Conversation conversation1 : this.conversations) {
      if ((conversation1 instanceof ConversationChat))
      {
        ConversationChat conversationChat = (ConversationChat)conversation1;
        if (conversationChat.getFriendUUID().equals(friend.getUniqueId())) {
          conversation = conversationChat;
        }
      }
    }
    if (conversation == null) {
      conversation = newConversation(friend);
    }
    return conversation;
  }
  
  public boolean conversationExists(Friend friend)
  {
    synchronized (this.conversations)
    {
      for (Conversation conversation : this.conversations) {
        if ((conversation instanceof ConversationChat))
        {
          ConversationChat conversationChat = (ConversationChat)conversation;
          if (conversationChat.getFriendUUID().equals(friend.getUniqueId())) {
            return true;
          }
        }
      }
      return false;
    }
  }
  
  public ConversationGroupChat getConversation(Group group)
  {
    ConversationGroupChat conversation = null;
    synchronized (this.conversations)
    {
      for (Conversation conversation1 : this.conversations) {
        if ((conversation1 instanceof ConversationGroupChat))
        {
          ConversationGroupChat conversationGroupChat = (ConversationGroupChat)conversation1;
          if (conversationGroupChat.getGroupId() == group.getId()) {
            conversation = conversationGroupChat;
          }
        }
      }
    }
    if (conversation == null) {
      conversation = newConversation(group);
    }
    return conversation;
  }
  
  public ConversationAnnouncements getAnnouncementsConversation()
  {
    synchronized (this.conversations)
    {
      for (Conversation conversation : this.conversations) {
        if ((conversation instanceof ConversationAnnouncements)) {
          return (ConversationAnnouncements)conversation;
        }
      }
      return newConversation();
    }
  }
  
  public void deleteGroupConversation(int groupId)
  {
    Conversation con = null;
    synchronized (this.conversations)
    {
      for (Conversation conversation : this.conversations) {
        if ((conversation instanceof ConversationGroupChat))
        {
          ConversationGroupChat c = (ConversationGroupChat)conversation;
          if (c.getGroupId() == groupId) {
            con = conversation;
          }
        }
      }
    }
    if (con == null) {
      return;
    }
    deleteConversation(con);
  }
  
  public void sendConversationMessage(UUID uuid, String string)
  {
    String coloredMessage = string;
    if (The5zigMod.getDataManager().getProfile().getRank() != Rank.NONE) {
      coloredMessage = ChatColor.translateAlternateColorCodes('&', string);
    }
    final String message = coloredMessage;
    Friend friend = The5zigMod.getFriendManager().getFriend(uuid);
    final Conversation conversation = getConversation(friend);
    setConversationStatus(conversation, Message.MessageStatus.PENDING);
    final long time = System.currentTimeMillis();
    The5zigMod.getNetworkManager().sendPacket(new PacketMessageFriend(friend.getUniqueId(), string, time), new GenericFutureListener[0]);
    
    Message msg = new Message(conversation, -1, The5zigMod.getDataManager().getColoredName(), message, time, Message.MessageType.RIGHT);
    checkNewDay(conversation, msg);
    conversation.addMessage(msg);
    addChatLineToGui(conversation, msg);
    setConversationLastUsed(conversation);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO conversation_chat_messages(conversationid, player, message, time, type) VALUES (?, ?, ?, ?, ?)", new Object[] { Integer.valueOf(conversation.getId()), 
          The5zigMod.getDataManager().getColoredName(), message, Long.valueOf(time), Integer.valueOf(Message.MessageType.RIGHT.ordinal()) });
        this.val$msg.setId(id);
      }
    });
  }
  
  public void handleFriendMessageReceive(UUID uuid, final String username, final String message, final long time)
  {
    Friend friend = The5zigMod.getFriendManager().getFriend(uuid);
    The5zigMod.getNetworkManager().sendPacket(new PacketMessageFriendStatus(friend.getUniqueId(), Message.MessageStatus.DELIVERED), new GenericFutureListener[0]);
    final Conversation conversation = getConversation(friend);
    conversation.setRead(false);
    
    String title = I18n.translate("chat.new_message", new Object[] { username });
    TabConversations.resetScroll();
    if ((!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) || (!(((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)) || 
      (!(((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getSelectedConversation() instanceof ConversationChat)) || 
      (!((ConversationChat)((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getSelectedConversation()).getFriendUUID().equals(friend
      .getUniqueId()))) {
      if (((conversation.getBehaviour() == Conversation.Behaviour.DEFAULT) && (The5zigMod.getConfig().getBool("showMessages"))) || 
        (conversation.getBehaviour() == Conversation.Behaviour.SHOW))
      {
        The5zigMod.getOverlayMessage().displayMessage(title, message);
        if (The5zigMod.getDataManager().getAfkManager().isAfk()) {
          The5zigMod.getDataManager().getAfkManager().addNewMessage();
        }
      }
    }
    if (!Display.isActive()) {
      The5zigMod.getTrayManager().displayMessage(ChatColor.stripColor(title), ChatColor.stripColor(message));
    }
    Message msg = new Message(conversation, -1, username, message, time, Message.MessageType.LEFT);
    checkNewDay(conversation, msg);
    conversation.addMessage(msg);
    addChatLineToGui(conversation, msg);
    setConversationLastUsed(conversation);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO conversation_chat_messages (conversationid, player, message, time, type) VALUES (?, ?, ?, ?, ?)", new Object[] { Integer.valueOf(conversation.getId()), username, message, 
          Long.valueOf(time), Integer.valueOf(Message.MessageType.LEFT.ordinal()) });
        this.val$msg.setId(id);
      }
    });
  }
  
  public void sendGroupMessage(Group group, String string)
  {
    String coloredMessage = string;
    if (The5zigMod.getDataManager().getProfile().getRank() != Rank.NONE) {
      coloredMessage = ChatColor.translateAlternateColorCodes('&', string);
    }
    final String message = coloredMessage;
    final Conversation conversation = getConversation(group);
    setConversationStatus(conversation, Message.MessageStatus.PENDING);
    The5zigMod.getNetworkManager().sendPacket(new PacketGroupChatMessage(group.getId(), string, System.currentTimeMillis()), new GenericFutureListener[0]);
    
    final long time = System.currentTimeMillis();
    Message msg = new Message(conversation, -1, The5zigMod.getDataManager().getColoredName(), message, time, Message.MessageType.RIGHT);
    checkNewDay(conversation, msg);
    conversation.addMessage(msg);
    addChatLineToGui(conversation, msg);
    setConversationLastUsed(conversation);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO conversation_groupchat_messages (conversationid, player, message, time, type) VALUES (?, ?, ?, ?, ?)", new Object[] {
          Integer.valueOf(conversation.getId()), The5zigMod.getDataManager().getColoredName(), message, Long.valueOf(time), Integer.valueOf(Message.MessageType.RIGHT.ordinal()) });
        this.val$msg.setId(id);
      }
    });
  }
  
  public void handleGroupChatMessage(Group group, final String username, final String message, final long time)
  {
    final Conversation conversation = getConversation(group);
    if (((!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) || (!(((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)) || 
      (!(((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getSelectedConversation() instanceof ConversationGroupChat)) || 
      (((ConversationGroupChat)((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getSelectedConversation()).getGroupId() == group.getId())) && (
      ((conversation.getBehaviour() == Conversation.Behaviour.DEFAULT) && (The5zigMod.getConfig().getBool("showGroupMessages"))) || 
      (conversation.getBehaviour() == Conversation.Behaviour.SHOW)))
    {
      The5zigMod.getOverlayMessage().displayMessage(I18n.translate("chat.new_group_message", new Object[] { group.getName() }), message);
      The5zigMod.getDataManager().getAfkManager().addNewMessage();
    }
    Message msg = new Message(conversation, -1, username, message, time, Message.MessageType.LEFT);
    checkNewDay(conversation, msg);
    conversation.addMessage(msg);
    addChatLineToGui(conversation, msg);
    setConversationLastUsed(conversation);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO conversation_groupchat_messages (conversationid, player, message, time, type) VALUES (?, ?, ?, ?, ?)", new Object[] {
          Integer.valueOf(conversation.getId()), username, message, Long.valueOf(time), Integer.valueOf(Message.MessageType.LEFT.ordinal()) });
        this.val$msg.setId(id);
      }
    });
  }
  
  public void handleGroupBroadcast(Group group, final String message, final long time)
  {
    final Conversation conversation = getConversation(group);
    
    Message msg = new Message(conversation, -1, "", message, time, Message.MessageType.CENTERED);
    checkNewDay(conversation, msg);
    conversation.addMessage(msg);
    addChatLineToGui(conversation, msg);
    setConversationLastUsed(conversation);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO conversation_groupchat_messages (conversationid, player, message, time, type) VALUES (?, ?, ?, ?, ?)", new Object[] {
          Integer.valueOf(conversation.getId()), "", message, Long.valueOf(time), Integer.valueOf(Message.MessageType.CENTERED.ordinal()) });
        this.val$msg.setId(id);
      }
    });
  }
  
  public void sendImage(UUID uuid, File imageFile)
  {
    if (imageFile.length() > 5000000L)
    {
      The5zigMod.getOverlayMessage().displayMessageAndSplit(
        I18n.translate("chat.image.too_large", new Object[] {Utils.bytesToReadable(Math.floor(5242880.0D)) }));
      return;
    }
    try
    {
      File mediaDir = eu.the5zig.util.io.FileUtils.createDir(new File("the5zigmod/media/" + The5zigMod.getDataManager().getUniqueId().toString() + "/" + uuid.toString()));
      String hash;
      File mediaFile = new File(mediaDir, hash = FileTransferManager.sha1(imageFile));
      org.apache.commons.io.FileUtils.copyFile(imageFile, mediaFile);
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not copy " + imageFile, e); return;
    }
    String hash;
    final File mediaFile;
    File mediaDir;
    Friend friend = The5zigMod.getFriendManager().getFriend(uuid);
    final Conversation conversation = getConversation(friend);
    setConversationStatus(conversation, Message.MessageStatus.PENDING);
    setConversationLastUsed(conversation);
    
    ImageMessage.ImageData fileData = new ImageMessage.ImageData(FileMessage.Status.WAITING);
    fileData.setHash(hash);
    final long time = System.currentTimeMillis();
    
    final Message msg = new ImageMessage(conversation, -1, The5zigMod.getDataManager().getColoredName(), fileData, time, Message.MessageType.IMAGE);
    checkNewDay(conversation, msg);
    conversation.addMessage(msg);
    addChatLineToGui(conversation, msg);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO conversation_chat_messages (conversationid, player, message, time, type) VALUES (?, ?, ?, ?, ?)", new Object[] { Integer.valueOf(conversation.getId()), 
          The5zigMod.getDataManager().getColoredName(), msg.getMessage(), Long.valueOf(time), Integer.valueOf(Message.MessageType.IMAGE.ordinal()) });
        msg.setId(id);
        The5zigMod.getNetworkManager().sendPacket(new PacketFileTransferRequest(mediaFile.getUniqueId(), id, PacketFileTransferStart.Type.IMAGE, this.val$mediaFile.length()), new GenericFutureListener[0]);
      }
    });
  }
  
  public void sendAudio(UUID uuid, File audioFile)
  {
    if (audioFile.length() > 5000000L)
    {
      The5zigMod.getOverlayMessage().displayMessageAndSplit(
        I18n.translate("chat.audio.too_large", new Object[] {Utils.bytesToReadable(Math.floor(5242880.0D)) }));
      return;
    }
    try
    {
      File mediaDir = eu.the5zig.util.io.FileUtils.createDir(new File("the5zigmod/media/" + The5zigMod.getDataManager().getUniqueId().toString() + "/" + uuid.toString()));
      String hash;
      File mediaFile = new File(mediaDir, hash = FileTransferManager.sha1(audioFile));
      org.apache.commons.io.FileUtils.moveFile(audioFile, mediaFile);
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not create audio file", e); return;
    }
    String hash;
    final File mediaFile;
    File mediaDir;
    Friend friend = The5zigMod.getFriendManager().getFriend(uuid);
    final Conversation conversation = getConversation(friend);
    setConversationStatus(conversation, Message.MessageStatus.PENDING);
    setConversationLastUsed(conversation);
    
    AudioMessage.AudioData fileData = new AudioMessage.AudioData(FileMessage.Status.WAITING);
    fileData.setHash(hash);
    final long time = System.currentTimeMillis();
    
    final Message msg = new AudioMessage(conversation, -1, The5zigMod.getDataManager().getColoredName(), fileData, time, Message.MessageType.AUDIO);
    checkNewDay(conversation, msg);
    conversation.addMessage(msg);
    addChatLineToGui(conversation, msg);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO conversation_chat_messages (conversationid, player, message, time, type) VALUES (?, ?, ?, ?, ?)", new Object[] { Integer.valueOf(conversation.getId()), 
          The5zigMod.getDataManager().getColoredName(), msg.getMessage(), Long.valueOf(time), Integer.valueOf(Message.MessageType.AUDIO.ordinal()) });
        msg.setId(id);
        The5zigMod.getNetworkManager().sendPacket(new PacketFileTransferRequest(mediaFile.getUniqueId(), id, PacketFileTransferStart.Type.AUDIO, this.val$mediaFile.length()), new GenericFutureListener[0]);
      }
    });
  }
  
  public void handleFileId(UUID uuid, int messageId, int fileID)
  {
    final Message message = getMessageById(uuid, messageId);
    if ((message == null) || (!(message instanceof FileMessage)))
    {
      The5zigMod.logger.error("Could not find message for file id " + fileID);
      return;
    }
    ((FileMessage)message).getFileData().setFileId(fileID);
    ((FileMessage)message).saveData();
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        ConversationManager.this.sql.update("UPDATE conversation_chat_messages SET message=? WHERE id=?", new Object[] { message.getMessage(), Integer.valueOf(message.getId()) });
      }
    });
  }
  
  public void handleFileRequest(UUID uuid, int fileId, PacketFileTransferStart.Type type, long length)
  {
    Friend friend = The5zigMod.getFriendManager().getFriend(uuid);
    The5zigMod.getNetworkManager().sendPacket(new PacketMessageFriendStatus(friend.getUniqueId(), Message.MessageStatus.DELIVERED), new GenericFutureListener[0]);
    final Conversation conversation = getConversation(friend);
    conversation.setRead(false);
    
    final String username = friend.getDisplayName();
    String title = I18n.translate("chat.new_file_request", new Object[] { username });
    
    final long time = System.currentTimeMillis();
    if ((!(The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) || (!(((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)) || 
      (!(((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getSelectedConversation() instanceof ConversationChat)) || 
      (!((ConversationChat)((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getSelectedConversation()).getFriendUUID().equals(friend
      .getUniqueId()))) {
      if (((conversation.getBehaviour() == Conversation.Behaviour.DEFAULT) && (The5zigMod.getConfig().getBool("showMessages"))) || 
        (conversation.getBehaviour() == Conversation.Behaviour.SHOW))
      {
        The5zigMod.getOverlayMessage().displayMessageAndSplit(title);
        if (The5zigMod.getDataManager().getAfkManager().isAfk()) {
          The5zigMod.getDataManager().getAfkManager().addNewMessage();
        }
      }
    }
    if (!Display.isActive()) {
      The5zigMod.getTrayManager().displayMessage(ChatColor.stripColor(title), I18n.translate("chat.accept_file_transfer"));
    }
    Message tmp;
    Message tmp;
    if (type == PacketFileTransferStart.Type.IMAGE)
    {
      ImageMessage.ImageData fileData = new ImageMessage.ImageData(FileMessage.Status.REQUEST);
      fileData.setFileId(fileId);
      fileData.setLength(length);
      tmp = new ImageMessage(conversation, -1, username, fileData, time, Message.MessageType.IMAGE);
    }
    else
    {
      AudioMessage.AudioData fileData = new AudioMessage.AudioData(FileMessage.Status.REQUEST);
      fileData.setFileId(fileId);
      fileData.setLength(length);
      tmp = new AudioMessage(conversation, -1, username, fileData, time, Message.MessageType.AUDIO);
    }
    final Message msg = tmp;
    checkNewDay(conversation, msg);
    conversation.addMessage(msg);
    addChatLineToGui(conversation, msg);
    setConversationLastUsed(conversation);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO conversation_chat_messages (conversationid, player, message, time, type) VALUES (?, ?, ?, ?, ?)", new Object[] { Integer.valueOf(conversation.getId()), username, msg
          .getMessage(), Long.valueOf(time), Integer.valueOf(msg.getMessageType().ordinal()) });
        msg.setId(id);
      }
    });
  }
  
  public void handleFileResponse(int fileId, boolean accepted)
  {
    FileMessage message = getMessageByFileId(fileId);
    if (message == null)
    {
      The5zigMod.logger.error("Could not find message for file id " + fileId);
      return;
    }
    if (!accepted)
    {
      message.getFileData().setStatus(FileMessage.Status.DENIED);
      message.saveData();
    }
    else
    {
      message.getFileData().setStatus(FileMessage.Status.ACCEPTED);
      message.saveData();
      try
      {
        The5zigMod.getDataManager().getFileTransferManager().initFileUpload(message);
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    updateMessageText(message);
  }
  
  public void handleStartResponse(int fileId)
  {
    FileMessage message = getMessageByFileId(fileId);
    if (message == null) {
      return;
    }
    message.getFileData().setStatus(FileMessage.Status.UPLOADING);
    message.saveData();
    The5zigMod.getDataManager().getFileTransferManager().startUpload(fileId);
  }
  
  public void handleFileStart(int fileId, int parts, int chunkSize)
  {
    FileMessage message = getMessageByFileId(fileId);
    if (message == null) {
      return;
    }
    message.getFileData().setStatus(FileMessage.Status.DOWNLOADING);
    message.saveData();
    try
    {
      The5zigMod.getDataManager().getFileTransferManager().initFileDownload(fileId, parts, chunkSize, message);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  public void handleFileChunk(int fileId, int partId, byte[] data)
  {
    try
    {
      FileMessage message = getMessageByFileId(fileId);
      if (message == null) {
        return;
      }
      if (The5zigMod.getDataManager().getFileTransferManager().handleChunkDownload(Integer.valueOf(fileId), partId, data, message))
      {
        message.getFileData().setStatus(FileMessage.Status.DOWNLOADED);
        message.saveData();
        if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)))
        {
          TabConversations gui = (TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
          synchronized (gui.chatLines)
          {
            for (ChatLine chatLine : gui.chatLines) {
              if (((chatLine instanceof ImageChatLine)) && (chatLine.getMessage() == message))
              {
                ImageChatLine imageChatLine = (ImageChatLine)chatLine;
                imageChatLine.updateImage();
                break;
              }
            }
          }
        }
        updateMessageText(message);
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (FileTransferException e)
    {
      e.printStackTrace();
    }
    catch (NoSuchAlgorithmException e)
    {
      e.printStackTrace();
    }
  }
  
  public void handleFileAbort(int fileId)
  {
    FileMessage message = getMessageByFileId(fileId);
    if (message == null) {
      return;
    }
    if (message.getFileData().isOwn())
    {
      The5zigMod.getDataManager().getFileTransferManager().abortUpload(fileId);
      message.getFileData().setStatus(FileMessage.Status.UPLOAD_FAILED);
    }
    else
    {
      The5zigMod.getDataManager().getFileTransferManager().abortDownload(Integer.valueOf(fileId));
      message.getFileData().setStatus(FileMessage.Status.DOWNLOAD_FAILED);
    }
    message.saveData();
    updateMessageText(message);
  }
  
  public void setImageUploaded(FileMessage message)
  {
    message.setPercentage(1.0F);
    message.getFileData().setStatus(FileMessage.Status.UPLOADED);
    message.saveData();
    updateMessageText(message);
  }
  
  public Message getMessageById(UUID uuid, int id)
  {
    for (Conversation conversation : this.conversations) {
      if (((conversation instanceof ConversationChat)) && (((ConversationChat)conversation).getFriendUUID().equals(uuid))) {
        for (Message msg : conversation.getMessages()) {
          if (msg.getId() == id) {
            return msg;
          }
        }
      }
    }
    return null;
  }
  
  public FileMessage getMessageByFileId(int fileId)
  {
    FileMessage result = null;
    for (Conversation conversation : this.conversations) {
      for (Message msg : conversation.getMessages()) {
        if (((msg instanceof FileMessage)) && (((FileMessage)msg).getFileData().getFileId() == fileId))
        {
          if (result != null)
          {
            result.getFileData().setStatus(result.getFileData().isOwn() ? FileMessage.Status.UPLOAD_FAILED : FileMessage.Status.DOWNLOAD_FAILED);
            result.saveData();
            updateMessageText(result);
          }
          result = (FileMessage)msg;
        }
      }
    }
    if (result == null)
    {
      The5zigMod.logger.error("Could not find message for file id " + fileId);
      The5zigMod.getNetworkManager().sendPacket(new PacketFileTransferAbort(fileId), new GenericFutureListener[0]);
    }
    return result;
  }
  
  public void updateMessageText(final Message message)
  {
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        ConversationManager.this.sql.update("UPDATE conversation_chat_messages SET message=? WHERE id=?", new Object[] { message.getMessage(), Integer.valueOf(message.getId()) });
      }
    });
  }
  
  public void checkNewDay(final Conversation conversation, Message newMessage)
  {
    if ((!conversation.getMessages().isEmpty()) && (Utils.isSameDay(newMessage.getTime(), ((Message)conversation.getMessages().get(conversation.getMessages().size() - 1)).getTime()))) {
      return;
    }
    final long time = newMessage.getTime() - 1L;
    Message dateMessage = new Message(conversation, -1, "", "", time, Message.MessageType.DATE);
    conversation.addMessage(dateMessage);
    addChatLineToGui(conversation, dateMessage);
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id;
        int id;
        if ((conversation instanceof ConversationAnnouncements)) {
          id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO announcements_messages (message, time, type) VALUES (?, ?, ?)", new Object[] { "", Long.valueOf(time), Integer.valueOf(Message.MessageType.DATE.ordinal()) });
        } else {
          id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO " + ConversationManager.getMessagesTableNameByConversation(conversation) + "(conversationid, player, message, time, type) VALUES (?, ?, ?, ?," + " ?)", new Object[] {
            Integer.valueOf(conversation.getId()), "", "", Long.valueOf(time), Integer.valueOf(Message.MessageType.DATE.ordinal()) });
        }
        this.val$dateMessage.setId(id);
      }
    });
  }
  
  public void addChatLineToGui(Conversation conversation, Message message)
  {
    if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)))
    {
      TabConversations gui = (TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
      if (gui.getSelectedConversation().equals(conversation))
      {
        synchronized (gui.chatLines)
        {
          gui.chatLines.add(ChatLine.fromMessage(message));
        }
        gui.scrollToBottom();
        if (Display.isActive())
        {
          setConversationRead(conversation, true);
          return;
        }
      }
    }
    setConversationRead(conversation, false);
  }
  
  public void setAnnouncementMessages(List<Announcement> announcements)
  {
    if (announcements.isEmpty()) {
      return;
    }
    final ConversationAnnouncements conversation = getAnnouncementsConversation();
    for (final Announcement announcement : announcements) {
      The5zigMod.getAsyncExecutor().execute(new Runnable()
      {
        public void run()
        {
          int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO announcements_messages (message, time) VALUES (?, ?)", new Object[] { announcement.getMessage(), Long.valueOf(announcement.getTime()) });
          Message announcementMessage = new Message(conversation, id, "Announcement", announcement.getMessage(), announcement.getTime(), Message.MessageType.LEFT);
          ConversationManager.this.checkNewDay(conversation, announcementMessage);
          conversation.addMessage(announcementMessage);
        }
      });
    }
    if (conversation.getBehaviour() != Conversation.Behaviour.HIDE) {
      The5zigMod.getOverlayMessage().displayMessage(ChatColor.YELLOW + I18n.translate("announcement.new", new Object[] { Integer.valueOf(announcements.size()) }));
    }
    if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)))
    {
      TabConversations gui = (TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
      if (gui.getSelectedConversation().equals(conversation))
      {
        gui.scrollToBottom();
        synchronized (this.conversations)
        {
          gui.onSelect(this.conversations.indexOf(gui.getSelectedConversation()), gui.getSelectedConversation(), false);
        }
        setConversationRead(conversation, true);
      }
      else
      {
        setConversationRead(conversation, false);
      }
    }
    else
    {
      setConversationRead(conversation, false);
    }
    setConversationLastUsed(conversation);
  }
  
  public void addAnnouncementMessage(final String message, final long time)
  {
    ConversationAnnouncements conversation = getAnnouncementsConversation();
    if (conversation.getBehaviour() != Conversation.Behaviour.HIDE)
    {
      The5zigMod.getOverlayMessage().displayMessage(I18n.translate("announcement.new", new Object[] { Integer.valueOf(1) }), message);
      The5zigMod.getDataManager().getAfkManager().addNewMessage();
    }
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        int id = ConversationManager.this.sql.updateWithGeneratedKeys("INSERT INTO announcements_messages (message, time) VALUES (?, ?)", new Object[] { message, Long.valueOf(time) });
        Message msg = new Message(this.val$conversation, id, "Announcement", message, time, Message.MessageType.LEFT);
        ConversationManager.this.checkNewDay(this.val$conversation, msg);
        this.val$conversation.addMessage(msg);
        ConversationManager.this.addChatLineToGui(this.val$conversation, msg);
        ConversationManager.this.setConversationLastUsed(this.val$conversation);
      }
    });
  }
  
  public void setConversationStatus(final Conversation conversation, final Message.MessageStatus messageStatus)
  {
    Validate.notNull(conversation, "Conversation cannot be null", new Object[0]);
    Validate.notNull(messageStatus, "Message Status cannot be null", new Object[0]);
    if ((conversation instanceof ConversationAnnouncements)) {
      return;
    }
    conversation.setStatus(messageStatus);
    
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        ConversationManager.this.sql.update("UPDATE " + ConversationManager.getTableNameByConversation(conversation) + " SET status=? WHERE id=?", new Object[] { Integer.valueOf(messageStatus.ordinal()), Integer.valueOf(conversation.getId()) });
      }
    });
  }
  
  public void setConversationLastUsed(final Conversation conversation)
  {
    conversation.setLastUsed(System.currentTimeMillis());
    sortConversations();
    
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        ConversationManager.this.sql.update("UPDATE " + ConversationManager.getTableNameByConversation(conversation) + " SET lastused=? WHERE id=?", new Object[] { Long.valueOf(System.currentTimeMillis()), Integer.valueOf(conversation.getId()) });
      }
    });
  }
  
  public void setBehaviour(final Conversation conversation, final Conversation.Behaviour behaviour)
  {
    conversation.setBehaviour(behaviour);
    
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        ConversationManager.this.sql.update("UPDATE " + ConversationManager.getTableNameByConversation(conversation) + " SET behaviour=? WHERE id=?", new Object[] { Integer.valueOf(behaviour.ordinal()), Integer.valueOf(conversation.getId()) });
      }
    });
  }
  
  public void setConversationRead(final Conversation conversation, final boolean read)
  {
    if (conversation.isRead() == read) {
      return;
    }
    conversation.setRead(read);
    if (!read) {
      sortConversations();
    } else if (((conversation instanceof ConversationChat)) && (The5zigMod.getDataManager().getProfile().isShowMessageRead())) {
      The5zigMod.getNetworkManager().sendPacket(new PacketMessageFriendStatus(((ConversationChat)conversation).getFriendUUID(), Message.MessageStatus.READ), new GenericFutureListener[0]);
    }
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        ConversationManager.this.sql.update("UPDATE " + ConversationManager.getTableNameByConversation(conversation) + " SET read=? WHERE id=?", new Object[] { Boolean.valueOf(read), Integer.valueOf(conversation.getId()) });
      }
    });
  }
  
  public void sortConversations()
  {
    synchronized (this.conversations)
    {
      if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)))
      {
        TabConversations gui = (TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
        Conversation selected = gui.getSelectedConversation();
        Collections.sort(this.conversations, new Comparator()
        {
          public int compare(Conversation o1, Conversation o2)
          {
            return Long.valueOf(o2.getLastUsed()).compareTo(Long.valueOf(o1.getLastUsed()));
          }
        });
        gui.setCurrentConversation(selected);
      }
      else
      {
        Collections.sort(this.conversations, new Comparator()
        {
          public int compare(Conversation o1, Conversation o2)
          {
            return Long.valueOf(o2.getLastUsed()).compareTo(Long.valueOf(o1.getLastUsed()));
          }
        });
      }
    }
  }
  
  public void deleteConversation(final Conversation conversation)
  {
    synchronized (this.conversations)
    {
      if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)) && 
        (((TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab()).getSelectedConversation().equals(conversation)))
      {
        TabConversations gui = (TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
        this.conversations.remove(conversation);
        gui.onSelect(this.conversations.indexOf(gui.getSelectedConversation()), gui.getSelectedConversation(), false);
      }
      else
      {
        this.conversations.remove(conversation);
      }
    }
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        ConversationManager.this.sql.update("DELETE FROM " + ConversationManager.getTableNameByConversation(conversation) + " WHERE id=?", new Object[] { Integer.valueOf(conversation.getId()) });
        if ((conversation instanceof ConversationAnnouncements)) {
          ConversationManager.this.sql.update("TRUNCATE TABLE " + ConversationManager.getMessagesTableNameByConversation(conversation), new Object[0]);
        } else {
          ConversationManager.this.sql.update("DELETE FROM " + ConversationManager.getMessagesTableNameByConversation(conversation) + " WHERE conversationid=?", new Object[] { Integer.valueOf(conversation.getId()) });
        }
      }
    });
  }
  
  public static String getTableNameByConversation(Conversation conversation)
  {
    String table = "conversations_chat";
    if ((conversation instanceof ConversationGroupChat)) {
      table = "conversations_groupchat";
    }
    if ((conversation instanceof ConversationAnnouncements)) {
      table = "announcements";
    }
    return table;
  }
  
  public static String getMessagesTableNameByConversation(Conversation conversation)
  {
    String table = "conversation_chat_messages";
    if ((conversation instanceof ConversationGroupChat)) {
      table = "conversation_groupchat_messages";
    }
    if ((conversation instanceof ConversationAnnouncements)) {
      table = "announcements_messages";
    }
    return table;
  }
}
