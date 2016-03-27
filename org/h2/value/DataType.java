package org.h2.value;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.h2.engine.Constants;
import org.h2.engine.SessionInterface;
import org.h2.engine.SysProperties;
import org.h2.jdbc.JdbcBlob;
import org.h2.jdbc.JdbcClob;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.store.DataHandler;
import org.h2.store.LobStorageInterface;
import org.h2.tools.SimpleResultSet;
import org.h2.util.JdbcUtils;
import org.h2.util.New;
import org.h2.util.Utils;

public class DataType
{
  public static final int TYPE_RESULT_SET = -10;
  public static final Class<?> GEOMETRY_CLASS;
  private static final String GEOMETRY_CLASS_NAME = "com.vividsolutions.jts.geom.Geometry";
  private static final ArrayList<DataType> TYPES = ;
  private static final HashMap<String, DataType> TYPES_BY_NAME = New.hashMap();
  private static final ArrayList<DataType> TYPES_BY_VALUE_TYPE = New.arrayList();
  public int type;
  public String name;
  public int sqlType;
  public String jdbc;
  public int sqlTypePos;
  public long maxPrecision;
  public int minScale;
  public int maxScale;
  public boolean decimal;
  public String prefix;
  public String suffix;
  public String params;
  public boolean autoIncrement;
  public boolean caseSensitive;
  public boolean supportsPrecision;
  public boolean supportsScale;
  public long defaultPrecision;
  public int defaultScale;
  public int defaultDisplaySize;
  public boolean hidden;
  public int memory;
  
  static
  {
    Class localClass;
    try
    {
      localClass = JdbcUtils.loadUserClass("com.vividsolutions.jts.geom.Geometry");
    }
    catch (Exception localException)
    {
      localClass = null;
    }
    GEOMETRY_CLASS = localClass;
    for (int i = 0; i < 23; i++) {
      TYPES_BY_VALUE_TYPE.add(null);
    }
    add(0, 0, "Null", new DataType(), new String[] { "NULL" }, 0);
    
    add(13, 12, "String", createString(true), new String[] { "VARCHAR", "VARCHAR2", "NVARCHAR", "NVARCHAR2", "VARCHAR_CASESENSITIVE", "CHARACTER VARYING", "TID" }, 48);
    
    add(13, -1, "String", createString(true), new String[] { "LONGVARCHAR", "LONGNVARCHAR" }, 48);
    
    add(21, 1, "String", createString(true), new String[] { "CHAR", "CHARACTER", "NCHAR" }, 48);
    
    add(14, 12, "String", createString(false), new String[] { "VARCHAR_IGNORECASE" }, 48);
    
    add(1, 16, "Boolean", createDecimal(1, 1, 0, 5, false, false), new String[] { "BOOLEAN", "BIT", "BOOL" }, 0);
    
    add(2, -6, "Byte", createDecimal(3, 3, 0, 4, false, false), new String[] { "TINYINT" }, 1);
    
    add(3, 5, "Short", createDecimal(5, 5, 0, 6, false, false), new String[] { "SMALLINT", "YEAR", "INT2" }, 20);
    
    add(4, 4, "Int", createDecimal(10, 10, 0, 11, false, false), new String[] { "INTEGER", "INT", "MEDIUMINT", "INT4", "SIGNED" }, 20);
    
    add(4, 4, "Int", createDecimal(10, 10, 0, 11, false, true), new String[] { "SERIAL" }, 20);
    
    add(5, -5, "Long", createDecimal(19, 19, 0, 20, false, false), new String[] { "BIGINT", "INT8", "LONG" }, 24);
    
    add(5, -5, "Long", createDecimal(19, 19, 0, 20, false, true), new String[] { "IDENTITY", "BIGSERIAL" }, 24);
    
    add(6, 3, "BigDecimal", createDecimal(Integer.MAX_VALUE, 65535, 32767, 65535, true, false), new String[] { "DECIMAL", "DEC" }, 64);
    
    add(6, 2, "BigDecimal", createDecimal(Integer.MAX_VALUE, 65535, 32767, 65535, true, false), new String[] { "NUMERIC", "NUMBER" }, 64);
    
    add(8, 7, "Float", createDecimal(7, 7, 0, 15, false, false), new String[] { "REAL", "FLOAT4" }, 24);
    
    add(7, 8, "Double", createDecimal(17, 17, 0, 24, false, false), new String[] { "DOUBLE", "DOUBLE PRECISION" }, 24);
    
    add(7, 6, "Double", createDecimal(17, 17, 0, 24, false, false), new String[] { "FLOAT", "FLOAT8" }, 24);
    
    add(9, 92, "Time", createDate(6, "TIME", 0, 8), new String[] { "TIME" }, 56);
    
    add(10, 91, "Date", createDate(8, "DATE", 0, 10), new String[] { "DATE" }, 56);
    
    add(11, 93, "Timestamp", createDate(23, "TIMESTAMP", 10, 23), new String[] { "TIMESTAMP", "DATETIME", "DATETIME2", "SMALLDATETIME" }, 56);
    
    add(12, -3, "Bytes", createString(false), new String[] { "VARBINARY" }, 32);
    
    add(12, -2, "Bytes", createString(false), new String[] { "BINARY", "RAW", "BYTEA", "LONG RAW" }, 32);
    
    add(12, -4, "Bytes", createString(false), new String[] { "LONGVARBINARY" }, 32);
    
    add(20, -2, "Bytes", createString(false), new String[] { "UUID" }, 32);
    
    add(19, 1111, "Object", createString(false), new String[] { "OTHER", "OBJECT", "JAVA_OBJECT" }, 24);
    
    add(15, 2004, "Blob", createLob(), new String[] { "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB", "IMAGE", "OID" }, 104);
    
    add(16, 2005, "Clob", createLob(), new String[] { "CLOB", "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT", "NTEXT", "NCLOB" }, 104);
    
    add(22, 1111, "Geometry", createString(false), new String[] { "GEOMETRY" }, 32);
    
    DataType localDataType1 = new DataType();
    localDataType1.prefix = "(";
    localDataType1.suffix = "')";
    add(17, 2003, "Array", localDataType1, new String[] { "ARRAY" }, 32);
    
    localDataType1 = new DataType();
    add(18, -10, "ResultSet", localDataType1, new String[] { "RESULT_SET" }, 400);
    
    int j = 0;
    for (int k = TYPES_BY_VALUE_TYPE.size(); j < k; j++)
    {
      DataType localDataType2 = (DataType)TYPES_BY_VALUE_TYPE.get(j);
      if (localDataType2 == null) {
        DbException.throwInternalError("unmapped type " + j);
      }
      Value.getOrder(j);
    }
  }
  
