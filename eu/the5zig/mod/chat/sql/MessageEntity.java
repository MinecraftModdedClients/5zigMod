package eu.the5zig.mod.chat.sql;

public class MessageEntity
{
  private int id;
  private int conversationid;
  private String player;
  private String message;
  private long time;
  private int type;
  
  public int getId()
  {
    return this.id;
  }
  
  public int getConversationid()
  {
    return this.conversationid;
  }
  
  public String getPlayer()
  {
    return this.player;
  }
  
  public String getMessage()
  {
    return this.message;
  }
  
  public long getTime()
  {
    return this.time;
  }
  
  public int getType()
  {
    return this.type;
  }
}
