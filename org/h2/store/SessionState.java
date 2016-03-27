package org.h2.store;

class SessionState
{
  public int sessionId;
  public int lastCommitLog;
  public int lastCommitPos;
  public PageStoreInDoubtTransaction inDoubtTransaction;
  
  public boolean isCommitted(int paramInt1, int paramInt2)
  {
    if (paramInt1 != this.lastCommitLog) {
      return this.lastCommitLog > paramInt1;
    }
    return this.lastCommitPos >= paramInt2;
  }
  
  public String toString()
  {
    return "sessionId:" + this.sessionId + " log:" + this.lastCommitLog + " pos:" + this.lastCommitPos + " inDoubt:" + this.inDoubtTransaction;
  }
}
