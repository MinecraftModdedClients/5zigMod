package org.h2.engine;

import java.sql.SQLException;
import org.h2.api.DatabaseEventListener;
import org.h2.command.Prepared;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.SearchRow;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueString;

public class MetaRecord
  implements Comparable<MetaRecord>
{
  private final int id;
  private final int objectType;
  private final String sql;
  
  public MetaRecord(SearchRow paramSearchRow)
  {
    this.id = paramSearchRow.getValue(0).getInt();
    this.objectType = paramSearchRow.getValue(2).getInt();
    this.sql = paramSearchRow.getValue(3).getString();
  }
  
  MetaRecord(DbObject paramDbObject)
  {
    this.id = paramDbObject.getId();
    this.objectType = paramDbObject.getType();
    this.sql = paramDbObject.getCreateSQL();
  }
  
  void setRecord(SearchRow paramSearchRow)
  {
    paramSearchRow.setValue(0, ValueInt.get(this.id));
    paramSearchRow.setValue(1, ValueInt.get(0));
    paramSearchRow.setValue(2, ValueInt.get(this.objectType));
    paramSearchRow.setValue(3, ValueString.get(this.sql));
  }
  
  void execute(Database paramDatabase, Session paramSession, DatabaseEventListener paramDatabaseEventListener)
  {
    try
    {
      Prepared localPrepared = paramSession.prepare(this.sql);
      localPrepared.setObjectId(this.id);
      localPrepared.update();
    }
    catch (DbException localDbException1)
    {
      DbException localDbException2 = localDbException1.addSQL(this.sql);
      SQLException localSQLException = localDbException2.getSQLException();
      paramDatabase.getTrace("database").error(localSQLException, this.sql);
      if (paramDatabaseEventListener != null) {
        paramDatabaseEventListener.exceptionThrown(localSQLException, this.sql);
      } else {
        throw localDbException2;
      }
    }
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public int getObjectType()
  {
    return this.objectType;
  }
  
  public String getSQL()
  {
    return this.sql;
  }
  
  public int compareTo(MetaRecord paramMetaRecord)
  {
    int i = getCreateOrder();
    int j = paramMetaRecord.getCreateOrder();
    if (i != j) {
      return i - j;
    }
    return getId() - paramMetaRecord.getId();
  }
  
  private int getCreateOrder()
  {
    switch (this.objectType)
    {
    case 6: 
      return 0;
    case 2: 
      return 1;
    case 10: 
      return 2;
    case 9: 
      return 3;
    case 12: 
      return 4;
    case 3: 
      return 5;
    case 11: 
      return 6;
    case 0: 
      return 7;
    case 1: 
      return 8;
    case 5: 
      return 9;
    case 4: 
      return 10;
    case 7: 
      return 11;
    case 8: 
      return 12;
    case 14: 
      return 13;
    case 13: 
      return 14;
    }
    throw DbException.throwInternalError("type=" + this.objectType);
  }
  
  public String toString()
  {
    return "MetaRecord [id=" + this.id + ", objectType=" + this.objectType + ", sql=" + this.sql + "]";
  }
}
