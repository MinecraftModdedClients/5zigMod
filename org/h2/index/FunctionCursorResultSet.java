package org.h2.index;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.value.DataType;
import org.h2.value.Value;

public class FunctionCursorResultSet
  implements Cursor
{
  private final Session session;
  private final ResultSet result;
  private final ResultSetMetaData meta;
  private Value[] values;
  private Row row;
  
  FunctionCursorResultSet(Session paramSession, ResultSet paramResultSet)
  {
    this.session = paramSession;
    this.result = paramResultSet;
    try
    {
      this.meta = paramResultSet.getMetaData();
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
  
  public Row get()
  {
    if (this.values == null) {
      return null;
    }
    if (this.row == null) {
      this.row = new Row(this.values, 1);
    }
    return this.row;
  }
  
  public SearchRow getSearchRow()
  {
    return get();
  }
  
  public boolean next()
  {
    this.row = null;
    try
    {
      if ((this.result != null) && (this.result.next()))
      {
        int i = this.meta.getColumnCount();
        this.values = new Value[i];
        for (int j = 0; j < i; j++)
        {
          int k = DataType.getValueTypeFromResultSet(this.meta, j + 1);
          this.values[j] = DataType.readValue(this.session, this.result, j + 1, k);
        }
      }
      else
      {
        this.values = null;
      }
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
    return this.values != null;
  }
  
  public boolean previous()
  {
    throw DbException.throwInternalError();
  }
}
