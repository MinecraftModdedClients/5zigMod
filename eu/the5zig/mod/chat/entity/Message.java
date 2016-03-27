package eu.the5zig.mod.chat.entity;

import eu.the5zig.util.minecraft.ChatColor;

public class Message
  implements Comparable<Message>
{
  private final Conversation conversation;
  private int id;
  private String username;
  private String message;
  private long time;
  private MessageType messageType;
  
  public Message(Conversation conversation, int id, String username, String message, long time, MessageType messageType)
  {
    this.conversation = conversation;
    this.id = id;
    this.username = username;
    this.message = message;
    this.time = time;
    this.messageType = messageType;
  }
  
  public Conversation getConversation()
  {
    return this.conversation;
  }
  
  public void setId(int id)
  {
    this.id = id;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public MessageType getMessageType()
  {
    return this.messageType;
  }
  
  public String getUsername()
  {
    return this.username;
  }
  
  public void setMessage(String message)
  {
    this.message = message;
  }
  
  public String getMessage()
  {
    return this.message;
  }
  
  public long getTime()
  {
    return this.time;
  }
  
  public int compareTo(Message o)
  {
    return Long.valueOf(getTime()).compareTo(Long.valueOf(o.getTime()));
  }
  
  public String toString()
  {
    return this.username + ChatColor.RESET + ": " + this.message;
  }
  
  public static enum MessageStatus
  {
    PENDING,  SENT,  DELIVERED,  READ;
    
    private MessageStatus() {}
  }
  
  public static enum MessageType
  {
    LEFT,  RIGHT,  CENTERED,  DATE,  IMAGE,  AUDIO;
    
    private MessageType() {}
  }
}
