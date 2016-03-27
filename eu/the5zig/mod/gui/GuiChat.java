package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.SkinManager;
import eu.the5zig.mod.render.Base64Renderer;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import org.lwjgl.input.Keyboard;

public class GuiChat
  extends Gui
{
  private final Base64Renderer base64Renderer = new Base64Renderer();
  private static Tab currentTab;
  
  public GuiChat(Gui lastScreen)
  {
    super(lastScreen);
    if (currentTab == null) {
      currentTab = new TabConversations(this);
    }
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, 8, 6, 50, 20, I18n.translate("gui.back")));
    addButton(The5zigMod.getVars().createButton(30, 2, 32, 49, 20, I18n.translate("chat.chats")));
    addButton(The5zigMod.getVars().createButton(31, 51, 32, 50, 20, I18n.translate("chat.friends")));
    
    currentTab.setResolution(getWidth(), getHeight());
    Keyboard.enableRepeatEvents(true);
  }
  
  protected void mouseClicked(int x, int y, int button)
  {
    currentTab.mouseClicked0(x, y, button);
    if (this.hover)
    {
      The5zigMod.getVars().displayScreen(new GuiProfile(this));
      The5zigMod.getVars().playSound("ui.button.click", 1.0F);
    }
  }
  
  protected void mouseReleased(int x, int y, int state)
  {
    currentTab.mouseReleased0(x, y, state);
  }
  
  protected void guiClosed()
  {
    currentTab.guiClosed0();
    Keyboard.enableRepeatEvents(false);
  }
  
  public void handleMouseInput()
  {
    currentTab.handleMouseInput0();
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    currentTab.drawScreen0(mouseX, mouseY, partialTicks);
    
    drawOwnProfile(mouseX, mouseY);
  }
  
  private boolean hover = false;
  
  private void drawOwnProfile(int mouseX, int mouseY)
  {
    String base64EncodedSkin = The5zigMod.getSkinManager().getBase64EncodedSkin(The5zigMod.getDataManager().getUniqueId());
    if ((this.base64Renderer.getBase64String() != null) && (base64EncodedSkin == null)) {
      this.base64Renderer.reset();
    } else if ((base64EncodedSkin != null) && (!base64EncodedSkin.equals(this.base64Renderer.getBase64String()))) {
      this.base64Renderer.setBase64String(base64EncodedSkin, "player_skin/" + The5zigMod.getDataManager().getUniqueId());
    }
    int width = 20;int height = 20;
    String coloredName = The5zigMod.getDataManager().getColoredName();
    String profile = I18n.translate("friend.profile");
    int x1 = (int)(getWidth() - width - 14 - Math.max(Math.max(The5zigMod.getVars().getStringWidth(coloredName) * 0.7F, The5zigMod.getVars().getStringWidth(profile) * 0.7F), 60.0F));
    int x2 = x1 + width;
    int y1 = 6;
    int y2 = y1 + height;
    
    int boxX2 = getWidth() - 8;
    this.hover = ((mouseX >= x1) && (mouseX <= boxX2) && (mouseY >= y1) && (mouseY < y2));
    
    int c = this.hover ? -13421773 : -16777216;
    Gui.drawRect(x1 - 1, y1 - 1, boxX2 + 1, y2 + 1, -5592406);
    Gui.drawRect(x2, y1, boxX2, y2, c);
    if (this.hover) {
      this.base64Renderer.renderImage(x1, y1, width, height, 0.5F, 0.5F, 0.5F, 1.0F);
    } else {
      this.base64Renderer.renderImage(x1, y1, width, height);
    }
    GLUtil.pushMatrix();
    GLUtil.scale(0.7F, 0.7F, 0.7F);
    GLUtil.translate((x2 + 4) / 0.7F, (y1 + 4) / 0.7F, 0.0F);
    The5zigMod.getVars().drawString(coloredName, 0, 0);
    The5zigMod.getVars().drawString(profile, 0, 10);
    GLUtil.popMatrix();
  }
  
  protected void onKeyType(char character, int key)
  {
    currentTab.keyTyped0(character, key);
  }
  
  protected void tick()
  {
    currentTab.tick0();
    getButtonById(30).setEnabled(!(currentTab instanceof TabConversations));
    getButtonById(31).setEnabled(!(currentTab instanceof TabFriends));
  }
  
  protected void actionPerformed(IButton button)
  {
    if ((button.getId() == 30) || (button.getId() == 31)) {
      if ((currentTab instanceof TabConversations)) {
        setCurrentTab(new TabFriends(this));
      } else if ((currentTab instanceof TabFriends)) {
        setCurrentTab(new TabConversations(this));
      }
    }
  }
  
  public String getTitleName()
  {
    return String.format("The 5zig Mod - Chat | %s", new Object[] {
      ChatColor.RED + I18n.translate("connection.offline") });
  }
  
  public void setCurrentTab(Tab currentTab)
  {
    currentTab = currentTab;
    currentTab.setResolution(getWidth(), getHeight());
  }
  
  public Tab getCurrentTab()
  {
    return currentTab;
  }
}
