package org.h2.mvstore.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.h2.mvstore.DataUtils;

public class CacheLongKeyLIRS<V>
{
  private long maxMemory;
  private int averageMemory;
  private final Segment<V>[] segments;
  private final int segmentCount;
  private final int segmentShift;
  private final int segmentMask;
  private final int stackMoveDistance;
  
  public CacheLongKeyLIRS(int paramInt)
  {
    this(paramInt, 1, 16, paramInt / 100);
  }
  
  public CacheLongKeyLIRS(long paramLong, int paramInt1, int paramInt2, int paramInt3)
  {
    setMaxMemory(paramLong);
    setAverageMemory(paramInt1);
    DataUtils.checkArgument(Integer.bitCount(paramInt2) == 1, "The segment count must be a power of 2, is {0}", new Object[] { Integer.valueOf(paramInt2) });
    
    this.segmentCount = paramInt2;
    this.segmentMask = (paramInt2 - 1);
    this.stackMoveDistance = paramInt3;
    this.segments = new Segment[paramInt2];
    clear();
    
    this.segmentShift = (32 - Integer.bitCount(this.segmentMask));
  }
  
  public void clear()
  {
    long l = Math.max(1L, this.maxMemory / this.segmentCount);
    int i = getSegmentLen(l);
    for (int j = 0; j < this.segmentCount; j++) {
      this.segments[j] = new Segment(l, i, this.stackMoveDistance);
    }
  }
  
  private int getSegmentLen(long paramLong)
  {
    long l1 = (paramLong / this.averageMemory / 0.75D);
    
    long l2 = 8L;
    while (l2 < l1) {
      l2 += l2;
    }
    return (int)Math.min(2147483648L, l2);
  }
  
  private Entry<V> find(long paramLong)
  {
    int i = getHash(paramLong);
    return getSegment(i).find(paramLong, i);
  }
  
  public boolean containsKey(long paramLong)
  {
    int i = getHash(paramLong);
    return getSegment(i).containsKey(paramLong, i);
  }
  
  public V peek(long paramLong)
  {
    Entry localEntry = find(paramLong);
    return localEntry == null ? null : localEntry.value;
  }
  
  public V put(long paramLong, V paramV)
  {
    return (V)put(paramLong, paramV, sizeOf(paramV));
  }
  
  public V put(long paramLong, V paramV, int paramInt)
  {
    int i = getHash(paramLong);
    int j = getSegmentIndex(i);
    Segment localSegment = this.segments[j];
    synchronized (localSegment)
    {
      if (localSegment.isFull())
      {
        localSegment = this.segments[j];
        if (localSegment.isFull())
        {
          localSegment = new Segment(localSegment, 2);
          this.segments[j] = localSegment;
        }
      }
      return (V)localSegment.put(paramLong, i, paramV, paramInt);
    }
  }
  
  protected int sizeOf(V paramV)
  {
    return this.averageMemory;
  }
  
  public V remove(long paramLong)
  {
    int i = getHash(paramLong);
    return (V)getSegment(i).remove(paramLong, i);
  }
  
  public int getMemory(long paramLong)
  {
    int i = getHash(paramLong);
    return getSegment(i).getMemory(paramLong, i);
  }
  
  public V get(long paramLong)
  {
    int i = getHash(paramLong);
    return (V)getSegment(i).get(paramLong, i);
  }
  
  private Segment<V> getSegment(int paramInt)
  {
    return this.segments[getSegmentIndex(paramInt)];
  }
  
  private int getSegmentIndex(int paramInt)
  {
    return paramInt >>> this.segmentShift & this.segmentMask;
  }
  
  static int getHash(long paramLong)
  {
    int i = (int)(paramLong >>> 32 ^ paramLong);
    
    i = (i >>> 16 ^ i) * 73244475;
    i = (i >>> 16 ^ i) * 73244475;
    i = i >>> 16 ^ i;
    return i;
  }
  
  public long getUsedMemory()
  {
    long l = 0L;
    for (Segment localSegment : this.segments) {
      l += localSegment.usedMemory;
    }
    return l;
  }
  
  public void setMaxMemory(long paramLong)
  {
    DataUtils.checkArgument(paramLong > 0L, "Max memory must be larger than 0, is {0}", new Object[] { Long.valueOf(paramLong) });
    
    this.maxMemory = paramLong;
    if (this.segments != null)
    {
      long l = 1L + paramLong / this.segments.length;
      for (Segment localSegment : this.segments) {
        localSegment.setMaxMemory(l);
      }
    }
  }
  
