package org.h2.value;

import java.text.CollationKey;
import java.text.Collator;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.util.SmallLRUCache;

public class CompareModeDefault
  extends CompareMode
{
  private final Collator collator;
  private final SmallLRUCache<String, CollationKey> collationKeys;
  
  protected CompareModeDefault(String paramString, int paramInt, boolean paramBoolean)
  {
    super(paramString, paramInt, paramBoolean);
    this.collator = CompareMode.getCollator(paramString);
    if (this.collator == null) {
      throw DbException.throwInternalError(paramString);
    }
    this.collator.setStrength(paramInt);
    int i = SysProperties.COLLATOR_CACHE_SIZE;
    if (i != 0) {
      this.collationKeys = SmallLRUCache.newInstance(i);
    } else {
      this.collationKeys = null;
    }
  }
  
  public int compareString(String paramString1, String paramString2, boolean paramBoolean)
  {
    if (paramBoolean)
    {
      paramString1 = paramString1.toUpperCase();
      paramString2 = paramString2.toUpperCase();
    }
    int i;
    if (this.collationKeys != null)
    {
      CollationKey localCollationKey1 = getKey(paramString1);
      CollationKey localCollationKey2 = getKey(paramString2);
      i = localCollationKey1.compareTo(localCollationKey2);
    }
    else
    {
      i = this.collator.compare(paramString1, paramString2);
    }
    return i;
  }
  
  public boolean equalsChars(String paramString1, int paramInt1, String paramString2, int paramInt2, boolean paramBoolean)
  {
    return compareString(paramString1.substring(paramInt1, paramInt1 + 1), paramString2.substring(paramInt2, paramInt2 + 1), paramBoolean) == 0;
  }
  
  private CollationKey getKey(String paramString)
  {
    synchronized (this.collationKeys)
    {
      CollationKey localCollationKey = (CollationKey)this.collationKeys.get(paramString);
      if (localCollationKey == null)
      {
        localCollationKey = this.collator.getCollationKey(paramString);
        this.collationKeys.put(paramString, localCollationKey);
      }
      return localCollationKey;
    }
  }
}
