package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.BasicRow;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.hypixel.HypixelGameType;
import eu.the5zig.mod.server.hypixel.api.HypixelAPICallback;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIManager;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIMissingKeyException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIResponse;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIResponseException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPITooManyRequestsException;
import eu.the5zig.mod.server.hypixel.api.HypixelStatArcade;
import eu.the5zig.mod.server.hypixel.api.HypixelStatArena;
import eu.the5zig.mod.server.hypixel.api.HypixelStatBattleground;
import eu.the5zig.mod.server.hypixel.api.HypixelStatCategory;
import eu.the5zig.mod.server.hypixel.api.HypixelStatGeneral;
import eu.the5zig.mod.server.hypixel.api.HypixelStatMCGO;
import eu.the5zig.mod.server.hypixel.api.HypixelStatPaintball;
import eu.the5zig.mod.server.hypixel.api.HypixelStatQuake;
import eu.the5zig.mod.server.hypixel.api.HypixelStatSkyWars;
import eu.the5zig.mod.server.hypixel.api.HypixelStatSurvivalGames;
import eu.the5zig.mod.server.hypixel.api.HypixelStatTNTGames;
import eu.the5zig.mod.server.hypixel.api.HypixelStatTurboKartRacers;
import eu.the5zig.mod.server.hypixel.api.HypixelStatUHC;
import eu.the5zig.mod.server.hypixel.api.HypixelStatVampireZ;
import eu.the5zig.mod.server.hypixel.api.HypixelStatWalls;
import eu.the5zig.mod.server.hypixel.api.HypixelStatWalls3;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.HashMap;
import java.util.List;