  public void setAverageMemory(int paramInt)
  {
    DataUtils.checkArgument(paramInt > 0, "Average memory must be larger than 0, is {0}", new Object[] { Integer.valueOf(paramInt) });
    
    this.averageMemory = paramInt;
  }
  
  public int getAverageMemory()
  {
    return this.averageMemory;
  }
  
  public long getMaxMemory()
  {
    return this.maxMemory;
  }
  
  public synchronized Set<Map.Entry<Long, V>> entrySet()
  {
    HashMap localHashMap = new HashMap();
    for (Iterator localIterator = keySet().iterator(); localIterator.hasNext();)
    {
      long l = ((Long)localIterator.next()).longValue();
      localHashMap.put(Long.valueOf(l), find(l).value);
    }
    return localHashMap.entrySet();
  }
  
  public synchronized Set<Long> keySet()
  {
    HashSet localHashSet = new HashSet();
    for (Segment localSegment : this.segments) {
      localHashSet.addAll(localSegment.keySet());
    }
    return localHashSet;
  }
  
  public int sizeNonResident()
  {
    int i = 0;
    for (Segment localSegment : this.segments) {
      i += localSegment.queue2Size;
    }
    return i;
  }
  
  public int sizeMapArray()
  {
    int i = 0;
    for (Segment localSegment : this.segments) {
      i += localSegment.entries.length;
    }
    return i;
  }
  
  public int sizeHot()
  {
    int i = 0;
    for (Segment localSegment : this.segments) {
      i += localSegment.mapSize - localSegment.queueSize - localSegment.queue2Size;
    }
    return i;
  }
  
  public int size()
  {
    int i = 0;
    for (Segment localSegment : this.segments) {
      i += localSegment.mapSize - localSegment.queue2Size;
    }
    return i;
  }
  
  public synchronized List<Long> keys(boolean paramBoolean1, boolean paramBoolean2)
  {
    ArrayList localArrayList = new ArrayList();
    for (Segment localSegment : this.segments) {
      localArrayList.addAll(localSegment.keys(paramBoolean1, paramBoolean2));
    }
    return localArrayList;
  }
  
  public List<V> values()
  {
    ArrayList localArrayList = new ArrayList();
    for (Iterator localIterator = keySet().iterator(); localIterator.hasNext();)
    {
      long l = ((Long)localIterator.next()).longValue();
      Object localObject = find(l).value;
      if (localObject != null) {
        localArrayList.add(localObject);
      }
    }
    return localArrayList;
  }
  
  public boolean isEmpty()
  {
    return size() == 0;
  }
  
  public boolean containsValue(Object paramObject)
  {
    return getMap().containsValue(paramObject);
  }
  
  public Map<Long, V> getMap()
  {
    HashMap localHashMap = new HashMap();
    for (Iterator localIterator = keySet().iterator(); localIterator.hasNext();)
    {
      long l = ((Long)localIterator.next()).longValue();
      Object localObject = find(l).value;
      if (localObject != null) {
        localHashMap.put(Long.valueOf(l), localObject);
      }
    }
    return localHashMap;
  }
  
  public void putAll(Map<Long, ? extends V> paramMap)
  {
    for (Map.Entry localEntry : paramMap.entrySet()) {
      put(((Long)localEntry.getKey()).longValue(), localEntry.getValue());
    }
  }
  
  private static class Segment<V>
  {
    int mapSize;
    int queueSize;
    int queue2Size;
    final CacheLongKeyLIRS.Entry<V>[] entries;
    long usedMemory;
    private final int stackMoveDistance;
    private long maxMemory;
    private int mask;
    private int stackSize;
    private CacheLongKeyLIRS.Entry<V> stack;
    private CacheLongKeyLIRS.Entry<V> queue;
    private CacheLongKeyLIRS.Entry<V> queue2;
    private int stackMoveCounter;
    
