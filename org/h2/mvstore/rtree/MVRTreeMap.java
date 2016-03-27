package org.h2.mvstore.rtree;

import java.util.ArrayList;
import java.util.Iterator;
import org.h2.mvstore.CursorPos;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.MapBuilder;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.Page;
import org.h2.mvstore.Page.PageReference;
import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.ObjectDataType;
import org.h2.util.New;

public class MVRTreeMap<V>
  extends MVMap<SpatialKey, V>
{
  final SpatialDataType keyType;
  private boolean quadraticSplit;
  
  public MVRTreeMap(int paramInt, DataType paramDataType)
  {
    super(new SpatialDataType(paramInt), paramDataType);
    this.keyType = ((SpatialDataType)getKeyType());
  }
  
  public static <V> MVRTreeMap<V> create(int paramInt, DataType paramDataType)
  {
    return new MVRTreeMap(paramInt, paramDataType);
  }
  
  public V get(Object paramObject)
  {
    return (V)get(this.root, paramObject);
  }
  
  public RTreeCursor findIntersectingKeys(SpatialKey paramSpatialKey)
  {
    new RTreeCursor(this.root, paramSpatialKey)
    {
      protected boolean check(boolean paramAnonymousBoolean, SpatialKey paramAnonymousSpatialKey1, SpatialKey paramAnonymousSpatialKey2)
      {
        return MVRTreeMap.this.keyType.isOverlap(paramAnonymousSpatialKey1, paramAnonymousSpatialKey2);
      }
    };
  }
  
  public RTreeCursor findContainedKeys(SpatialKey paramSpatialKey)
  {
    new RTreeCursor(this.root, paramSpatialKey)
    {
      protected boolean check(boolean paramAnonymousBoolean, SpatialKey paramAnonymousSpatialKey1, SpatialKey paramAnonymousSpatialKey2)
      {
        if (paramAnonymousBoolean) {
          return MVRTreeMap.this.keyType.isInside(paramAnonymousSpatialKey1, paramAnonymousSpatialKey2);
        }
        return MVRTreeMap.this.keyType.isOverlap(paramAnonymousSpatialKey1, paramAnonymousSpatialKey2);
      }
    };
  }
  
  private boolean contains(Page paramPage, int paramInt, Object paramObject)
  {
    return this.keyType.contains(paramPage.getKey(paramInt), paramObject);
  }
  
  protected Object get(Page paramPage, Object paramObject)
  {
    int i;
    if (!paramPage.isLeaf()) {
      for (i = 0; i < paramPage.getKeyCount(); i++) {
        if (contains(paramPage, i, paramObject))
        {
          Object localObject = get(paramPage.getChildPage(i), paramObject);
          if (localObject != null) {
            return localObject;
          }
        }
      }
    } else {
      for (i = 0; i < paramPage.getKeyCount(); i++) {
        if (this.keyType.equals(paramPage.getKey(i), paramObject)) {
          return paramPage.getValue(i);
        }
      }
    }
    return null;
  }
  
  protected synchronized Object remove(Page paramPage, long paramLong, Object paramObject)
  {
    Object localObject1 = null;
    if (paramPage.isLeaf())
    {
      for (i = 0; i < paramPage.getKeyCount(); i++) {
        if (this.keyType.equals(paramPage.getKey(i), paramObject))
        {
          localObject1 = paramPage.getValue(i);
          paramPage.remove(i);
          break;
        }
      }
      return localObject1;
    }
    for (int i = 0; i < paramPage.getKeyCount(); i++) {
      if (contains(paramPage, i, paramObject))
      {
        Page localPage1 = paramPage.getChildPage(i);
        
        Page localPage2 = localPage1.copy(paramLong);
        long l = localPage2.getTotalCount();
        localObject1 = remove(localPage2, paramLong, paramObject);
        paramPage.setChild(i, localPage2);
        if (l != localPage2.getTotalCount())
        {
          if (localPage2.getTotalCount() == 0L)
          {
            paramPage.remove(i);
            if (paramPage.getKeyCount() != 0) {
              break;
            }
            localPage2.removePage(); break;
          }
          Object localObject2 = paramPage.getKey(i);
          if (this.keyType.isInside(paramObject, localObject2)) {
            break;
          }
          paramPage.setKey(i, getBounds(localPage2)); break;
        }
      }
    }
    return localObject1;
  }
  
  private Object getBounds(Page paramPage)
  {
    Object localObject = this.keyType.createBoundingBox(paramPage.getKey(0));
    for (int i = 1; i < paramPage.getKeyCount(); i++) {
      this.keyType.increaseBounds(localObject, paramPage.getKey(i));
    }
    return localObject;
  }
  
  public V put(SpatialKey paramSpatialKey, V paramV)
  {
    return (V)putOrAdd(paramSpatialKey, paramV, false);
  }
  
  public void add(SpatialKey paramSpatialKey, V paramV)
  {
    putOrAdd(paramSpatialKey, paramV, true);
  }
  
  private synchronized Object putOrAdd(SpatialKey paramSpatialKey, V paramV, boolean paramBoolean)
  {
    beforeWrite();
    long l1 = this.writeVersion;
    Page localPage1 = this.root.copy(l1);
    Object localObject1;
    if ((paramBoolean) || (get(paramSpatialKey) == null))
    {
      if ((localPage1.getMemory() > this.store.getPageSplitSize()) && (localPage1.getKeyCount() > 3))
      {
        long l2 = localPage1.getTotalCount();
        Page localPage2 = split(localPage1, l1);
        Object localObject2 = getBounds(localPage1);
        Object localObject3 = getBounds(localPage2);
        Object[] arrayOfObject = { localObject2, localObject3 };
        Page.PageReference[] arrayOfPageReference = { new Page.PageReference(localPage1, localPage1.getPos(), localPage1.getTotalCount()), new Page.PageReference(localPage2, localPage2.getPos(), localPage2.getTotalCount()), new Page.PageReference(null, 0L, 0L) };
        
        localPage1 = Page.create(this, l1, arrayOfObject, null, arrayOfPageReference, l2, 0);
      }
      add(localPage1, l1, paramSpatialKey, paramV);
      localObject1 = null;
    }
    else
    {
      localObject1 = set(localPage1, l1, paramSpatialKey, paramV);
    }
    newRoot(localPage1);
    return localObject1;
  }
  
  private Object set(Page paramPage, long paramLong, Object paramObject1, Object paramObject2)
  {
    int i;
    if (paramPage.isLeaf()) {
      for (i = 0; i < paramPage.getKeyCount(); i++) {
        if (this.keyType.equals(paramPage.getKey(i), paramObject1)) {
          return paramPage.setValue(i, paramObject2);
        }
      }
    } else {
      for (i = 0; i < paramPage.getKeyCount(); i++) {
        if (contains(paramPage, i, paramObject1))
        {
          Page localPage = paramPage.getChildPage(i);
          if (get(localPage, paramObject1) != null)
          {
            localPage = localPage.copy(paramLong);
            Object localObject = set(localPage, paramLong, paramObject1, paramObject2);
            paramPage.setChild(i, localPage);
            return localObject;
          }
        }
      }
    }
    throw DataUtils.newIllegalStateException(3, "Not found: {0}", new Object[] { paramObject1 });
  }
  
  private void add(Page paramPage, long paramLong, Object paramObject1, Object paramObject2)
  {
    if (paramPage.isLeaf())
    {
      paramPage.insertLeaf(paramPage.getKeyCount(), paramObject1, paramObject2);
      return;
    }
    int i = -1;
    for (int j = 0; j < paramPage.getKeyCount(); j++) {
      if (contains(paramPage, j, paramObject1))
      {
        i = j;
        break;
      }
    }
    if (i < 0)
    {
      float f1 = Float.MAX_VALUE;
      for (int k = 0; k < paramPage.getKeyCount(); k++)
      {
        Object localObject2 = paramPage.getKey(k);
        float f2 = this.keyType.getAreaIncrease(localObject2, paramObject1);
        if (f2 < f1)
        {
          i = k;
          f1 = f2;
        }
      }
    }
    Page localPage = paramPage.getChildPage(i).copy(paramLong);
    if ((localPage.getMemory() > this.store.getPageSplitSize()) && (localPage.getKeyCount() > 4))
    {
      localObject1 = split(localPage, paramLong);
      paramPage.setKey(i, getBounds(localPage));
      paramPage.setChild(i, localPage);
      paramPage.insertNode(i, getBounds((Page)localObject1), (Page)localObject1);
      
      add(paramPage, paramLong, paramObject1, paramObject2);
      return;
    }
    add(localPage, paramLong, paramObject1, paramObject2);
    Object localObject1 = paramPage.getKey(i);
    this.keyType.increaseBounds(localObject1, paramObject1);
    paramPage.setKey(i, localObject1);
    paramPage.setChild(i, localPage);
  }
  
  private Page split(Page paramPage, long paramLong)
  {
    return this.quadraticSplit ? splitQuadratic(paramPage, paramLong) : splitLinear(paramPage, paramLong);
  }
  
  private Page splitLinear(Page paramPage, long paramLong)
  {
    ArrayList localArrayList = New.arrayList();
    for (int i = 0; i < paramPage.getKeyCount(); i++) {
      localArrayList.add(paramPage.getKey(i));
    }
    int[] arrayOfInt = this.keyType.getExtremes(localArrayList);
    if (arrayOfInt == null) {
      return splitQuadratic(paramPage, paramLong);
    }
    Page localPage1 = newPage(paramPage.isLeaf(), paramLong);
    Page localPage2 = newPage(paramPage.isLeaf(), paramLong);
    move(paramPage, localPage1, arrayOfInt[0]);
    if (arrayOfInt[1] > arrayOfInt[0]) {
      arrayOfInt[1] -= 1;
    }
    move(paramPage, localPage2, arrayOfInt[1]);
    Object localObject1 = this.keyType.createBoundingBox(localPage1.getKey(0));
    Object localObject2 = this.keyType.createBoundingBox(localPage2.getKey(0));
    while (paramPage.getKeyCount() > 0)
    {
      Object localObject3 = paramPage.getKey(0);
      float f1 = this.keyType.getAreaIncrease(localObject1, localObject3);
      float f2 = this.keyType.getAreaIncrease(localObject2, localObject3);
      if (f1 < f2)
      {
        this.keyType.increaseBounds(localObject1, localObject3);
        move(paramPage, localPage1, 0);
      }
      else
      {
        this.keyType.increaseBounds(localObject2, localObject3);
        move(paramPage, localPage2, 0);
      }
    }
    while (localPage2.getKeyCount() > 0) {
      move(localPage2, paramPage, 0);
    }
    return localPage1;
  }
  
  private Page splitQuadratic(Page paramPage, long paramLong)
  {
    Page localPage1 = newPage(paramPage.isLeaf(), paramLong);
    Page localPage2 = newPage(paramPage.isLeaf(), paramLong);
    float f1 = Float.MIN_VALUE;
    int i = 0;int j = 0;
    float f4;
    for (int k = 0; k < paramPage.getKeyCount(); k++)
    {
      localObject2 = paramPage.getKey(k);
      for (int m = 0; m < paramPage.getKeyCount(); m++) {
        if (k != m)
        {
          Object localObject3 = paramPage.getKey(m);
          f4 = this.keyType.getCombinedArea(localObject2, localObject3);
          if (f4 > f1)
          {
            f1 = f4;
            i = k;
            j = m;
          }
        }
      }
    }
    move(paramPage, localPage1, i);
    if (i < j) {
      j--;
    }
    move(paramPage, localPage2, j);
    Object localObject1 = this.keyType.createBoundingBox(localPage1.getKey(0));
    Object localObject2 = this.keyType.createBoundingBox(localPage2.getKey(0));
    while (paramPage.getKeyCount() > 0)
    {
      float f2 = 0.0F;float f3 = 0.0F;f4 = 0.0F;
      int n = 0;
      for (int i1 = 0; i1 < paramPage.getKeyCount(); i1++)
      {
        Object localObject4 = paramPage.getKey(i1);
        float f5 = this.keyType.getAreaIncrease(localObject1, localObject4);
        float f6 = this.keyType.getAreaIncrease(localObject2, localObject4);
        float f7 = Math.abs(f5 - f6);
        if (f7 > f2)
        {
          f2 = f7;
          f3 = f5;
          f4 = f6;
          n = i1;
        }
      }
      if (f3 < f4)
      {
        this.keyType.increaseBounds(localObject1, paramPage.getKey(n));
        move(paramPage, localPage1, n);
      }
      else
      {
        this.keyType.increaseBounds(localObject2, paramPage.getKey(n));
        move(paramPage, localPage2, n);
      }
    }
    while (localPage2.getKeyCount() > 0) {
      move(localPage2, paramPage, 0);
    }
    return localPage1;
  }
  
  private Page newPage(boolean paramBoolean, long paramLong)
  {
    Object[] arrayOfObject;
    Page.PageReference[] arrayOfPageReference;
    if (paramBoolean)
    {
      arrayOfObject = Page.EMPTY_OBJECT_ARRAY;
      arrayOfPageReference = null;
    }
    else
    {
      arrayOfObject = null;
      arrayOfPageReference = new Page.PageReference[] { new Page.PageReference(null, 0L, 0L) };
    }
    return Page.create(this, paramLong, Page.EMPTY_OBJECT_ARRAY, arrayOfObject, arrayOfPageReference, 0L, 0);
  }
  
  private static void move(Page paramPage1, Page paramPage2, int paramInt)
  {
    Object localObject1 = paramPage1.getKey(paramInt);
    Object localObject2;
    if (paramPage1.isLeaf())
    {
      localObject2 = paramPage1.getValue(paramInt);
      paramPage2.insertLeaf(0, localObject1, localObject2);
    }
    else
    {
      localObject2 = paramPage1.getChildPage(paramInt);
      paramPage2.insertNode(0, localObject1, (Page)localObject2);
    }
    paramPage1.remove(paramInt);
  }
  
  public void addNodeKeys(ArrayList<SpatialKey> paramArrayList, Page paramPage)
  {
    if ((paramPage != null) && (!paramPage.isLeaf())) {
      for (int i = 0; i < paramPage.getKeyCount(); i++)
      {
        paramArrayList.add((SpatialKey)paramPage.getKey(i));
        addNodeKeys(paramArrayList, paramPage.getChildPage(i));
      }
    }
  }
  
  public boolean isQuadraticSplit()
  {
    return this.quadraticSplit;
  }
  
  public void setQuadraticSplit(boolean paramBoolean)
  {
    this.quadraticSplit = paramBoolean;
  }
  
  protected int getChildPageCount(Page paramPage)
  {
    return paramPage.getRawChildPageCount() - 1;
  }
  
  public static class RTreeCursor
    implements Iterator<SpatialKey>
  {
    private final SpatialKey filter;
    private CursorPos pos;
    private SpatialKey current;
    private final Page root;
    private boolean initialized;
    
    protected RTreeCursor(Page paramPage, SpatialKey paramSpatialKey)
    {
      this.root = paramPage;
      this.filter = paramSpatialKey;
    }
    
    public boolean hasNext()
    {
      if (!this.initialized)
      {
        this.pos = new CursorPos(this.root, 0, null);
        fetchNext();
        this.initialized = true;
      }
      return this.current != null;
    }
    
    public void skip(long paramLong)
    {
      while ((hasNext()) && (paramLong-- > 0L)) {
        fetchNext();
      }
    }
    
    public SpatialKey next()
    {
      if (!hasNext()) {
        return null;
      }
      SpatialKey localSpatialKey = this.current;
      fetchNext();
      return localSpatialKey;
    }
    
    public void remove()
    {
      throw DataUtils.newUnsupportedOperationException("Removing is not supported");
    }
    
    protected void fetchNext()
    {
      while (this.pos != null)
      {
        Page localPage1 = this.pos.page;
        if (localPage1.isLeaf()) {
          while (this.pos.index < localPage1.getKeyCount())
          {
            SpatialKey localSpatialKey1 = (SpatialKey)localPage1.getKey(this.pos.index++);
            if ((this.filter == null) || (check(true, localSpatialKey1, this.filter)))
            {
              this.current = localSpatialKey1;
              return;
            }
          }
        }
        int i = 0;
        while (this.pos.index < localPage1.getKeyCount())
        {
          int j = this.pos.index++;
          SpatialKey localSpatialKey2 = (SpatialKey)localPage1.getKey(j);
          if ((this.filter == null) || (check(false, localSpatialKey2, this.filter)))
          {
            Page localPage2 = this.pos.page.getChildPage(j);
            this.pos = new CursorPos(localPage2, 0, this.pos);
            i = 1;
            break;
          }
        }
        if (i == 0) {
          this.pos = this.pos.parent;
        }
      }
      this.current = null;
    }
    
    protected boolean check(boolean paramBoolean, SpatialKey paramSpatialKey1, SpatialKey paramSpatialKey2)
    {
      return true;
    }
  }
  
  public String getType()
  {
    return "rtree";
  }
  
  public static class Builder<V>
    implements MVMap.MapBuilder<MVRTreeMap<V>, SpatialKey, V>
  {
    private int dimensions = 2;
    private DataType valueType;
    
    public Builder<V> dimensions(int paramInt)
    {
      this.dimensions = paramInt;
      return this;
    }
    
    public Builder<V> valueType(DataType paramDataType)
    {
      this.valueType = paramDataType;
      return this;
    }
    
    public MVRTreeMap<V> create()
    {
      if (this.valueType == null) {
        this.valueType = new ObjectDataType();
      }
      return new MVRTreeMap(this.dimensions, this.valueType);
    }
  }
}
