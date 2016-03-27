package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.BasicRow;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.server.hypixel.api.HypixelAPICallback;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIManager;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIMissingKeyException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIResponse;
import eu.the5zig.mod.server.hypixel.api.HypixelAPIResponseException;
import eu.the5zig.mod.server.hypixel.api.HypixelAPITooManyRequestsException;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.JsonUtil;
import eu.the5zig.mod.util.MojangAPIManager;
import eu.the5zig.util.ExceptionCallback;
import eu.the5zig.util.Utils;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.Logger;

public class GuiHypixelGuild
  extends Gui
{
  private IGuiList guiList;
  private List<BasicRow> stats = Lists.newArrayList();
  private String status;
  private List<String> statusSplit;
  
  public GuiHypixelGuild(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    addTextField(The5zigMod.getVars().createTextfield(I18n.translate("server.hypixel.guild.guild"), 1, getWidth() / 2 - 100, getHeight() / 6 - 6, 80, 20));
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 - 15, getHeight() / 6 - 6, 55, 20, I18n.translate("server.hypixel.guild.search.by_name")));
    addButton(The5zigMod.getVars().createButton(101, getWidth() / 2 + 45, getHeight() / 6 - 6, 55, 20, I18n.translate("server.hypixel.guild.search.by_player")));
    if ((this.status == null) && (this.stats.isEmpty())) {
      updateStatus(I18n.translate("server.hypixel.guild.help"));
    } else if (this.stats.isEmpty()) {
      updateStatus(this.status);
    }
    this.guiList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), getHeight() / 6 + 18, getHeight() - 64, getWidth() / 2 - 100, getWidth() / 2 + 100, this.stats);
    this.guiList.setScrollX(getWidth() / 2 + 95);
    
    addBottomDoneButton();
  }
  
  protected void actionPerformed(IButton button)
  {
    if (((button.getId() == 100) || (button.getId() == 101)) && (!getTextfieldById(1).getText().isEmpty())) {
      findGuild(getTextfieldById(1).getText(), button.getId() == 101);
    }
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    int y;
    if (this.status == null)
    {
      this.guiList.drawScreen(mouseX, mouseY, partialTicks);
    }
    else
    {
      y = getHeight() / 6 + 50;
      for (String s : this.statusSplit)
      {
        drawCenteredString(s, getWidth() / 2, y);
        y += 12;
      }
    }
  }
  
  protected void handleMouseInput()
  {
    if (this.status == null) {
      this.guiList.handleMouseInput();
    }
  }
  
  private void findGuild(String name, boolean byPlayerName)
  {
    if (this.status != null) {
      updateStatus(I18n.translate("server.hypixel.loading"));
    }
    makeRequest("findGuild?" + (byPlayerName ? "byPlayer" : "byName") + "=" + StringEscapeUtils.escapeHtml4(name), new HypixelAPICallback()
    {
      public void call(HypixelAPIResponse findGuildResponse)
      {
        if ((findGuildResponse.data().get("guild") == null) || (findGuildResponse.data().get("guild").isJsonNull()))
        {
          GuiHypixelGuild.this.updateStatus(I18n.translate("server.hypixel.guild.not_found"));
          return;
        }
        GuiHypixelGuild.this.makeRequest("guild?id=" + findGuildResponse.data().get("guild").getAsString(), new HypixelAPICallback()
        {
          public void call(HypixelAPIResponse response)
          {
            GuiHypixelGuild.this.updateStatus(null);
            GuiHypixelGuild.this.stats.clear();
            
            JsonObject guild = response.data().get("guild").getAsJsonObject();
            LinkedHashMap<String, Object> l = Maps.newLinkedHashMap();
            l.put("name", JsonUtil.getString(guild, "name"));
            l.put("tag", JsonUtil.getString(guild, "tag"));
            l.put("created", Utils.convertToDate(JsonUtil.getLong(guild, "created"))
              .replace("Today", I18n.translate("profile.today").replace("Yesterday", I18n.translate("profile.yesterday"))));
            l.put("coins", Integer.valueOf(JsonUtil.getInt(guild, "coins")));
            l.put("coins_ever", Integer.valueOf(JsonUtil.getInt(guild, "coinsEver")));
            l.put("members", "(" + guild.get("members").getAsJsonArray().size() + "/75)");
            for (Map.Entry<String, Object> entry : l.entrySet()) {
              GuiHypixelGuild.this.stats.add(new BasicRow(I18n.translate(new StringBuilder().append("server.hypixel.guild.info.").append((String)entry.getKey()).toString()) + ": " + entry.getValue(), 190));
            }
            Object namesToResolve = Lists.newArrayList();
            final HashMap<String, GuiHypixelGuild.MemberRow> membersToResolve = Maps.newHashMap();
            List<GuiHypixelGuild.MemberRow> members = Lists.newArrayList();
            for (JsonElement element : guild.get("members").getAsJsonArray())
            {
              JsonObject member = element.getAsJsonObject();
              if ((!member.isJsonNull()) && (member.get("uuid") != null) && (!member.get("uuid").isJsonNull()))
              {
                String rank = "NONE";
                if ((member.get("rank") != null) && (!member.get("rank").isJsonNull())) {
                  rank = member.get("rank").getAsString();
                }
                if ((member.get("name") != null) && (!member.get("name").isJsonNull()))
                {
                  members.add(new GuiHypixelGuild.MemberRow(GuiHypixelGuild.this, member.get("name").getAsString(), rank, 190));
                }
                else
                {
                  String uuid = member.get("uuid").getAsString();
                  GuiHypixelGuild.MemberRow row = new GuiHypixelGuild.MemberRow(GuiHypixelGuild.this, uuid, rank, 190);
                  members.add(row);
                  membersToResolve.put(uuid, row);
                  ((List)namesToResolve).add(uuid);
                }
              }
            }
            Collections.sort(members, new Comparator()
            {
              public int compare(GuiHypixelGuild.MemberRow o1, GuiHypixelGuild.MemberRow o2)
              {
                if ((o1.rank.equals("GUILDMASTER")) && (!o2.rank.equals("GUILDMASTER"))) {
                  return -1;
                }
                if ((!o1.rank.equals("GUILDMASTER")) && (o2.rank.equals("GUILDMASTER"))) {
                  return 1;
                }
                if ((o1.rank.equals("GUILDMASTER")) && (o2.rank.equals("GUILDMASTER"))) {
                  return 0;
                }
                if ((o1.rank.equals("OFFICER")) && (!o2.rank.equals("OFFICER"))) {
                  return -1;
                }
                if ((!o1.rank.equals("OFFICER")) && (o2.rank.equals("OFFICER"))) {
                  return 1;
                }
                if ((o1.rank.equals("OFFICER")) && (o2.rank.equals("OFFICER"))) {
                  return 0;
                }
                if ((o1.rank.equals("MEMBER")) && (!o2.rank.equals("MEMBER"))) {
                  return -1;
                }
                if ((!o1.rank.equals("MEMBER")) && (o2.rank.equals("MEMBER"))) {
                  return 1;
                }
                if ((o1.rank.equals("MEMBER")) && (o2.rank.equals("MEMBER"))) {
                  return 0;
                }
                return 0;
              }
            });
            GuiHypixelGuild.this.stats.addAll(members);
            for (final String uuid : (List)namesToResolve) {
              try
              {
                The5zigMod.getMojangAPIManager().resolveUUID(uuid, new ExceptionCallback()
                {
                  public void call(String callback, Throwable throwable)
                  {
                    if (throwable != null)
                    {
                      GuiHypixelGuild.this.updateStatus(throwable.getMessage());
                      return;
                    }
                    ((GuiHypixelGuild.MemberRow)membersToResolve.get(uuid)).setName(callback);
                  }
                });
              }
              catch (Exception e)
              {
                The5zigMod.logger.warn("Could not resolve UUID " + uuid + "!", e);
              }
            }
          }
        });
      }
      
      public void call(HypixelAPIResponseException e)
      {
        GuiHypixelGuild.this.updateStatus(e.getErrorMessage());
      }
    });
  }
  
  private void makeRequest(String endpoint, HypixelAPICallback callback)
  {
    try
    {
      The5zigMod.getHypixelAPIManager().get(endpoint, callback);
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
  
  public String getTitleKey()
  {
    return "server.hypixel.guild.title";
  }
  
  public class MemberRow
    extends BasicRow
  {
    private String rank;
    
    public MemberRow(String string, String rank, int maxWidth)
    {
      super(maxWidth);
      this.rank = rank;
      setName(getName());
    }
    
    public String getName()
    {
      return this.string;
    }
    
    public void setName(String name)
    {
      this.string = ("  - " + name + " (" + this.rank + ")");
    }
  }
}
