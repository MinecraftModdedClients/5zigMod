package eu.the5zig.mod.chat.gui;

import com.google.common.collect.Maps;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.entity.ConversationChat;
import eu.the5zig.mod.chat.entity.FileMessage.FileData;
import eu.the5zig.mod.chat.entity.ImageMessage;
import eu.the5zig.mod.chat.entity.ImageMessage.ImageData;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.TabConversations;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import javax.imageio.ImageIO;

public class ImageChatLine
  extends FileChatLine
{
  private static final HashMap<String, CachedImage> resourceLocations = ;
  private final int maxImageWidth = 160;
  private final int maxImageHeight = 90;
  private final int minImageWidth = 60;
  private final int minImageHeight = 35;
  private File file;
  private Object resourceLocation;
  private BufferedImage bufferedImage;
  private boolean hoverImage = false;
  
  public ImageChatLine(Message message)
  {
    super(message);
    updateImage();
  }
  
  private ImageMessage getImageMessage()
  {
    return (ImageMessage)getMessage();
  }
  
  private ImageMessage.ImageData getImageData()
  {
    return (ImageMessage.ImageData)getImageMessage().getFileData();
  }
  
  public void updateImage()
  {
    final String hash = getImageMessage().getFileData().getHash();
    if ((hash == null) || (resourceLocations.containsKey(hash)))
    {
      if ((hash != null) && (resourceLocations.containsKey(hash))) {
        setImageWidthAndHeight(((CachedImage)resourceLocations.get(hash)).width, ((CachedImage)resourceLocations.get(hash)).height);
      }
      return;
    }
    resourceLocations.put(hash, new CachedImage(null, 100, 50, 0, 0));
    new Thread()
    {
      public void run()
      {
        ImageChatLine.this.file = new File("the5zigmod/media/" + 
          The5zigMod.getDataManager().getUniqueId() + "/" + ((ConversationChat)ImageChatLine.this.getMessage().getConversation()).getFriendUUID().toString() + "/" + hash);
        try
        {
          BufferedImage bufferedImage = ImageIO.read(ImageChatLine.this.file);
          if (bufferedImage == null) {
            throw new IOException("Image could not be loaded!");
          }
          double imageWidth = bufferedImage.getWidth();
          double imageHeight = bufferedImage.getHeight();
          while ((imageWidth > 160.0D) || (imageHeight > 90.0D))
          {
            imageWidth /= 1.1D;
            imageHeight /= 1.1D;
          }
          while ((imageWidth < 60.0D) || (imageHeight < 35.0D))
          {
            imageWidth *= 1.1D;
            imageHeight *= 1.1D;
          }
          ((ImageChatLine.CachedImage)ImageChatLine.resourceLocations.get(hash)).realWidth = bufferedImage.getWidth();
          ((ImageChatLine.CachedImage)ImageChatLine.resourceLocations.get(hash)).realHeight = bufferedImage.getHeight();
          ImageChatLine.this.getImageData().setRealHeight(bufferedImage.getHeight());
          ImageChatLine.this.getImageData().setRealWidth(bufferedImage.getWidth());
          ImageChatLine.this.setImageWidthAndHeight((int)imageWidth, (int)imageHeight);
          ImageChatLine.this.bufferedImage = bufferedImage;
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
      }
    }.start();
  }
  
  private void setImageWidthAndHeight(int width, int height)
  {
    int currentHeight = getImageData().getHeight();
    getImageData().setWidth(width);
    getImageData().setHeight(height);
    getImageMessage().saveData();
    if (((The5zigMod.getVars().getCurrentScreen() instanceof GuiChat)) && ((((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab() instanceof TabConversations)))
    {
      TabConversations tab = (TabConversations)((GuiChat)The5zigMod.getVars().getCurrentScreen()).getCurrentTab();
      tab.chatList.scrollTo(tab.chatList.getCurrentScroll() + height - currentHeight);
    }
    The5zigMod.getConversationManager().updateMessageText(getImageMessage());
  }
  
  protected String getName()
  {
    return "Image";
  }
  
  protected void preDraw(int x, int y, int width, int height, int mouseX, int mouseY)
  {
    String hash = getImageMessage().getFileData().getHash();
    if ((this.resourceLocation == null) && (hash != null) && (resourceLocations.containsKey(hash)) && (((CachedImage)resourceLocations.get(hash)).resourceLocation != null))
    {
      this.resourceLocation = ((CachedImage)resourceLocations.get(hash)).resourceLocation;
      getImageData().setRealWidth(((CachedImage)resourceLocations.get(hash)).realWidth);
      getImageData().setRealHeight(((CachedImage)resourceLocations.get(hash)).realHeight);
      setImageWidthAndHeight(((CachedImage)resourceLocations.get(hash)).width, ((CachedImage)resourceLocations.get(hash)).height);
    }
    else if ((this.bufferedImage != null) && (this.resourceLocation == null))
    {
      this.resourceLocation = The5zigMod.getVars().bindDynamicImage(hash, this.bufferedImage);
      resourceLocations.put(hash, new CachedImage(this.resourceLocation, getWidth(), getHeight(), getImageData().getRealWidth(), getImageData().getRealHeight()));
      setImageWidthAndHeight(((CachedImage)resourceLocations.get(hash)).width, ((CachedImage)resourceLocations.get(hash)).height);
    }
  }
  
  protected boolean drawOverlay()
  {
    return this.resourceLocation != null;
  }
  
  protected void drawBackground(int x, int y, int width, int height, int mouseX, int mouseY)
  {
    The5zigMod.getVars().bindTexture(this.resourceLocation);
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
    Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, width, height, width, height);
    this.hoverImage = ((mouseX >= x) && (mouseX <= x + width) && (mouseY >= y) && (mouseY <= y + height));
    if (this.hoverImage) {
      drawRect(x, y, width, height, true);
    }
  }
  
  protected void postDraw(int x, int y, int width, int height, int mouseX, int mouseY)
  {
    if (this.resourceLocation != null) {
      return;
    }
    this.hoverImage = false;
    drawRect(x, y, width, height, false);
    drawStatus(I18n.translate("chat.image.not_found"), x, y, width, height, 0.8F);
  }
  
  public IButton mousePressed(int mouseX, int mouseY)
  {
    IButton result = super.mousePressed(mouseX, mouseY);
    if (this.hoverImage) {
      The5zigMod.getVars().displayScreen(new GuiViewImage(The5zigMod.getVars().getCurrentScreen(), this.resourceLocation, getImageData(), "the5zigmod/media/" + 
        The5zigMod.getDataManager().getUniqueId() + "/" + ((ConversationChat)getMessage().getConversation()).getFriendUUID().toString()));
    }
    return result;
  }
  
  protected int getWidth()
  {
    return getImageData().getWidth();
  }
  
  protected int getHeight()
  {
    return getImageData().getHeight();
  }
  
  public int getLineHeight()
  {
    return getImageData().getHeight() + 18 + 10;
  }
  
  private class CachedImage
  {
    private Object resourceLocation;
    private int width;
    private int height;
    private int realWidth;
    private int realHeight;
    
    public CachedImage(Object resourceLocation, int width, int height, int realWidth, int realHeight)
    {
      this.resourceLocation = resourceLocation;
      this.width = width;
      this.height = height;
      this.realWidth = realWidth;
      this.realHeight = realHeight;
    }
  }
}
