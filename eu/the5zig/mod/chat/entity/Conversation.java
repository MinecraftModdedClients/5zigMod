package eu.the5zig.mod.chat.entity;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.gui.elements.Row;
import java.util.List;

public abstract class Conversation
  implements Row, Comparable<Conversation>
{
  public final int MAX_MESSAGES_PLUS = 50;
  private final int id;
  private List<Message> messages = Lists.newArrayList();
  private long lastUsed;
  private boolean read;
  private int maxMessages = 100;
  private Message.MessageStatus status;
  private Behaviour behaviour;
  private String currentMessage = "";
  private List<String> lastMessages = Lists.newArrayList();
  private int lastMessageIndex;
  
  public Conversation(int id, long lastUsed, boolean read, Message.MessageStatus status, Behaviour behaviour)
  {
    this.id = id;
    this.lastUsed = lastUsed;
    this.read = read;
    this.status = status;
    this.behaviour = behaviour;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public void addMessage(Message message)
  {
    this.messages.add(message);
  }
  
  public long getLastUsed()
  {
    return this.lastUsed;
  }
  
  public void setLastUsed(long lastUsed)
  {
    this.lastUsed = lastUsed;
  }
  
  public List<Message> getMessages()
  {
    return this.messages;
  }
  
  public void setMessages(List<Message> messages)
  {
    this.messages = messages;
  }
  
  public boolean isRead()
  {
    return this.read;
  }
  
  public void setRead(boolean read)
  {
    this.read = read;
  }
  
  public Message.MessageStatus getStatus()
  {
    return this.status;
  }
  
  public void setStatus(Message.MessageStatus status)
  {
    this.status = status;
  }
  
  public int getMaxMessages()
  {
    return this.maxMessages;
  }
  
  public void setMaxMessages(int maxMessages)
  {
    this.maxMessages = maxMessages;
  }
  
  public Behaviour getBehaviour()
  {
    return this.behaviour;
  }
  
  public void setBehaviour(Behaviour behaviour)
  {
    this.behaviour = behaviour;
  }
  
  public String getCurrentMessage()
  {
    return this.currentMessage;
  }
  
  public void setCurrentMessage(String currentMessage)
  {
    this.currentMessage = currentMessage;
  }
  
  public void addLastSentMessage(String message)
  {
    this.lastMessages.add(message);
    this.lastMessageIndex = this.lastMessages.size();
  }
  
  public String getPreviousSentMessage()
  {
    if (this.lastMessages.isEmpty()) {
      return "";
    }
    this.lastMessageIndex -= 1;
    if (this.lastMessageIndex < 0) {
      return (String)this.lastMessages.get(this.lastMessageIndex = 0);
    }
    return (String)this.lastMessages.get(this.lastMessageIndex);
  }
  
  public String getNextSentMessage()
  {
    if (this.lastMessages.isEmpty()) {
      return "";
    }
    this.lastMessageIndex += 1;
    if (this.lastMessageIndex > this.lastMessages.size() - 1)
    {
      this.lastMessageIndex = this.lastMessages.size();
      return "";
    }
    return (String)this.lastMessages.get(this.lastMessageIndex);
  }
  
  public int compareTo(Conversation conversation)
  {
    return Long.valueOf(conversation.getLastUsed()).compareTo(Long.valueOf(getLastUsed()));
  }
  
  public static enum Behaviour
  {
    DEFAULT("chat.conversation_settings.behaviour.default"),  SHOW("chat.conversation_settings.behaviour.show"),  HIDE("chat.conversation_settings.behaviour.hide");
    
    private String type;
    
    private Behaviour(String type)
    {
      this.type = type;
    }
    
    public Behaviour getNext()
    {
      return values()[((ordinal() + 1) % values().length)];
    }
    
    public String getName()
    {
      return I18n.translate(this.type);
    }
  }
}
