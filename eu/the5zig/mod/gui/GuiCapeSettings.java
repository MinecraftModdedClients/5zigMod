package eu.the5zig.mod.gui;

import com.google.common.base.Charsets;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.entity.Rank;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketCapeSettings;
import eu.the5zig.mod.chat.network.packets.PacketCapeSettings.Action;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.ingame.resource.IResourceManager;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.FileSelectorCallback;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.concurrent.GenericFutureListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class GuiCapeSettings
  extends Gui
{
  private static long throttle;
  private String status;
  
  public GuiCapeSettings(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  protected void tick()
  {
    enableCapeButtons(true);
    if (!The5zigMod.getNetworkManager().isConnected())
    {
      this.status = (ChatColor.RED + I18n.translate("cape.not_connected"));
      enableCapeButtons(false);
    }
    else if (The5zigMod.getDataManager().getProfile().getRank() == Rank.NONE)
    {
      this.status = (ChatColor.RED + I18n.translate("cape.not_donator"));
      enableCapeButtons(false);
    }
    else if (The5zigMod.getDataManager().getProfile().getRank() == Rank.DEFAULT)
    {
      this.status = (ChatColor.GOLD + I18n.translate("cape.not_custom"));
      getButtonById(2).setEnabled(false);
    }
    else
    {
      this.status = null;
      if (!The5zigMod.getDataManager().getProfile().isCapeEnabled())
      {
        getButtonById(2).setEnabled(false);
        getButtonById(3).setEnabled(false);
      }
      else
      {
        getButtonById(2).setEnabled(true);
        getButtonById(3).setEnabled(true);
      }
    }
  }
  
  private void enableCapeButtons(boolean enable)
  {
    if (System.currentTimeMillis() - throttle > 0L) {
      throttle = 0L;
    } else {
      enable = false;
    }
    for (int i = 1; i < 4; i++) {
      getButtonById(i).setEnabled(enable);
    }
  }
  
  public void initGui()
  {
    addDoneButton();
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 155, getHeight() / 6 - 6, 150, 20, 
      The5zigMod.getDataManager().getProfile().isCapeEnabled() ? I18n.translate("cape.disable") : I18n.translate("cape.enable")));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 + 5, getHeight() / 6 - 6, 150, 20, I18n.translate("cape.upload")));
    addButton(The5zigMod.getVars().createButton(3, getWidth() / 2 - 155, getHeight() / 6 + 24 - 6, 150, 20, I18n.translate("cape.default")));
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      The5zigMod.getDataManager().getProfile().setCapeEnabled(!The5zigMod.getDataManager().getProfile().isCapeEnabled());
      if (!The5zigMod.getVars().isPlayerNull()) {
        The5zigMod.getVars().setOwnCape(null);
      }
      button.setLabel(The5zigMod.getDataManager().getProfile().isCapeEnabled() ? I18n.translate("cape.disable") : I18n.translate("cape.enable"));
      button.setTicksDisabled(100);
      throttle = System.currentTimeMillis() + 5000L;
    }
    if (button.getId() == 2) {
      The5zigMod.getVars().displayScreen(new GuiFileSelector(this, new FileSelectorCallback()
      {
        public void onDone(File file)
        {
          try
          {
            if (file.length() > 200000L)
            {
              The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.RED + I18n.translate("cape.upload.max_size"));
              return;
            }
            BufferedImage image = ImageIO.read(file);
            int width = image.getWidth();
            int height = image.getHeight();
            if (((width != 1024) || (height != 512)) && ((width != 128) || (height != 64)) && ((width != 64) || (height != 32)))
            {
              The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.RED + I18n.translate("cape.upload.wrong_dimension"));
              return;
            }
            String base64 = GuiCapeSettings.getBase64(image);
            The5zigMod.getNetworkManager().sendPacket(new PacketCapeSettings(PacketCapeSettings.Action.UPLOAD_CUSTOM, base64), new GenericFutureListener[0]);
            
            The5zigMod.getOverlayMessage().displayMessage(I18n.translate("cape.upload.uploading"));
            if (!The5zigMod.getVars().isPlayerNull()) {
              The5zigMod.getVars().setOwnCape(null);
            }
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
        }
        
        public String getTitle()
        {
          return "The 5zig Mod - " + I18n.translate("cape.upload.title");
        }
      }, new String[] { "png" }));
    }
    if (button.getId() == 3) {
      The5zigMod.getVars().displayScreen(new GuiUploadDefaultCape(this));
    }
  }
  
  private static String getBase64(BufferedImage bufferedImage)
    throws IOException
  {
    ByteBuf localByteBuf1 = Unpooled.buffer();
    ImageIO.write(bufferedImage, "PNG", new ByteBufOutputStream(localByteBuf1));
    ByteBuf localByteBuf2 = Base64.encode(localByteBuf1);
    return localByteBuf2.toString(Charsets.UTF_8);
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    if (this.status != null) {
      drawCenteredString(this.status, getWidth() / 2, getHeight() / 6 + 43);
    }
    Object capeLocation = The5zigMod.getVars().getResourceManager().getOwnCapeLocation();
    if (capeLocation == null) {
      return;
    }
    The5zigMod.getVars().bindTexture(capeLocation);
    int width = 384;
    int height = 192;
    int texWidth = 22 * width / 64;
    int texHeight = 17 * height / 32;
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
    Gui.drawModalRectWithCustomSizedTexture(getWidth() / 2 - texWidth / 2, getHeight() / 6 + 110 - texHeight / 2, 0.0F, 0.0F, texWidth, texHeight, width, height);
  }
  
  public String getTitleKey()
  {
    return "cape.title";
  }
}
