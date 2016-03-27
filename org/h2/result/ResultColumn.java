package org.h2.result;

import java.io.IOException;
import org.h2.value.Transfer;

public class ResultColumn
{
  final String alias;
  final String schemaName;
  final String tableName;
  final String columnName;
  final int columnType;
  final long precision;
  final int scale;
  final int displaySize;
  final boolean autoIncrement;
  final int nullable;
  
  ResultColumn(Transfer paramTransfer)
    throws IOException
  {
    this.alias = paramTransfer.readString();
    this.schemaName = paramTransfer.readString();
    this.tableName = paramTransfer.readString();
    this.columnName = paramTransfer.readString();
    this.columnType = paramTransfer.readInt();
    this.precision = paramTransfer.readLong();
    this.scale = paramTransfer.readInt();
    this.displaySize = paramTransfer.readInt();
    this.autoIncrement = paramTransfer.readBoolean();
    this.nullable = paramTransfer.readInt();
  }
  
  public static void writeColumn(Transfer paramTransfer, ResultInterface paramResultInterface, int paramInt)
    throws IOException
  {
    paramTransfer.writeString(paramResultInterface.getAlias(paramInt));
    paramTransfer.writeString(paramResultInterface.getSchemaName(paramInt));
    paramTransfer.writeString(paramResultInterface.getTableName(paramInt));
    paramTransfer.writeString(paramResultInterface.getColumnName(paramInt));
    paramTransfer.writeInt(paramResultInterface.getColumnType(paramInt));
    paramTransfer.writeLong(paramResultInterface.getColumnPrecision(paramInt));
    paramTransfer.writeInt(paramResultInterface.getColumnScale(paramInt));
    paramTransfer.writeInt(paramResultInterface.getDisplaySize(paramInt));
    paramTransfer.writeBoolean(paramResultInterface.isAutoIncrement(paramInt));
    paramTransfer.writeInt(paramResultInterface.getNullable(paramInt));
  }
}
