package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.mod.chat.network.packets.PacketCapeSettings;
import eu.the5zig.mod.chat.network.packets.PacketCapeSettings.Action;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IResourceLocation;
import eu.the5zig.mod.util.IVariables;
import io.netty.util.concurrent.GenericFutureListener;

public class GuiUploadDefaultCape
  extends Gui
{
  private static final IResourceLocation capesTexture = The5zigMod.getVars().createResourceLocation("the5zigmod", "textures/capes.png");
  private int selectedCape = -1;
  
  public GuiUploadDefaultCape(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    addCancelButton();
  }
  
  protected void actionPerformed(IButton button) {}
  
  protected void mouseClicked(int mouseX, int mouseY, int button)
  {
    if ((button != 0) || (this.selectedCape < 0) || (this.selectedCape > PacketCapeSettings.Action.values().length)) {
      return;
    }
    The5zigMod.getNetworkManager().sendPacket(new PacketCapeSettings(PacketCapeSettings.Action.UPLOAD_DEFAULT, eu.the5zig.mod.chat.network.packets.PacketCapeSettings.Cape.values()[this.selectedCape]), new GenericFutureListener[0]);
    The5zigMod.getVars().displayScreen(this.lastScreen);
    The5zigMod.getOverlayMessage().displayMessage(I18n.translate("cape.upload.uploading"));
    if (!The5zigMod.getVars().isPlayerNull()) {
      The5zigMod.getVars().setOwnCape(null);
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawCenteredString(I18n.translate("cape.upload.default.help"), getWidth() / 2, getHeight() / 6 - 10);
    
    The5zigMod.getVars().bindTexture(capesTexture);
    int capeWidth = 66;
    int capeHeight = 51;
    int xOffset = getWidth() / 2 - capeWidth * 2 - 4;
    int yOffset = getHeight() / 6 + 4;
    this.selectedCape = -1;
    for (int y = 0; y < 1; y++)
    {
      for (int x = 0; x < 4; x++)
      {
        GLUtil.color(0.7F, 0.7F, 0.7F, 1.0F);
        if ((mouseX > xOffset) && (mouseX < xOffset + capeWidth) && (mouseY > yOffset) && (mouseY < yOffset + capeHeight))
        {
          this.selectedCape = (x + y * 4);
          GLUtil.enableBlend();
          GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
          Gui.drawModalRectWithCustomSizedTexture(xOffset, yOffset, capeWidth * x, capeHeight * y, capeWidth, capeHeight, capeWidth * 4, capeHeight * 4);
          GLUtil.disableBlend();
        }
        else
        {
          Gui.drawModalRectWithCustomSizedTexture(xOffset, yOffset, capeWidth * x, capeHeight * y, capeWidth, capeHeight, capeWidth * 4, capeHeight * 4);
        }
        xOffset += capeWidth + 8;
      }
      xOffset = getWidth() / 2 - capeWidth * 2 - 4;
      yOffset += capeHeight + 10;
    }
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
  }
  
  public String getTitleKey()
  {
    return "cape.upload.default.title";
  }
}
