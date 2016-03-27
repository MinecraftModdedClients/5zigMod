package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Profile;
import eu.the5zig.mod.chat.entity.Rank;
import eu.the5zig.mod.gui.elements.BasicRow;
import eu.the5zig.mod.gui.elements.ButtonRow;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.manager.SkinManager;
import eu.the5zig.mod.render.Base64Renderer;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;
import org.apache.commons.lang3.text.WordUtils;

public class GuiProfile
  extends GuiOptions
  implements CenteredTextfieldCallback
{
  private final Base64Renderer base64Renderer = new Base64Renderer();
  private List<Row> rows = Lists.newArrayList();
  
  public GuiProfile(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    if (((this.base64Renderer.getBase64String() == null) || (!this.base64Renderer.getBase64String().equals(
      The5zigMod.getSkinManager().getBase64EncodedSkin(The5zigMod.getDataManager().getUniqueId())))) && (The5zigMod.getSkinManager().getBase64EncodedSkin(
      The5zigMod.getDataManager().getUniqueId()) != null)) {
      this.base64Renderer.setBase64String(The5zigMod.getSkinManager().getBase64EncodedSkin(The5zigMod.getDataManager().getUniqueId()), "player_skin/" + 
        The5zigMod.getDataManager().getUniqueId());
    }
    this.base64Renderer.renderImage();
    
    super.drawScreen(mouseX, mouseY, partialTicks);
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, 8, 6, 50, 20, I18n.translate("gui.back")));
    
    this.base64Renderer.setX(getWidth() / 2 - 155);
    this.base64Renderer.setY(40);
    this.base64Renderer.setWidthAndHeight(88);
    
    this.rows.clear();
    int maxWidth = getWidth() / 2 + 155 - (getWidth() / 2 - 155 + 16 + 88) - 10;
    this.rows.add(new BasicRow(ChatColor.UNDERLINE + I18n.translate("profile.title"), maxWidth)
    {
      public int getLineHeight()
      {
        return 14;
      }
    });
    this.rows.add(new BasicRow(String.format("%s%s: %s#%s", new Object[] { ChatColor.YELLOW, I18n.translate("profile.id"), ChatColor.RESET, Integer.valueOf(The5zigMod.getDataManager().getProfile().getId()) }), maxWidth));
    this.rows.add(new BasicRow(String.format("%s%s: %s", new Object[] { ChatColor.YELLOW, I18n.translate("profile.name"), The5zigMod.getDataManager().getColoredName() }), maxWidth));
    this.rows.add(new BasicRow(String.format("%s%s: %s", new Object[] { ChatColor.YELLOW, I18n.translate("profile.first_login_time"), ChatColor.RESET + Utils.convertToDate(
      The5zigMod.getDataManager().getProfile().getFirstTime()).replace("Today", I18n.translate("profile.today")).replace("Yesterday", I18n.translate("profile.yesterday")) }), maxWidth));
    
    this.rows.add(new BasicRow(String.format("%s%s: %s", new Object[] { ChatColor.YELLOW, I18n.translate("profile.cape"), ChatColor.RESET + 
      WordUtils.capitalize(The5zigMod.getDataManager().getProfile().getRank().toString().toLowerCase()) }), maxWidth));
    int x = getWidth() / 2 - 155 + 16 + 88 + The5zigMod.getVars().getStringWidth(ChatColor.YELLOW + I18n.translate("profile.message") + ":") + 10;
    this.rows.add(new ButtonRow(
      The5zigMod.getVars().createStringButton(9, x, 128, The5zigMod.getVars().getStringWidth(I18n.translate("profile.edit")) + 2, 9, I18n.translate("profile.edit")), null)
      {
        public void draw(int x, int y)
        {
          The5zigMod.getVars().drawString(ChatColor.YELLOW + I18n.translate("profile.message") + ":", x + 2, y + 2);
        }
        
        public int getLineHeight()
        {
          return 12;
        }
      });
    this.rows.add(new BasicRow(The5zigMod.getDataManager().getProfile().getProfileMessage(), maxWidth));
    
    IGuiList statusMessage = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 40, 128, getWidth() / 2 - 155 + 16 + 88, getWidth() / 2 + 155, this.rows);
    statusMessage.setBottomPadding(4);
    statusMessage.setRowWidth(400);
    statusMessage.setLeftbound(true);
    statusMessage.setDrawSelection(false);
    statusMessage.setScrollX(getWidth() / 2 + 155 - 5);
    addGuiList(statusMessage);
    
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 155, getHeight() / 6 + 120, 150, 20, I18n.translate("profile.settings.view")));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 + 5, getHeight() / 6 + 120, 150, 20, I18n.translate("chat.settings")));
    addButton(The5zigMod.getVars().createButton(3, getWidth() / 2 - 155, getHeight() / 6 + 144, 150, 20, I18n.translate("profile.blocked_contacts")));
    addButton(The5zigMod.getVars().createButton(4, getWidth() / 2 + 5, getHeight() / 6 + 144, 150, 20, I18n.translate("profile.show_statistics")));
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1) {
      The5zigMod.getVars().displayScreen(new GuiSettings(this, "profile_settings"));
    }
    if (button.getId() == 2) {
      The5zigMod.getVars().displayScreen(new GuiSettings(this, "chat_settings"));
    }
    if (button.getId() == 3) {
      The5zigMod.getVars().displayScreen(new GuiBlockedUsers(this));
    }
    if (button.getId() == 4) {
      The5zigMod.getVars().displayScreen(new GuiNetworkStatistics(this));
    }
    if (button.getId() == 9) {
      The5zigMod.getVars().displayScreen(new GuiCenteredTextfield(this, this, 255));
    }
  }
  
  public void onDone(String text)
  {
    The5zigMod.getDataManager().getProfile().setProfileMessage(text);
  }
  
  public String title()
  {
    return I18n.translate("profile.enter_new_profile_message");
  }
  
  public String getTitleKey()
  {
    return "profile.title";
  }
}
