package eu.the5zig.mod.chat;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.network.packets.PacketBuffer;
import eu.the5zig.mod.chat.sql.NetworkStatsEntity;
import eu.the5zig.util.AsyncExecutor;
import eu.the5zig.util.Container;
import eu.the5zig.util.Utils;
import eu.the5zig.util.db.Database;
import eu.the5zig.util.db.SQLQuery;
import eu.the5zig.util.db.SQLResult;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class NetworkStats
{
  private Database sql;
  private long lastSaveTime;
  private int totalPacketsSent;
  private int currentPacketsSent;
  private int totalPacketsReceived;
  private int currentPacketsReceived;
  private long totalBytesSent;
  private long currentBytesSent;
  private long totalBytesReceived;
  private long currentBytesReceived;
  private long since;
  private Container<Integer> connectedClients = new Container(Integer.valueOf(0));
  private Container<Integer> maxClients = new Container(Integer.valueOf(0));
  private Container<Long> upTime = new Container(Long.valueOf(0L));
  private Container<Integer> ping = new Container(Integer.valueOf(0));
  private Container<Integer> lag = new Container(Integer.valueOf(0));
  private Container<String> serverUpTime = new Container()
  {
    public String getValue()
    {
      return Utils.convertToTimeWithDays(System.currentTimeMillis() - ((Long)NetworkStats.this.getStartTime().getValue()).longValue());
    }
  };
  
  public NetworkStats()
  {
    this.sql = The5zigMod.getConversationDatabase();
    if (this.sql == null) {
      return;
    }
    this.sql.update("CREATE TABLE IF NOT EXISTS network_stats (key VARCHAR(20), value VARCHAR(20))", new Object[0]);
    
    List<NetworkStatsEntity> entityBaseList = this.sql.get(NetworkStatsEntity.class).query("SELECT * FROM network_stats", new Object[0]).getAll();
    if (entityBaseList.isEmpty())
    {
      this.sql.update("INSERT INTO network_stats (key, value) VALUES ('packetsSent', '0'), ('packetsReceived', '0'), ('bytesSent', '0'), ('bytesReceived', '0'), ('since', ?)", new Object[] {
        Long.valueOf(System.currentTimeMillis()) });
      this.since = System.currentTimeMillis();
    }
    else
    {
      for (NetworkStatsEntity entity : entityBaseList)
      {
        String key = entity.getKey();
        String value = entity.getValue();
        if (key.equals("packetsSent")) {
          this.totalPacketsSent = Integer.parseInt(value);
        }
        if (key.equals("packetsReceived")) {
          this.totalPacketsReceived = Integer.parseInt(value);
        }
        if (key.equals("bytesSent")) {
          this.totalBytesSent = Long.parseLong(value);
        }
        if (key.equals("bytesReceived")) {
          this.totalBytesReceived = Long.parseLong(value);
        }
        if (key.equals("since")) {
          this.since = Long.parseLong(value);
        }
      }
    }
  }
  
  public void tick()
  {
    if (System.currentTimeMillis() - this.lastSaveTime > 60000L)
    {
      saveStats();
      this.lastSaveTime = System.currentTimeMillis();
    }
  }
  
  private void saveStats()
  {
    The5zigMod.getAsyncExecutor().execute(new Runnable()
    {
      public void run()
      {
        The5zigMod.logger.debug("Saving Network Stats...");
        NetworkStats.this.sql.update("UPDATE network_stats SET value=? WHERE key=?", new Object[] { String.valueOf(NetworkStats.this.totalPacketsSent), "packetsSent" });
        NetworkStats.this.sql.update("UPDATE network_stats SET value=? WHERE key=?", new Object[] { String.valueOf(NetworkStats.this.totalPacketsReceived), "packetsReceived" });
        NetworkStats.this.sql.update("UPDATE network_stats SET value=? WHERE key=?", new Object[] { String.valueOf(NetworkStats.this.totalBytesSent), "bytesSent" });
        NetworkStats.this.sql.update("UPDATE network_stats SET value=? WHERE key=?", new Object[] { String.valueOf(NetworkStats.this.totalBytesReceived), "bytesReceived" });
      }
    });
  }
  
  public void onPacketSend(PacketBuffer buf)
  {
    this.totalPacketsSent += 1;
    this.currentPacketsSent += 1;
    this.totalBytesSent += buf.readableBytes();
    this.currentBytesSent += buf.readableBytes();
  }
  
  public void onPacketReceive(PacketBuffer buf)
  {
    this.totalPacketsReceived += 1;
    this.currentPacketsReceived += 1;
    this.totalBytesReceived += buf.readableBytes();
    this.currentBytesReceived += buf.readableBytes();
  }
  
  public int getTotalPacketsSent()
  {
    return this.totalPacketsSent;
  }
  
  public int getTotalPacketsReceived()
  {
    return this.totalPacketsReceived;
  }
  
  public int getCurrentPacketsSent()
  {
    return this.currentPacketsSent;
  }
  
  public int getCurrentPacketsReceived()
  {
    return this.currentPacketsReceived;
  }
  
  public String getTotalBytesSent()
  {
    return Utils.bytesToReadable(this.totalBytesSent);
  }
  
  public String getTotalBytesReceived()
  {
    return Utils.bytesToReadable(this.totalBytesReceived);
  }
  
  public String getCurrentBytesSent()
  {
    return Utils.bytesToReadable(this.currentBytesSent);
  }
  
  public String getCurrentBytesReceived()
  {
    return Utils.bytesToReadable(this.currentBytesReceived);
  }
  
  public String getBytesTotal()
  {
    return Utils.bytesToReadable(this.totalBytesReceived + this.totalBytesSent);
  }
  
  public int getPacketsTotal()
  {
    return this.totalPacketsReceived + this.totalPacketsSent;
  }
  
  public String since()
  {
    return Utils.convertToDate(this.since).replace("Today", I18n.translate("profile.today").replace("Yesterday", I18n.translate("profile.yesterday")));
  }
  
  public Container<Integer> getConnectedClients()
  {
    return this.connectedClients;
  }
  
  public void setConnectedClients(int connectedClients)
  {
    this.connectedClients.setValue(Integer.valueOf(connectedClients));
  }
  
  public Container<Long> getStartTime()
  {
    return this.upTime;
  }
  
  public void setStartTime(long upTime)
  {
    this.upTime.setValue(Long.valueOf(upTime));
  }
  
  public Container<Integer> getPing()
  {
    return this.ping;
  }
  
  public void setPing(int ping)
  {
    this.ping.setValue(Integer.valueOf(ping));
  }
  
  public Container<Integer> getMaxClients()
  {
    return this.maxClients;
  }
  
  public void setMaxClients(int maxClients)
  {
    this.maxClients.setValue(Integer.valueOf(maxClients));
  }
  
  public Container<Integer> getLag()
  {
    return this.lag;
  }
  
  public void setLag(int lag)
  {
    this.lag.setValue(Integer.valueOf(lag));
  }
  
  public Container<String> getServerUpTime()
  {
    return this.serverUpTime;
  }
  
  public void resetCurrent()
  {
    this.currentPacketsSent = 0;
    this.currentPacketsReceived = 0;
    this.currentBytesSent = 0L;
    this.currentBytesReceived = 0L;
  }
}
