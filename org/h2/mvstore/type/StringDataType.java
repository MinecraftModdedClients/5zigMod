package org.h2.mvstore.type;

import java.nio.ByteBuffer;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;

public class StringDataType
  implements DataType
{
  public static final StringDataType INSTANCE = new StringDataType();
  
  public int compare(Object paramObject1, Object paramObject2)
  {
    return paramObject1.toString().compareTo(paramObject2.toString());
  }
  
  public int getMemory(Object paramObject)
  {
    return 24 + 2 * paramObject.toString().length();
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
  
  public String read(ByteBuffer paramByteBuffer)
  {
    int i = DataUtils.readVarInt(paramByteBuffer);
    return DataUtils.readString(paramByteBuffer, i);
  }
  
  public void write(WriteBuffer paramWriteBuffer, Object paramObject)
  {
    String str = paramObject.toString();
    int i = str.length();
    paramWriteBuffer.putVarInt(i).putStringData(str, i);
  }
}
