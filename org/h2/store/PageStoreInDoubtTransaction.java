package org.h2.store;

import org.h2.message.DbException;

public class PageStoreInDoubtTransaction
  implements InDoubtTransaction
{
  private final PageStore store;
  private final int sessionId;
  private final int pos;
  private final String transactionName;
  private int state;
  
  public PageStoreInDoubtTransaction(PageStore paramPageStore, int paramInt1, int paramInt2, String paramString)
  {
    this.store = paramPageStore;
    this.sessionId = paramInt1;
    this.pos = paramInt2;
    this.transactionName = paramString;
    this.state = 0;
  }
  
  public void setState(int paramInt)
  {
    switch (paramInt)
    {
    case 1: 
      this.store.setInDoubtTransactionState(this.sessionId, this.pos, true);
      break;
    case 2: 
      this.store.setInDoubtTransactionState(this.sessionId, this.pos, false);
      break;
    default: 
      DbException.throwInternalError("state=" + paramInt);
    }
    this.state = paramInt;
  }
  
  public String getState()
  {
    switch (this.state)
    {
    case 0: 
      return "IN_DOUBT";
    case 1: 
      return "COMMIT";
    case 2: 
      return "ROLLBACK";
    }
    throw DbException.throwInternalError("state=" + this.state);
  }
  
  public String getTransactionName()
  {
    return this.transactionName;
  }
}
