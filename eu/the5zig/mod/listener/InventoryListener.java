package eu.the5zig.mod.listener;

import com.google.common.collect.Lists;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.server.GameMode;
import eu.the5zig.mod.server.Server;
import eu.the5zig.mod.server.timolia.ServerTimolia;
import eu.the5zig.mod.server.timolia.ServerTimolia.PvP;
import eu.the5zig.mod.server.timolia.ServerTimolia.PvPTournament;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ItemStack;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class InventoryListener
{
  public void handleInventorySetSlot(int slot, ItemStack itemStack)
  {
    if (itemStack == null) {
      return;
    }
    handleTimoliaTournament(slot, itemStack);
  }
  
  private void handleTimoliaTournament(int slot, ItemStack itemStack)
  {
    Server currentServer = The5zigMod.getDataManager().getServer();
    if ((currentServer == null) || (!(currentServer instanceof ServerTimolia))) {
      return;
    }
    ServerTimolia server = (ServerTimolia)currentServer;
    GameMode gameMode = server.getGameMode();
    if ((gameMode == null) || (!(gameMode instanceof ServerTimolia.PvP)) || (((ServerTimolia.PvP)gameMode).getTournament() == null)) {
      return;
    }
    ServerTimolia.PvPTournament tournament = ((ServerTimolia.PvP)gameMode).getTournament();
    if (!tournament.isInventoryRequested()) {
      return;
    }
    String title = The5zigMod.getVars().getOpenContainerTitle();
    if ((!"Turnier-Infos".equals(title)) || (slot != 1)) {
      return;
    }
    String displayName = ChatColor.stripColor(itemStack.getDisplayName());
    if (!"Informationen".equals(displayName)) {
      return;
    }
    List<String> formattedLore = itemStack.getLore();
    List<String> unformattedLore = Lists.newArrayList();
    for (String s : formattedLore) {
      unformattedLore.add(ChatColor.stripColor(s));
    }
    if (unformattedLore.size() != 11) {
      return;
    }
    String host = ((String)unformattedLore.get(1)).substring("Turnierleiter: ".length());
    String kit = ((String)unformattedLore.get(2)).substring("Kit: ".length());
    int players = Integer.parseInt(((String)unformattedLore.get(3)).substring("Spieleranzahl: ".length()));
    int qualificationRounds = Integer.parseInt(((String)unformattedLore.get(5)).split("Quali-Phase: | Runden")[1]);
    int bestOf = Integer.parseInt(((String)unformattedLore.get(6)).split("Best of ")[1]);
    String timeString = ((String)unformattedLore.get(7)).split(": ")[1];
    long time = Utils.parseTimeFormatToMillis(timeString);
    tournament.setHost(host);
    tournament.setKit(kit);
    tournament.setParticipants(players);
    tournament.setQualificationRounds(qualificationRounds);
    tournament.setCurrentRound(0);
    tournament.setBestOf(bestOf);
    tournament.setRoundTime(time);
    tournament.setCurrentRound(1);
    
    tournament.setInventoryRequested(false);
    The5zigMod.getVars().closeContainer();
    The5zigMod.logger.debug("Registered Tournament Information!");
  }
}