  private static void add(int paramInt1, int paramInt2, String paramString, DataType paramDataType, String[] paramArrayOfString, int paramInt3)
  {
    for (int i = 0; i < paramArrayOfString.length; i++)
    {
      DataType localDataType1 = new DataType();
      localDataType1.type = paramInt1;
      localDataType1.sqlType = paramInt2;
      localDataType1.jdbc = paramString;
      localDataType1.name = paramArrayOfString[i];
      localDataType1.autoIncrement = paramDataType.autoIncrement;
      localDataType1.decimal = paramDataType.decimal;
      localDataType1.maxPrecision = paramDataType.maxPrecision;
      localDataType1.maxScale = paramDataType.maxScale;
      localDataType1.minScale = paramDataType.minScale;
      localDataType1.params = paramDataType.params;
      localDataType1.prefix = paramDataType.prefix;
      localDataType1.suffix = paramDataType.suffix;
      localDataType1.supportsPrecision = paramDataType.supportsPrecision;
      localDataType1.supportsScale = paramDataType.supportsScale;
      localDataType1.defaultPrecision = paramDataType.defaultPrecision;
      localDataType1.defaultScale = paramDataType.defaultScale;
      localDataType1.defaultDisplaySize = paramDataType.defaultDisplaySize;
      localDataType1.caseSensitive = paramDataType.caseSensitive;
      localDataType1.hidden = (i > 0);
      localDataType1.memory = paramInt3;
      for (DataType localDataType2 : TYPES) {
        if (localDataType2.sqlType == localDataType1.sqlType) {
          localDataType1.sqlTypePos += 1;
        }
      }
      TYPES_BY_NAME.put(localDataType1.name, localDataType1);
      if (TYPES_BY_VALUE_TYPE.get(paramInt1) == null) {
        TYPES_BY_VALUE_TYPE.set(paramInt1, localDataType1);
      }
      TYPES.add(localDataType1);
    }
  }
  
