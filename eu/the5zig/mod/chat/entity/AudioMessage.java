package eu.the5zig.mod.chat.entity;

public class AudioMessage
  extends FileMessage
{
  public AudioMessage(Conversation conversation, int id, String username, String message, long time, Message.MessageType type)
  {
    super(conversation, id, username, message, time, type);
  }
  
  public AudioMessage(Conversation conversation, int id, String username, FileMessage.FileData fileData, long time, Message.MessageType type)
  {
    super(conversation, id, username, fileData, time, type);
  }
  
  protected Class<? extends FileMessage.FileData> getDataClass()
  {
    return AudioData.class;
  }
  
  public static class AudioData
    extends FileMessage.FileData
  {
    public AudioData() {}
    
    public AudioData(FileMessage.Status status)
    {
      super();
    }
  }
}
