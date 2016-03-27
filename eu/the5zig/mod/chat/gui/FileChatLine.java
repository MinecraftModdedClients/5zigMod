package eu.the5zig.mod.chat.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.ConversationManager;
import eu.the5zig.mod.chat.entity.Conversation;
import eu.the5zig.mod.chat.entity.FileMessage;
import eu.the5zig.mod.chat.entity.FileMessage.FileData;
import eu.the5zig.mod.chat.entity.FileMessage.Status;
import eu.the5zig.mod.chat.entity.Message;
import eu.the5zig.mod.chat.entity.Message.MessageType;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketFileTransferResponse;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.GuiChat;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.RowExtended;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.AsyncExecutor;
import eu.the5zig.util.Utils;
import eu.the5zig.util.db.Database;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.List;

public abstract class FileChatLine
  extends ChatLine
  implements RowExtended
{
  protected boolean hoverDeny = false;
  protected boolean hoverAccept = false;
  
  public FileChatLine(Message message)
  {
    super(message);
  }
  
  protected FileMessage getFileMessage()
  {
    return (FileMessage)getMessage();
  }
  
  protected FileMessage.FileData getFileData()
  {
    return getFileMessage().getFileData();
  }
  
  public void draw(int x, int y) {}
  
  public void draw(int x, int y, int slotHeight, int mouseX, int mouseY)
  {
    GuiChat gui = (GuiChat)The5zigMod.getVars().getCurrentScreen();
    
    int imageWidth = getWidth();
    int imageHeight = getHeight();
    
    String time = ChatColor.GRAY + Utils.convertToTimeWithMinutes(getMessage().getTime());
    int timeWidth = (int)(The5zigMod.getVars().getStringWidth(time) * 0.6F);
    
    String message = getMessage().getUsername() + ChatColor.RESET + ": " + getName();
    FileMessage.Status status = getFileData().getStatus();
    int yButton;
    if ((status.ordinal() >= FileMessage.Status.REQUEST.ordinal()) && (status.ordinal() < FileMessage.Status.WAITING.ordinal()))
    {
      The5zigMod.getVars().drawString(message, x + 2, y + 2);
      Gui.drawScaledString(time, x + 4, y + imageHeight + 12 + 4, 0.6F);
      
      preDraw(x + 4, y + 12 + 2, imageWidth, imageHeight, mouseX, mouseY);
      
      this.hoverAccept = (this.hoverDeny = 0);
      if (drawOverlay())
      {
        drawBackground(x + 2, y + 12 + 2, imageWidth, imageHeight, mouseX, mouseY);
      }
      else
      {
        int yy = y + 12 + 2;
        if (status == FileMessage.Status.DOWNLOAD_FAILED)
        {
          drawRect(x + 2, yy, imageWidth, imageHeight, drawOverlay());
          drawStatus(I18n.translate("chat.file.download_failed"), x + 2, yy, imageWidth, imageHeight, 0.8F);
        }
        else if (status == FileMessage.Status.REQUEST)
        {
          int xAccept = x + imageWidth / 2 - 20;
          int xDeny = x + imageWidth / 2 + 4;
          int yBox = y + 12 + 2;
          yButton = yy + imageHeight - 24;
          this.hoverAccept = ((mouseX >= xAccept) && (mouseX <= xAccept + 16) && (mouseY >= yButton) && (mouseY <= yButton + 16));
          this.hoverDeny = ((mouseX >= xDeny) && (mouseX <= xDeny + 16) && (mouseY >= yButton) && (mouseY <= yButton + 16));
          
          Gui.drawRect(x + 2, yBox, x + imageWidth, yBox + imageHeight, -5592406);
          Gui.drawRect(x + 3, yBox + 1, x + imageWidth - 1, yBox + imageHeight - 1, -14540254);
          Gui.drawScaledCenteredString(ChatColor.UNDERLINE + I18n.translate("chat.file.accept"), x + imageWidth / 2, yBox + 2, 0.8F);
          Gui.drawScaledCenteredString(Utils.bytesToReadable(getFileData().getLength()), x + imageWidth / 2, yBox + 10, 0.6F);
          The5zigMod.getVars().bindTexture(The5zigMod.ITEMS);
          GLUtil.enableBlend();
          if (this.hoverAccept) {
            GLUtil.color(0.2F, 0.6F, 0.2F, 1.0F);
          } else {
            GLUtil.color(0.0F, 1.0F, 0.0F, 1.0F);
          }
          Gui.drawModalRectWithCustomSizedTexture(xAccept, yButton, 32.0F, 0.0F, 16, 16, 132.0F, 132.0F);
          if (this.hoverDeny) {
            GLUtil.color(0.6F, 0.6F, 0.6F, 1.0F);
          } else {
            GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
          }
          Gui.drawModalRectWithCustomSizedTexture(xDeny, yButton, 48.0F, 0.0F, 16, 16, 132.0F, 132.0F);
          GLUtil.disableBlend();
        }
        else if (status == FileMessage.Status.REQUEST_ACCEPTED)
        {
          drawRect(x + 2, yy, imageWidth, imageHeight, drawOverlay());
          drawStatus(I18n.translate("chat.file.waiting"), x + 2, yy, imageWidth, imageHeight, 0.8F);
        }
        else if (status == FileMessage.Status.REQUEST_DENIED)
        {
          drawRect(x + 2, yy, imageWidth, imageHeight, drawOverlay());
          drawStatus(I18n.translate("chat.file.download_denied"), x + 2, yy, imageWidth, imageHeight, 0.8F);
        }
        else if (status == FileMessage.Status.DOWNLOADING)
        {
          drawProgressStatus(x + 2, yy, imageWidth, imageHeight, drawOverlay());
          Gui.drawScaledString(I18n.translate("chat.file.downloading"), x + 2 + (imageWidth - Math.min(imageWidth - 8, 60)) / 2, yy + imageHeight / 2 - 10, 0.8F);
        }
        else
        {
          postDraw(x + 2, yy, imageWidth, imageHeight, mouseX, mouseY);
        }
      }
    }
    else
    {
      The5zigMod.getVars().drawString(message, gui.getWidth() - 22 - The5zigMod.getVars().getStringWidth(message), y + 2);
      Gui.drawScaledString(time, gui.getWidth() - 22 - timeWidth, y + imageHeight + 12 + 4, 0.6F);
      
      int xx = gui.getWidth() - 22 - imageWidth;
      int yy = y + 12 + 2;
      preDraw(xx, yy, imageWidth, imageHeight, mouseX, mouseY);
      if ((imageWidth == 0) || (imageHeight == 0)) {
        return;
      }
      if (drawOverlay()) {
        drawBackground(xx, yy, imageWidth, imageHeight, mouseX, mouseY);
      }
      if (status == FileMessage.Status.UPLOAD_FAILED)
      {
        drawRect(xx, yy, imageWidth, imageHeight, drawOverlay());
        drawStatus(I18n.translate("chat.file.upload_failed"), xx, yy, imageWidth, imageHeight, 0.8F);
      }
      else if (status == FileMessage.Status.WAITING)
      {
        drawRect(xx, yy, imageWidth, imageHeight, drawOverlay());
        drawStatus(I18n.translate("chat.file.request_sent"), xx, yy, imageWidth, imageHeight, 0.8F);
      }
      else if (status == FileMessage.Status.ACCEPTED)
      {
        drawRect(xx, yy, imageWidth, imageHeight, drawOverlay());
        drawStatus(I18n.translate("chat.file.waiting"), xx, yy, imageWidth, imageHeight, 0.8F);
      }
      else if (status == FileMessage.Status.DENIED)
      {
        drawRect(xx, yy, imageWidth, imageHeight, drawOverlay());
        drawStatus(I18n.translate("chat.file.upload_denied"), xx, yy, imageWidth, imageHeight, 0.8F);
      }
      else if (status == FileMessage.Status.UPLOADING)
      {
        drawProgressStatus(xx, yy, imageWidth, imageHeight, drawOverlay());
        Gui.drawScaledString(I18n.translate("chat.file.uploading"), xx + (imageWidth - Math.min(imageWidth - 8, 60)) / 2, yy + imageHeight / 2 - 10, 0.8F);
      }
      else
      {
        postDraw(xx, yy, imageWidth, imageHeight, mouseX, mouseY);
      }
      Message lastMessage = null;
      List<Message> messages = Lists.newArrayList(getMessage().getConversation().getMessages());
      for (Message conversationMessage : messages) {
        if ((conversationMessage.getMessageType() == Message.MessageType.RIGHT) || (((conversationMessage instanceof FileMessage)) && 
          (((FileMessage)conversationMessage).getFileData().isOwn()))) {
          lastMessage = conversationMessage;
        }
      }
      if ((lastMessage != null) && (lastMessage.equals(getMessage())))
      {
        String status1;
        String status1;
        String status1;
        String status1;
        switch (getMessage().getConversation().getStatus())
        {
        case SENT: 
          status1 = I18n.translate("chat.status.sent");
          break;
        case DELIVERED: 
          status1 = I18n.translate("chat.status.delivered");
          break;
        case READ: 
          status1 = I18n.translate("chat.status.read");
          break;
        default: 
          status1 = I18n.translate("chat.status.pending");
        }
        String string = ChatColor.ITALIC.toString() + status1;
        int stringWidth = (int)(0.6F * The5zigMod.getVars().getStringWidth(string));
        Gui.drawScaledString(string, gui.getWidth() - 22 - stringWidth, yy + imageHeight + 10, 0.6F);
      }
    }
  }
  
  public IButton mousePressed(int mouseX, int mouseY)
  {
    if ((getFileData().getStatus() == FileMessage.Status.REQUEST) && ((this.hoverAccept) || (this.hoverDeny)))
    {
      The5zigMod.getNetworkManager().sendPacket(new PacketFileTransferResponse(getFileData().getFileId(), this.hoverAccept), new GenericFutureListener[0]);
      getFileData().setStatus(this.hoverAccept ? FileMessage.Status.REQUEST_ACCEPTED : FileMessage.Status.REQUEST_DENIED);
      getFileMessage().saveData();
      The5zigMod.getAsyncExecutor().execute(new Runnable()
      {
        public void run()
        {
          The5zigMod.getConversationDatabase().update("UPDATE " + ConversationManager.getMessagesTableNameByConversation(FileChatLine.this.getMessage().getConversation()) + " SET message=? " + "WHERE id=?", new Object[] {FileChatLine.this
            .getMessage().getMessage(), Integer.valueOf(FileChatLine.this.getMessage().getId()) });
        }
      });
    }
    return null;
  }
  
  protected abstract String getName();
  
  protected abstract int getWidth();
  
  protected abstract int getHeight();
  
  protected void preDraw(int x, int y, int width, int height, int mouseX, int mouseY) {}
  
  protected abstract boolean drawOverlay();
  
  protected void drawBackground(int x, int y, int width, int height, int mouseX, int mouseY) {}
  
  protected void postDraw(int x, int y, int width, int height, int mouseX, int mouseY) {}
  
  protected void drawStatus(String string, int x, int y, int width, int height, float scale)
  {
    List<String> split = The5zigMod.getVars().splitStringToWidth(string, width);
    y = y + height / 2 - 5 * split.size();
    for (int i = 0; i < split.size(); i++) {
      Gui.drawScaledCenteredString((String)split.get(i), x + width / 2, y + i * 10, scale);
    }
  }
  
  protected void drawRect(int x, int y, int width, int height, boolean overlay)
  {
    if (overlay)
    {
      Gui.drawRect(x, y, x + width, y + height, -1728053248);
    }
    else
    {
      Gui.drawRect(x, y, x + width, y + height, -3355444);
      Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, -14540254);
    }
  }
  
  protected void drawProgressStatus(int imageX, int imageY, int width, int height, boolean overlay)
  {
    drawRect(imageX, imageY, width, height, overlay);
    int progressWidth1 = Math.min(width - 8, 60);
    int progressWidth2 = Math.min(width - 10, 58);
    int progressHeight1 = 6;
    int progressHeight2 = 4;
    int progressX1 = imageX + (width - progressWidth1) / 2;
    int progressX2 = imageX + (width - progressWidth2) / 2;
    int progressY1 = imageY + (height - progressHeight1) / 2;
    int progressY2 = imageY + (height - progressHeight2) / 2;
    Gui.drawRect(progressX1, progressY1, progressX1 + progressWidth1, progressY1 + progressHeight1, -15658735);
    Gui.drawRect(progressX2, progressY2, progressX2 + (int)(progressWidth2 * getFileMessage().getPercentage()), progressY2 + progressHeight2, -5592406);
  }
}