  private static DataType createDecimal(int paramInt1, int paramInt2, int paramInt3, int paramInt4, boolean paramBoolean1, boolean paramBoolean2)
  {
    DataType localDataType = new DataType();
    localDataType.maxPrecision = paramInt1;
    localDataType.defaultPrecision = paramInt2;
    localDataType.defaultScale = paramInt3;
    localDataType.defaultDisplaySize = paramInt4;
    if (paramBoolean1)
    {
      localDataType.params = "PRECISION,SCALE";
      localDataType.supportsPrecision = true;
      localDataType.supportsScale = true;
    }
    localDataType.decimal = true;
    localDataType.autoIncrement = paramBoolean2;
    return localDataType;
  }
  
  private static DataType createDate(int paramInt1, String paramString, int paramInt2, int paramInt3)
  {
    DataType localDataType = new DataType();
    localDataType.prefix = (paramString + " '");
    localDataType.suffix = "'";
    localDataType.maxPrecision = paramInt1;
    localDataType.supportsScale = (paramInt2 != 0);
    localDataType.maxScale = paramInt2;
    localDataType.defaultPrecision = paramInt1;
    localDataType.defaultScale = paramInt2;
    localDataType.defaultDisplaySize = paramInt3;
    return localDataType;
  }
  
  private static DataType createString(boolean paramBoolean)
  {
    DataType localDataType = new DataType();
    localDataType.prefix = "'";
    localDataType.suffix = "'";
    localDataType.params = "LENGTH";
    localDataType.caseSensitive = paramBoolean;
    localDataType.supportsPrecision = true;
    localDataType.maxPrecision = 2147483647L;
    localDataType.defaultPrecision = 2147483647L;
    localDataType.defaultDisplaySize = Integer.MAX_VALUE;
    return localDataType;
  }
  
  private static DataType createLob()
  {
    DataType localDataType = createString(true);
    localDataType.maxPrecision = Long.MAX_VALUE;
    localDataType.defaultPrecision = Long.MAX_VALUE;
    return localDataType;
  }
  
  public static ArrayList<DataType> getTypes()
  {
    return TYPES;
  }
  
