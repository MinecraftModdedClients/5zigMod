package org.h2.command.ddl;

import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.table.TableView;

public class AlterView
  extends DefineCommand
{
  private TableView view;
  
  public AlterView(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setView(TableView paramTableView)
  {
    this.view = paramTableView;
  }
  
  public int update()
  {
    this.session.commit(true);
    this.session.getUser().checkRight(this.view, 15);
    DbException localDbException = this.view.recompile(this.session, false);
    if (localDbException != null) {
      throw localDbException;
    }
    return 0;
  }
  
  public int getType()
  {
    return 20;
  }
}