    Segment(long paramLong, int paramInt1, int paramInt2)
    {
      setMaxMemory(paramLong);
      this.stackMoveDistance = paramInt2;
      
      this.mask = (paramInt1 - 1);
      
      this.stack = new CacheLongKeyLIRS.Entry();
      this.stack.stackPrev = (this.stack.stackNext = this.stack);
      this.queue = new CacheLongKeyLIRS.Entry();
      this.queue.queuePrev = (this.queue.queueNext = this.queue);
      this.queue2 = new CacheLongKeyLIRS.Entry();
      this.queue2.queuePrev = (this.queue2.queueNext = this.queue2);
      
      CacheLongKeyLIRS.Entry[] arrayOfEntry = new CacheLongKeyLIRS.Entry[paramInt1];
      this.entries = arrayOfEntry;
      
      this.mapSize = 0;
      this.usedMemory = 0L;
      this.stackSize = (this.queueSize = this.queue2Size = 0);
    }
    
    Segment(Segment<V> paramSegment, int paramInt)
    {
      this(paramSegment.maxMemory, paramSegment.entries.length * paramInt, paramSegment.stackMoveDistance);
      
      CacheLongKeyLIRS.Entry localEntry1 = paramSegment.stack.stackPrev;
      CacheLongKeyLIRS.Entry localEntry2;
      while (localEntry1 != paramSegment.stack)
      {
        localEntry2 = copy(localEntry1);
        addToMap(localEntry2);
        addToStack(localEntry2);
        localEntry1 = localEntry1.stackPrev;
      }
      localEntry1 = paramSegment.queue.queuePrev;
      while (localEntry1 != paramSegment.queue)
      {
        localEntry2 = find(localEntry1.key, CacheLongKeyLIRS.getHash(localEntry1.key));
        if (localEntry2 == null)
        {
          localEntry2 = copy(localEntry1);
          addToMap(localEntry2);
        }
        addToQueue(this.queue, localEntry2);
        localEntry1 = localEntry1.queuePrev;
      }
      localEntry1 = paramSegment.queue2.queuePrev;
      while (localEntry1 != paramSegment.queue2)
      {
        localEntry2 = find(localEntry1.key, CacheLongKeyLIRS.getHash(localEntry1.key));
        if (localEntry2 == null)
        {
          localEntry2 = copy(localEntry1);
          addToMap(localEntry2);
        }
        addToQueue(this.queue2, localEntry2);
        localEntry1 = localEntry1.queuePrev;
      }
    }
    
    private void addToMap(CacheLongKeyLIRS.Entry<V> paramEntry)
    {
      int i = CacheLongKeyLIRS.getHash(paramEntry.key) & this.mask;
      paramEntry.mapNext = this.entries[i];
      this.entries[i] = paramEntry;
      this.usedMemory += paramEntry.memory;
      this.mapSize += 1;
    }
    
    private static <V> CacheLongKeyLIRS.Entry<V> copy(CacheLongKeyLIRS.Entry<V> paramEntry)
    {
      CacheLongKeyLIRS.Entry localEntry = new CacheLongKeyLIRS.Entry();
      localEntry.key = paramEntry.key;
      localEntry.value = paramEntry.value;
      localEntry.memory = paramEntry.memory;
      localEntry.topMove = paramEntry.topMove;
      return localEntry;
    }
    
    public boolean isFull()
    {
      return this.mapSize > this.mask;
    }
    
    int getMemory(long paramLong, int paramInt)
    {
      CacheLongKeyLIRS.Entry localEntry = find(paramLong, paramInt);
      return localEntry == null ? 0 : localEntry.memory;
    }
    
    V get(long paramLong, int paramInt)
    {
      CacheLongKeyLIRS.Entry localEntry = find(paramLong, paramInt);
      if (localEntry == null) {
        return null;
      }
      Object localObject = localEntry.value;
      if (localObject == null) {
        return null;
      }
      if (localEntry.isHot())
      {
        if ((localEntry != this.stack.stackNext) && (
          (this.stackMoveDistance == 0) || (this.stackMoveCounter - localEntry.topMove > this.stackMoveDistance))) {
          access(paramLong, paramInt);
        }
      }
      else {
        access(paramLong, paramInt);
      }
      return (V)localObject;
    }
    
    private synchronized void access(long paramLong, int paramInt)
    {
      CacheLongKeyLIRS.Entry localEntry = find(paramLong, paramInt);
      if ((localEntry == null) || (localEntry.value == null)) {
        return;
      }
      if (localEntry.isHot())
      {
        if ((localEntry != this.stack.stackNext) && (
          (this.stackMoveDistance == 0) || (this.stackMoveCounter - localEntry.topMove > this.stackMoveDistance)))
        {
          int i = localEntry == this.stack.stackPrev ? 1 : 0;
          removeFromStack(localEntry);
          if (i != 0) {
            pruneStack();
          }
          addToStack(localEntry);
        }
      }
      else
      {
        removeFromQueue(localEntry);
        if (localEntry.stackNext != null)
        {
          removeFromStack(localEntry);
          
          convertOldestHotToCold();
        }
        else
        {
          addToQueue(this.queue, localEntry);
        }
        addToStack(localEntry);
      }
    }
    
