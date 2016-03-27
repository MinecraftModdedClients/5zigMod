package eu.the5zig.mod.chat.entity;

public class ImageMessage
  extends FileMessage
{
  public ImageMessage(Conversation conversation, int id, String username, String message, long time, Message.MessageType type)
  {
    super(conversation, id, username, message, time, type);
  }
  
  public ImageMessage(Conversation conversation, int id, String username, ImageData imageData, long time, Message.MessageType type)
  {
    super(conversation, id, username, imageData, time, type);
  }
  
  protected Class<? extends FileMessage.FileData> getDataClass()
  {
    return ImageData.class;
  }
  
  public static class ImageData
    extends FileMessage.FileData
  {
    private int width;
    private int height;
    private int realWidth;
    private int realHeight;
    
    public ImageData()
    {
      this.width = 100;
      this.height = 50;
    }
    
    public ImageData(FileMessage.Status status)
    {
      super();
      this.width = 100;
      this.height = 50;
    }
    
    public int getWidth()
    {
      return this.width;
    }
    
    public void setWidth(int width)
    {
      this.width = width;
    }
    
    public int getHeight()
    {
      return this.height;
    }
    
    public void setHeight(int height)
    {
      this.height = height;
    }
    
    public int getRealWidth()
    {
      return this.realWidth;
    }
    
    public void setRealWidth(int realWidth)
    {
      this.realWidth = realWidth;
    }
    
    public int getRealHeight()
    {
      return this.realHeight;
    }
    
    public void setRealHeight(int realHeight)
    {
      this.realHeight = realHeight;
    }
  }
}
