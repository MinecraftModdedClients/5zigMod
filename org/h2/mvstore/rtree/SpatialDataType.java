package org.h2.mvstore.rtree;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

public class SpatialDataType
  implements DataType
{
  private final int dimensions;
  
  public SpatialDataType(int paramInt)
  {
    DataUtils.checkArgument((paramInt >= 1) && (paramInt < 32), "Dimensions must be between 1 and 31, is {0}", new Object[] { Integer.valueOf(paramInt) });
    
    this.dimensions = paramInt;
  }
  
  public int compare(Object paramObject1, Object paramObject2)
  {
    long l1 = ((SpatialKey)paramObject1).getId();
    long l2 = ((SpatialKey)paramObject2).getId();
    return l1 > l2 ? 1 : l1 < l2 ? -1 : 0;
  }
  
  public boolean equals(Object paramObject1, Object paramObject2)
  {
    long l1 = ((SpatialKey)paramObject1).getId();
    long l2 = ((SpatialKey)paramObject2).getId();
    return l1 == l2;
  }
  
  public int getMemory(Object paramObject)
  {
    return 40 + this.dimensions * 4;
  }
  
  public void read(ByteBuffer paramByteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
  {
    for (int i = 0; i < paramInt; i++) {
      paramArrayOfObject[i] = read(paramByteBuffer);
    }
  }
  
  public void write(WriteBuffer paramWriteBuffer, Object[] paramArrayOfObject, int paramInt, boolean paramBoolean)
  {
    for (int i = 0; i < paramInt; i++) {
      write(paramWriteBuffer, paramArrayOfObject[i]);
    }
  }
  
  public void write(WriteBuffer paramWriteBuffer, Object paramObject)
  {
    SpatialKey localSpatialKey = (SpatialKey)paramObject;
    int i = 0;
    for (int j = 0; j < this.dimensions; j++) {
      if (localSpatialKey.min(j) == localSpatialKey.max(j)) {
        i |= 1 << j;
      }
    }
    paramWriteBuffer.putVarInt(i);
    for (j = 0; j < this.dimensions; j++)
    {
      paramWriteBuffer.putFloat(localSpatialKey.min(j));
      if ((i & 1 << j) == 0) {
        paramWriteBuffer.putFloat(localSpatialKey.max(j));
      }
    }
    paramWriteBuffer.putVarLong(localSpatialKey.getId());
  }
  
  public Object read(ByteBuffer paramByteBuffer)
  {
    int i = DataUtils.readVarInt(paramByteBuffer);
    float[] arrayOfFloat = new float[this.dimensions * 2];
    for (int j = 0; j < this.dimensions; j++)
    {
      float f1 = paramByteBuffer.getFloat();
      float f2;
      if ((i & 1 << j) != 0) {
        f2 = f1;
      } else {
        f2 = paramByteBuffer.getFloat();
      }
      arrayOfFloat[(j + j)] = f1;
      arrayOfFloat[(j + j + 1)] = f2;
    }
    long l = DataUtils.readVarLong(paramByteBuffer);
    return new SpatialKey(l, arrayOfFloat);
  }
  
  public boolean isOverlap(Object paramObject1, Object paramObject2)
  {
    SpatialKey localSpatialKey1 = (SpatialKey)paramObject1;
    SpatialKey localSpatialKey2 = (SpatialKey)paramObject2;
    for (int i = 0; i < this.dimensions; i++) {
      if ((localSpatialKey1.max(i) < localSpatialKey2.min(i)) || (localSpatialKey1.min(i) > localSpatialKey2.max(i))) {
        return false;
      }
    }
    return true;
  }
  
  public void increaseBounds(Object paramObject1, Object paramObject2)
  {
    SpatialKey localSpatialKey1 = (SpatialKey)paramObject1;
    SpatialKey localSpatialKey2 = (SpatialKey)paramObject2;
    for (int i = 0; i < this.dimensions; i++)
    {
      localSpatialKey1.setMin(i, Math.min(localSpatialKey1.min(i), localSpatialKey2.min(i)));
      localSpatialKey1.setMax(i, Math.max(localSpatialKey1.max(i), localSpatialKey2.max(i)));
    }
  }
  
  public float getAreaIncrease(Object paramObject1, Object paramObject2)
  {
    SpatialKey localSpatialKey1 = (SpatialKey)paramObject1;
    SpatialKey localSpatialKey2 = (SpatialKey)paramObject2;
    float f1 = localSpatialKey1.min(0);
    float f2 = localSpatialKey1.max(0);
    float f3 = f2 - f1;
    f1 = Math.min(f1, localSpatialKey2.min(0));
    f2 = Math.max(f2, localSpatialKey2.max(0));
    float f4 = f2 - f1;
    for (int i = 1; i < this.dimensions; i++)
    {
      f1 = localSpatialKey1.min(i);
      f2 = localSpatialKey1.max(i);
      f3 *= (f2 - f1);
      f1 = Math.min(f1, localSpatialKey2.min(i));
      f2 = Math.max(f2, localSpatialKey2.max(i));
      f4 *= (f2 - f1);
    }
    return f4 - f3;
  }
  
  float getCombinedArea(Object paramObject1, Object paramObject2)
  {
    SpatialKey localSpatialKey1 = (SpatialKey)paramObject1;
    SpatialKey localSpatialKey2 = (SpatialKey)paramObject2;
    float f1 = 1.0F;
    for (int i = 0; i < this.dimensions; i++)
    {
      float f2 = Math.min(localSpatialKey1.min(i), localSpatialKey2.min(i));
      float f3 = Math.max(localSpatialKey1.max(i), localSpatialKey2.max(i));
      f1 *= (f3 - f2);
    }
    return f1;
  }
  
  public boolean contains(Object paramObject1, Object paramObject2)
  {
    SpatialKey localSpatialKey1 = (SpatialKey)paramObject1;
    SpatialKey localSpatialKey2 = (SpatialKey)paramObject2;
    for (int i = 0; i < this.dimensions; i++) {
      if ((localSpatialKey1.min(i) > localSpatialKey2.min(i)) || (localSpatialKey1.max(i) < localSpatialKey2.max(i))) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isInside(Object paramObject1, Object paramObject2)
  {
    SpatialKey localSpatialKey1 = (SpatialKey)paramObject1;
    SpatialKey localSpatialKey2 = (SpatialKey)paramObject2;
    for (int i = 0; i < this.dimensions; i++) {
      if ((localSpatialKey1.min(i) <= localSpatialKey2.min(i)) || (localSpatialKey1.max(i) >= localSpatialKey2.max(i))) {
        return false;
      }
    }
    return true;
  }
  
  Object createBoundingBox(Object paramObject)
  {
    float[] arrayOfFloat = new float[this.dimensions * 2];
    SpatialKey localSpatialKey = (SpatialKey)paramObject;
    for (int i = 0; i < this.dimensions; i++)
    {
      arrayOfFloat[(i + i)] = localSpatialKey.min(i);
      arrayOfFloat[(i + i + 1)] = localSpatialKey.max(i);
    }
    return new SpatialKey(0L, arrayOfFloat);
  }
  
  public int[] getExtremes(ArrayList<Object> paramArrayList)
  {
    SpatialKey localSpatialKey1 = (SpatialKey)createBoundingBox(paramArrayList.get(0));
    SpatialKey localSpatialKey2 = (SpatialKey)createBoundingBox(localSpatialKey1);
    for (int i = 0; i < this.dimensions; i++)
    {
      float f1 = localSpatialKey2.min(i);
      localSpatialKey2.setMin(i, localSpatialKey2.max(i));
      localSpatialKey2.setMax(i, f1);
    }
    for (i = 0; i < paramArrayList.size(); i++)
    {
      Object localObject = paramArrayList.get(i);
      increaseBounds(localSpatialKey1, localObject);
      increaseMaxInnerBounds(localSpatialKey2, localObject);
    }
    double d = 0.0D;
    int j = 0;
    for (int k = 0; k < this.dimensions; k++)
    {
      f3 = localSpatialKey2.max(k) - localSpatialKey2.min(k);
      if (f3 >= 0.0F)
      {
        float f4 = localSpatialKey1.max(k) - localSpatialKey1.min(k);
        float f5 = f3 / f4;
        if (f5 > d)
        {
          d = f5;
          j = k;
        }
      }
    }
    if (d <= 0.0D) {
      return null;
    }
    float f2 = localSpatialKey2.min(j);
    float f3 = localSpatialKey2.max(j);
    int m = -1;int n = -1;
    for (int i1 = 0; (i1 < paramArrayList.size()) && ((m < 0) || (n < 0)); i1++)
    {
      SpatialKey localSpatialKey3 = (SpatialKey)paramArrayList.get(i1);
      if ((m < 0) && (localSpatialKey3.max(j) == f2)) {
        m = i1;
      } else if ((n < 0) && (localSpatialKey3.min(j) == f3)) {
        n = i1;
      }
    }
    return new int[] { m, n };
  }
  
  private void increaseMaxInnerBounds(Object paramObject1, Object paramObject2)
  {
    SpatialKey localSpatialKey1 = (SpatialKey)paramObject1;
    SpatialKey localSpatialKey2 = (SpatialKey)paramObject2;
    for (int i = 0; i < this.dimensions; i++)
    {
      localSpatialKey1.setMin(i, Math.min(localSpatialKey1.min(i), localSpatialKey2.max(i)));
      localSpatialKey1.setMax(i, Math.max(localSpatialKey1.max(i), localSpatialKey2.min(i)));
    }
  }
}
