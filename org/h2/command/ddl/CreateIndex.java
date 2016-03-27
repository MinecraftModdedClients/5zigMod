package org.h2.command.ddl;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.IndexColumn;
import org.h2.table.Table;

public class CreateIndex
  extends SchemaCommand
{
  private String tableName;
  private String indexName;
  private IndexColumn[] indexColumns;
  private boolean primaryKey;
  private boolean unique;
  private boolean hash;
  private boolean spatial;
  private boolean ifNotExists;
  private String comment;
  
  public CreateIndex(Session paramSession, Schema paramSchema)
  {
    super(paramSession, paramSchema);
  }
  
  public void setIfNotExists(boolean paramBoolean)
  {
    this.ifNotExists = paramBoolean;
  }
  
  public void setTableName(String paramString)
  {
    this.tableName = paramString;
  }
  
  public void setIndexName(String paramString)
  {
    this.indexName = paramString;
  }
  
  public void setIndexColumns(IndexColumn[] paramArrayOfIndexColumn)
  {
    this.indexColumns = paramArrayOfIndexColumn;
  }
  
  public int update()
  {
    if (!this.transactional) {
      this.session.commit(true);
    }
    Database localDatabase = this.session.getDatabase();
    boolean bool = localDatabase.isPersistent();
    Table localTable = getSchema().getTableOrView(this.session, this.tableName);
    if (getSchema().findIndex(this.session, this.indexName) != null)
    {
      if (this.ifNotExists) {
        return 0;
      }
      throw DbException.get(42111, this.indexName);
    }
    this.session.getUser().checkRight(localTable, 15);
    localTable.lock(this.session, true, true);
    if (!localTable.isPersistIndexes()) {
      bool = false;
    }
    int i = getObjectId();
    if (this.indexName == null) {
      if (this.primaryKey) {
        this.indexName = localTable.getSchema().getUniqueIndexName(this.session, localTable, "PRIMARY_KEY_");
      } else {
        this.indexName = localTable.getSchema().getUniqueIndexName(this.session, localTable, "INDEX_");
      }
    }
    IndexType localIndexType;
    if (this.primaryKey)
    {
      if (localTable.findPrimaryKey() != null) {
        throw DbException.get(90017);
      }
      localIndexType = IndexType.createPrimaryKey(bool, this.hash);
    }
    else if (this.unique)
    {
      localIndexType = IndexType.createUnique(bool, this.hash);
    }
    else
    {
      localIndexType = IndexType.createNonUnique(bool, this.hash, this.spatial);
    }
    IndexColumn.mapColumns(this.indexColumns, localTable);
    localTable.addIndex(this.session, this.indexName, i, this.indexColumns, localIndexType, this.create, this.comment);
    
    return 0;
  }
  
  public void setPrimaryKey(boolean paramBoolean)
  {
    this.primaryKey = paramBoolean;
  }
  
  public void setUnique(boolean paramBoolean)
  {
    this.unique = paramBoolean;
  }
  
  public void setHash(boolean paramBoolean)
  {
    this.hash = paramBoolean;
  }
  
  public void setSpatial(boolean paramBoolean)
  {
    this.spatial = paramBoolean;
  }
  
  public void setComment(String paramString)
  {
    this.comment = paramString;
  }
  
  public int getType()
  {
    return 25;
  }
}