    synchronized V put(long paramLong, int paramInt1, V paramV, int paramInt2)
    {
      if (paramV == null) {
        throw DataUtils.newIllegalArgumentException("The value may not be null", new Object[0]);
      }
      CacheLongKeyLIRS.Entry localEntry = find(paramLong, paramInt1);
      Object localObject;
      if (localEntry == null)
      {
        localObject = null;
      }
      else
      {
        localObject = localEntry.value;
        remove(paramLong, paramInt1);
      }
      localEntry = new CacheLongKeyLIRS.Entry();
      localEntry.key = paramLong;
      localEntry.value = paramV;
      localEntry.memory = paramInt2;
      int i = paramInt1 & this.mask;
      localEntry.mapNext = this.entries[i];
      this.entries[i] = localEntry;
      this.usedMemory += paramInt2;
      if ((this.usedMemory > this.maxMemory) && (this.mapSize > 0)) {
        evict(localEntry);
      }
      this.mapSize += 1;
      
      addToStack(localEntry);
      return (V)localObject;
    }
    
    synchronized V remove(long paramLong, int paramInt)
    {
      int i = paramInt & this.mask;
      CacheLongKeyLIRS.Entry localEntry1 = this.entries[i];
      if (localEntry1 == null) {
        return null;
      }
      Object localObject;
      if (localEntry1.key == paramLong)
      {
        localObject = localEntry1.value;
        this.entries[i] = localEntry1.mapNext;
      }
      else
      {
        CacheLongKeyLIRS.Entry localEntry2;
        do
        {
          localEntry2 = localEntry1;
          localEntry1 = localEntry1.mapNext;
          if (localEntry1 == null) {
            return null;
          }
        } while (localEntry1.key != paramLong);
        localObject = localEntry1.value;
        localEntry2.mapNext = localEntry1.mapNext;
      }
      this.mapSize -= 1;
      this.usedMemory -= localEntry1.memory;
      if (localEntry1.stackNext != null) {
        removeFromStack(localEntry1);
      }
      if (localEntry1.isHot())
      {
        localEntry1 = this.queue.queueNext;
        if (localEntry1 != this.queue)
        {
          removeFromQueue(localEntry1);
          if (localEntry1.stackNext == null) {
            addToStackBottom(localEntry1);
          }
        }
      }
      else
      {
        removeFromQueue(localEntry1);
      }
      pruneStack();
      return (V)localObject;
    }
    
    private void evict(CacheLongKeyLIRS.Entry<V> paramEntry)
    {
      while ((this.queueSize <= this.mapSize >>> 5) && (this.stackSize > 0)) {
        convertOldestHotToCold();
      }
      if (this.stackSize > 0) {
        addToQueue(this.queue, paramEntry);
      }
      while ((this.usedMemory > this.maxMemory) && (this.queueSize > 1))
      {
        CacheLongKeyLIRS.Entry localEntry = this.queue.queuePrev;
        this.usedMemory -= localEntry.memory;
        removeFromQueue(localEntry);
        localEntry.value = null;
        localEntry.memory = 0;
        addToQueue(this.queue2, localEntry);
        while (this.queue2Size + this.queue2Size > this.stackSize)
        {
          localEntry = this.queue2.queuePrev;
          int i = CacheLongKeyLIRS.getHash(localEntry.key);
          remove(localEntry.key, i);
        }
      }
    }
    
    private void convertOldestHotToCold()
    {
      CacheLongKeyLIRS.Entry localEntry = this.stack.stackPrev;
      if (localEntry == this.stack) {
        throw new IllegalStateException();
      }
      removeFromStack(localEntry);
      
      addToQueue(this.queue, localEntry);
      pruneStack();
    }
    
    private void pruneStack()
    {
      for (;;)
      {
        CacheLongKeyLIRS.Entry localEntry = this.stack.stackPrev;
        if (localEntry.isHot()) {
          break;
        }
        removeFromStack(localEntry);
      }
    }
    
