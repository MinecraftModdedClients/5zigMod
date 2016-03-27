package org.h2.command.dml;

import java.util.ArrayList;
import org.h2.util.New;

public class SetTypes
{
  public static final int IGNORECASE = 1;
  public static final int MAX_LOG_SIZE = 2;
  public static final int MODE = 3;
  public static final int READONLY = 4;
  public static final int LOCK_TIMEOUT = 5;
  public static final int DEFAULT_LOCK_TIMEOUT = 6;
  public static final int DEFAULT_TABLE_TYPE = 7;
  public static final int CACHE_SIZE = 8;
  public static final int TRACE_LEVEL_SYSTEM_OUT = 9;
  public static final int TRACE_LEVEL_FILE = 10;
  public static final int TRACE_MAX_FILE_SIZE = 11;
  public static final int COLLATION = 12;
  public static final int CLUSTER = 13;
  public static final int WRITE_DELAY = 14;
  public static final int DATABASE_EVENT_LISTENER = 15;
  public static final int MAX_MEMORY_ROWS = 16;
  public static final int LOCK_MODE = 17;
  public static final int DB_CLOSE_DELAY = 18;
  public static final int LOG = 19;
  public static final int THROTTLE = 20;
  public static final int MAX_MEMORY_UNDO = 21;
  public static final int MAX_LENGTH_INPLACE_LOB = 22;
  public static final int COMPRESS_LOB = 23;
  public static final int ALLOW_LITERALS = 24;
  public static final int MULTI_THREADED = 25;
  public static final int SCHEMA = 26;
  public static final int OPTIMIZE_REUSE_RESULTS = 27;
  public static final int SCHEMA_SEARCH_PATH = 28;
  public static final int UNDO_LOG = 29;
  public static final int REFERENTIAL_INTEGRITY = 30;
  public static final int MVCC = 31;
  public static final int MAX_OPERATION_MEMORY = 32;
  public static final int EXCLUSIVE = 33;
  public static final int CREATE_BUILD = 34;
  public static final int VARIABLE = 35;
  public static final int QUERY_TIMEOUT = 36;
  public static final int REDO_LOG_BINARY = 37;
  public static final int BINARY_COLLATION = 38;
  public static final int JAVA_OBJECT_SERIALIZER = 39;
  public static final int RETENTION_TIME = 40;
  public static final int QUERY_STATISTICS = 41;
  private static final ArrayList<String> TYPES = ;
  
  static
  {
    ArrayList localArrayList = TYPES;
    localArrayList.add(null);
    localArrayList.add(1, "IGNORECASE");
    localArrayList.add(2, "MAX_LOG_SIZE");
    localArrayList.add(3, "MODE");
    localArrayList.add(4, "READONLY");
    localArrayList.add(5, "LOCK_TIMEOUT");
    localArrayList.add(6, "DEFAULT_LOCK_TIMEOUT");
    localArrayList.add(7, "DEFAULT_TABLE_TYPE");
    localArrayList.add(8, "CACHE_SIZE");
    localArrayList.add(9, "TRACE_LEVEL_SYSTEM_OUT");
    localArrayList.add(10, "TRACE_LEVEL_FILE");
    localArrayList.add(11, "TRACE_MAX_FILE_SIZE");
    localArrayList.add(12, "COLLATION");
    localArrayList.add(13, "CLUSTER");
    localArrayList.add(14, "WRITE_DELAY");
    localArrayList.add(15, "DATABASE_EVENT_LISTENER");
    localArrayList.add(16, "MAX_MEMORY_ROWS");
    localArrayList.add(17, "LOCK_MODE");
    localArrayList.add(18, "DB_CLOSE_DELAY");
    localArrayList.add(19, "LOG");
    localArrayList.add(20, "THROTTLE");
    localArrayList.add(21, "MAX_MEMORY_UNDO");
    localArrayList.add(22, "MAX_LENGTH_INPLACE_LOB");
    localArrayList.add(23, "COMPRESS_LOB");
    localArrayList.add(24, "ALLOW_LITERALS");
    localArrayList.add(25, "MULTI_THREADED");
    localArrayList.add(26, "SCHEMA");
    localArrayList.add(27, "OPTIMIZE_REUSE_RESULTS");
    localArrayList.add(28, "SCHEMA_SEARCH_PATH");
    localArrayList.add(29, "UNDO_LOG");
    localArrayList.add(30, "REFERENTIAL_INTEGRITY");
    localArrayList.add(31, "MVCC");
    localArrayList.add(32, "MAX_OPERATION_MEMORY");
    localArrayList.add(33, "EXCLUSIVE");
    localArrayList.add(34, "CREATE_BUILD");
    localArrayList.add(35, "@");
    localArrayList.add(36, "QUERY_TIMEOUT");
    localArrayList.add(37, "REDO_LOG_BINARY");
    localArrayList.add(38, "BINARY_COLLATION");
    localArrayList.add(39, "JAVA_OBJECT_SERIALIZER");
    localArrayList.add(40, "RETENTION_TIME");
    localArrayList.add(41, "QUERY_STATISTICS");
  }
  
  public static int getType(String paramString)
  {
    for (int i = 0; i < getTypes().size(); i++) {
      if (paramString.equals(getTypes().get(i))) {
        return i;
      }
    }
    return -1;
  }
  
  public static ArrayList<String> getTypes()
  {
    return TYPES;
  }
  
  public static String getTypeName(int paramInt)
  {
    return (String)getTypes().get(paramInt);
  }
}
