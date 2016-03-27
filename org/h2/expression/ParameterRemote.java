package org.h2.expression;

import java.io.IOException;
import org.h2.message.DbException;
import org.h2.value.Transfer;
import org.h2.value.Value;

public class ParameterRemote
  implements ParameterInterface
{
  private Value value;
  private final int index;
  private int dataType = -1;
  private long precision;
  private int scale;
  private int nullable = 2;
  
  public ParameterRemote(int paramInt)
  {
    this.index = paramInt;
  }
  
  public void setValue(Value paramValue, boolean paramBoolean)
  {
    if ((paramBoolean) && (this.value != null)) {
      this.value.close();
    }
    this.value = paramValue;
  }
  
  public Value getParamValue()
  {
    return this.value;
  }
  
  public void checkSet()
  {
    if (this.value == null) {
      throw DbException.get(90012, "#" + (this.index + 1));
    }
  }
  
  public boolean isValueSet()
  {
    return this.value != null;
  }
  
  public int getType()
  {
    return this.value == null ? this.dataType : this.value.getType();
  }
  
  public long getPrecision()
  {
    return this.value == null ? this.precision : this.value.getPrecision();
  }
  
  public int getScale()
  {
    return this.value == null ? this.scale : this.value.getScale();
  }
  
  public int getNullable()
  {
    return this.nullable;
  }
  
  public void readMetaData(Transfer paramTransfer)
    throws IOException
  {
    this.dataType = paramTransfer.readInt();
    this.precision = paramTransfer.readLong();
    this.scale = paramTransfer.readInt();
    this.nullable = paramTransfer.readInt();
  }
  
  public static void writeMetaData(Transfer paramTransfer, ParameterInterface paramParameterInterface)
    throws IOException
  {
    paramTransfer.writeInt(paramParameterInterface.getType());
    paramTransfer.writeLong(paramParameterInterface.getPrecision());
    paramTransfer.writeInt(paramParameterInterface.getScale());
    paramTransfer.writeInt(paramParameterInterface.getNullable());
  }
}
