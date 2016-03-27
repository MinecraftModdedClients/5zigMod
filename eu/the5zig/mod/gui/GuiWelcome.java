package eu.the5zig.mod.gui;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.TrayManager;
import eu.the5zig.util.minecraft.ChatColor;

public class GuiWelcome
  extends Gui
{
  private int index = 0;
  private int time = 0;
  private int underlined = 0;
  private String string = "Welcome to The 5zig Mod v3.5.3";
  private boolean underlinePlus = true;
  private int pulse = 0;
  
  public GuiWelcome()
  {
    The5zigMod.getTrayManager().displayMessage("Welcome to The 5zig Mod!", "This Tray Notification will be displayed every time you get a new message from a Friend while you haven't focused your game. You can disable this feature by simply right clicking this icon and clicking on \"Disable\"");
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 - 100, getHeight() / 6 + 168, "GO!"));
  }
  
  protected void actionPerformed(IButton button) {}
  
  protected void tick()
  {
    this.time += 1;
    if (this.time > 1)
    {
      this.time = 0;
      this.index += 1;
      if (this.index > this.string.length() + 10) {
        this.index = 0;
      }
    }
    if (this.underlinePlus) {
      this.underlined += 1;
    } else {
      this.underlined -= 1;
    }
    if (this.underlined > this.string.length() / 2 + 4) {
      this.underlinePlus = false;
    }
    if (this.underlined < 0) {
      this.underlinePlus = true;
    }
    this.pulse += 1;
    if (this.pulse > 40) {
      this.pulse = 0;
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    String result = "";
    for (int i = 0; i < this.string.length(); i++)
    {
      char c = this.string.charAt(i);
      int u = this.string.length() / 2 - this.underlined;
      int u2 = this.string.length() / 2 + this.underlined;
      if ((i >= u) && (i <= u2)) {
        result = result + (i == this.index ? ChatColor.YELLOW : ChatColor.GOLD) + ChatColor.UNDERLINE.toString() + ChatColor.BOLD.toString();
      } else {
        result = result + ChatColor.RESET.toString() + (i == this.index ? ChatColor.YELLOW : ChatColor.GOLD) + ChatColor.BOLD;
      }
      result = result + c;
    }
    drawCenteredString(result, getWidth() / 2, 20);
    drawCenteredString(ChatColor.ITALIC + "The new PvP Experience", getWidth() / 2, 30);
    
    int y = 48;
    The5zigMod.getVars().drawString("Get supported while PvPing with many useful stats.", getWidth() / 2 - 150, y);
    y += 16;The5zigMod.getVars().drawString("Go to \"Options\" -> \"The 5zig Mod...\" to customize the mod.", getWidth() / 2 - 150, y);
    y += 12;The5zigMod.getVars().drawString("Discover many different options.", getWidth() / 2 - 140, y);
    y += 12;The5zigMod.getVars().drawString("Enable, disable or scale the mod.", getWidth() / 2 - 140, y);
    y += 16;The5zigMod.getVars().drawString("Enjoy full support for many different servers", getWidth() / 2 - 150, y);
    y += 12;The5zigMod.getVars().drawString("Supports Servers like timolia.de, gommehd.net, mc.hypixel.net", getWidth() / 2 - 140, y);
    y += 12;The5zigMod.getVars().drawString("mcpvp.com, mc.playminity.com and many more and shows all kinds", getWidth() / 2 - 140, y);
    y += 12;The5zigMod.getVars().drawString("of different stats.", getWidth() / 2 - 140, y);
    y += 16;The5zigMod.getVars().drawString("Press F4 to open the Chat Gui.", getWidth() / 2 - 150, y);
    y += 12;The5zigMod.getVars().drawString("Send unlimited and ultra-fast messages to your friends.", getWidth() / 2 - 140, y);
    y += 12;The5zigMod.getVars().drawString("Add Friends and see what server they are currently playing on.", getWidth() / 2 - 140, y);
    y += 12;The5zigMod.getVars().drawString("Create Group Chats and Chat with multiple Friends at once.", getWidth() / 2 - 140, y);
    
    getButtonById(200).setLabel("What are you waiting for? Start " + (this.pulse > 20 ? ChatColor.GOLD : ChatColor.RESET) + "NOW" + ChatColor.RESET + "!");
  }
  
  public String getTitleName()
  {
    return "";
  }
}
