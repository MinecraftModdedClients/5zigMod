package org.h2.mvstore;

import java.util.Iterator;

public class Cursor<K, V>
  implements Iterator<K>
{
  private final MVMap<K, ?> map;
  private final K from;
  private CursorPos pos;
  private K current;
  private K last;
  private V currentValue;
  private V lastValue;
  private Page lastPage;
  private final Page root;
  private boolean initialized;
  
  Cursor(MVMap<K, ?> paramMVMap, Page paramPage, K paramK)
  {
    this.map = paramMVMap;
    this.root = paramPage;
    this.from = paramK;
  }
  
  public boolean hasNext()
  {
    if (!this.initialized)
    {
      min(this.root, this.from);
      this.initialized = true;
      fetchNext();
    }
    return this.current != null;
  }
  
  public K next()
  {
    hasNext();
    Object localObject = this.current;
    this.last = this.current;
    this.lastValue = this.currentValue;
    this.lastPage = (this.pos == null ? null : this.pos.page);
    fetchNext();
    return (K)localObject;
  }
  
  public K getKey()
  {
    return (K)this.last;
  }
  
  public V getValue()
  {
    return (V)this.lastValue;
  }
  
  Page getPage()
  {
    return this.lastPage;
  }
  
  public void skip(long paramLong)
  {
    if (!hasNext()) {
      return;
    }
    if (paramLong < 10L)
    {
      while (paramLong-- > 0L) {
        fetchNext();
      }
      return;
    }
    long l = this.map.getKeyIndex(this.current);
    Object localObject = this.map.getKey(l + paramLong);
    this.pos = null;
    min(this.root, localObject);
    fetchNext();
  }
  
  public void remove()
  {
    throw DataUtils.newUnsupportedOperationException("Removing is not supported");
  }
  
  private void min(Page paramPage, K paramK)
  {
    for (;;)
    {
      if (paramPage.isLeaf())
      {
        i = paramK == null ? 0 : paramPage.binarySearch(paramK);
        if (i < 0) {
          i = -i - 1;
        }
        this.pos = new CursorPos(paramPage, i, this.pos);
        break;
      }
      int i = paramK == null ? -1 : paramPage.binarySearch(paramK);
      if (i < 0) {
        i = -i - 1;
      } else {
        i++;
      }
      this.pos = new CursorPos(paramPage, i + 1, this.pos);
      paramPage = paramPage.getChildPage(i);
    }
  }
  
  private void fetchNext()
  {
    while (this.pos != null)
    {
      if (this.pos.index < this.pos.page.getKeyCount())
      {
        int i = this.pos.index++;
        this.current = this.pos.page.getKey(i);
        this.currentValue = this.pos.page.getValue(i);
        return;
      }
      this.pos = this.pos.parent;
      if (this.pos == null) {
        break;
      }
      if (this.pos.index < this.map.getChildPageCount(this.pos.page)) {
        min(this.pos.page.getChildPage(this.pos.index++), null);
      }
    }
    this.current = null;
  }
}