  public static Value readValue(SessionInterface paramSessionInterface, ResultSet paramResultSet, int paramInt1, int paramInt2)
  {
    try
    {
      byte[] arrayOfByte;
      Object localObject1;
      Object localObject2;
      Object localObject3;
      switch (paramInt2)
      {
      case 0: 
        return ValueNull.INSTANCE;
      case 12: 
        arrayOfByte = paramResultSet.getBytes(paramInt1);
        localObject1 = arrayOfByte == null ? ValueNull.INSTANCE : ValueBytes.getNoCopy(arrayOfByte);
        
        break;
      case 20: 
        arrayOfByte = paramResultSet.getBytes(paramInt1);
        localObject1 = arrayOfByte == null ? ValueNull.INSTANCE : ValueUuid.get(arrayOfByte);
        
        break;
      case 1: 
        boolean bool = paramResultSet.getBoolean(paramInt1);
        localObject1 = paramResultSet.wasNull() ? ValueNull.INSTANCE : ValueBoolean.get(bool);
        
        break;
      case 2: 
        byte b = paramResultSet.getByte(paramInt1);
        localObject1 = paramResultSet.wasNull() ? ValueNull.INSTANCE : ValueByte.get(b);
        
        break;
      case 10: 
        localObject2 = paramResultSet.getDate(paramInt1);
        localObject1 = localObject2 == null ? ValueNull.INSTANCE : ValueDate.get((java.sql.Date)localObject2);
        
        break;
      case 9: 
        localObject2 = paramResultSet.getTime(paramInt1);
        localObject1 = localObject2 == null ? ValueNull.INSTANCE : ValueTime.get((Time)localObject2);
        
        break;
      case 11: 
        localObject2 = paramResultSet.getTimestamp(paramInt1);
        localObject1 = localObject2 == null ? ValueNull.INSTANCE : ValueTimestamp.get((Timestamp)localObject2);
        
        break;
      case 6: 
        localObject2 = paramResultSet.getBigDecimal(paramInt1);
        localObject1 = localObject2 == null ? ValueNull.INSTANCE : ValueDecimal.get((BigDecimal)localObject2);
        
        break;
      case 7: 
        double d = paramResultSet.getDouble(paramInt1);
        localObject1 = paramResultSet.wasNull() ? ValueNull.INSTANCE : ValueDouble.get(d);
        
        break;
      case 8: 
        float f = paramResultSet.getFloat(paramInt1);
        localObject1 = paramResultSet.wasNull() ? ValueNull.INSTANCE : ValueFloat.get(f);
        
        break;
      case 4: 
        int i = paramResultSet.getInt(paramInt1);
        localObject1 = paramResultSet.wasNull() ? ValueNull.INSTANCE : ValueInt.get(i);
        
        break;
      case 5: 
        long l = paramResultSet.getLong(paramInt1);
        localObject1 = paramResultSet.wasNull() ? ValueNull.INSTANCE : ValueLong.get(l);
        
        break;
      case 3: 
        short s = paramResultSet.getShort(paramInt1);
        localObject1 = paramResultSet.wasNull() ? ValueNull.INSTANCE : ValueShort.get(s);
        
        break;
      case 14: 
        localObject3 = paramResultSet.getString(paramInt1);
        localObject1 = localObject3 == null ? ValueNull.INSTANCE : ValueStringIgnoreCase.get((String)localObject3);
        
        break;
      case 21: 
        localObject3 = paramResultSet.getString(paramInt1);
        localObject1 = localObject3 == null ? ValueNull.INSTANCE : ValueStringFixed.get((String)localObject3);
        
        break;
      case 13: 
        localObject3 = paramResultSet.getString(paramInt1);
        localObject1 = localObject3 == null ? ValueNull.INSTANCE : ValueString.get((String)localObject3);
        
        break;
      case 16: 
        if (paramSessionInterface == null)
        {
          localObject1 = ValueLobDb.createSmallLob(16, paramResultSet.getString(paramInt1).getBytes(Constants.UTF8));
        }
        else
        {
          localObject3 = paramResultSet.getCharacterStream(paramInt1);
          if (localObject3 == null) {
            localObject1 = ValueNull.INSTANCE;
          } else {
            localObject1 = paramSessionInterface.getDataHandler().getLobStorage().createClob(new BufferedReader((Reader)localObject3), -1L);
          }
        }
        break;
      case 15: 
        if (paramSessionInterface == null)
        {
          localObject1 = ValueLobDb.createSmallLob(15, paramResultSet.getBytes(paramInt1));
        }
        else
        {
          localObject3 = paramResultSet.getBinaryStream(paramInt1);
          localObject1 = localObject3 == null ? ValueNull.INSTANCE : paramSessionInterface.getDataHandler().getLobStorage().createBlob((InputStream)localObject3, -1L);
        }
        break;
      case 19: 
        if (SysProperties.serializeJavaObject)
        {
          localObject3 = paramResultSet.getBytes(paramInt1);
          localObject1 = localObject3 == null ? ValueNull.INSTANCE : ValueJavaObject.getNoCopy(null, (byte[])localObject3, paramSessionInterface.getDataHandler());
        }
        else
        {
          localObject3 = paramResultSet.getObject(paramInt1);
          localObject1 = localObject3 == null ? ValueNull.INSTANCE : ValueJavaObject.getNoCopy(localObject3, null, paramSessionInterface.getDataHandler());
        }
        break;
      case 17: 
        localObject3 = paramResultSet.getArray(paramInt1);
        if (localObject3 == null) {
          return ValueNull.INSTANCE;
        }
        Object[] arrayOfObject = (Object[])((Array)localObject3).getArray();
        if (arrayOfObject == null) {
          return ValueNull.INSTANCE;
        }
        int j = arrayOfObject.length;
        Value[] arrayOfValue = new Value[j];
        for (int k = 0; k < j; k++) {
          arrayOfValue[k] = convertToValue(paramSessionInterface, arrayOfObject[k], 0);
        }
        localObject1 = ValueArray.get(arrayOfValue);
        break;
      case 18: 
        localObject3 = (ResultSet)paramResultSet.getObject(paramInt1);
        if (localObject3 == null) {
          return ValueNull.INSTANCE;
        }
        return ValueResultSet.get(paramResultSet);
      case 22: 
        localObject3 = paramResultSet.getObject(paramInt1);
        if (localObject3 == null) {
          return ValueNull.INSTANCE;
        }
        return ValueGeometry.getFromGeometry(localObject3);
      default: 
        throw DbException.throwInternalError("type=" + paramInt2);
      }
      return (Value)localObject1;
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
  
  public static String getTypeClassName(int paramInt)
  {
    switch (paramInt)
    {
    case 1: 
      return Boolean.class.getName();
    case 2: 
      return Byte.class.getName();
    case 3: 
      return Short.class.getName();
    case 4: 
      return Integer.class.getName();
    case 5: 
      return Long.class.getName();
    case 6: 
      return BigDecimal.class.getName();
    case 9: 
      return Time.class.getName();
    case 10: 
      return java.sql.Date.class.getName();
    case 11: 
      return Timestamp.class.getName();
    case 12: 
    case 20: 
      return byte[].class.getName();
    case 13: 
    case 14: 
    case 21: 
      return String.class.getName();
    case 15: 
      return Blob.class.getName();
    case 16: 
      return Clob.class.getName();
    case 7: 
      return Double.class.getName();
    case 8: 
      return Float.class.getName();
    case 0: 
      return null;
    case 19: 
      return Object.class.getName();
    case -1: 
      return Object.class.getName();
    case 17: 
      return Array.class.getName();
    case 18: 
      return ResultSet.class.getName();
    case 22: 
      return "com.vividsolutions.jts.geom.Geometry";
    }
    throw DbException.throwInternalError("type=" + paramInt);
  }
  
  public static DataType getDataType(int paramInt)
  {
    if (paramInt == -1) {
      throw DbException.get(50004, "?");
    }
    DataType localDataType = (DataType)TYPES_BY_VALUE_TYPE.get(paramInt);
    if (localDataType == null) {
      localDataType = (DataType)TYPES_BY_VALUE_TYPE.get(0);
    }
    return localDataType;
  }
  
  public static int convertTypeToSQLType(int paramInt)
  {
    return getDataType(paramInt).sqlType;
  }
  
  private static int convertSQLTypeToValueType(int paramInt, String paramString)
  {
    switch (paramInt)
    {
    case 1111: 
    case 2000: 
      if (paramString.equalsIgnoreCase("geometry")) {
        return 22;
      }
      break;
    }
    return convertSQLTypeToValueType(paramInt);
  }
  
  public static int getValueTypeFromResultSet(ResultSetMetaData paramResultSetMetaData, int paramInt)
    throws SQLException
  {
    return convertSQLTypeToValueType(paramResultSetMetaData.getColumnType(paramInt), paramResultSetMetaData.getColumnTypeName(paramInt));
  }
  
  public static int convertSQLTypeToValueType(int paramInt)
  {
    switch (paramInt)
    {
    case -15: 
    case 1: 
      return 21;
    case -16: 
    case -9: 
    case -1: 
    case 12: 
      return 13;
    case 2: 
    case 3: 
      return 6;
    case -7: 
    case 16: 
      return 1;
    case 4: 
      return 4;
    case 5: 
      return 3;
    case -6: 
      return 2;
    case -5: 
      return 5;
    case 7: 
      return 8;
    case 6: 
    case 8: 
      return 7;
    case -4: 
    case -3: 
    case -2: 
      return 12;
    case 1111: 
    case 2000: 
      return 19;
    case 91: 
      return 10;
    case 92: 
      return 9;
    case 93: 
      return 11;
    case 2004: 
      return 15;
    case 2005: 
    case 2011: 
      return 16;
    case 0: 
      return 0;
    case 2003: 
      return 17;
    case -10: 
      return 18;
    }
    throw DbException.get(50004, "" + paramInt);
  }
  
  public static int getTypeFromClass(Class<?> paramClass)
  {
    if ((paramClass == null) || (Void.TYPE == paramClass)) {
      return 0;
    }
    if (paramClass.isPrimitive()) {
      paramClass = Utils.getNonPrimitiveClass(paramClass);
    }
    if (String.class == paramClass) {
      return 13;
    }
    if (Integer.class == paramClass) {
      return 4;
    }
    if (Long.class == paramClass) {
      return 5;
    }
    if (Boolean.class == paramClass) {
      return 1;
    }
    if (Double.class == paramClass) {
      return 7;
    }
    if (Byte.class == paramClass) {
      return 2;
    }
    if (Short.class == paramClass) {
      return 3;
    }
    if (Character.class == paramClass) {
      throw DbException.get(22018, "char (not supported)");
    }
    if (Float.class == paramClass) {
      return 8;
    }
    if (byte[].class == paramClass) {
      return 12;
    }
    if (UUID.class == paramClass) {
      return 20;
    }
    if (Void.class == paramClass) {
      return 0;
    }
    if (BigDecimal.class.isAssignableFrom(paramClass)) {
      return 6;
    }
    if (ResultSet.class.isAssignableFrom(paramClass)) {
      return 18;
    }
    if (Value.ValueBlob.class.isAssignableFrom(paramClass)) {
      return 15;
    }
    if (Value.ValueClob.class.isAssignableFrom(paramClass)) {
      return 16;
    }
    if (java.sql.Date.class.isAssignableFrom(paramClass)) {
      return 10;
    }
    if (Time.class.isAssignableFrom(paramClass)) {
      return 9;
    }
    if (Timestamp.class.isAssignableFrom(paramClass)) {
      return 11;
    }
    if (java.util.Date.class.isAssignableFrom(paramClass)) {
      return 11;
    }
    if (Reader.class.isAssignableFrom(paramClass)) {
      return 16;
    }
    if (Clob.class.isAssignableFrom(paramClass)) {
      return 16;
    }
    if (InputStream.class.isAssignableFrom(paramClass)) {
      return 15;
    }
    if (Blob.class.isAssignableFrom(paramClass)) {
      return 15;
    }
    if (Object[].class.isAssignableFrom(paramClass)) {
      return 17;
    }
    if (isGeometryClass(paramClass)) {
      return 22;
    }
    return 19;
  }
  
  public static Value convertToValue(SessionInterface paramSessionInterface, Object paramObject, int paramInt)
  {
    if (paramObject == null) {
      return ValueNull.INSTANCE;
    }
    if (paramInt == 19) {
      return ValueJavaObject.getNoCopy(paramObject, null, paramSessionInterface.getDataHandler());
    }
    if ((paramObject instanceof String)) {
      return ValueString.get((String)paramObject);
    }
    if ((paramObject instanceof Value)) {
      return (Value)paramObject;
    }
    if ((paramObject instanceof Long)) {
      return ValueLong.get(((Long)paramObject).longValue());
    }
    if ((paramObject instanceof Integer)) {
      return ValueInt.get(((Integer)paramObject).intValue());
    }
    if ((paramObject instanceof BigDecimal)) {
      return ValueDecimal.get((BigDecimal)paramObject);
    }
    if ((paramObject instanceof Boolean)) {
      return ValueBoolean.get(((Boolean)paramObject).booleanValue());
    }
    if ((paramObject instanceof Byte)) {
      return ValueByte.get(((Byte)paramObject).byteValue());
    }
    if ((paramObject instanceof Short)) {
      return ValueShort.get(((Short)paramObject).shortValue());
    }
    if ((paramObject instanceof Float)) {
      return ValueFloat.get(((Float)paramObject).floatValue());
    }
    if ((paramObject instanceof Double)) {
      return ValueDouble.get(((Double)paramObject).doubleValue());
    }
    if ((paramObject instanceof byte[])) {
      return ValueBytes.get((byte[])paramObject);
    }
    if ((paramObject instanceof java.sql.Date)) {
      return ValueDate.get((java.sql.Date)paramObject);
    }
    if ((paramObject instanceof Time)) {
      return ValueTime.get((Time)paramObject);
    }
    if ((paramObject instanceof Timestamp)) {
      return ValueTimestamp.get((Timestamp)paramObject);
    }
    if ((paramObject instanceof java.util.Date)) {
      return ValueTimestamp.fromMillis(((java.util.Date)paramObject).getTime());
    }
    BufferedReader localBufferedReader;
    if ((paramObject instanceof Reader))
    {
      localBufferedReader = new BufferedReader((Reader)paramObject);
      return paramSessionInterface.getDataHandler().getLobStorage().createClob(localBufferedReader, -1L);
    }
    if ((paramObject instanceof Clob)) {
      try
      {
        localBufferedReader = new BufferedReader(((Clob)paramObject).getCharacterStream());
        
        return paramSessionInterface.getDataHandler().getLobStorage().createClob(localBufferedReader, -1L);
      }
      catch (SQLException localSQLException1)
      {
        throw DbException.convert(localSQLException1);
      }
    }
    if ((paramObject instanceof InputStream)) {
      return paramSessionInterface.getDataHandler().getLobStorage().createBlob((InputStream)paramObject, -1L);
    }
    if ((paramObject instanceof Blob)) {
      try
      {
        return paramSessionInterface.getDataHandler().getLobStorage().createBlob(((Blob)paramObject).getBinaryStream(), -1L);
      }
      catch (SQLException localSQLException2)
      {
        throw DbException.convert(localSQLException2);
      }
    }
    if ((paramObject instanceof ResultSet))
    {
      if ((paramObject instanceof SimpleResultSet)) {
        return ValueResultSet.get((ResultSet)paramObject);
      }
      return ValueResultSet.getCopy((ResultSet)paramObject, Integer.MAX_VALUE);
    }
    Object localObject;
    if ((paramObject instanceof UUID))
    {
      localObject = (UUID)paramObject;
      return ValueUuid.get(((UUID)localObject).getMostSignificantBits(), ((UUID)localObject).getLeastSignificantBits());
    }
    if ((paramObject instanceof Object[]))
    {
      localObject = (Object[])paramObject;
      int i = localObject.length;
      Value[] arrayOfValue = new Value[i];
      for (int j = 0; j < i; j++) {
        arrayOfValue[j] = convertToValue(paramSessionInterface, localObject[j], paramInt);
      }
      return ValueArray.get(paramObject.getClass().getComponentType(), arrayOfValue);
    }
    if ((paramObject instanceof Character)) {
      return ValueStringFixed.get(((Character)paramObject).toString());
    }
    if (isGeometry(paramObject)) {
      return ValueGeometry.getFromGeometry(paramObject);
    }
    return ValueJavaObject.getNoCopy(paramObject, null, paramSessionInterface.getDataHandler());
  }
  
  public static boolean isGeometryClass(Class<?> paramClass)
  {
    if ((paramClass == null) || (GEOMETRY_CLASS == null)) {
      return false;
    }
    return GEOMETRY_CLASS.isAssignableFrom(paramClass);
  }
  
  public static boolean isGeometry(Object paramObject)
  {
    if (paramObject == null) {
      return false;
    }
    return isGeometryClass(paramObject.getClass());
  }
  
  public static DataType getTypeByName(String paramString)
  {
    return (DataType)TYPES_BY_NAME.get(paramString);
  }
  
  public static boolean isLargeObject(int paramInt)
  {
    if ((paramInt == 15) || (paramInt == 16)) {
      return true;
    }
    return false;
  }
  
  public static boolean isStringType(int paramInt)
  {
    if ((paramInt == 13) || (paramInt == 21) || (paramInt == 14)) {
      return true;
    }
    return false;
  }
  
  public static boolean supportsAdd(int paramInt)
  {
    switch (paramInt)
    {
    case 2: 
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
      return true;
    }
    return false;
  }
  
  public static int getAddProofType(int paramInt)
  {
    switch (paramInt)
    {
    case 2: 
      return 5;
    case 8: 
      return 7;
    case 4: 
      return 5;
    case 5: 
      return 6;
    case 3: 
      return 5;
    }
    return paramInt;
  }
  
  public static Object getDefaultForPrimitiveType(Class<?> paramClass)
  {
    if (paramClass == Boolean.TYPE) {
      return Boolean.FALSE;
    }
    if (paramClass == Byte.TYPE) {
      return Byte.valueOf((byte)0);
    }
    if (paramClass == Character.TYPE) {
      return Character.valueOf('\000');
    }
    if (paramClass == Short.TYPE) {
      return Short.valueOf((short)0);
    }
    if (paramClass == Integer.TYPE) {
      return Integer.valueOf(0);
    }
    if (paramClass == Long.TYPE) {
      return Long.valueOf(0L);
    }
    if (paramClass == Float.TYPE) {
      return Float.valueOf(0.0F);
    }
    if (paramClass == Double.TYPE) {
      return Double.valueOf(0.0D);
    }
    throw DbException.throwInternalError("primitive=" + paramClass.toString());
  }
  
  public static Object convertTo(JdbcConnection paramJdbcConnection, Value paramValue, Class<?> paramClass)
  {
    if (paramClass == Blob.class) {
      return new JdbcBlob(paramJdbcConnection, paramValue, 0);
    }
    if (paramClass == Clob.class) {
      return new JdbcClob(paramJdbcConnection, paramValue, 0);
    }
    if (paramValue.getType() == 19)
    {
      Object localObject = SysProperties.serializeJavaObject ? JdbcUtils.deserialize(paramValue.getBytes(), paramJdbcConnection.getSession().getDataHandler()) : paramValue.getObject();
      if (paramClass.isAssignableFrom(localObject.getClass())) {
        return localObject;
      }
    }
    throw DbException.getUnsupportedException(paramClass.getName());
  }
}
