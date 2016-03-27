package eu.the5zig.mod.chat.sql;

import eu.the5zig.mod.chat.entity.Conversation.Behaviour;

public class SkypeEntity
{
  private int id;
  private String friend;
  private String chat_id;
  private long lastused;
  private boolean read;
  private int behaviour;
  
  public int getId()
  {
    return this.id;
  }
  
  public String getFriend()
  {
    return this.friend;
  }
  
  public String getChatId()
  {
    return this.chat_id;
  }
  
  public long getLastused()
  {
    return this.lastused;
  }
  
  public boolean isRead()
  {
    return this.read;
  }
  
  public Conversation.Behaviour getBehaviour()
  {
    return Conversation.Behaviour.values()[this.behaviour];
  }
}
