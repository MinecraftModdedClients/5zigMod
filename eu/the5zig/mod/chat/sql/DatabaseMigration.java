package eu.the5zig.mod.chat.sql;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.chat.entity.Conversation.Behaviour;
import eu.the5zig.util.db.Database;
import eu.the5zig.util.db.SQLQuery;
import eu.the5zig.util.db.SQLResult;
import org.apache.logging.log4j.Logger;

public class DatabaseMigration
{
  private final int CURRENT_VERSION = 3;
  private Database database;
  
  public DatabaseMigration(Database database)
  {
    this.database = database;
  }
  
  public void start()
  {
    init();
    
    int version = getVersion();
    if (version == 3) {
      return;
    }
    if (version > 3)
    {
      updateVersion();
      return;
    }
    The5zigMod.logger.info("Old Database Found! Migrating...");
    migrate(version);
  }
  
  private void init()
  {
    this.database.update("CREATE TABLE IF NOT EXISTS version (version INT)", new Object[0]);
  }
  
  private int getVersion()
  {
    VersionEntity versionEntity = (VersionEntity)this.database.get(VersionEntity.class).query("SELECT * FROM version", new Object[0]).unique();
    if (versionEntity == null)
    {
      this.database.update("INSERT INTO version (version) VALUES (?)", new Object[] { Integer.valueOf(3) });
      return 3;
    }
    return versionEntity.getVersion();
  }
  
  private void updateVersion()
  {
    this.database.update("UPDATE version SET version=?", new Object[] { Integer.valueOf(3) });
  }
  
  private void migrate(int dbVersion)
  {
    if (dbVersion == 3)
    {
      updateVersion();
      return;
    }
    if (dbVersion == 0)
    {
      this.database.update("ALTER TABLE conversations_chat ADD behaviour INT", new Object[0]);
      this.database.update("UPDATE conversations_chat SET behaviour=?", new Object[] { Integer.valueOf(Conversation.Behaviour.DEFAULT.ordinal()) });
      this.database.update("ALTER TABLE conversations_groupchat ADD behaviour INT", new Object[0]);
      this.database.update("UPDATE conversations_groupchat SET behaviour=?", new Object[] { Integer.valueOf(Conversation.Behaviour.DEFAULT.ordinal()) });
      this.database.update("ALTER TABLE announcements ADD behaviour INT", new Object[0]);
      this.database.update("UPDATE announcements SET behaviour=?", new Object[] { Integer.valueOf(Conversation.Behaviour.DEFAULT.ordinal()) });
    }
    else if (dbVersion == 1)
    {
      this.database.update("ALTER TABLE conversation_chat_messages MODIFY message VARCHAR(512)", new Object[0]);
      this.database.update("ALTER TABLE conversation_groupchat_messages MODIFY message VARCHAR(512)", new Object[0]);
      this.database.update("ALTER TABLE announcements_messages MODIFY message VARCHAR(512)", new Object[0]);
    }
    else if (dbVersion != 2) {}
    The5zigMod.logger.info("Migrating Database from version {} to version {}...", new Object[] { Integer.valueOf(dbVersion), Integer.valueOf(dbVersion + 1) });
    migrate(++dbVersion);
  }
}