public class GuiHypixelStats
  extends Gui
{
  private static HashMap<HypixelGameType, HypixelStatCategory> categories = ;
  private IGuiList<BasicRow> guiListGameTypes;
  private IGuiList guiListStat;
  private List<BasicRow> stats = Lists.newArrayList();
  private int selected;
  private HypixelAPIResponse hypixelStats;
  private String status;
  private List<String> statusSplit;
  private String player;
  
  public GuiHypixelStats(Gui lastScreen)
  {
    this(lastScreen, The5zigMod.getDataManager().getUsername());
  }
  
  public GuiHypixelStats(Gui lastScreen, String player)
  {
    super(lastScreen);
    
    this.player = player;
    
    categories.put(HypixelGameType.GENERAL, new HypixelStatGeneral());
    categories.put(HypixelGameType.QUAKECRAFT, new HypixelStatQuake());
    categories.put(HypixelGameType.WALLS, new HypixelStatWalls());
    categories.put(HypixelGameType.PAINTBALL, new HypixelStatPaintball());
    categories.put(HypixelGameType.SURVIVAL_GAMES, new HypixelStatSurvivalGames());
    categories.put(HypixelGameType.TNTGAMES, new HypixelStatTNTGames());
    categories.put(HypixelGameType.VAMPIREZ, new HypixelStatVampireZ());
    categories.put(HypixelGameType.WALLS3, new HypixelStatWalls3());
    categories.put(HypixelGameType.ARCADE, new HypixelStatArcade());
    categories.put(HypixelGameType.ARENA, new HypixelStatArena());
    categories.put(HypixelGameType.MCGO, new HypixelStatMCGO());
    categories.put(HypixelGameType.UHC, new HypixelStatUHC());
    categories.put(HypixelGameType.BATTLEGROUND, new HypixelStatBattleground());
    categories.put(HypixelGameType.TURBO_KART_RACERS, new HypixelStatTurboKartRacers());
    categories.put(HypixelGameType.SKYWARS, new HypixelStatSkyWars());
  }
  
  public void initGui()
  {
    addTextField(The5zigMod.getVars().createTextfield(I18n.translate("server.hypixel.stats.player"), 1, getWidth() / 2 - 30, getHeight() / 6 - 4, 105, 16, 16));
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 + 80, getHeight() / 6 - 6, 70, 20, I18n.translate("server.hypixel.stats.search")));
    getTextfieldById(1).setText(this.player);
    
    List<BasicRow> rows = Lists.newArrayList();
    for (HypixelGameType gameType : HypixelGameType.values()) {
      rows.add(new BasicRow(gameType.getName(), 100)
      {
        public int getLineHeight()
        {
          return 18;
        }
      });
    }
    this.guiListGameTypes = The5zigMod.getVars().createGuiList(new Clickable()
    {
      public void onSelect(int id, BasicRow row, boolean doubleClick)
      {
        GuiHypixelStats.this.selected = id;
        GuiHypixelStats.this.guiListStat.scrollTo(0.0F);
        GuiHypixelStats.this.updateSelected(GuiHypixelStats.this.hypixelStats == null ? null : GuiHypixelStats.this.hypixelStats);
      }
    }, getWidth(), getHeight(), getHeight() / 6 + 18, getHeight() - 48, getWidth() / 2 - 155, getWidth() / 2 - 45, rows);
    this.guiListGameTypes.setLeftbound(true);
    this.guiListGameTypes.setScrollX(getWidth() / 2 - 50);
    this.guiListGameTypes.setRowWidth(105);
    
    this.guiListStat = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), getHeight() / 6 + 18, getHeight() - 48, getWidth() / 2 - 30, getWidth() / 2 + 155, this.stats);
    this.guiListStat.setLeftbound(true);
    this.guiListStat.setDrawSelection(false);
    this.guiListStat.setScrollX(getWidth() / 2 + 150);
    
    this.guiListGameTypes.setSelectedId(this.selected);
    if ((this.status == null) && (this.stats.isEmpty())) {
      refresh(this.player);
    } else if (this.status != null) {
      updateStatus(this.status);
    }
    addBottomDoneButton();
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 100)
    {
      if (getTextfieldById(1).getText().isEmpty()) {
        return;
      }
      refresh(this.player = getTextfieldById(1).getText());
    }
  }
  
  protected void tick()
  {
    ITextfield textfield = getTextfieldById(1);
    if (textfield.getText().contains(" ")) {
      textfield.setText(textfield.getText().replace(" ", ""));
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawMenuBackground();
    int y;
    if (this.status == null)
    {
      this.guiListGameTypes.drawScreen(mouseX, mouseY, partialTicks);
      this.guiListStat.drawScreen(mouseX, mouseY, partialTicks);
      The5zigMod.getVars().drawString(ChatColor.UNDERLINE + I18n.translate("server.hypixel.stats.categories"), getWidth() / 2 - 155, getHeight() / 6 + 6);
    }
    else
    {
      y = getHeight() / 6 + 30;
      for (String s : this.statusSplit)
      {
        drawCenteredString(s, getWidth() / 2, y);
        y += 12;
      }
    }
  }
  
  protected void handleMouseInput()
  {
    if (this.status == null)
    {
      this.guiListGameTypes.handleMouseInput();
      this.guiListStat.handleMouseInput();
    }
  }
  
  public String getTitleKey()
  {
    return "server.hypixel.stats.title";
  }
  
  private void refresh(String player)
  {
    this.stats.clear();
    if (this.status != null) {
      updateStatus(I18n.translate("server.hypixel.loading"));
    }
    this.player = player;
    try
    {
      The5zigMod.getHypixelAPIManager().get("player?name=" + player, new HypixelAPICallback()
      {
        public void call(HypixelAPIResponse response)
        {
          GuiHypixelStats.this.hypixelStats = response;
          GuiHypixelStats.this.updateStatus(null);
          GuiHypixelStats.this.updateSelected(response);
        }
        
        public void call(HypixelAPIResponseException e)
        {
          GuiHypixelStats.this.updateStatus(e.getErrorMessage());
        }
      });
    }
    catch (HypixelAPITooManyRequestsException e)
    {
      updateStatus(I18n.translate("server.hypixel.too_many_requests"));
    }
    catch (HypixelAPIMissingKeyException e)
    {
      updateStatus(I18n.translate("server.hypixel.no_key"));
    }
    catch (HypixelAPIException e)
    {
      updateStatus(e.getMessage());
    }
  }
  
  private void updateStatus(String status)
  {
    this.status = status;
    if (status == null) {
      this.statusSplit = null;
    } else {
      this.statusSplit = The5zigMod.getVars().splitStringToWidth(status, getWidth() - 50);
    }
  }
  
  private void updateSelected(HypixelAPIResponse response)
  {
    if (response == null) {
      return;
    }
    this.stats.clear();
    if (response.data() == null)
    {
      updateStatus(I18n.translate("server.hypixel.stats.player_not_found"));
      return;
    }
    BasicRow selectedRow = (BasicRow)this.guiListGameTypes.getSelectedRow();
    HypixelGameType gameType = HypixelGameType.fromName(selectedRow.getString());
    if ((gameType == null) || (!categories.containsKey(gameType))) {
      return;
    }
    if ((response.data().get("player") == null) || (response.data().get("player").isJsonNull()))
    {
      updateStatus(I18n.translate("server.hypixel.stats.player_not_found"));
      return;
    }
    JsonObject root = response.data().get("player").getAsJsonObject();
    if ((root.get("stats") == null) || (root.get("stats").isJsonNull()))
    {
      updateStatus(I18n.translate("server.hypixel.stats.stats_not_found"));
      return;
    }
    JsonElement element = gameType == HypixelGameType.GENERAL ? root : root.get("stats").getAsJsonObject().get(gameType.getDatabaseName());
    if (element == null) {
      element = new JsonObject();
    }
    this.stats.add(new BasicRow(ChatColor.UNDERLINE + I18n.translate("server.hypixel.stats.info", new Object[] { gameType.getName() }), 175));
    for (String s : ((HypixelStatCategory)categories.get(gameType)).getStats(element.getAsJsonObject())) {
      this.stats.add(new BasicRow(s, 175));
    }
  }
}
