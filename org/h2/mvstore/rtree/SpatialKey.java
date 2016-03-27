package org.h2.mvstore.rtree;

import java.util.Arrays;

public class SpatialKey
{
  private final long id;
  private final float[] minMax;
  
  public SpatialKey(long paramLong, float... paramVarArgs)
  {
    this.id = paramLong;
    this.minMax = paramVarArgs;
  }
  
  public float min(int paramInt)
  {
    return this.minMax[(paramInt + paramInt)];
  }
  
  public void setMin(int paramInt, float paramFloat)
  {
    this.minMax[(paramInt + paramInt)] = paramFloat;
  }
  
  public float max(int paramInt)
  {
    return this.minMax[(paramInt + paramInt + 1)];
  }
  
  public void setMax(int paramInt, float paramFloat)
  {
    this.minMax[(paramInt + paramInt + 1)] = paramFloat;
  }
  
  public long getId()
  {
    return this.id;
  }
  
  public String toString()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append(this.id).append(": (");
    for (int i = 0; i < this.minMax.length; i += 2)
    {
      if (i > 0) {
        localStringBuilder.append(", ");
      }
      localStringBuilder.append(this.minMax[i]).append('/').append(this.minMax[(i + 1)]);
    }
    return ")";
  }
  
  public int hashCode()
  {
    return (int)(this.id >>> 32 ^ this.id);
  }
  
  public boolean equals(Object paramObject)
  {
    if (paramObject == this) {
      return true;
    }
    if (!(paramObject instanceof SpatialKey)) {
      return false;
    }
    SpatialKey localSpatialKey = (SpatialKey)paramObject;
    if (this.id != localSpatialKey.id) {
      return false;
    }
    return equalsIgnoringId(localSpatialKey);
  }
  
  public boolean equalsIgnoringId(SpatialKey paramSpatialKey)
  {
    return Arrays.equals(this.minMax, paramSpatialKey.minMax);
  }
}
