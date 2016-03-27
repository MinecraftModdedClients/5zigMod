package org.h2.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.table.Column;
import org.h2.table.TableLink;
import org.h2.value.DataType;
import org.h2.value.Value;

public class LinkedCursor
  implements Cursor
{
  private final TableLink tableLink;
  private final PreparedStatement prep;
  private final String sql;
  private final Session session;
  private final ResultSet rs;
  private Row current;
  
  LinkedCursor(TableLink paramTableLink, ResultSet paramResultSet, Session paramSession, String paramString, PreparedStatement paramPreparedStatement)
  {
    this.session = paramSession;
    this.tableLink = paramTableLink;
    this.rs = paramResultSet;
    this.sql = paramString;
    this.prep = paramPreparedStatement;
  }
  
  public Row get()
  {
    return this.current;
  }
  
  public SearchRow getSearchRow()
  {
    return this.current;
  }
  
  public boolean next()
  {
    try
    {
      boolean bool = this.rs.next();
      if (!bool)
      {
        this.rs.close();
        this.tableLink.reusePreparedStatement(this.prep, this.sql);
        this.current = null;
        return false;
      }
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
    this.current = this.tableLink.getTemplateRow();
    for (int i = 0; i < this.current.getColumnCount(); i++)
    {
      Column localColumn = this.tableLink.getColumn(i);
      Value localValue = DataType.readValue(this.session, this.rs, i + 1, localColumn.getType());
      this.current.setValue(i, localValue);
    }
    return true;
  }
  
  public boolean previous()
  {
    throw DbException.throwInternalError();
  }
}
