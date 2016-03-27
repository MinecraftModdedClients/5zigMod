package org.h2.expression;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.PatternSyntaxException;
import org.h2.command.Command;
import org.h2.command.Parser;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Mode;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.schema.Schema;
import org.h2.schema.Sequence;
import org.h2.security.BlockCipher;
import org.h2.security.CipherFactory;
import org.h2.security.SHA256;
import org.h2.store.LobStorageInterface;
import org.h2.store.fs.FileUtils;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.LinkSchema;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.tools.CompressTool;
import org.h2.tools.Csv;
import org.h2.util.AutoCloseInputStream;
import org.h2.util.DateTimeUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.ToChar;
import org.h2.util.Utils;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueBytes;
import org.h2.value.ValueDate;
import org.h2.value.ValueDouble;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueResultSet;
import org.h2.value.ValueString;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimestamp;
import org.h2.value.ValueUuid;

public class Function
  extends Expression
  implements FunctionCall
{
  public static final int ABS = 0;
  public static final int ACOS = 1;
  public static final int ASIN = 2;
  public static final int ATAN = 3;
  public static final int ATAN2 = 4;
  public static final int BITAND = 5;
  public static final int BITOR = 6;
  public static final int BITXOR = 7;
  public static final int CEILING = 8;
  public static final int COS = 9;
  public static final int COT = 10;
  public static final int DEGREES = 11;
  public static final int EXP = 12;
  public static final int FLOOR = 13;
  public static final int LOG = 14;
  public static final int LOG10 = 15;
  public static final int MOD = 16;
  public static final int PI = 17;
  public static final int POWER = 18;
  public static final int RADIANS = 19;
  public static final int RAND = 20;
  public static final int ROUND = 21;
  public static final int ROUNDMAGIC = 22;
  public static final int SIGN = 23;
  public static final int SIN = 24;
  public static final int SQRT = 25;
  public static final int TAN = 26;
  public static final int TRUNCATE = 27;
  public static final int SECURE_RAND = 28;
  public static final int HASH = 29;
  public static final int ENCRYPT = 30;
  public static final int DECRYPT = 31;
  public static final int COMPRESS = 32;
  public static final int EXPAND = 33;
  public static final int ZERO = 34;
  public static final int RANDOM_UUID = 35;
  public static final int COSH = 36;
  public static final int SINH = 37;
  public static final int TANH = 38;
  public static final int LN = 39;
  public static final int ASCII = 50;
  public static final int BIT_LENGTH = 51;
  public static final int CHAR = 52;
  public static final int CHAR_LENGTH = 53;
  public static final int CONCAT = 54;
  public static final int DIFFERENCE = 55;
  public static final int HEXTORAW = 56;
  public static final int INSERT = 57;
  public static final int INSTR = 58;
  public static final int LCASE = 59;
  public static final int LEFT = 60;
  public static final int LENGTH = 61;
  public static final int LOCATE = 62;
  public static final int LTRIM = 63;
  public static final int OCTET_LENGTH = 64;
  public static final int RAWTOHEX = 65;
  public static final int REPEAT = 66;
  public static final int REPLACE = 67;
  public static final int RIGHT = 68;
  public static final int RTRIM = 69;
  public static final int SOUNDEX = 70;
  public static final int SPACE = 71;
  public static final int SUBSTR = 72;
  public static final int SUBSTRING = 73;
  public static final int UCASE = 74;
  public static final int LOWER = 75;
  public static final int UPPER = 76;
  public static final int POSITION = 77;
  public static final int TRIM = 78;
  public static final int STRINGENCODE = 79;
  public static final int STRINGDECODE = 80;
  public static final int STRINGTOUTF8 = 81;
  public static final int UTF8TOSTRING = 82;
  public static final int XMLATTR = 83;
  public static final int XMLNODE = 84;
  public static final int XMLCOMMENT = 85;
  public static final int XMLCDATA = 86;
  public static final int XMLSTARTDOC = 87;
  public static final int XMLTEXT = 88;
  public static final int REGEXP_REPLACE = 89;
  public static final int RPAD = 90;
  public static final int LPAD = 91;
  public static final int CONCAT_WS = 92;
  public static final int TO_CHAR = 93;
  public static final int TRANSLATE = 94;
  public static final int CURDATE = 100;
  public static final int CURTIME = 101;
  public static final int DATE_ADD = 102;
  public static final int DATE_DIFF = 103;
  public static final int DAY_NAME = 104;
  public static final int DAY_OF_MONTH = 105;
  public static final int DAY_OF_WEEK = 106;
  public static final int DAY_OF_YEAR = 107;
  public static final int HOUR = 108;
  public static final int MINUTE = 109;
  public static final int MONTH = 110;
  public static final int MONTH_NAME = 111;
  public static final int NOW = 112;
  public static final int QUARTER = 113;
  public static final int SECOND = 114;
  public static final int WEEK = 115;
  public static final int YEAR = 116;
  public static final int CURRENT_DATE = 117;
  public static final int CURRENT_TIME = 118;
  public static final int CURRENT_TIMESTAMP = 119;
  public static final int EXTRACT = 120;
  public static final int FORMATDATETIME = 121;
  public static final int PARSEDATETIME = 122;
  public static final int ISO_YEAR = 123;
  public static final int ISO_WEEK = 124;
  public static final int ISO_DAY_OF_WEEK = 125;
  public static final int DATABASE = 150;
  public static final int USER = 151;
  public static final int CURRENT_USER = 152;
  public static final int IDENTITY = 153;
  public static final int SCOPE_IDENTITY = 154;
  public static final int AUTOCOMMIT = 155;
  public static final int READONLY = 156;
  public static final int DATABASE_PATH = 157;
  public static final int LOCK_TIMEOUT = 158;
  public static final int DISK_SPACE_USED = 159;
  public static final int IFNULL = 200;
  public static final int CASEWHEN = 201;
  public static final int CONVERT = 202;
  public static final int CAST = 203;
  public static final int COALESCE = 204;
  public static final int NULLIF = 205;
  public static final int CASE = 206;
  public static final int NEXTVAL = 207;
  public static final int CURRVAL = 208;
  public static final int ARRAY_GET = 209;
  public static final int CSVREAD = 210;
  public static final int CSVWRITE = 211;
  public static final int MEMORY_FREE = 212;
  public static final int MEMORY_USED = 213;
  public static final int LOCK_MODE = 214;
  public static final int SCHEMA = 215;
  public static final int SESSION_ID = 216;
  public static final int ARRAY_LENGTH = 217;
  public static final int LINK_SCHEMA = 218;
  public static final int GREATEST = 219;
  public static final int LEAST = 220;
  public static final int CANCEL_SESSION = 221;
  public static final int SET = 222;
  public static final int TABLE = 223;
  public static final int TABLE_DISTINCT = 224;
  public static final int FILE_READ = 225;
  public static final int TRANSACTION_ID = 226;
  public static final int TRUNCATE_VALUE = 227;
  public static final int NVL2 = 228;
  public static final int DECODE = 229;
  public static final int ARRAY_CONTAINS = 230;
  public static final int VALUES = 250;
  public static final int H2VERSION = 231;
  public static final int ROW_NUMBER = 300;
  private static final int VAR_ARGS = -1;
  private static final long PRECISION_UNKNOWN = -1L;
  private static final HashMap<String, FunctionInfo> FUNCTIONS = ;
  private static final HashMap<String, Integer> DATE_PART = New.hashMap();
  private static final char[] SOUNDEX_INDEX = new char[''];
  protected Expression[] args;
  private final FunctionInfo info;
  private ArrayList<Expression> varArgs;
  private int dataType;
  private int scale;
  private long precision = -1L;
  private int displaySize;
  private final Database database;
  
  static
  {
    DATE_PART.put("SQL_TSI_YEAR", Integer.valueOf(1));
    DATE_PART.put("YEAR", Integer.valueOf(1));
    DATE_PART.put("YYYY", Integer.valueOf(1));
    DATE_PART.put("YY", Integer.valueOf(1));
    DATE_PART.put("SQL_TSI_MONTH", Integer.valueOf(2));
    DATE_PART.put("MONTH", Integer.valueOf(2));
    DATE_PART.put("MM", Integer.valueOf(2));
    DATE_PART.put("M", Integer.valueOf(2));
    DATE_PART.put("SQL_TSI_WEEK", Integer.valueOf(3));
    DATE_PART.put("WW", Integer.valueOf(3));
    DATE_PART.put("WK", Integer.valueOf(3));
    DATE_PART.put("WEEK", Integer.valueOf(3));
    DATE_PART.put("DAY", Integer.valueOf(5));
    DATE_PART.put("DD", Integer.valueOf(5));
    DATE_PART.put("D", Integer.valueOf(5));
    DATE_PART.put("SQL_TSI_DAY", Integer.valueOf(5));
    DATE_PART.put("DAYOFYEAR", Integer.valueOf(6));
    DATE_PART.put("DAY_OF_YEAR", Integer.valueOf(6));
    DATE_PART.put("DY", Integer.valueOf(6));
    DATE_PART.put("DOY", Integer.valueOf(6));
    DATE_PART.put("SQL_TSI_HOUR", Integer.valueOf(11));
    DATE_PART.put("HOUR", Integer.valueOf(11));
    DATE_PART.put("HH", Integer.valueOf(11));
    DATE_PART.put("SQL_TSI_MINUTE", Integer.valueOf(12));
    DATE_PART.put("MINUTE", Integer.valueOf(12));
    DATE_PART.put("MI", Integer.valueOf(12));
    DATE_PART.put("N", Integer.valueOf(12));
    DATE_PART.put("SQL_TSI_SECOND", Integer.valueOf(13));
    DATE_PART.put("SECOND", Integer.valueOf(13));
    DATE_PART.put("SS", Integer.valueOf(13));
    DATE_PART.put("S", Integer.valueOf(13));
    DATE_PART.put("MILLISECOND", Integer.valueOf(14));
    DATE_PART.put("MS", Integer.valueOf(14));
    
    String str = "7AEIOUY8HW1BFPV2CGJKQSXZ3DT4L5MN6R";
    int i = 0;
    int j = 0;
    for (int k = str.length(); j < k; j++)
    {
      char c = str.charAt(j);
      if (c < '9')
      {
        i = c;
      }
      else
      {
        SOUNDEX_INDEX[c] = i;
        SOUNDEX_INDEX[Character.toLowerCase(c)] = i;
      }
    }
    addFunction("ABS", 0, 1, 0);
    addFunction("ACOS", 1, 1, 7);
    addFunction("ASIN", 2, 1, 7);
    addFunction("ATAN", 3, 1, 7);
    addFunction("ATAN2", 4, 2, 7);
    addFunction("BITAND", 5, 2, 5);
    addFunction("BITOR", 6, 2, 5);
    addFunction("BITXOR", 7, 2, 5);
    addFunction("CEILING", 8, 1, 7);
    addFunction("CEIL", 8, 1, 7);
    addFunction("COS", 9, 1, 7);
    addFunction("COSH", 36, 1, 7);
    addFunction("COT", 10, 1, 7);
    addFunction("DEGREES", 11, 1, 7);
    addFunction("EXP", 12, 1, 7);
    addFunction("FLOOR", 13, 1, 7);
    addFunction("LOG", 14, 1, 7);
    addFunction("LN", 39, 1, 7);
    addFunction("LOG10", 15, 1, 7);
    addFunction("MOD", 16, 2, 5);
    addFunction("PI", 17, 0, 7);
    addFunction("POWER", 18, 2, 7);
    addFunction("RADIANS", 19, 1, 7);
    
    addFunctionNotDeterministic("RAND", 20, -1, 7);
    addFunctionNotDeterministic("RANDOM", 20, -1, 7);
    addFunction("ROUND", 21, -1, 7);
    addFunction("ROUNDMAGIC", 22, 1, 7);
    addFunction("SIGN", 23, 1, 4);
    addFunction("SIN", 24, 1, 7);
    addFunction("SINH", 37, 1, 7);
    addFunction("SQRT", 25, 1, 7);
    addFunction("TAN", 26, 1, 7);
    addFunction("TANH", 38, 1, 7);
    addFunction("TRUNCATE", 27, -1, 0);
    
    addFunction("TRUNC", 27, -1, 0);
    addFunction("HASH", 29, 3, 12);
    addFunction("ENCRYPT", 30, 3, 12);
    addFunction("DECRYPT", 31, 3, 12);
    addFunctionNotDeterministic("SECURE_RAND", 28, 1, 12);
    addFunction("COMPRESS", 32, -1, 12);
    addFunction("EXPAND", 33, 1, 12);
    addFunction("ZERO", 34, 0, 4);
    addFunctionNotDeterministic("RANDOM_UUID", 35, 0, 20);
    addFunctionNotDeterministic("SYS_GUID", 35, 0, 20);
    
    addFunction("ASCII", 50, 1, 4);
    addFunction("BIT_LENGTH", 51, 1, 5);
    addFunction("CHAR", 52, 1, 13);
    addFunction("CHR", 52, 1, 13);
    addFunction("CHAR_LENGTH", 53, 1, 4);
    
    addFunction("CHARACTER_LENGTH", 53, 1, 4);
    addFunctionWithNull("CONCAT", 54, -1, 13);
    addFunctionWithNull("CONCAT_WS", 92, -1, 13);
    addFunction("DIFFERENCE", 55, 2, 4);
    addFunction("HEXTORAW", 56, 1, 13);
    addFunctionWithNull("INSERT", 57, 4, 13);
    addFunction("LCASE", 59, 1, 13);
    addFunction("LEFT", 60, 2, 13);
    addFunction("LENGTH", 61, 1, 5);
    
    addFunction("LOCATE", 62, -1, 4);
    
    addFunction("CHARINDEX", 62, -1, 4);
    
    addFunction("POSITION", 62, 2, 4);
    addFunction("INSTR", 58, -1, 4);
    addFunction("LTRIM", 63, -1, 13);
    addFunction("OCTET_LENGTH", 64, 1, 5);
    addFunction("RAWTOHEX", 65, 1, 13);
    addFunction("REPEAT", 66, 2, 13);
    addFunction("REPLACE", 67, -1, 13);
    addFunction("RIGHT", 68, 2, 13);
    addFunction("RTRIM", 69, -1, 13);
    addFunction("SOUNDEX", 70, 1, 13);
    addFunction("SPACE", 71, 1, 13);
    addFunction("SUBSTR", 72, -1, 13);
    addFunction("SUBSTRING", 73, -1, 13);
    addFunction("UCASE", 74, 1, 13);
    addFunction("LOWER", 75, 1, 13);
    addFunction("UPPER", 76, 1, 13);
    addFunction("POSITION", 77, 2, 4);
    addFunction("TRIM", 78, -1, 13);
    addFunction("STRINGENCODE", 79, 1, 13);
    addFunction("STRINGDECODE", 80, 1, 13);
    addFunction("STRINGTOUTF8", 81, 1, 12);
    addFunction("UTF8TOSTRING", 82, 1, 13);
    addFunction("XMLATTR", 83, 2, 13);
    addFunctionWithNull("XMLNODE", 84, -1, 13);
    addFunction("XMLCOMMENT", 85, 1, 13);
    addFunction("XMLCDATA", 86, 1, 13);
    addFunction("XMLSTARTDOC", 87, 0, 13);
    addFunction("XMLTEXT", 88, -1, 13);
    addFunction("REGEXP_REPLACE", 89, 3, 13);
    addFunction("RPAD", 90, -1, 13);
    addFunction("LPAD", 91, -1, 13);
    addFunction("TO_CHAR", 93, -1, 13);
    addFunction("TRANSLATE", 94, 3, 13);
    
    addFunctionNotDeterministic("CURRENT_DATE", 117, 0, 10);
    
    addFunctionNotDeterministic("CURDATE", 100, 0, 10);
    
    addFunctionNotDeterministic("GETDATE", 100, 0, 10);
    
    addFunctionNotDeterministic("CURRENT_TIME", 118, 0, 9);
    
    addFunctionNotDeterministic("CURTIME", 101, 0, 9);
    
    addFunctionNotDeterministic("CURRENT_TIMESTAMP", 119, -1, 11);
    
    addFunctionNotDeterministic("NOW", 112, -1, 11);
    
    addFunction("DATEADD", 102, 3, 11);
    
    addFunction("TIMESTAMPADD", 102, 3, 5);
    
    addFunction("DATEDIFF", 103, 3, 5);
    
    addFunction("TIMESTAMPDIFF", 103, 3, 5);
    
    addFunction("DAYNAME", 104, 1, 13);
    
    addFunction("DAYNAME", 104, 1, 13);
    
    addFunction("DAY", 105, 1, 4);
    
    addFunction("DAY_OF_MONTH", 105, 1, 4);
    
    addFunction("DAY_OF_WEEK", 106, 1, 4);
    
    addFunction("DAY_OF_YEAR", 107, 1, 4);
    
    addFunction("DAYOFMONTH", 105, 1, 4);
    
    addFunction("DAYOFWEEK", 106, 1, 4);
    
    addFunction("DAYOFYEAR", 107, 1, 4);
    
    addFunction("HOUR", 108, 1, 4);
    
    addFunction("MINUTE", 109, 1, 4);
    
    addFunction("MONTH", 110, 1, 4);
    
    addFunction("MONTHNAME", 111, 1, 13);
    
    addFunction("QUARTER", 113, 1, 4);
    
    addFunction("SECOND", 114, 1, 4);
    
    addFunction("WEEK", 115, 1, 4);
    
    addFunction("YEAR", 116, 1, 4);
    
    addFunction("EXTRACT", 120, 2, 4);
    
    addFunctionWithNull("FORMATDATETIME", 121, -1, 13);
    
    addFunctionWithNull("PARSEDATETIME", 122, -1, 11);
    
    addFunction("ISO_YEAR", 123, 1, 4);
    
    addFunction("ISO_WEEK", 124, 1, 4);
    
    addFunction("ISO_DAY_OF_WEEK", 125, 1, 4);
    
    addFunctionNotDeterministic("DATABASE", 150, 0, 13);
    
    addFunctionNotDeterministic("USER", 151, 0, 13);
    
    addFunctionNotDeterministic("CURRENT_USER", 152, 0, 13);
    
    addFunctionNotDeterministic("IDENTITY", 153, 0, 5);
    
    addFunctionNotDeterministic("SCOPE_IDENTITY", 154, 0, 5);
    
    addFunctionNotDeterministic("IDENTITY_VAL_LOCAL", 153, 0, 5);
    
    addFunctionNotDeterministic("LAST_INSERT_ID", 153, 0, 5);
    
    addFunctionNotDeterministic("LASTVAL", 153, 0, 5);
    
    addFunctionNotDeterministic("AUTOCOMMIT", 155, 0, 1);
    
    addFunctionNotDeterministic("READONLY", 156, 0, 1);
    
    addFunction("DATABASE_PATH", 157, 0, 13);
    
    addFunctionNotDeterministic("LOCK_TIMEOUT", 158, 0, 4);
    
    addFunctionWithNull("IFNULL", 200, 2, 0);
    
    addFunctionWithNull("ISNULL", 200, 2, 0);
    
    addFunctionWithNull("CASEWHEN", 201, 3, 0);
    
    addFunctionWithNull("CONVERT", 202, 1, 0);
    
    addFunctionWithNull("CAST", 203, 1, 0);
    
    addFunctionWithNull("TRUNCATE_VALUE", 227, 3, 0);
    
    addFunctionWithNull("COALESCE", 204, -1, 0);
    
    addFunctionWithNull("NVL", 204, -1, 0);
    
    addFunctionWithNull("NVL2", 228, 3, 0);
    
    addFunctionWithNull("NULLIF", 205, 2, 0);
    
    addFunctionWithNull("CASE", 206, -1, 0);
    
    addFunctionNotDeterministic("NEXTVAL", 207, -1, 5);
    
    addFunctionNotDeterministic("CURRVAL", 208, -1, 5);
    
    addFunction("ARRAY_GET", 209, 2, 13);
    
    addFunction("ARRAY_CONTAINS", 230, 2, 1, false, true, true);
    
    addFunction("CSVREAD", 210, -1, 18, false, false, false);
    
    addFunction("CSVWRITE", 211, -1, 4, false, false, true);
    
    addFunctionNotDeterministic("MEMORY_FREE", 212, 0, 4);
    
    addFunctionNotDeterministic("MEMORY_USED", 213, 0, 4);
    
    addFunctionNotDeterministic("LOCK_MODE", 214, 0, 4);
    
    addFunctionNotDeterministic("SCHEMA", 215, 0, 13);
    
    addFunctionNotDeterministic("SESSION_ID", 216, 0, 4);
    
    addFunction("ARRAY_LENGTH", 217, 1, 4);
    
    addFunctionNotDeterministic("LINK_SCHEMA", 218, 6, 18);
    
    addFunctionWithNull("LEAST", 220, -1, 0);
    
    addFunctionWithNull("GREATEST", 219, -1, 0);
    
    addFunctionNotDeterministic("CANCEL_SESSION", 221, 1, 1);
    
    addFunction("SET", 222, 2, 0, false, false, true);
    
    addFunction("FILE_READ", 225, -1, 0, false, false, true);
    
    addFunctionNotDeterministic("TRANSACTION_ID", 226, 0, 13);
    
    addFunctionWithNull("DECODE", 229, -1, 0);
    
    addFunctionNotDeterministic("DISK_SPACE_USED", 159, 1, 5);
    
    addFunction("H2VERSION", 231, 0, 13);
    
    addFunctionWithNull("TABLE", 223, -1, 18);
    
    addFunctionWithNull("TABLE_DISTINCT", 224, -1, 18);
    
    addFunctionWithNull("ROW_NUMBER", 300, 0, 5);
    
    addFunction("VALUES", 250, 1, 0, false, true, false);
  }
  
  protected Function(Database paramDatabase, FunctionInfo paramFunctionInfo)
  {
    this.database = paramDatabase;
    this.info = paramFunctionInfo;
    if (paramFunctionInfo.parameterCount == -1) {
      this.varArgs = New.arrayList();
    } else {
      this.args = new Expression[paramFunctionInfo.parameterCount];
    }
  }
  
  private static void addFunction(String paramString, int paramInt1, int paramInt2, int paramInt3, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    FunctionInfo localFunctionInfo = new FunctionInfo();
    localFunctionInfo.name = paramString;
    localFunctionInfo.type = paramInt1;
    localFunctionInfo.parameterCount = paramInt2;
    localFunctionInfo.dataType = paramInt3;
    localFunctionInfo.nullIfParameterIsNull = paramBoolean1;
    localFunctionInfo.deterministic = paramBoolean2;
    localFunctionInfo.bufferResultSetToLocalTemp = paramBoolean3;
    FUNCTIONS.put(paramString, localFunctionInfo);
  }
  
  private static void addFunctionNotDeterministic(String paramString, int paramInt1, int paramInt2, int paramInt3)
  {
    addFunction(paramString, paramInt1, paramInt2, paramInt3, true, false, true);
  }
  
  private static void addFunction(String paramString, int paramInt1, int paramInt2, int paramInt3)
  {
    addFunction(paramString, paramInt1, paramInt2, paramInt3, true, true, true);
  }
  
  private static void addFunctionWithNull(String paramString, int paramInt1, int paramInt2, int paramInt3)
  {
    addFunction(paramString, paramInt1, paramInt2, paramInt3, false, true, true);
  }
  
  private static FunctionInfo getFunctionInfo(String paramString)
  {
    return (FunctionInfo)FUNCTIONS.get(paramString);
  }
  
  public static Function getFunction(Database paramDatabase, String paramString)
  {
    if (!paramDatabase.getSettings().databaseToUpper) {
      paramString = StringUtils.toUpperEnglish(paramString);
    }
    FunctionInfo localFunctionInfo = getFunctionInfo(paramString);
    if (localFunctionInfo == null) {
      return null;
    }
    switch (localFunctionInfo.type)
    {
    case 223: 
    case 224: 
      return new TableFunction(paramDatabase, localFunctionInfo, Long.MAX_VALUE);
    }
    return new Function(paramDatabase, localFunctionInfo);
  }
  
  public void setParameter(int paramInt, Expression paramExpression)
  {
    if (this.varArgs != null)
    {
      this.varArgs.add(paramExpression);
    }
    else
    {
      if (paramInt >= this.args.length) {
        throw DbException.get(7001, new String[] { this.info.name, "" + this.args.length });
      }
      this.args[paramInt] = paramExpression;
    }
  }
  
  private static strictfp double log10(double paramDouble)
  {
    return roundMagic(StrictMath.log(paramDouble) / StrictMath.log(10.0D));
  }
  
  public Value getValue(Session paramSession)
  {
    return getValueWithArgs(paramSession, this.args);
  }
  
  private Value getSimpleValue(Session paramSession, Value paramValue, Expression[] paramArrayOfExpression, Value[] paramArrayOfValue)
  {
    Object localObject1;
    int i;
    Object localObject4;
    int i2;
    Object localObject5;
    SimpleDateFormat localSimpleDateFormat;
    long l;
    Object localObject2;
    int i3;
    Value localValue2;
    int k;
    Object localObject3;
    Value[] arrayOfValue2;
    switch (this.info.type)
    {
    case 0: 
      localObject1 = paramValue.getSignum() > 0 ? paramValue : paramValue.negate();
      break;
    case 1: 
      localObject1 = ValueDouble.get(Math.acos(paramValue.getDouble()));
      break;
    case 2: 
      localObject1 = ValueDouble.get(Math.asin(paramValue.getDouble()));
      break;
    case 3: 
      localObject1 = ValueDouble.get(Math.atan(paramValue.getDouble()));
      break;
    case 8: 
      localObject1 = ValueDouble.get(Math.ceil(paramValue.getDouble()));
      break;
    case 9: 
      localObject1 = ValueDouble.get(Math.cos(paramValue.getDouble()));
      break;
    case 36: 
      localObject1 = ValueDouble.get(Math.cosh(paramValue.getDouble()));
      break;
    case 10: 
      double d = Math.tan(paramValue.getDouble());
      if (d == 0.0D) {
        throw DbException.get(22012, getSQL());
      }
      localObject1 = ValueDouble.get(1.0D / d);
      break;
    case 11: 
      localObject1 = ValueDouble.get(Math.toDegrees(paramValue.getDouble()));
      break;
    case 12: 
      localObject1 = ValueDouble.get(Math.exp(paramValue.getDouble()));
      break;
    case 13: 
      localObject1 = ValueDouble.get(Math.floor(paramValue.getDouble()));
      break;
    case 39: 
      localObject1 = ValueDouble.get(Math.log(paramValue.getDouble()));
      break;
    case 14: 
      if (this.database.getMode().logIsLogBase10) {
        localObject1 = ValueDouble.get(Math.log10(paramValue.getDouble()));
      } else {
        localObject1 = ValueDouble.get(Math.log(paramValue.getDouble()));
      }
      break;
    case 15: 
      localObject1 = ValueDouble.get(log10(paramValue.getDouble()));
      break;
    case 17: 
      localObject1 = ValueDouble.get(3.141592653589793D);
      break;
    case 19: 
      localObject1 = ValueDouble.get(Math.toRadians(paramValue.getDouble()));
      break;
    case 20: 
      if (paramValue != null) {
        paramSession.getRandom().setSeed(paramValue.getInt());
      }
      localObject1 = ValueDouble.get(paramSession.getRandom().nextDouble());
      break;
    case 22: 
      localObject1 = ValueDouble.get(roundMagic(paramValue.getDouble()));
      break;
    case 23: 
      localObject1 = ValueInt.get(paramValue.getSignum());
      break;
    case 24: 
      localObject1 = ValueDouble.get(Math.sin(paramValue.getDouble()));
      break;
    case 37: 
      localObject1 = ValueDouble.get(Math.sinh(paramValue.getDouble()));
      break;
    case 25: 
      localObject1 = ValueDouble.get(Math.sqrt(paramValue.getDouble()));
      break;
    case 26: 
      localObject1 = ValueDouble.get(Math.tan(paramValue.getDouble()));
      break;
    case 38: 
      localObject1 = ValueDouble.get(Math.tanh(paramValue.getDouble()));
      break;
    case 28: 
      localObject1 = ValueBytes.getNoCopy(MathUtils.secureRandomBytes(paramValue.getInt()));
      
      break;
    case 33: 
      localObject1 = ValueBytes.getNoCopy(CompressTool.getInstance().expand(paramValue.getBytesNoCopy()));
      
      break;
    case 34: 
      localObject1 = ValueInt.get(0);
      break;
    case 35: 
      localObject1 = ValueUuid.getNewRandom();
      break;
    case 50: 
      String str1 = paramValue.getString();
      if (str1.length() == 0) {
        localObject1 = ValueNull.INSTANCE;
      } else {
        localObject1 = ValueInt.get(str1.charAt(0));
      }
      break;
    case 51: 
      localObject1 = ValueLong.get(16L * length(paramValue));
      break;
    case 52: 
      localObject1 = ValueString.get(String.valueOf((char)paramValue.getInt()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 53: 
    case 61: 
      localObject1 = ValueLong.get(length(paramValue));
      break;
    case 64: 
      localObject1 = ValueLong.get(2L * length(paramValue));
      break;
    case 54: 
    case 92: 
      localObject1 = ValueNull.INSTANCE;
      i = 0;
      localObject4 = "";
      if (this.info.type == 92)
      {
        i = 1;
        localObject4 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, 0).getString();
      }
      for (i2 = i; i2 < paramArrayOfExpression.length; i2++)
      {
        localObject5 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, i2);
        if (localObject5 != ValueNull.INSTANCE) {
          if (localObject1 == ValueNull.INSTANCE)
          {
            localObject1 = localObject5;
          }
          else
          {
            String str2 = ((Value)localObject5).getString();
            if ((!StringUtils.isNullOrEmpty((String)localObject4)) && (!StringUtils.isNullOrEmpty(str2))) {
              str2 = ((String)localObject4).concat(str2);
            }
            localObject1 = ValueString.get(((Value)localObject1).getString().concat(str2), this.database.getMode().treatEmptyStringsAsNull);
          }
        }
      }
      if ((this.info.type == 92) && 
        (localObject4 != null) && (localObject1 == ValueNull.INSTANCE)) {
        localObject1 = ValueString.get("", this.database.getMode().treatEmptyStringsAsNull);
      }
      break;
    case 56: 
      localObject1 = ValueString.get(hexToRaw(paramValue.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 59: 
    case 75: 
      localObject1 = ValueString.get(paramValue.getString().toLowerCase(), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 65: 
      localObject1 = ValueString.get(rawToHex(paramValue.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 70: 
      localObject1 = ValueString.get(getSoundex(paramValue.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 71: 
      i = Math.max(0, paramValue.getInt());
      localObject4 = new char[i];
      for (i2 = i - 1; i2 >= 0; i2--) {
        localObject4[i2] = 32;
      }
      localObject1 = ValueString.get(new String((char[])localObject4), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 74: 
    case 76: 
      localObject1 = ValueString.get(paramValue.getString().toUpperCase(), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 79: 
      localObject1 = ValueString.get(StringUtils.javaEncode(paramValue.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 80: 
      localObject1 = ValueString.get(StringUtils.javaDecode(paramValue.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 81: 
      localObject1 = ValueBytes.getNoCopy(paramValue.getString().getBytes(Constants.UTF8));
      
      break;
    case 82: 
      localObject1 = ValueString.get(new String(paramValue.getBytesNoCopy(), Constants.UTF8), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 85: 
      localObject1 = ValueString.get(StringUtils.xmlComment(paramValue.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 86: 
      localObject1 = ValueString.get(StringUtils.xmlCData(paramValue.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 87: 
      localObject1 = ValueString.get(StringUtils.xmlStartDoc(), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 104: 
      localSimpleDateFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
      
      localObject1 = ValueString.get(localSimpleDateFormat.format(paramValue.getDate()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 105: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getDate(), 5));
      
      break;
    case 106: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getDate(), 7));
      
      break;
    case 107: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getDate(), 6));
      
      break;
    case 108: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getTimestamp(), 11));
      
      break;
    case 109: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getTimestamp(), 12));
      
      break;
    case 110: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getDate(), 2));
      
      break;
    case 111: 
      localSimpleDateFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH);
      
      localObject1 = ValueString.get(localSimpleDateFormat.format(paramValue.getDate()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 113: 
      localObject1 = ValueInt.get((DateTimeUtils.getDatePart(paramValue.getDate(), 2) - 1) / 3 + 1);
      
      break;
    case 114: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getTimestamp(), 13));
      
      break;
    case 115: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getDate(), 3));
      
      break;
    case 116: 
      localObject1 = ValueInt.get(DateTimeUtils.getDatePart(paramValue.getDate(), 1));
      
      break;
    case 123: 
      localObject1 = ValueInt.get(DateTimeUtils.getIsoYear(paramValue.getDate()));
      break;
    case 124: 
      localObject1 = ValueInt.get(DateTimeUtils.getIsoWeek(paramValue.getDate()));
      break;
    case 125: 
      localObject1 = ValueInt.get(DateTimeUtils.getIsoDayOfWeek(paramValue.getDate()));
      break;
    case 100: 
    case 117: 
      l = paramSession.getTransactionStart();
      
      localObject1 = ValueDate.fromMillis(l);
      break;
    case 101: 
    case 118: 
      l = paramSession.getTransactionStart();
      
      localObject1 = ValueTime.fromMillis(l);
      break;
    case 112: 
    case 119: 
      l = paramSession.getTransactionStart();
      ValueTimestamp localValueTimestamp = ValueTimestamp.fromMillis(l);
      if (paramValue != null)
      {
        localObject5 = this.database.getMode();
        localValueTimestamp = (ValueTimestamp)localValueTimestamp.convertScale(((Mode)localObject5).convertOnlyToSmallerScale, paramValue.getInt());
      }
      localObject1 = localValueTimestamp;
      break;
    case 150: 
      localObject1 = ValueString.get(this.database.getShortName(), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 151: 
    case 152: 
      localObject1 = ValueString.get(paramSession.getUser().getName(), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 153: 
      localObject1 = paramSession.getLastIdentity();
      break;
    case 154: 
      localObject1 = paramSession.getLastScopeIdentity();
      break;
    case 155: 
      localObject1 = ValueBoolean.get(paramSession.getAutoCommit());
      break;
    case 156: 
      localObject1 = ValueBoolean.get(this.database.isReadOnly());
      break;
    case 157: 
      localObject2 = this.database.getDatabasePath();
      localObject1 = localObject2 == null ? ValueNull.INSTANCE : ValueString.get((String)localObject2, this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 158: 
      localObject1 = ValueInt.get(paramSession.getLockTimeout());
      break;
    case 159: 
      localObject1 = ValueLong.get(getDiskSpaceUsed(paramSession, paramValue));
      break;
    case 202: 
    case 203: 
      paramValue = paramValue.convertTo(this.dataType);
      localObject2 = this.database.getMode();
      paramValue = paramValue.convertScale(((Mode)localObject2).convertOnlyToSmallerScale, this.scale);
      paramValue = paramValue.convertPrecision(getPrecision(), false);
      localObject1 = paramValue;
      break;
    case 212: 
      paramSession.getUser().checkAdmin();
      localObject1 = ValueInt.get(Utils.getMemoryFree());
      break;
    case 213: 
      paramSession.getUser().checkAdmin();
      localObject1 = ValueInt.get(Utils.getMemoryUsed());
      break;
    case 214: 
      localObject1 = ValueInt.get(this.database.getLockMode());
      break;
    case 215: 
      localObject1 = ValueString.get(paramSession.getCurrentSchemaName(), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 216: 
      localObject1 = ValueInt.get(paramSession.getId());
      break;
    case 200: 
      localObject1 = paramValue;
      if (paramValue == ValueNull.INSTANCE) {
        localObject1 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, 1);
      }
      break;
    case 201: 
      if ((paramValue == ValueNull.INSTANCE) || (!paramValue.getBoolean().booleanValue())) {
        localObject2 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, 2);
      } else {
        localObject2 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, 1);
      }
      localObject1 = ((Value)localObject2).convertTo(this.dataType);
      break;
    case 229: 
      int j = -1;
      int m = 1;
      for (i3 = paramArrayOfExpression.length - 1; m < i3; m += 2) {
        if (this.database.areEqual(paramValue, getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, m)))
        {
          j = m + 1;
          break;
        }
      }
      if ((j < 0) && (paramArrayOfExpression.length % 2 == 0)) {
        j = paramArrayOfExpression.length - 1;
      }
      localValue2 = j < 0 ? ValueNull.INSTANCE : getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, j);
      
      localObject1 = localValue2.convertTo(this.dataType);
      break;
    case 228: 
      Value localValue1;
      if (paramValue == ValueNull.INSTANCE) {
        localValue1 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, 2);
      } else {
        localValue1 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, 1);
      }
      localObject1 = localValue1.convertTo(this.dataType);
      break;
    case 204: 
      localObject1 = paramValue;
      for (k = 0; k < paramArrayOfExpression.length; k++)
      {
        localValue2 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, k);
        if (localValue2 != ValueNull.INSTANCE)
        {
          localObject1 = localValue2.convertTo(this.dataType);
          break;
        }
      }
      break;
    case 219: 
    case 220: 
      localObject1 = ValueNull.INSTANCE;
      for (k = 0; k < paramArrayOfExpression.length; k++)
      {
        localValue2 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, k);
        if (localValue2 != ValueNull.INSTANCE)
        {
          localValue2 = localValue2.convertTo(this.dataType);
          if (localObject1 == ValueNull.INSTANCE)
          {
            localObject1 = localValue2;
          }
          else
          {
            i3 = this.database.compareTypeSave((Value)localObject1, localValue2);
            if ((this.info.type == 219) && (i3 < 0)) {
              localObject1 = localValue2;
            } else if ((this.info.type == 220) && (i3 > 0)) {
              localObject1 = localValue2;
            }
          }
        }
      }
      break;
    case 206: 
      localObject3 = null;
      int n;
      if (paramValue == null)
      {
        n = 1;
        for (i3 = paramArrayOfExpression.length - 1; n < i3; n += 2)
        {
          localObject5 = paramArrayOfExpression[n].getValue(paramSession);
          if ((localObject5 != ValueNull.INSTANCE) && (((Value)localObject5).getBoolean().booleanValue()))
          {
            localObject3 = paramArrayOfExpression[(n + 1)];
            break;
          }
        }
      }
      else if (paramValue != ValueNull.INSTANCE)
      {
        n = 1;
        for (i3 = paramArrayOfExpression.length - 1; n < i3; n += 2)
        {
          localObject5 = paramArrayOfExpression[n].getValue(paramSession);
          if (this.database.areEqual(paramValue, (Value)localObject5))
          {
            localObject3 = paramArrayOfExpression[(n + 1)];
            break;
          }
        }
      }
      if ((localObject3 == null) && (paramArrayOfExpression.length % 2 == 0)) {
        localObject3 = paramArrayOfExpression[(paramArrayOfExpression.length - 1)];
      }
      Value localValue3 = localObject3 == null ? ValueNull.INSTANCE : ((Expression)localObject3).getValue(paramSession);
      localObject1 = localValue3.convertTo(this.dataType);
      break;
    case 209: 
      if (paramValue.getType() == 17)
      {
        localObject3 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, 1);
        int i1 = ((Value)localObject3).getInt();
        arrayOfValue2 = ((ValueArray)paramValue).getList();
        if ((i1 < 1) || (i1 > arrayOfValue2.length)) {
          localObject1 = ValueNull.INSTANCE;
        } else {
          localObject1 = arrayOfValue2[(i1 - 1)];
        }
      }
      else
      {
        localObject1 = ValueNull.INSTANCE;
      }
      break;
    case 217: 
      if (paramValue.getType() == 17)
      {
        localObject3 = ((ValueArray)paramValue).getList();
        localObject1 = ValueInt.get(localObject3.length);
      }
      else
      {
        localObject1 = ValueNull.INSTANCE;
      }
      break;
    case 230: 
      localObject1 = ValueBoolean.get(false);
      if (paramValue.getType() == 17)
      {
        localObject3 = getNullOrValue(paramSession, paramArrayOfExpression, paramArrayOfValue, 1);
        Value[] arrayOfValue1 = ((ValueArray)paramValue).getList();
        for (Value localValue4 : arrayOfValue1) {
          if (localValue4.equals(localObject3))
          {
            localObject1 = ValueBoolean.get(true);
            break;
          }
        }
      }
      break;
    case 221: 
      localObject1 = ValueBoolean.get(cancelStatement(paramSession, paramValue.getInt()));
      break;
    case 226: 
      localObject1 = paramSession.getTransactionId();
      break;
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 16: 
    case 18: 
    case 21: 
    case 27: 
    case 29: 
    case 30: 
    case 31: 
    case 32: 
    case 40: 
    case 41: 
    case 42: 
    case 43: 
    case 44: 
    case 45: 
    case 46: 
    case 47: 
    case 48: 
    case 49: 
    case 55: 
    case 57: 
    case 58: 
    case 60: 
    case 62: 
    case 63: 
    case 66: 
    case 67: 
    case 68: 
    case 69: 
    case 72: 
    case 73: 
    case 77: 
    case 78: 
    case 83: 
    case 84: 
    case 88: 
    case 89: 
    case 90: 
    case 91: 
    case 93: 
    case 94: 
    case 95: 
    case 96: 
    case 97: 
    case 98: 
    case 99: 
    case 102: 
    case 103: 
    case 120: 
    case 121: 
    case 122: 
    case 126: 
    case 127: 
    case 128: 
    case 129: 
    case 130: 
    case 131: 
    case 132: 
    case 133: 
    case 134: 
    case 135: 
    case 136: 
    case 137: 
    case 138: 
    case 139: 
    case 140: 
    case 141: 
    case 142: 
    case 143: 
    case 144: 
    case 145: 
    case 146: 
    case 147: 
    case 148: 
    case 149: 
    case 160: 
    case 161: 
    case 162: 
    case 163: 
    case 164: 
    case 165: 
    case 166: 
    case 167: 
    case 168: 
    case 169: 
    case 170: 
    case 171: 
    case 172: 
    case 173: 
    case 174: 
    case 175: 
    case 176: 
    case 177: 
    case 178: 
    case 179: 
    case 180: 
    case 181: 
    case 182: 
    case 183: 
    case 184: 
    case 185: 
    case 186: 
    case 187: 
    case 188: 
    case 189: 
    case 190: 
    case 191: 
    case 192: 
    case 193: 
    case 194: 
    case 195: 
    case 196: 
    case 197: 
    case 198: 
    case 199: 
    case 205: 
    case 207: 
    case 208: 
    case 210: 
    case 211: 
    case 218: 
    case 222: 
    case 223: 
    case 224: 
    case 225: 
    case 227: 
    default: 
      localObject1 = null;
    }
    return (Value)localObject1;
  }
  
  private static boolean cancelStatement(Session paramSession, int paramInt)
  {
    paramSession.getUser().checkAdmin();
    Session[] arrayOfSession1 = paramSession.getDatabase().getSessions(false);
    for (Session localSession : arrayOfSession1) {
      if (localSession.getId() == paramInt)
      {
        Command localCommand = localSession.getCurrentCommand();
        if (localCommand == null) {
          return false;
        }
        localCommand.cancel();
        return true;
      }
    }
    return false;
  }
  
  private static long getDiskSpaceUsed(Session paramSession, Value paramValue)
  {
    Parser localParser = new Parser(paramSession);
    String str = paramValue.getString();
    Table localTable = localParser.parseTableName(str);
    return localTable.getDiskSpaceUsed();
  }
  
  private static Value getNullOrValue(Session paramSession, Expression[] paramArrayOfExpression, Value[] paramArrayOfValue, int paramInt)
  {
    if (paramInt >= paramArrayOfExpression.length) {
      return null;
    }
    Value localValue = paramArrayOfValue[paramInt];
    if (localValue == null)
    {
      Expression localExpression = paramArrayOfExpression[paramInt];
      if (localExpression == null) {
        return null;
      }
      localValue = paramArrayOfValue[paramInt] = localExpression.getValue(paramSession);
    }
    return localValue;
  }
  
  private Value getValueWithArgs(Session paramSession, Expression[] paramArrayOfExpression)
  {
    Value[] arrayOfValue = new Value[paramArrayOfExpression.length];
    if (this.info.nullIfParameterIsNull) {
      for (int i = 0; i < paramArrayOfExpression.length; i++)
      {
        localObject1 = paramArrayOfExpression[i];
        localValue2 = ((Expression)localObject1).getValue(paramSession);
        if (localValue2 == ValueNull.INSTANCE) {
          return ValueNull.INSTANCE;
        }
        arrayOfValue[i] = localValue2;
      }
    }
    Value localValue1 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 0);
    Object localObject1 = getSimpleValue(paramSession, localValue1, paramArrayOfExpression, arrayOfValue);
    if (localObject1 != null) {
      return (Value)localObject1;
    }
    Value localValue2 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 1);
    Value localValue3 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 2);
    Value localValue4 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 3);
    Value localValue5 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 4);
    Value localValue6 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 5);
    Object localObject2;
    Object localObject4;
    int j;
    String str2;
    Object localObject5;
    Object localObject3;
    Object localObject6;
    Object localObject7;
    Object localObject8;
    Object localObject9;
    Object localObject11;
    Object localObject12;
    switch (this.info.type)
    {
    case 4: 
      localObject2 = ValueDouble.get(Math.atan2(localValue1.getDouble(), localValue2.getDouble()));
      
      break;
    case 5: 
      localObject2 = ValueLong.get(localValue1.getLong() & localValue2.getLong());
      break;
    case 6: 
      localObject2 = ValueLong.get(localValue1.getLong() | localValue2.getLong());
      break;
    case 7: 
      localObject2 = ValueLong.get(localValue1.getLong() ^ localValue2.getLong());
      break;
    case 16: 
      long l = localValue2.getLong();
      if (l == 0L) {
        throw DbException.get(22012, getSQL());
      }
      localObject2 = ValueLong.get(localValue1.getLong() % l);
      break;
    case 18: 
      localObject2 = ValueDouble.get(Math.pow(localValue1.getDouble(), localValue2.getDouble()));
      
      break;
    case 21: 
      double d1 = localValue2 == null ? 1.0D : Math.pow(10.0D, localValue2.getDouble());
      localObject2 = ValueDouble.get(Math.round(localValue1.getDouble() * d1) / d1);
      break;
    case 27: 
      if (localValue1.getType() == 11)
      {
        Timestamp localTimestamp = localValue1.getTimestamp();
        localObject4 = Calendar.getInstance();
        ((Calendar)localObject4).setTime(localTimestamp);
        ((Calendar)localObject4).set(11, 0);
        ((Calendar)localObject4).set(12, 0);
        ((Calendar)localObject4).set(13, 0);
        ((Calendar)localObject4).set(14, 0);
        localObject2 = ValueTimestamp.fromMillis(((Calendar)localObject4).getTimeInMillis());
      }
      else
      {
        double d2 = localValue1.getDouble();
        int i1 = localValue2 == null ? 0 : localValue2.getInt();
        double d3 = Math.pow(10.0D, i1);
        double d4 = d2 * d3;
        localObject2 = ValueDouble.get((d2 < 0.0D ? Math.ceil(d4) : Math.floor(d4)) / d3);
      }
      break;
    case 29: 
      localObject2 = ValueBytes.getNoCopy(getHash(localValue1.getString(), localValue2.getBytesNoCopy(), localValue3.getInt()));
      
      break;
    case 30: 
      localObject2 = ValueBytes.getNoCopy(encrypt(localValue1.getString(), localValue2.getBytesNoCopy(), localValue3.getBytesNoCopy()));
      
      break;
    case 31: 
      localObject2 = ValueBytes.getNoCopy(decrypt(localValue1.getString(), localValue2.getBytesNoCopy(), localValue3.getBytesNoCopy()));
      
      break;
    case 32: 
      String str1 = null;
      if (localValue2 != null) {
        str1 = localValue2.getString();
      }
      localObject2 = ValueBytes.getNoCopy(CompressTool.getInstance().compress(localValue1.getBytesNoCopy(), str1));
      
      break;
    case 55: 
      localObject2 = ValueInt.get(getDifference(localValue1.getString(), localValue2.getString()));
      
      break;
    case 57: 
      if ((localValue2 == ValueNull.INSTANCE) || (localValue3 == ValueNull.INSTANCE)) {
        localObject2 = localValue2;
      } else {
        localObject2 = ValueString.get(insert(localValue1.getString(), localValue2.getInt(), localValue3.getInt(), localValue4.getString()), this.database.getMode().treatEmptyStringsAsNull);
      }
      break;
    case 60: 
      localObject2 = ValueString.get(left(localValue1.getString(), localValue2.getInt()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 62: 
      j = localValue3 == null ? 0 : localValue3.getInt();
      localObject2 = ValueInt.get(locate(localValue1.getString(), localValue2.getString(), j));
      break;
    case 58: 
      j = localValue3 == null ? 0 : localValue3.getInt();
      localObject2 = ValueInt.get(locate(localValue2.getString(), localValue1.getString(), j));
      break;
    case 66: 
      j = Math.max(0, localValue2.getInt());
      localObject2 = ValueString.get(repeat(localValue1.getString(), j), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 67: 
      str2 = localValue1.getString();
      localObject4 = localValue2.getString();
      String str3 = localValue3 == null ? "" : localValue3.getString();
      localObject2 = ValueString.get(replace(str2, (String)localObject4, str3), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 68: 
      localObject2 = ValueString.get(right(localValue1.getString(), localValue2.getInt()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 63: 
      localObject2 = ValueString.get(StringUtils.trim(localValue1.getString(), true, false, localValue2 == null ? " " : localValue2.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 78: 
      localObject2 = ValueString.get(StringUtils.trim(localValue1.getString(), true, true, localValue2 == null ? " " : localValue2.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 69: 
      localObject2 = ValueString.get(StringUtils.trim(localValue1.getString(), false, true, localValue2 == null ? " " : localValue2.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 72: 
    case 73: 
      str2 = localValue1.getString();
      int m = localValue2.getInt();
      if (m < 0) {
        m = str2.length() + m + 1;
      }
      int i2 = localValue3 == null ? str2.length() : localValue3.getInt();
      localObject2 = ValueString.get(substring(str2, m, i2), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 77: 
      localObject2 = ValueInt.get(locate(localValue1.getString(), localValue2.getString(), 0));
      break;
    case 83: 
      localObject2 = ValueString.get(StringUtils.xmlAttr(localValue1.getString(), localValue2.getString()), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 84: 
      str2 = localValue2 == ValueNull.INSTANCE ? null : localValue2 == null ? null : localValue2.getString();
      
      localObject5 = localValue3 == ValueNull.INSTANCE ? null : localValue3 == null ? null : localValue3.getString();
      
      boolean bool = localValue4 == null ? true : localValue4.getBoolean().booleanValue();
      
      localObject2 = ValueString.get(StringUtils.xmlNode(localValue1.getString(), str2, (String)localObject5, bool), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 89: 
      str2 = localValue2.getString();
      localObject5 = localValue3.getString();
      try
      {
        localObject2 = ValueString.get(localValue1.getString().replaceAll(str2, (String)localObject5), this.database.getMode().treatEmptyStringsAsNull);
      }
      catch (StringIndexOutOfBoundsException localStringIndexOutOfBoundsException)
      {
        throw DbException.get(22025, localStringIndexOutOfBoundsException, new String[] { localObject5 });
      }
      catch (PatternSyntaxException localPatternSyntaxException)
      {
        throw DbException.get(22025, localPatternSyntaxException, new String[] { str2 });
      }
    case 90: 
      localObject2 = ValueString.get(StringUtils.pad(localValue1.getString(), localValue2.getInt(), localValue3 == null ? null : localValue3.getString(), true), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 91: 
      localObject2 = ValueString.get(StringUtils.pad(localValue1.getString(), localValue2.getInt(), localValue3 == null ? null : localValue3.getString(), false), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 93: 
      switch (localValue1.getType())
      {
      case 9: 
      case 10: 
      case 11: 
        localObject2 = ValueString.get(ToChar.toChar(localValue1.getTimestamp(), localValue2 == null ? null : localValue2.getString(), localValue3 == null ? null : localValue3.getString()), this.database.getMode().treatEmptyStringsAsNull);
        
        break;
      case 3: 
      case 4: 
      case 5: 
      case 6: 
      case 7: 
      case 8: 
        localObject2 = ValueString.get(ToChar.toChar(localValue1.getBigDecimal(), localValue2 == null ? null : localValue2.getString(), localValue3 == null ? null : localValue3.getString()), this.database.getMode().treatEmptyStringsAsNull);
        
        break;
      default: 
        localObject2 = ValueString.get(localValue1.getString(), this.database.getMode().treatEmptyStringsAsNull);
      }
      break;
    case 94: 
      str2 = localValue2.getString();
      localObject5 = localValue3.getString();
      localObject2 = ValueString.get(translate(localValue1.getString(), str2, (String)localObject5), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 231: 
      localObject2 = ValueString.get(Constants.getVersion(), this.database.getMode().treatEmptyStringsAsNull);
      
      break;
    case 102: 
      localObject2 = ValueTimestamp.get(dateadd(localValue1.getString(), localValue2.getInt(), localValue3.getTimestamp()));
      
      break;
    case 103: 
      localObject2 = ValueLong.get(datediff(localValue1.getString(), localValue2.getTimestamp(), localValue3.getTimestamp()));
      
      break;
    case 120: 
      int k = getDatePart(localValue1.getString());
      localObject2 = ValueInt.get(DateTimeUtils.getDatePart(localValue2.getTimestamp(), k));
      
      break;
    case 121: 
      if ((localValue1 == ValueNull.INSTANCE) || (localValue2 == ValueNull.INSTANCE))
      {
        localObject2 = ValueNull.INSTANCE;
      }
      else
      {
        localObject3 = localValue3 == ValueNull.INSTANCE ? null : localValue3 == null ? null : localValue3.getString();
        
        localObject5 = localValue4 == ValueNull.INSTANCE ? null : localValue4 == null ? null : localValue4.getString();
        
        localObject2 = ValueString.get(DateTimeUtils.formatDateTime(localValue1.getTimestamp(), localValue2.getString(), (String)localObject3, (String)localObject5), this.database.getMode().treatEmptyStringsAsNull);
      }
      break;
    case 122: 
      if ((localValue1 == ValueNull.INSTANCE) || (localValue2 == ValueNull.INSTANCE))
      {
        localObject2 = ValueNull.INSTANCE;
      }
      else
      {
        localObject3 = localValue3 == ValueNull.INSTANCE ? null : localValue3 == null ? null : localValue3.getString();
        
        localObject5 = localValue4 == ValueNull.INSTANCE ? null : localValue4 == null ? null : localValue4.getString();
        
        localObject6 = DateTimeUtils.parseDateTime(localValue1.getString(), localValue2.getString(), (String)localObject3, (String)localObject5);
        
        localObject2 = ValueTimestamp.fromMillis(((Date)localObject6).getTime());
      }
      break;
    case 205: 
      localObject2 = this.database.areEqual(localValue1, localValue2) ? ValueNull.INSTANCE : localValue1;
      break;
    case 207: 
      localObject3 = getSequence(paramSession, localValue1, localValue2);
      localObject5 = new SequenceValue((Sequence)localObject3);
      localObject2 = ((SequenceValue)localObject5).getValue(paramSession);
      break;
    case 208: 
      localObject3 = getSequence(paramSession, localValue1, localValue2);
      localObject2 = ValueLong.get(((Sequence)localObject3).getCurrentValue());
      break;
    case 210: 
      localObject3 = localValue1.getString();
      localObject5 = localValue2 == null ? null : localValue2.getString();
      localObject6 = new Csv();
      localObject7 = localValue3 == null ? null : localValue3.getString();
      localObject8 = null;
      Object localObject10;
      if ((localObject7 != null) && (((String)localObject7).indexOf('=') >= 0))
      {
        localObject8 = ((Csv)localObject6).setOptions((String)localObject7);
      }
      else
      {
        localObject8 = localObject7;
        String str4 = localValue4 == null ? null : localValue4.getString();
        localObject9 = localValue5 == null ? null : localValue5.getString();
        localObject10 = localValue6 == null ? null : localValue6.getString();
        localObject11 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 6);
        localObject12 = localObject11 == null ? null : ((Value)localObject11).getString();
        setCsvDelimiterEscape((Csv)localObject6, str4, (String)localObject9, (String)localObject10);
        
        ((Csv)localObject6).setNullString((String)localObject12);
      }
      char c = ((Csv)localObject6).getFieldSeparatorRead();
      localObject9 = StringUtils.arraySplit((String)localObject5, c, true);
      try
      {
        localObject10 = ValueResultSet.get(((Csv)localObject6).read((String)localObject3, (String[])localObject9, (String)localObject8));
        
        localObject2 = localObject10;
      }
      catch (SQLException localSQLException2)
      {
        throw DbException.convert(localSQLException2);
      }
    case 218: 
      paramSession.getUser().checkAdmin();
      localObject3 = paramSession.createConnection(false);
      localObject5 = LinkSchema.linkSchema((Connection)localObject3, localValue1.getString(), localValue2.getString(), localValue3.getString(), localValue4.getString(), localValue5.getString(), localValue6.getString());
      
      localObject2 = ValueResultSet.get((ResultSet)localObject5);
      break;
    case 211: 
      paramSession.getUser().checkAdmin();
      localObject3 = paramSession.createConnection(false);
      localObject5 = new Csv();
      localObject6 = localValue3 == null ? null : localValue3.getString();
      localObject7 = null;
      if ((localObject6 != null) && (((String)localObject6).indexOf('=') >= 0))
      {
        localObject7 = ((Csv)localObject5).setOptions((String)localObject6);
      }
      else
      {
        localObject7 = localObject6;
        localObject8 = localValue4 == null ? null : localValue4.getString();
        String str5 = localValue5 == null ? null : localValue5.getString();
        localObject9 = localValue6 == null ? null : localValue6.getString();
        Value localValue7 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 6);
        localObject11 = localValue7 == null ? null : localValue7.getString();
        localObject12 = getNullOrValue(paramSession, paramArrayOfExpression, arrayOfValue, 7);
        String str6 = localObject12 == null ? null : ((Value)localObject12).getString();
        setCsvDelimiterEscape((Csv)localObject5, (String)localObject8, str5, (String)localObject9);
        
        ((Csv)localObject5).setNullString((String)localObject11);
        if (str6 != null) {
          ((Csv)localObject5).setLineSeparator(str6);
        }
      }
      try
      {
        int i3 = ((Csv)localObject5).write((Connection)localObject3, localValue1.getString(), localValue2.getString(), (String)localObject7);
        
        localObject2 = ValueInt.get(i3);
      }
      catch (SQLException localSQLException1)
      {
        throw DbException.convert(localSQLException1);
      }
    case 222: 
      localObject3 = (Variable)paramArrayOfExpression[0];
      paramSession.setVariable(((Variable)localObject3).getName(), localValue2);
      localObject2 = localValue2;
      break;
    case 225: 
      paramSession.getUser().checkAdmin();
      localObject3 = localValue1.getString();
      int n = paramArrayOfExpression.length == 1 ? 1 : 0;
      try
      {
        localObject6 = new AutoCloseInputStream(FileUtils.newInputStream((String)localObject3));
        if (n != 0)
        {
          localObject2 = this.database.getLobStorage().createBlob((InputStream)localObject6, -1L);
        }
        else
        {
          if (localValue2 == ValueNull.INSTANCE) {
            localObject7 = new InputStreamReader((InputStream)localObject6);
          } else {
            localObject7 = new InputStreamReader((InputStream)localObject6, localValue2.getString());
          }
          localObject2 = this.database.getLobStorage().createClob((Reader)localObject7, -1L);
        }
      }
      catch (IOException localIOException)
      {
        throw DbException.convertIOException(localIOException, (String)localObject3);
      }
    case 227: 
      localObject2 = localValue1.convertPrecision(localValue2.getLong(), localValue3.getBoolean().booleanValue());
      break;
    case 88: 
      if (localValue2 == null) {
        localObject2 = ValueString.get(StringUtils.xmlText(localValue1.getString()), this.database.getMode().treatEmptyStringsAsNull);
      } else {
        localObject2 = ValueString.get(StringUtils.xmlText(localValue1.getString(), localValue2.getBoolean().booleanValue()), this.database.getMode().treatEmptyStringsAsNull);
      }
      break;
    case 250: 
      localObject2 = paramSession.getVariable(paramArrayOfExpression[0].getSchemaName() + "." + paramArrayOfExpression[0].getTableName() + "." + paramArrayOfExpression[0].getColumnName());
      
      break;
    default: 
      throw DbException.throwInternalError("type=" + this.info.type);
    }
    return (Value)localObject2;
  }
  
  private Sequence getSequence(Session paramSession, Value paramValue1, Value paramValue2)
  {
    String str;
    Object localObject1;
    if (paramValue2 == null)
    {
      localObject2 = new Parser(paramSession);
      localObject3 = paramValue1.getString();
      Expression localExpression = ((Parser)localObject2).parseExpression((String)localObject3);
      if ((localExpression instanceof ExpressionColumn))
      {
        ExpressionColumn localExpressionColumn = (ExpressionColumn)localExpression;
        str = localExpressionColumn.getOriginalTableAliasName();
        if (str == null)
        {
          str = paramSession.getCurrentSchemaName();
          localObject1 = localObject3;
        }
        else
        {
          localObject1 = localExpressionColumn.getColumnName();
        }
      }
      else
      {
        throw DbException.getSyntaxError((String)localObject3, 1);
      }
    }
    else
    {
      str = paramValue1.getString();
      localObject1 = paramValue2.getString();
    }
    Object localObject2 = this.database.findSchema(str);
    if (localObject2 == null)
    {
      str = StringUtils.toUpperEnglish(str);
      localObject2 = this.database.getSchema(str);
    }
    Object localObject3 = ((Schema)localObject2).findSequence((String)localObject1);
    if (localObject3 == null)
    {
      localObject1 = StringUtils.toUpperEnglish((String)localObject1);
      localObject3 = ((Schema)localObject2).getSequence((String)localObject1);
    }
    return (Sequence)localObject3;
  }
  
  private static long length(Value paramValue)
  {
    switch (paramValue.getType())
    {
    case 12: 
    case 15: 
    case 16: 
    case 19: 
      return paramValue.getPrecision();
    }
    return paramValue.getString().length();
  }
  
  private static byte[] getPaddedArrayCopy(byte[] paramArrayOfByte, int paramInt)
  {
    int i = MathUtils.roundUpInt(paramArrayOfByte.length, paramInt);
    byte[] arrayOfByte = DataUtils.newBytes(i);
    System.arraycopy(paramArrayOfByte, 0, arrayOfByte, 0, paramArrayOfByte.length);
    return arrayOfByte;
  }
  
  private static byte[] decrypt(String paramString, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    BlockCipher localBlockCipher = CipherFactory.getBlockCipher(paramString);
    byte[] arrayOfByte1 = getPaddedArrayCopy(paramArrayOfByte1, localBlockCipher.getKeyLength());
    localBlockCipher.setKey(arrayOfByte1);
    byte[] arrayOfByte2 = getPaddedArrayCopy(paramArrayOfByte2, 16);
    localBlockCipher.decrypt(arrayOfByte2, 0, arrayOfByte2.length);
    return arrayOfByte2;
  }
  
  private static byte[] encrypt(String paramString, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    BlockCipher localBlockCipher = CipherFactory.getBlockCipher(paramString);
    byte[] arrayOfByte1 = getPaddedArrayCopy(paramArrayOfByte1, localBlockCipher.getKeyLength());
    localBlockCipher.setKey(arrayOfByte1);
    byte[] arrayOfByte2 = getPaddedArrayCopy(paramArrayOfByte2, 16);
    localBlockCipher.encrypt(arrayOfByte2, 0, arrayOfByte2.length);
    return arrayOfByte2;
  }
  
  private static byte[] getHash(String paramString, byte[] paramArrayOfByte, int paramInt)
  {
    if (!"SHA256".equalsIgnoreCase(paramString)) {
      throw DbException.getInvalidValueException("algorithm", paramString);
    }
    for (int i = 0; i < paramInt; i++) {
      paramArrayOfByte = SHA256.getHash(paramArrayOfByte, false);
    }
    return paramArrayOfByte;
  }
  
  public static boolean isDatePart(String paramString)
  {
    Integer localInteger = (Integer)DATE_PART.get(StringUtils.toUpperEnglish(paramString));
    return localInteger != null;
  }
  
  private static int getDatePart(String paramString)
  {
    Integer localInteger = (Integer)DATE_PART.get(StringUtils.toUpperEnglish(paramString));
    if (localInteger == null) {
      throw DbException.getInvalidValueException("date part", paramString);
    }
    return localInteger.intValue();
  }
  
  private static Timestamp dateadd(String paramString, int paramInt, Timestamp paramTimestamp)
  {
    int i = getDatePart(paramString);
    Calendar localCalendar = Calendar.getInstance();
    int j = paramTimestamp.getNanos() % 1000000;
    localCalendar.setTime(paramTimestamp);
    localCalendar.add(i, paramInt);
    long l = localCalendar.getTime().getTime();
    Timestamp localTimestamp = new Timestamp(l);
    localTimestamp.setNanos(localTimestamp.getNanos() + j);
    return localTimestamp;
  }
  
  private static long datediff(String paramString, Timestamp paramTimestamp1, Timestamp paramTimestamp2)
  {
    int i = getDatePart(paramString);
    Calendar localCalendar = Calendar.getInstance();
    long l1 = paramTimestamp1.getTime();long l2 = paramTimestamp2.getTime();
    
    TimeZone localTimeZone = localCalendar.getTimeZone();
    localCalendar.setTime(paramTimestamp1);
    l1 += localTimeZone.getOffset(localCalendar.get(0), localCalendar.get(1), localCalendar.get(2), localCalendar.get(5), localCalendar.get(7), localCalendar.get(14));
    
    localCalendar.setTime(paramTimestamp2);
    l2 += localTimeZone.getOffset(localCalendar.get(0), localCalendar.get(1), localCalendar.get(2), localCalendar.get(5), localCalendar.get(7), localCalendar.get(14));
    switch (i)
    {
    case 14: 
      return l2 - l1;
    case 11: 
    case 12: 
    case 13: 
      long l3 = 3600000L;
      long l4 = Math.min(l1 / l3 * l3, l2 / l3 * l3);
      l1 -= l4;
      l2 -= l4;
      switch (i)
      {
      case 13: 
        return l2 / 1000L - l1 / 1000L;
      case 12: 
        return l2 / 60000L - l1 / 60000L;
      case 11: 
        return l2 / l3 - l1 / l3;
      }
      throw DbException.throwInternalError("field:" + i);
    case 5: 
      return l2 / 86400000L - l1 / 86400000L;
    }
    localCalendar.setTimeInMillis(l1);
    int j = localCalendar.get(1);
    int k = localCalendar.get(2);
    localCalendar.setTimeInMillis(l2);
    int m = localCalendar.get(1);
    int n = localCalendar.get(2);
    int i1 = m - j;
    if (i == 2) {
      i1 = 12 * i1 + (n - k);
    }
    return i1;
  }
  
  private static String substring(String paramString, int paramInt1, int paramInt2)
  {
    int i = paramString.length();
    paramInt1--;
    if (paramInt1 < 0) {
      paramInt1 = 0;
    }
    if (paramInt2 < 0) {
      paramInt2 = 0;
    }
    paramInt1 = paramInt1 > i ? i : paramInt1;
    if (paramInt1 + paramInt2 > i) {
      paramInt2 = i - paramInt1;
    }
    return paramString.substring(paramInt1, paramInt1 + paramInt2);
  }
  
  private static String replace(String paramString1, String paramString2, String paramString3)
  {
    if ((paramString1 == null) || (paramString2 == null) || (paramString3 == null)) {
      return null;
    }
    if (paramString2.length() == 0) {
      return paramString1;
    }
    StringBuilder localStringBuilder = new StringBuilder(paramString1.length());
    int i = 0;
    int j = paramString2.length();
    for (;;)
    {
      int k = paramString1.indexOf(paramString2, i);
      if (k == -1) {
        break;
      }
      localStringBuilder.append(paramString1.substring(i, k)).append(paramString3);
      i = k + j;
    }
    localStringBuilder.append(paramString1.substring(i));
    return localStringBuilder.toString();
  }
  
  private static String repeat(String paramString, int paramInt)
  {
    StringBuilder localStringBuilder = new StringBuilder(paramString.length() * paramInt);
    while (paramInt-- > 0) {
      localStringBuilder.append(paramString);
    }
    return localStringBuilder.toString();
  }
  
  private static String rawToHex(String paramString)
  {
    int i = paramString.length();
    StringBuilder localStringBuilder = new StringBuilder(4 * i);
    for (int j = 0; j < i; j++)
    {
      String str = Integer.toHexString(paramString.charAt(j) & 0xFFFF);
      for (int k = str.length(); k < 4; k++) {
        localStringBuilder.append('0');
      }
      localStringBuilder.append(str);
    }
    return localStringBuilder.toString();
  }
  
  private static int locate(String paramString1, String paramString2, int paramInt)
  {
    if (paramInt < 0)
    {
      i = paramString2.length() + paramInt;
      return paramString2.lastIndexOf(paramString1, i) + 1;
    }
    int i = paramInt == 0 ? 0 : paramInt - 1;
    return paramString2.indexOf(paramString1, i) + 1;
  }
  
  private static String right(String paramString, int paramInt)
  {
    if (paramInt < 0) {
      paramInt = 0;
    } else if (paramInt > paramString.length()) {
      paramInt = paramString.length();
    }
    return paramString.substring(paramString.length() - paramInt);
  }
  
  private static String left(String paramString, int paramInt)
  {
    if (paramInt < 0) {
      paramInt = 0;
    } else if (paramInt > paramString.length()) {
      paramInt = paramString.length();
    }
    return paramString.substring(0, paramInt);
  }
  
  private static String insert(String paramString1, int paramInt1, int paramInt2, String paramString2)
  {
    if (paramString1 == null) {
      return paramString2;
    }
    if (paramString2 == null) {
      return paramString1;
    }
    int i = paramString1.length();
    int j = paramString2.length();
    paramInt1--;
    if ((paramInt1 < 0) || (paramInt2 <= 0) || (j == 0) || (paramInt1 > i)) {
      return paramString1;
    }
    if (paramInt1 + paramInt2 > i) {
      paramInt2 = i - paramInt1;
    }
    return paramString1.substring(0, paramInt1) + paramString2 + paramString1.substring(paramInt1 + paramInt2);
  }
  
  private static String hexToRaw(String paramString)
  {
    int i = paramString.length();
    if (i % 4 != 0) {
      throw DbException.get(22018, paramString);
    }
    StringBuilder localStringBuilder = new StringBuilder(i / 4);
    for (int j = 0; j < i; j += 4) {
      try
      {
        char c = (char)Integer.parseInt(paramString.substring(j, j + 4), 16);
        localStringBuilder.append(c);
      }
      catch (NumberFormatException localNumberFormatException)
      {
        throw DbException.get(22018, paramString);
      }
    }
    return localStringBuilder.toString();
  }
  
  private static int getDifference(String paramString1, String paramString2)
  {
    paramString1 = getSoundex(paramString1);
    paramString2 = getSoundex(paramString2);
    int i = 0;
    for (int j = 0; j < 4; j++) {
      if (paramString1.charAt(j) == paramString2.charAt(j)) {
        i++;
      }
    }
    return i;
  }
  
  private static String translate(String paramString1, String paramString2, String paramString3)
  {
    if ((StringUtils.isNullOrEmpty(paramString1)) || (StringUtils.isNullOrEmpty(paramString2))) {
      return paramString1;
    }
    StringBuilder localStringBuilder = null;
    
    int i = paramString3 == null ? 0 : paramString3.length();
    int j = 0;
    for (int k = paramString1.length(); j < k; j++)
    {
      char c = paramString1.charAt(j);
      int m = paramString2.indexOf(c);
      if (m >= 0)
      {
        if (localStringBuilder == null)
        {
          localStringBuilder = new StringBuilder(k);
          if (j > 0) {
            localStringBuilder.append(paramString1.substring(0, j));
          }
        }
        if (m < i) {
          c = paramString3.charAt(m);
        }
      }
      if (localStringBuilder != null) {
        localStringBuilder.append(c);
      }
    }
    return localStringBuilder == null ? paramString1 : localStringBuilder.toString();
  }
  
  private static double roundMagic(double paramDouble)
  {
    if ((paramDouble < 1.0E-13D) && (paramDouble > -1.0E-13D)) {
      return 0.0D;
    }
    if ((paramDouble > 1.0E12D) || (paramDouble < -1.0E12D)) {
      return paramDouble;
    }
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append(paramDouble);
    if (localStringBuilder.toString().indexOf('E') >= 0) {
      return paramDouble;
    }
    int i = localStringBuilder.length();
    if (i < 16) {
      return paramDouble;
    }
    if (localStringBuilder.toString().indexOf('.') > i - 3) {
      return paramDouble;
    }
    localStringBuilder.delete(i - 2, i);
    i -= 2;
    int j = localStringBuilder.charAt(i - 2);
    int k = localStringBuilder.charAt(i - 3);
    int m = localStringBuilder.charAt(i - 4);
    if ((j == 48) && (k == 48) && (m == 48))
    {
      localStringBuilder.setCharAt(i - 1, '0');
    }
    else if ((j == 57) && (k == 57) && (m == 57))
    {
      localStringBuilder.setCharAt(i - 1, '9');
      localStringBuilder.append('9');
      localStringBuilder.append('9');
      localStringBuilder.append('9');
    }
    return Double.parseDouble(localStringBuilder.toString());
  }
  
  private static String getSoundex(String paramString)
  {
    int i = paramString.length();
    char[] arrayOfChar = { '0', '0', '0', '0' };
    int j = 48;
    int k = 0;
    for (int m = 0; (k < i) && (m < 4); k++)
    {
      int n = paramString.charAt(k);
      int i1 = n > SOUNDEX_INDEX.length ? '\000' : SOUNDEX_INDEX[n];
      if (i1 != 0) {
        if (m == 0)
        {
          arrayOfChar[(m++)] = n;
          j = i1;
        }
        else if (i1 <= 54)
        {
          if (i1 != j)
          {
            arrayOfChar[(m++)] = i1;
            j = i1;
          }
        }
        else if (i1 == 55)
        {
          j = i1;
        }
      }
    }
    return new String(arrayOfChar);
  }
  
  public int getType()
  {
    return this.dataType;
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    for (Expression localExpression : this.args) {
      if (localExpression != null) {
        localExpression.mapColumns(paramColumnResolver, paramInt);
      }
    }
  }
  
  protected void checkParameterCount(int paramInt)
  {
    int i = 0;int j = Integer.MAX_VALUE;
    switch (this.info.type)
    {
    case 204: 
    case 210: 
    case 219: 
    case 220: 
      i = 1;
      break;
    case 20: 
    case 112: 
    case 119: 
      j = 1;
      break;
    case 21: 
    case 27: 
    case 32: 
    case 63: 
    case 69: 
    case 78: 
    case 88: 
    case 225: 
      i = 1;
      j = 2;
      break;
    case 93: 
      i = 1;
      j = 3;
      break;
    case 58: 
    case 62: 
    case 67: 
    case 72: 
    case 73: 
    case 90: 
    case 91: 
      i = 2;
      j = 3;
      break;
    case 54: 
    case 92: 
    case 211: 
      i = 2;
      break;
    case 84: 
      i = 1;
      j = 4;
      break;
    case 121: 
    case 122: 
      i = 2;
      j = 4;
      break;
    case 207: 
    case 208: 
      i = 1;
      j = 2;
      break;
    case 206: 
    case 229: 
      i = 3;
      break;
    default: 
      DbException.throwInternalError("type=" + this.info.type);
    }
    int k = (paramInt >= i) && (paramInt <= j) ? 1 : 0;
    if (k == 0) {
      throw DbException.get(7001, new String[] { this.info.name, i + ".." + j });
    }
  }
  
  public void doneWithParameters()
  {
    int i;
    if (this.info.parameterCount == -1)
    {
      i = this.varArgs.size();
      checkParameterCount(i);
      this.args = new Expression[i];
      this.varArgs.toArray(this.args);
      this.varArgs = null;
    }
    else
    {
      i = this.args.length;
      if ((i > 0) && (this.args[(i - 1)] == null)) {
        throw DbException.get(7001, new String[] { this.info.name, "" + i });
      }
    }
  }
  
  public void setDataType(Column paramColumn)
  {
    this.dataType = paramColumn.getType();
    this.precision = paramColumn.getPrecision();
    this.displaySize = paramColumn.getDisplaySize();
    this.scale = paramColumn.getScale();
  }
  
  public Expression optimize(Session paramSession)
  {
    boolean bool = this.info.deterministic;
    for (int i = 0; i < this.args.length; i++)
    {
      Expression localExpression1 = this.args[i];
      if (localExpression1 != null)
      {
        localExpression1 = localExpression1.optimize(paramSession);
        this.args[i] = localExpression1;
        if (!localExpression1.isConstant()) {
          bool = false;
        }
      }
    }
    Expression localExpression2 = this.args.length < 1 ? null : this.args[0];
    int j;
    long l;
    int k;
    Object localObject;
    switch (this.info.type)
    {
    case 200: 
    case 204: 
    case 205: 
    case 219: 
    case 220: 
      i = -1;
      j = 0;
      l = 0L;
      k = 0;
      for (Expression localExpression4 : this.args) {
        if (localExpression4 != ValueExpression.getNull())
        {
          int i3 = localExpression4.getType();
          if ((i3 != -1) && (i3 != 0))
          {
            i = Value.getHigherOrder(i, i3);
            j = Math.max(j, localExpression4.getScale());
            l = Math.max(l, localExpression4.getPrecision());
            k = Math.max(k, localExpression4.getDisplaySize());
          }
        }
      }
      if (i == -1)
      {
        i = 13;
        j = 0;
        l = 2147483647L;
        k = Integer.MAX_VALUE;
      }
      break;
    case 206: 
    case 229: 
      i = -1;
      j = 0;
      l = 0L;
      k = 0;
      
      int m = 2;
      for (??? = this.args.length; m < ???; m += 2)
      {
        Expression localExpression3 = this.args[m];
        if (localExpression3 != ValueExpression.getNull())
        {
          int i2 = localExpression3.getType();
          if ((i2 != -1) && (i2 != 0))
          {
            i = Value.getHigherOrder(i, i2);
            j = Math.max(j, localExpression3.getScale());
            l = Math.max(l, localExpression3.getPrecision());
            k = Math.max(k, localExpression3.getDisplaySize());
          }
        }
      }
      if (this.args.length % 2 == 0)
      {
        localObject = this.args[(this.args.length - 1)];
        if (localObject != ValueExpression.getNull())
        {
          ??? = ((Expression)localObject).getType();
          if ((??? != -1) && (??? != 0))
          {
            i = Value.getHigherOrder(i, ???);
            j = Math.max(j, ((Expression)localObject).getScale());
            l = Math.max(l, ((Expression)localObject).getPrecision());
            k = Math.max(k, ((Expression)localObject).getDisplaySize());
          }
        }
      }
      if (i == -1)
      {
        i = 13;
        j = 0;
        l = 2147483647L;
        k = Integer.MAX_VALUE;
      }
      break;
    case 201: 
      i = Value.getHigherOrder(this.args[1].getType(), this.args[2].getType());
      l = Math.max(this.args[1].getPrecision(), this.args[2].getPrecision());
      k = Math.max(this.args[1].getDisplaySize(), this.args[2].getDisplaySize());
      j = Math.max(this.args[1].getScale(), this.args[2].getScale());
      break;
    case 228: 
      switch (this.args[1].getType())
      {
      case 13: 
      case 14: 
      case 16: 
      case 21: 
        i = this.args[1].getType();
        break;
      case 15: 
      case 17: 
      case 18: 
      case 19: 
      case 20: 
      default: 
        i = Value.getHigherOrder(this.args[1].getType(), this.args[2].getType());
      }
      l = Math.max(this.args[1].getPrecision(), this.args[2].getPrecision());
      k = Math.max(this.args[1].getDisplaySize(), this.args[2].getDisplaySize());
      j = Math.max(this.args[1].getScale(), this.args[2].getScale());
      break;
    case 202: 
    case 203: 
    case 227: 
      i = this.dataType;
      l = this.precision;
      j = this.scale;
      k = this.displaySize;
      break;
    case 27: 
      i = localExpression2.getType();
      j = localExpression2.getScale();
      l = localExpression2.getPrecision();
      k = localExpression2.getDisplaySize();
      if (i == 0)
      {
        i = 4;
        l = 10L;
        k = 11;
        j = 0;
      }
      else if (i == 11)
      {
        i = 10;
        l = 8L;
        j = 0;
        k = 10;
      }
      break;
    case 0: 
    case 13: 
    case 21: 
      i = localExpression2.getType();
      j = localExpression2.getScale();
      l = localExpression2.getPrecision();
      k = localExpression2.getDisplaySize();
      if (i == 0)
      {
        i = 4;
        l = 10L;
        k = 11;
        j = 0;
      }
      break;
    case 222: 
      localObject = this.args[1];
      i = ((Expression)localObject).getType();
      l = ((Expression)localObject).getPrecision();
      j = ((Expression)localObject).getScale();
      k = ((Expression)localObject).getDisplaySize();
      if (!(localExpression2 instanceof Variable)) {
        throw DbException.get(90137, localExpression2.getSQL());
      }
      break;
    case 225: 
      if (this.args.length == 1) {
        i = 15;
      } else {
        i = 16;
      }
      l = 2147483647L;
      j = 0;
      k = Integer.MAX_VALUE;
      break;
    case 72: 
    case 73: 
      i = this.info.dataType;
      l = this.args[0].getPrecision();
      j = 0;
      if (this.args[1].isConstant()) {
        l -= this.args[1].getValue(paramSession).getLong() - 1L;
      }
      if ((this.args.length == 3) && (this.args[2].isConstant())) {
        l = Math.min(l, this.args[2].getValue(paramSession).getLong());
      }
      l = Math.max(0L, l);
      k = MathUtils.convertLongToInt(l);
      break;
    default: 
      i = this.info.dataType;
      localObject = DataType.getDataType(i);
      l = -1L;
      k = 0;
      j = ((DataType)localObject).defaultScale;
    }
    this.dataType = i;
    this.precision = l;
    this.scale = j;
    this.displaySize = k;
    if (bool)
    {
      localObject = getValue(paramSession);
      if ((localObject == ValueNull.INSTANCE) && (
        (this.info.type == 203) || (this.info.type == 202))) {
        return this;
      }
      return ValueExpression.get((Value)localObject);
    }
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    for (Expression localExpression : this.args) {
      if (localExpression != null) {
        localExpression.setEvaluatable(paramTableFilter, paramBoolean);
      }
    }
  }
  
  public int getScale()
  {
    return this.scale;
  }
  
  public long getPrecision()
  {
    if (this.precision == -1L) {
      calculatePrecisionAndDisplaySize();
    }
    return this.precision;
  }
  
  public int getDisplaySize()
  {
    if (this.precision == -1L) {
      calculatePrecisionAndDisplaySize();
    }
    return this.displaySize;
  }
  
  private void calculatePrecisionAndDisplaySize()
  {
    switch (this.info.type)
    {
    case 30: 
    case 31: 
      this.precision = this.args[2].getPrecision();
      this.displaySize = this.args[2].getDisplaySize();
      break;
    case 32: 
      this.precision = this.args[0].getPrecision();
      this.displaySize = this.args[0].getDisplaySize();
      break;
    case 52: 
      this.precision = 1L;
      this.displaySize = 1;
      break;
    case 54: 
      this.precision = 0L;
      this.displaySize = 0;
      for (Object localObject2 : this.args)
      {
        this.precision += ((Expression)localObject2).getPrecision();
        this.displaySize = MathUtils.convertLongToInt(this.displaySize + ((Expression)localObject2).getDisplaySize());
        if (this.precision < 0L) {
          this.precision = Long.MAX_VALUE;
        }
      }
      break;
    case 56: 
      this.precision = ((this.args[0].getPrecision() + 3L) / 4L);
      this.displaySize = MathUtils.convertLongToInt(this.precision);
      break;
    case 27: 
    case 59: 
    case 63: 
    case 68: 
    case 69: 
    case 74: 
    case 75: 
    case 76: 
    case 78: 
    case 80: 
    case 82: 
      this.precision = this.args[0].getPrecision();
      this.displaySize = this.args[0].getDisplaySize();
      break;
    case 65: 
      this.precision = (this.args[0].getPrecision() * 4L);
      this.displaySize = MathUtils.convertLongToInt(this.precision);
      break;
    case 70: 
      this.precision = 4L;
      this.displaySize = ((int)this.precision);
      break;
    case 104: 
    case 111: 
      this.precision = 20L;
      this.displaySize = ((int)this.precision);
      break;
    case 28: 
    case 29: 
    case 33: 
    case 34: 
    case 35: 
    case 36: 
    case 37: 
    case 38: 
    case 39: 
    case 40: 
    case 41: 
    case 42: 
    case 43: 
    case 44: 
    case 45: 
    case 46: 
    case 47: 
    case 48: 
    case 49: 
    case 50: 
    case 51: 
    case 53: 
    case 55: 
    case 57: 
    case 58: 
    case 60: 
    case 61: 
    case 62: 
    case 64: 
    case 66: 
    case 67: 
    case 71: 
    case 72: 
    case 73: 
    case 77: 
    case 79: 
    case 81: 
    case 83: 
    case 84: 
    case 85: 
    case 86: 
    case 87: 
    case 88: 
    case 89: 
    case 90: 
    case 91: 
    case 92: 
    case 93: 
    case 94: 
    case 95: 
    case 96: 
    case 97: 
    case 98: 
    case 99: 
    case 100: 
    case 101: 
    case 102: 
    case 103: 
    case 105: 
    case 106: 
    case 107: 
    case 108: 
    case 109: 
    case 110: 
    default: 
      ??? = DataType.getDataType(this.dataType);
      this.precision = ((DataType)???).defaultPrecision;
      this.displaySize = ((DataType)???).defaultDisplaySize;
    }
  }
  
  public String getSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder(this.info.name);
    int j;
    if (this.info.type == 206)
    {
      if (this.args[0] != null) {
        localStatementBuilder.append(" ").append(this.args[0].getSQL());
      }
      int i = 1;
      for (j = this.args.length - 1; i < j; i += 2)
      {
        localStatementBuilder.append(" WHEN ").append(this.args[i].getSQL());
        localStatementBuilder.append(" THEN ").append(this.args[(i + 1)].getSQL());
      }
      if (this.args.length % 2 == 0) {
        localStatementBuilder.append(" ELSE ").append(this.args[(this.args.length - 1)].getSQL());
      }
      return localStatementBuilder.append(" END").toString();
    }
    localStatementBuilder.append('(');
    Object localObject1;
    switch (this.info.type)
    {
    case 203: 
      localStatementBuilder.append(this.args[0].getSQL()).append(" AS ").append(new Column(null, this.dataType, this.precision, this.scale, this.displaySize).getCreateSQL());
      
      break;
    case 202: 
      localStatementBuilder.append(this.args[0].getSQL()).append(',').append(new Column(null, this.dataType, this.precision, this.scale, this.displaySize).getCreateSQL());
      
      break;
    case 120: 
      localObject1 = (ValueString)((ValueExpression)this.args[0]).getValue(null);
      localStatementBuilder.append(((ValueString)localObject1).getString()).append(" FROM ").append(this.args[1].getSQL());
      break;
    default: 
      for (Object localObject2 : this.args)
      {
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(((Expression)localObject2).getSQL());
      }
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public void updateAggregate(Session paramSession)
  {
    for (Expression localExpression : this.args) {
      if (localExpression != null) {
        localExpression.updateAggregate(paramSession);
      }
    }
  }
  
  public int getFunctionType()
  {
    return this.info.type;
  }
  
  public String getName()
  {
    return this.info.name;
  }
  
  public ValueResultSet getValueForColumnList(Session paramSession, Expression[] paramArrayOfExpression)
  {
    switch (this.info.type)
    {
    case 210: 
      String str1 = paramArrayOfExpression[0].getValue(paramSession).getString();
      if (str1 == null) {
        throw DbException.get(90012, "fileName");
      }
      String str2 = paramArrayOfExpression.length < 2 ? null : paramArrayOfExpression[1].getValue(paramSession).getString();
      
      Csv localCsv = new Csv();
      String str3 = paramArrayOfExpression.length < 3 ? null : paramArrayOfExpression[2].getValue(paramSession).getString();
      
      String str4 = null;
      if ((str3 != null) && (str3.indexOf('=') >= 0))
      {
        str4 = localCsv.setOptions(str3);
      }
      else
      {
        str4 = str3;
        String str5 = paramArrayOfExpression.length < 4 ? null : paramArrayOfExpression[3].getValue(paramSession).getString();
        
        localObject1 = paramArrayOfExpression.length < 5 ? null : paramArrayOfExpression[4].getValue(paramSession).getString();
        
        localObject2 = paramArrayOfExpression.length < 6 ? null : paramArrayOfExpression[5].getValue(paramSession).getString();
        
        setCsvDelimiterEscape(localCsv, str5, (String)localObject1, (String)localObject2);
      }
      char c = localCsv.getFieldSeparatorRead();
      Object localObject1 = StringUtils.arraySplit(str2, c, true);
      Object localObject2 = null;
      ValueResultSet localValueResultSet;
      try
      {
        localObject2 = localCsv.read(str1, (String[])localObject1, str4);
        localValueResultSet = ValueResultSet.getCopy((ResultSet)localObject2, 0);
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convert(localSQLException);
      }
      finally
      {
        localCsv.close();
        JdbcUtils.closeSilently((ResultSet)localObject2);
      }
      return localValueResultSet;
    }
    return (ValueResultSet)getValueWithArgs(paramSession, paramArrayOfExpression);
  }
  
  private static void setCsvDelimiterEscape(Csv paramCsv, String paramString1, String paramString2, String paramString3)
  {
    char c;
    if (paramString1 != null)
    {
      paramCsv.setFieldSeparatorWrite(paramString1);
      if (paramString1.length() > 0)
      {
        c = paramString1.charAt(0);
        paramCsv.setFieldSeparatorRead(c);
      }
    }
    if (paramString2 != null)
    {
      c = paramString2.length() == 0 ? '\000' : paramString2.charAt(0);
      
      paramCsv.setFieldDelimiter(c);
    }
    if (paramString3 != null)
    {
      c = paramString3.length() == 0 ? '\000' : paramString3.charAt(0);
      
      paramCsv.setEscapeCharacter(c);
    }
  }
  
  public Expression[] getArgs()
  {
    return this.args;
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    for (Expression localExpression : this.args) {
      if ((localExpression != null) && (!localExpression.isEverything(paramExpressionVisitor))) {
        return false;
      }
    }
    switch (paramExpressionVisitor.getType())
    {
    case 2: 
    case 5: 
    case 8: 
      return this.info.deterministic;
    case 0: 
    case 1: 
    case 3: 
    case 4: 
    case 6: 
    case 7: 
    case 9: 
      return true;
    }
    throw DbException.throwInternalError("type=" + paramExpressionVisitor.getType());
  }
  
  public int getCost()
  {
    int i = 3;
    for (Expression localExpression : this.args) {
      if (localExpression != null) {
        i += localExpression.getCost();
      }
    }
    return i;
  }
  
  public boolean isDeterministic()
  {
    return this.info.deterministic;
  }
  
  public boolean isBufferResultSetToLocalTemp()
  {
    return this.info.bufferResultSetToLocalTemp;
  }
}