    CacheLongKeyLIRS.Entry<V> find(long paramLong, int paramInt)
    {
      int i = paramInt & this.mask;
      CacheLongKeyLIRS.Entry localEntry = this.entries[i];
      while ((localEntry != null) && (localEntry.key != paramLong)) {
        localEntry = localEntry.mapNext;
      }
      return localEntry;
    }
    
    private void addToStack(CacheLongKeyLIRS.Entry<V> paramEntry)
    {
      paramEntry.stackPrev = this.stack;
      paramEntry.stackNext = this.stack.stackNext;
      paramEntry.stackNext.stackPrev = paramEntry;
      this.stack.stackNext = paramEntry;
      this.stackSize += 1;
      paramEntry.topMove = (this.stackMoveCounter++);
    }
    
    private void addToStackBottom(CacheLongKeyLIRS.Entry<V> paramEntry)
    {
      paramEntry.stackNext = this.stack;
      paramEntry.stackPrev = this.stack.stackPrev;
      paramEntry.stackPrev.stackNext = paramEntry;
      this.stack.stackPrev = paramEntry;
      this.stackSize += 1;
    }
    
    private void removeFromStack(CacheLongKeyLIRS.Entry<V> paramEntry)
    {
      paramEntry.stackPrev.stackNext = paramEntry.stackNext;
      paramEntry.stackNext.stackPrev = paramEntry.stackPrev;
      paramEntry.stackPrev = (paramEntry.stackNext = null);
      this.stackSize -= 1;
    }
    
    private void addToQueue(CacheLongKeyLIRS.Entry<V> paramEntry1, CacheLongKeyLIRS.Entry<V> paramEntry2)
    {
      paramEntry2.queuePrev = paramEntry1;
      paramEntry2.queueNext = paramEntry1.queueNext;
      paramEntry2.queueNext.queuePrev = paramEntry2;
      paramEntry1.queueNext = paramEntry2;
      if (paramEntry2.value != null) {
        this.queueSize += 1;
      } else {
        this.queue2Size += 1;
      }
    }
    
    private void removeFromQueue(CacheLongKeyLIRS.Entry<V> paramEntry)
    {
      paramEntry.queuePrev.queueNext = paramEntry.queueNext;
      paramEntry.queueNext.queuePrev = paramEntry.queuePrev;
      paramEntry.queuePrev = (paramEntry.queueNext = null);
      if (paramEntry.value != null) {
        this.queueSize -= 1;
      } else {
        this.queue2Size -= 1;
      }
    }
    
    synchronized List<Long> keys(boolean paramBoolean1, boolean paramBoolean2)
    {
      ArrayList localArrayList = new ArrayList();
      CacheLongKeyLIRS.Entry localEntry1;
      if (paramBoolean1)
      {
        localEntry1 = paramBoolean2 ? this.queue2 : this.queue;
        for (CacheLongKeyLIRS.Entry localEntry2 = localEntry1.queueNext; localEntry2 != localEntry1; localEntry2 = localEntry2.queueNext) {
          localArrayList.add(Long.valueOf(localEntry2.key));
        }
      }
      else
      {
        for (localEntry1 = this.stack.stackNext; localEntry1 != this.stack; localEntry1 = localEntry1.stackNext) {
          localArrayList.add(Long.valueOf(localEntry1.key));
        }
      }
      return localArrayList;
    }
    
    boolean containsKey(long paramLong, int paramInt)
    {
      CacheLongKeyLIRS.Entry localEntry = find(paramLong, paramInt);
      return (localEntry != null) && (localEntry.value != null);
    }
    
    synchronized Set<Long> keySet()
    {
      HashSet localHashSet = new HashSet();
      for (CacheLongKeyLIRS.Entry localEntry = this.stack.stackNext; localEntry != this.stack; localEntry = localEntry.stackNext) {
        localHashSet.add(Long.valueOf(localEntry.key));
      }
      for (localEntry = this.queue.queueNext; localEntry != this.queue; localEntry = localEntry.queueNext) {
        localHashSet.add(Long.valueOf(localEntry.key));
      }
      return localHashSet;
    }
    
    void setMaxMemory(long paramLong)
    {
      this.maxMemory = paramLong;
    }
  }
  
  static class Entry<V>
  {
    long key;
    V value;
    int memory;
    int topMove;
    Entry<V> stackNext;
    Entry<V> stackPrev;
    Entry<V> queueNext;
    Entry<V> queuePrev;
    Entry<V> mapNext;
    
    boolean isHot()
    {
      return this.queueNext == null;
    }
  }
}
