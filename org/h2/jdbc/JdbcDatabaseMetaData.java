package org.h2.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceObject;
import org.h2.tools.SimpleResultSet;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;

public class JdbcDatabaseMetaData
  extends TraceObject
  implements DatabaseMetaData
{
  private final JdbcConnection conn;
  private String mode;
  
  JdbcDatabaseMetaData(JdbcConnection paramJdbcConnection, Trace paramTrace, int paramInt)
  {
    setTrace(paramTrace, 2, paramInt);
    this.conn = paramJdbcConnection;
  }
  
  public int getDriverMajorVersion()
  {
    debugCodeCall("getDriverMajorVersion");
    return 1;
  }
  
  public int getDriverMinorVersion()
  {
    debugCodeCall("getDriverMinorVersion");
    return 4;
  }
  
  public String getDatabaseProductName()
  {
    debugCodeCall("getDatabaseProductName");
    
    return "H2";
  }
  
  public String getDatabaseProductVersion()
  {
    debugCodeCall("getDatabaseProductVersion");
    return Constants.getFullVersion();
  }
  
  public String getDriverName()
  {
    debugCodeCall("getDriverName");
    return "H2 JDBC Driver";
  }
  
  public String getDriverVersion()
  {
    debugCodeCall("getDriverVersion");
    return Constants.getFullVersion();
  }
  
  public ResultSet getTables(String paramString1, String paramString2, String paramString3, String[] paramArrayOfString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getTables(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ", " + quoteArray(paramArrayOfString) + ");");
      }
      checkClosed();
      String str;
      if ((paramArrayOfString != null) && (paramArrayOfString.length > 0))
      {
        localObject = new StatementBuilder("TABLE_TYPE IN(");
        for (i = 0; i < paramArrayOfString.length; i++)
        {
          ((StatementBuilder)localObject).appendExceptFirst(", ");
          ((StatementBuilder)localObject).append('?');
        }
        str = ((StatementBuilder)localObject).append(')').toString();
      }
      else
      {
        str = "TRUE";
      }
      Object localObject = this.conn.prepareAutoCloseStatement("SELECT TABLE_CATALOG TABLE_CAT, TABLE_SCHEMA TABLE_SCHEM, TABLE_NAME, TABLE_TYPE, REMARKS, TYPE_NAME TYPE_CAT, TYPE_NAME TYPE_SCHEM, TYPE_NAME, TYPE_NAME SELF_REFERENCING_COL_NAME, TYPE_NAME REF_GENERATION, SQL FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_CATALOG LIKE ? ESCAPE ? AND TABLE_SCHEMA LIKE ? ESCAPE ? AND TABLE_NAME LIKE ? ESCAPE ? AND (" + str + ") " + "ORDER BY TABLE_TYPE, TABLE_SCHEMA, TABLE_NAME");
      
      ((PreparedStatement)localObject).setString(1, getCatalogPattern(paramString1));
      ((PreparedStatement)localObject).setString(2, "\\");
      ((PreparedStatement)localObject).setString(3, getSchemaPattern(paramString2));
      ((PreparedStatement)localObject).setString(4, "\\");
      ((PreparedStatement)localObject).setString(5, getPattern(paramString3));
      ((PreparedStatement)localObject).setString(6, "\\");
      for (int i = 0; (paramArrayOfString != null) && (i < paramArrayOfString.length); i++) {
        ((PreparedStatement)localObject).setString(7 + i, paramArrayOfString[i]);
      }
      return ((PreparedStatement)localObject).executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getColumns(String paramString1, String paramString2, String paramString3, String paramString4)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getColumns(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ", " + quote(paramString4) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT TABLE_CATALOG TABLE_CAT, TABLE_SCHEMA TABLE_SCHEM, TABLE_NAME, COLUMN_NAME, DATA_TYPE, TYPE_NAME, CHARACTER_MAXIMUM_LENGTH COLUMN_SIZE, CHARACTER_MAXIMUM_LENGTH BUFFER_LENGTH, NUMERIC_SCALE DECIMAL_DIGITS, NUMERIC_PRECISION_RADIX NUM_PREC_RADIX, NULLABLE, REMARKS, COLUMN_DEFAULT COLUMN_DEF, DATA_TYPE SQL_DATA_TYPE, ZERO() SQL_DATETIME_SUB, CHARACTER_OCTET_LENGTH CHAR_OCTET_LENGTH, ORDINAL_POSITION, IS_NULLABLE IS_NULLABLE, CAST(SOURCE_DATA_TYPE AS VARCHAR) SCOPE_CATALOG, CAST(SOURCE_DATA_TYPE AS VARCHAR) SCOPE_SCHEMA, CAST(SOURCE_DATA_TYPE AS VARCHAR) SCOPE_TABLE, SOURCE_DATA_TYPE, CASE WHEN SEQUENCE_NAME IS NULL THEN CAST(? AS VARCHAR) ELSE CAST(? AS VARCHAR) END IS_AUTOINCREMENT, CAST(SOURCE_DATA_TYPE AS VARCHAR) SCOPE_CATLOG FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_CATALOG LIKE ? ESCAPE ? AND TABLE_SCHEMA LIKE ? ESCAPE ? AND TABLE_NAME LIKE ? ESCAPE ? AND COLUMN_NAME LIKE ? ESCAPE ? ORDER BY TABLE_SCHEM, TABLE_NAME, ORDINAL_POSITION");
      
      localPreparedStatement.setString(1, "NO");
      localPreparedStatement.setString(2, "YES");
      localPreparedStatement.setString(3, getCatalogPattern(paramString1));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, getSchemaPattern(paramString2));
      localPreparedStatement.setString(6, "\\");
      localPreparedStatement.setString(7, getPattern(paramString3));
      localPreparedStatement.setString(8, "\\");
      localPreparedStatement.setString(9, getPattern(paramString4));
      localPreparedStatement.setString(10, "\\");
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getIndexInfo(String paramString1, String paramString2, String paramString3, boolean paramBoolean1, boolean paramBoolean2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getIndexInfo(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ", " + paramBoolean1 + ", " + paramBoolean2 + ");");
      }
      String str;
      if (paramBoolean1) {
        str = "NON_UNIQUE=FALSE";
      } else {
        str = "TRUE";
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT TABLE_CATALOG TABLE_CAT, TABLE_SCHEMA TABLE_SCHEM, TABLE_NAME, NON_UNIQUE, TABLE_CATALOG INDEX_QUALIFIER, INDEX_NAME, INDEX_TYPE TYPE, ORDINAL_POSITION, COLUMN_NAME, ASC_OR_DESC, CARDINALITY, PAGES, FILTER_CONDITION, SORT_TYPE FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_CATALOG LIKE ? ESCAPE ? AND TABLE_SCHEMA LIKE ? ESCAPE ? AND (" + str + ") " + "AND TABLE_NAME = ? " + "ORDER BY NON_UNIQUE, TYPE, TABLE_SCHEM, INDEX_NAME, ORDINAL_POSITION");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, paramString3);
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getPrimaryKeys(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getPrimaryKeys(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT TABLE_CATALOG TABLE_CAT, TABLE_SCHEMA TABLE_SCHEM, TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION KEY_SEQ, IFNULL(CONSTRAINT_NAME, INDEX_NAME) PK_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_CATALOG LIKE ? ESCAPE ? AND TABLE_SCHEMA LIKE ? ESCAPE ? AND TABLE_NAME = ? AND PRIMARY_KEY = TRUE ORDER BY COLUMN_NAME");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, paramString3);
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean allProceduresAreCallable()
  {
    debugCodeCall("allProceduresAreCallable");
    return true;
  }
  
  public boolean allTablesAreSelectable()
  {
    debugCodeCall("allTablesAreSelectable");
    return true;
  }
  
  public String getURL()
    throws SQLException
  {
    try
    {
      debugCodeCall("getURL");
      return this.conn.getURL();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getUserName()
    throws SQLException
  {
    try
    {
      debugCodeCall("getUserName");
      return this.conn.getUser();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean isReadOnly()
    throws SQLException
  {
    try
    {
      debugCodeCall("isReadOnly");
      return this.conn.isReadOnly();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean nullsAreSortedHigh()
  {
    debugCodeCall("nullsAreSortedHigh");
    return SysProperties.SORT_NULLS_HIGH;
  }
  
  public boolean nullsAreSortedLow()
  {
    debugCodeCall("nullsAreSortedLow");
    return !SysProperties.SORT_NULLS_HIGH;
  }
  
  public boolean nullsAreSortedAtStart()
  {
    debugCodeCall("nullsAreSortedAtStart");
    return false;
  }
  
  public boolean nullsAreSortedAtEnd()
  {
    debugCodeCall("nullsAreSortedAtEnd");
    return false;
  }
  
  public Connection getConnection()
  {
    debugCodeCall("getConnection");
    return this.conn;
  }
  
  public ResultSet getProcedures(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getProcedures(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT ALIAS_CATALOG PROCEDURE_CAT, ALIAS_SCHEMA PROCEDURE_SCHEM, ALIAS_NAME PROCEDURE_NAME, COLUMN_COUNT NUM_INPUT_PARAMS, ZERO() NUM_OUTPUT_PARAMS, ZERO() NUM_RESULT_SETS, REMARKS, RETURNS_RESULT PROCEDURE_TYPE, ALIAS_NAME SPECIFIC_NAME FROM INFORMATION_SCHEMA.FUNCTION_ALIASES WHERE ALIAS_CATALOG LIKE ? ESCAPE ? AND ALIAS_SCHEMA LIKE ? ESCAPE ? AND ALIAS_NAME LIKE ? ESCAPE ? ORDER BY PROCEDURE_SCHEM, PROCEDURE_NAME, NUM_INPUT_PARAMS");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, getPattern(paramString3));
      localPreparedStatement.setString(6, "\\");
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getProcedureColumns(String paramString1, String paramString2, String paramString3, String paramString4)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getProcedureColumns(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ", " + quote(paramString4) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT ALIAS_CATALOG PROCEDURE_CAT, ALIAS_SCHEMA PROCEDURE_SCHEM, ALIAS_NAME PROCEDURE_NAME, COLUMN_NAME, COLUMN_TYPE, DATA_TYPE, TYPE_NAME, PRECISION, PRECISION LENGTH, SCALE, RADIX, NULLABLE, REMARKS, COLUMN_DEFAULT COLUMN_DEF, ZERO() SQL_DATA_TYPE, ZERO() SQL_DATETIME_SUB, ZERO() CHAR_OCTET_LENGTH, POS ORDINAL_POSITION, ? IS_NULLABLE, ALIAS_NAME SPECIFIC_NAME FROM INFORMATION_SCHEMA.FUNCTION_COLUMNS WHERE ALIAS_CATALOG LIKE ? ESCAPE ? AND ALIAS_SCHEMA LIKE ? ESCAPE ? AND ALIAS_NAME LIKE ? ESCAPE ? AND COLUMN_NAME LIKE ? ESCAPE ? ORDER BY PROCEDURE_SCHEM, PROCEDURE_NAME, ORDINAL_POSITION");
      
      localPreparedStatement.setString(1, "YES");
      localPreparedStatement.setString(2, getCatalogPattern(paramString1));
      localPreparedStatement.setString(3, "\\");
      localPreparedStatement.setString(4, getSchemaPattern(paramString2));
      localPreparedStatement.setString(5, "\\");
      localPreparedStatement.setString(6, getPattern(paramString3));
      localPreparedStatement.setString(7, "\\");
      localPreparedStatement.setString(8, getPattern(paramString4));
      localPreparedStatement.setString(9, "\\");
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getSchemas()
    throws SQLException
  {
    try
    {
      debugCodeCall("getSchemas");
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT SCHEMA_NAME TABLE_SCHEM, CATALOG_NAME TABLE_CATALOG,  IS_DEFAULT FROM INFORMATION_SCHEMA.SCHEMATA ORDER BY SCHEMA_NAME");
      
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getCatalogs()
    throws SQLException
  {
    try
    {
      debugCodeCall("getCatalogs");
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT CATALOG_NAME TABLE_CAT FROM INFORMATION_SCHEMA.CATALOGS");
      
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getTableTypes()
    throws SQLException
  {
    try
    {
      debugCodeCall("getTableTypes");
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT TYPE TABLE_TYPE FROM INFORMATION_SCHEMA.TABLE_TYPES ORDER BY TABLE_TYPE");
      
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getColumnPrivileges(String paramString1, String paramString2, String paramString3, String paramString4)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getColumnPrivileges(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ", " + quote(paramString4) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT TABLE_CATALOG TABLE_CAT, TABLE_SCHEMA TABLE_SCHEM, TABLE_NAME, COLUMN_NAME, GRANTOR, GRANTEE, PRIVILEGE_TYPE PRIVILEGE, IS_GRANTABLE FROM INFORMATION_SCHEMA.COLUMN_PRIVILEGES WHERE TABLE_CATALOG LIKE ? ESCAPE ? AND TABLE_SCHEMA LIKE ? ESCAPE ? AND TABLE_NAME = ? AND COLUMN_NAME LIKE ? ESCAPE ? ORDER BY COLUMN_NAME, PRIVILEGE");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, paramString3);
      localPreparedStatement.setString(6, getPattern(paramString4));
      localPreparedStatement.setString(7, "\\");
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getTablePrivileges(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getTablePrivileges(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT TABLE_CATALOG TABLE_CAT, TABLE_SCHEMA TABLE_SCHEM, TABLE_NAME, GRANTOR, GRANTEE, PRIVILEGE_TYPE PRIVILEGE, IS_GRANTABLE FROM INFORMATION_SCHEMA.TABLE_PRIVILEGES WHERE TABLE_CATALOG LIKE ? ESCAPE ? AND TABLE_SCHEMA LIKE ? ESCAPE ? AND TABLE_NAME LIKE ? ESCAPE ? ORDER BY TABLE_SCHEM, TABLE_NAME, PRIVILEGE");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, getPattern(paramString3));
      localPreparedStatement.setString(6, "\\");
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getBestRowIdentifier(String paramString1, String paramString2, String paramString3, int paramInt, boolean paramBoolean)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getBestRowIdentifier(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ", " + paramInt + ", " + paramBoolean + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT CAST(? AS SMALLINT) SCOPE, C.COLUMN_NAME, C.DATA_TYPE, C.TYPE_NAME, C.CHARACTER_MAXIMUM_LENGTH COLUMN_SIZE, C.CHARACTER_MAXIMUM_LENGTH BUFFER_LENGTH, CAST(C.NUMERIC_SCALE AS SMALLINT) DECIMAL_DIGITS, CAST(? AS SMALLINT) PSEUDO_COLUMN FROM INFORMATION_SCHEMA.INDEXES I,  INFORMATION_SCHEMA.COLUMNS C WHERE C.TABLE_NAME = I.TABLE_NAME AND C.COLUMN_NAME = I.COLUMN_NAME AND C.TABLE_CATALOG LIKE ? ESCAPE ? AND C.TABLE_SCHEMA LIKE ? ESCAPE ? AND C.TABLE_NAME = ? AND I.PRIMARY_KEY = TRUE ORDER BY SCOPE");
      
      localPreparedStatement.setInt(1, 2);
      
      localPreparedStatement.setInt(2, 1);
      localPreparedStatement.setString(3, getCatalogPattern(paramString1));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, getSchemaPattern(paramString2));
      localPreparedStatement.setString(6, "\\");
      localPreparedStatement.setString(7, paramString3);
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getVersionColumns(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getVersionColumns(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT ZERO() SCOPE, COLUMN_NAME, CAST(DATA_TYPE AS INT) DATA_TYPE, TYPE_NAME, NUMERIC_PRECISION COLUMN_SIZE, NUMERIC_PRECISION BUFFER_LENGTH, NUMERIC_PRECISION DECIMAL_DIGITS, ZERO() PSEUDO_COLUMN FROM INFORMATION_SCHEMA.COLUMNS WHERE FALSE");
      
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getImportedKeys(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getImportedKeys(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT PKTABLE_CATALOG PKTABLE_CAT, PKTABLE_SCHEMA PKTABLE_SCHEM, PKTABLE_NAME PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_CATALOG FKTABLE_CAT, FKTABLE_SCHEMA FKTABLE_SCHEM, FKTABLE_NAME, FKCOLUMN_NAME, ORDINAL_POSITION KEY_SEQ, UPDATE_RULE, DELETE_RULE, FK_NAME, PK_NAME, DEFERRABILITY FROM INFORMATION_SCHEMA.CROSS_REFERENCES WHERE FKTABLE_CATALOG LIKE ? ESCAPE ? AND FKTABLE_SCHEMA LIKE ? ESCAPE ? AND FKTABLE_NAME = ? ORDER BY PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, FK_NAME, KEY_SEQ");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, paramString3);
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getExportedKeys(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getExportedKeys(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT PKTABLE_CATALOG PKTABLE_CAT, PKTABLE_SCHEMA PKTABLE_SCHEM, PKTABLE_NAME PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_CATALOG FKTABLE_CAT, FKTABLE_SCHEMA FKTABLE_SCHEM, FKTABLE_NAME, FKCOLUMN_NAME, ORDINAL_POSITION KEY_SEQ, UPDATE_RULE, DELETE_RULE, FK_NAME, PK_NAME, DEFERRABILITY FROM INFORMATION_SCHEMA.CROSS_REFERENCES WHERE PKTABLE_CATALOG LIKE ? ESCAPE ? AND PKTABLE_SCHEMA LIKE ? ESCAPE ? AND PKTABLE_NAME = ? ORDER BY FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, FK_NAME, KEY_SEQ");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, paramString3);
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getCrossReference(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, String paramString6)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getCrossReference(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ", " + quote(paramString4) + ", " + quote(paramString5) + ", " + quote(paramString6) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT PKTABLE_CATALOG PKTABLE_CAT, PKTABLE_SCHEMA PKTABLE_SCHEM, PKTABLE_NAME PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_CATALOG FKTABLE_CAT, FKTABLE_SCHEMA FKTABLE_SCHEM, FKTABLE_NAME, FKCOLUMN_NAME, ORDINAL_POSITION KEY_SEQ, UPDATE_RULE, DELETE_RULE, FK_NAME, PK_NAME, DEFERRABILITY FROM INFORMATION_SCHEMA.CROSS_REFERENCES WHERE PKTABLE_CATALOG LIKE ? ESCAPE ? AND PKTABLE_SCHEMA LIKE ? ESCAPE ? AND PKTABLE_NAME = ? AND FKTABLE_CATALOG LIKE ? ESCAPE ? AND FKTABLE_SCHEMA LIKE ? ESCAPE ? AND FKTABLE_NAME = ? ORDER BY FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, FK_NAME, KEY_SEQ");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      localPreparedStatement.setString(5, paramString3);
      localPreparedStatement.setString(6, getCatalogPattern(paramString4));
      localPreparedStatement.setString(7, "\\");
      localPreparedStatement.setString(8, getSchemaPattern(paramString5));
      localPreparedStatement.setString(9, "\\");
      localPreparedStatement.setString(10, paramString6);
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getUDTs(String paramString1, String paramString2, String paramString3, int[] paramArrayOfInt)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getUDTs(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ", " + quoteIntArray(paramArrayOfInt) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT CAST(NULL AS VARCHAR) TYPE_CAT, CAST(NULL AS VARCHAR) TYPE_SCHEM, CAST(NULL AS VARCHAR) TYPE_NAME, CAST(NULL AS VARCHAR) CLASS_NAME, CAST(NULL AS SMALLINT) DATA_TYPE, CAST(NULL AS VARCHAR) REMARKS, CAST(NULL AS SMALLINT) BASE_TYPE FROM DUAL WHERE FALSE");
      
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getTypeInfo()
    throws SQLException
  {
    try
    {
      debugCodeCall("getTypeInfo");
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT TYPE_NAME, DATA_TYPE, PRECISION, PREFIX LITERAL_PREFIX, SUFFIX LITERAL_SUFFIX, PARAMS CREATE_PARAMS, NULLABLE, CASE_SENSITIVE, SEARCHABLE, FALSE UNSIGNED_ATTRIBUTE, FALSE FIXED_PREC_SCALE, AUTO_INCREMENT, TYPE_NAME LOCAL_TYPE_NAME, MINIMUM_SCALE, MAXIMUM_SCALE, DATA_TYPE SQL_DATA_TYPE, ZERO() SQL_DATETIME_SUB, RADIX NUM_PREC_RADIX FROM INFORMATION_SCHEMA.TYPE_INFO ORDER BY DATA_TYPE, POS");
      
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean usesLocalFiles()
  {
    debugCodeCall("usesLocalFiles");
    return true;
  }
  
  public boolean usesLocalFilePerTable()
  {
    debugCodeCall("usesLocalFilePerTable");
    return false;
  }
  
  public String getIdentifierQuoteString()
  {
    debugCodeCall("getIdentifierQuoteString");
    return "\"";
  }
  
  public String getSQLKeywords()
  {
    debugCodeCall("getSQLKeywords");
    return "LIMIT,MINUS,ROWNUM,SYSDATE,SYSTIME,SYSTIMESTAMP,TODAY";
  }
  
  public String getNumericFunctions()
    throws SQLException
  {
    debugCodeCall("getNumericFunctions");
    return getFunctions("Functions (Numeric)");
  }
  
  public String getStringFunctions()
    throws SQLException
  {
    debugCodeCall("getStringFunctions");
    return getFunctions("Functions (String)");
  }
  
  public String getSystemFunctions()
    throws SQLException
  {
    debugCodeCall("getSystemFunctions");
    return getFunctions("Functions (System)");
  }
  
  public String getTimeDateFunctions()
    throws SQLException
  {
    debugCodeCall("getTimeDateFunctions");
    return getFunctions("Functions (Time and Date)");
  }
  
  private String getFunctions(String paramString)
    throws SQLException
  {
    try
    {
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT TOPIC FROM INFORMATION_SCHEMA.HELP WHERE SECTION = ?");
      
      localPreparedStatement.setString(1, paramString);
      ResultSet localResultSet = localPreparedStatement.executeQuery();
      StatementBuilder localStatementBuilder = new StatementBuilder();
      while (localResultSet.next())
      {
        String str1 = localResultSet.getString(1).trim();
        String[] arrayOfString1 = StringUtils.arraySplit(str1, ',', true);
        for (String str2 : arrayOfString1)
        {
          localStatementBuilder.appendExceptFirst(",");
          String str3 = str2.trim();
          if (str3.indexOf(' ') >= 0) {
            str3 = str3.substring(0, str3.indexOf(' ')).trim();
          }
          localStatementBuilder.append(str3);
        }
      }
      localResultSet.close();
      localPreparedStatement.close();
      return localStatementBuilder.toString();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getSearchStringEscape()
  {
    debugCodeCall("getSearchStringEscape");
    return "\\";
  }
  
  public String getExtraNameCharacters()
  {
    debugCodeCall("getExtraNameCharacters");
    return "";
  }
  
  public boolean supportsAlterTableWithAddColumn()
  {
    debugCodeCall("supportsAlterTableWithAddColumn");
    return true;
  }
  
  public boolean supportsAlterTableWithDropColumn()
  {
    debugCodeCall("supportsAlterTableWithDropColumn");
    return true;
  }
  
  public boolean supportsColumnAliasing()
  {
    debugCodeCall("supportsColumnAliasing");
    return true;
  }
  
  public boolean nullPlusNonNullIsNull()
  {
    debugCodeCall("nullPlusNonNullIsNull");
    return true;
  }
  
  public boolean supportsConvert()
  {
    debugCodeCall("supportsConvert");
    return true;
  }
  
  public boolean supportsConvert(int paramInt1, int paramInt2)
  {
    if (isDebugEnabled()) {
      debugCode("supportsConvert(" + paramInt1 + ", " + paramInt1 + ");");
    }
    return true;
  }
  
  public boolean supportsTableCorrelationNames()
  {
    debugCodeCall("supportsTableCorrelationNames");
    return true;
  }
  
  public boolean supportsDifferentTableCorrelationNames()
  {
    debugCodeCall("supportsDifferentTableCorrelationNames");
    return false;
  }
  
  public boolean supportsExpressionsInOrderBy()
  {
    debugCodeCall("supportsExpressionsInOrderBy");
    return true;
  }
  
  public boolean supportsOrderByUnrelated()
  {
    debugCodeCall("supportsOrderByUnrelated");
    return true;
  }
  
  public boolean supportsGroupBy()
  {
    debugCodeCall("supportsGroupBy");
    return true;
  }
  
  public boolean supportsGroupByUnrelated()
  {
    debugCodeCall("supportsGroupByUnrelated");
    return true;
  }
  
  public boolean supportsGroupByBeyondSelect()
  {
    debugCodeCall("supportsGroupByBeyondSelect");
    return true;
  }
  
  public boolean supportsLikeEscapeClause()
  {
    debugCodeCall("supportsLikeEscapeClause");
    return true;
  }
  
  public boolean supportsMultipleResultSets()
  {
    debugCodeCall("supportsMultipleResultSets");
    return false;
  }
  
  public boolean supportsMultipleTransactions()
  {
    debugCodeCall("supportsMultipleTransactions");
    return true;
  }
  
  public boolean supportsNonNullableColumns()
  {
    debugCodeCall("supportsNonNullableColumns");
    return true;
  }
  
  public boolean supportsMinimumSQLGrammar()
  {
    debugCodeCall("supportsMinimumSQLGrammar");
    return true;
  }
  
  public boolean supportsCoreSQLGrammar()
  {
    debugCodeCall("supportsCoreSQLGrammar");
    return true;
  }
  
  public boolean supportsExtendedSQLGrammar()
  {
    debugCodeCall("supportsExtendedSQLGrammar");
    return false;
  }
  
  public boolean supportsANSI92EntryLevelSQL()
  {
    debugCodeCall("supportsANSI92EntryLevelSQL");
    return true;
  }
  
  public boolean supportsANSI92IntermediateSQL()
  {
    debugCodeCall("supportsANSI92IntermediateSQL");
    return false;
  }
  
  public boolean supportsANSI92FullSQL()
  {
    debugCodeCall("supportsANSI92FullSQL");
    return false;
  }
  
  public boolean supportsIntegrityEnhancementFacility()
  {
    debugCodeCall("supportsIntegrityEnhancementFacility");
    return true;
  }
  
  public boolean supportsOuterJoins()
  {
    debugCodeCall("supportsOuterJoins");
    return true;
  }
  
  public boolean supportsFullOuterJoins()
  {
    debugCodeCall("supportsFullOuterJoins");
    return false;
  }
  
  public boolean supportsLimitedOuterJoins()
  {
    debugCodeCall("supportsLimitedOuterJoins");
    return true;
  }
  
  public String getSchemaTerm()
  {
    debugCodeCall("getSchemaTerm");
    return "schema";
  }
  
  public String getProcedureTerm()
  {
    debugCodeCall("getProcedureTerm");
    return "procedure";
  }
  
  public String getCatalogTerm()
  {
    debugCodeCall("getCatalogTerm");
    return "catalog";
  }
  
  public boolean isCatalogAtStart()
  {
    debugCodeCall("isCatalogAtStart");
    return true;
  }
  
  public String getCatalogSeparator()
  {
    debugCodeCall("getCatalogSeparator");
    return ".";
  }
  
  public boolean supportsSchemasInDataManipulation()
  {
    debugCodeCall("supportsSchemasInDataManipulation");
    return true;
  }
  
  public boolean supportsSchemasInProcedureCalls()
  {
    debugCodeCall("supportsSchemasInProcedureCalls");
    return true;
  }
  
  public boolean supportsSchemasInTableDefinitions()
  {
    debugCodeCall("supportsSchemasInTableDefinitions");
    return true;
  }
  
  public boolean supportsSchemasInIndexDefinitions()
  {
    debugCodeCall("supportsSchemasInIndexDefinitions");
    return true;
  }
  
  public boolean supportsSchemasInPrivilegeDefinitions()
  {
    debugCodeCall("supportsSchemasInPrivilegeDefinitions");
    return true;
  }
  
  public boolean supportsCatalogsInDataManipulation()
  {
    debugCodeCall("supportsCatalogsInDataManipulation");
    return true;
  }
  
  public boolean supportsCatalogsInProcedureCalls()
  {
    debugCodeCall("supportsCatalogsInProcedureCalls");
    return false;
  }
  
  public boolean supportsCatalogsInTableDefinitions()
  {
    debugCodeCall("supportsCatalogsInTableDefinitions");
    return true;
  }
  
  public boolean supportsCatalogsInIndexDefinitions()
  {
    debugCodeCall("supportsCatalogsInIndexDefinitions");
    return true;
  }
  
  public boolean supportsCatalogsInPrivilegeDefinitions()
  {
    debugCodeCall("supportsCatalogsInPrivilegeDefinitions");
    return true;
  }
  
  public boolean supportsPositionedDelete()
  {
    debugCodeCall("supportsPositionedDelete");
    return true;
  }
  
  public boolean supportsPositionedUpdate()
  {
    debugCodeCall("supportsPositionedUpdate");
    return true;
  }
  
  public boolean supportsSelectForUpdate()
  {
    debugCodeCall("supportsSelectForUpdate");
    return true;
  }
  
  public boolean supportsStoredProcedures()
  {
    debugCodeCall("supportsStoredProcedures");
    return false;
  }
  
  public boolean supportsSubqueriesInComparisons()
  {
    debugCodeCall("supportsSubqueriesInComparisons");
    return true;
  }
  
  public boolean supportsSubqueriesInExists()
  {
    debugCodeCall("supportsSubqueriesInExists");
    return true;
  }
  
  public boolean supportsSubqueriesInIns()
  {
    debugCodeCall("supportsSubqueriesInIns");
    return true;
  }
  
  public boolean supportsSubqueriesInQuantifieds()
  {
    debugCodeCall("supportsSubqueriesInQuantifieds");
    return true;
  }
  
  public boolean supportsCorrelatedSubqueries()
  {
    debugCodeCall("supportsCorrelatedSubqueries");
    return true;
  }
  
  public boolean supportsUnion()
  {
    debugCodeCall("supportsUnion");
    return true;
  }
  
  public boolean supportsUnionAll()
  {
    debugCodeCall("supportsUnionAll");
    return true;
  }
  
  public boolean supportsOpenCursorsAcrossCommit()
  {
    debugCodeCall("supportsOpenCursorsAcrossCommit");
    return false;
  }
  
  public boolean supportsOpenCursorsAcrossRollback()
  {
    debugCodeCall("supportsOpenCursorsAcrossRollback");
    return false;
  }
  
  public boolean supportsOpenStatementsAcrossCommit()
  {
    debugCodeCall("supportsOpenStatementsAcrossCommit");
    return true;
  }
  
  public boolean supportsOpenStatementsAcrossRollback()
  {
    debugCodeCall("supportsOpenStatementsAcrossRollback");
    return true;
  }
  
  public boolean supportsTransactions()
  {
    debugCodeCall("supportsTransactions");
    return true;
  }
  
  public boolean supportsTransactionIsolationLevel(int paramInt)
    throws SQLException
  {
    debugCodeCall("supportsTransactionIsolationLevel");
    if (paramInt == 1)
    {
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME=?");
      
      localPreparedStatement.setString(1, "MULTI_THREADED");
      ResultSet localResultSet = localPreparedStatement.executeQuery();
      if ((localResultSet.next()) && (localResultSet.getString(1).equals("1"))) {
        return false;
      }
    }
    return true;
  }
  
  public boolean supportsDataDefinitionAndDataManipulationTransactions()
  {
    debugCodeCall("supportsDataDefinitionAndDataManipulationTransactions");
    return false;
  }
  
  public boolean supportsDataManipulationTransactionsOnly()
  {
    debugCodeCall("supportsDataManipulationTransactionsOnly");
    return true;
  }
  
  public boolean dataDefinitionCausesTransactionCommit()
  {
    debugCodeCall("dataDefinitionCausesTransactionCommit");
    return true;
  }
  
  public boolean dataDefinitionIgnoredInTransactions()
  {
    debugCodeCall("dataDefinitionIgnoredInTransactions");
    return false;
  }
  
  public boolean supportsResultSetType(int paramInt)
  {
    debugCodeCall("supportsResultSetType", paramInt);
    return paramInt != 1005;
  }
  
  public boolean supportsResultSetConcurrency(int paramInt1, int paramInt2)
  {
    if (isDebugEnabled()) {
      debugCode("supportsResultSetConcurrency(" + paramInt1 + ", " + paramInt2 + ");");
    }
    return paramInt1 != 1005;
  }
  
  public boolean ownUpdatesAreVisible(int paramInt)
  {
    debugCodeCall("ownUpdatesAreVisible", paramInt);
    return true;
  }
  
  public boolean ownDeletesAreVisible(int paramInt)
  {
    debugCodeCall("ownDeletesAreVisible", paramInt);
    return false;
  }
  
  public boolean ownInsertsAreVisible(int paramInt)
  {
    debugCodeCall("ownInsertsAreVisible", paramInt);
    return false;
  }
  
  public boolean othersUpdatesAreVisible(int paramInt)
  {
    debugCodeCall("othersUpdatesAreVisible", paramInt);
    return false;
  }
  
  public boolean othersDeletesAreVisible(int paramInt)
  {
    debugCodeCall("othersDeletesAreVisible", paramInt);
    return false;
  }
  
  public boolean othersInsertsAreVisible(int paramInt)
  {
    debugCodeCall("othersInsertsAreVisible", paramInt);
    return false;
  }
  
  public boolean updatesAreDetected(int paramInt)
  {
    debugCodeCall("updatesAreDetected", paramInt);
    return false;
  }
  
  public boolean deletesAreDetected(int paramInt)
  {
    debugCodeCall("deletesAreDetected", paramInt);
    return false;
  }
  
  public boolean insertsAreDetected(int paramInt)
  {
    debugCodeCall("insertsAreDetected", paramInt);
    return false;
  }
  
  public boolean supportsBatchUpdates()
  {
    debugCodeCall("supportsBatchUpdates");
    return true;
  }
  
  public boolean doesMaxRowSizeIncludeBlobs()
  {
    debugCodeCall("doesMaxRowSizeIncludeBlobs");
    return false;
  }
  
  public int getDefaultTransactionIsolation()
  {
    debugCodeCall("getDefaultTransactionIsolation");
    return 2;
  }
  
  public boolean supportsMixedCaseIdentifiers()
  {
    debugCodeCall("supportsMixedCaseIdentifiers");
    return false;
  }
  
  public boolean supportsMixedCaseQuotedIdentifiers()
    throws SQLException
  {
    debugCodeCall("supportsMixedCaseQuotedIdentifiers");
    String str = getMode();
    if (str.equals("MySQL")) {
      return false;
    }
    return true;
  }
  
  public boolean storesUpperCaseIdentifiers()
    throws SQLException
  {
    debugCodeCall("storesUpperCaseIdentifiers");
    String str = getMode();
    if (str.equals("MySQL")) {
      return false;
    }
    return true;
  }
  
  public boolean storesLowerCaseIdentifiers()
    throws SQLException
  {
    debugCodeCall("storesLowerCaseIdentifiers");
    String str = getMode();
    if (str.equals("MySQL")) {
      return true;
    }
    return false;
  }
  
  public boolean storesMixedCaseIdentifiers()
  {
    debugCodeCall("storesMixedCaseIdentifiers");
    return false;
  }
  
  public boolean storesUpperCaseQuotedIdentifiers()
    throws SQLException
  {
    debugCodeCall("storesUpperCaseQuotedIdentifiers");
    String str = getMode();
    if (str.equals("MySQL")) {
      return true;
    }
    return false;
  }
  
  public boolean storesLowerCaseQuotedIdentifiers()
    throws SQLException
  {
    debugCodeCall("storesLowerCaseQuotedIdentifiers");
    String str = getMode();
    if (str.equals("MySQL")) {
      return true;
    }
    return false;
  }
  
  public boolean storesMixedCaseQuotedIdentifiers()
    throws SQLException
  {
    debugCodeCall("storesMixedCaseQuotedIdentifiers");
    String str = getMode();
    if (str.equals("MySQL")) {
      return false;
    }
    return true;
  }
  
  public int getMaxBinaryLiteralLength()
  {
    debugCodeCall("getMaxBinaryLiteralLength");
    return 0;
  }
  
  public int getMaxCharLiteralLength()
  {
    debugCodeCall("getMaxCharLiteralLength");
    return 0;
  }
  
  public int getMaxColumnNameLength()
  {
    debugCodeCall("getMaxColumnNameLength");
    return 0;
  }
  
  public int getMaxColumnsInGroupBy()
  {
    debugCodeCall("getMaxColumnsInGroupBy");
    return 0;
  }
  
  public int getMaxColumnsInIndex()
  {
    debugCodeCall("getMaxColumnsInIndex");
    return 0;
  }
  
  public int getMaxColumnsInOrderBy()
  {
    debugCodeCall("getMaxColumnsInOrderBy");
    return 0;
  }
  
  public int getMaxColumnsInSelect()
  {
    debugCodeCall("getMaxColumnsInSelect");
    return 0;
  }
  
  public int getMaxColumnsInTable()
  {
    debugCodeCall("getMaxColumnsInTable");
    return 0;
  }
  
  public int getMaxConnections()
  {
    debugCodeCall("getMaxConnections");
    return 0;
  }
  
  public int getMaxCursorNameLength()
  {
    debugCodeCall("getMaxCursorNameLength");
    return 0;
  }
  
  public int getMaxIndexLength()
  {
    debugCodeCall("getMaxIndexLength");
    return 0;
  }
  
  public int getMaxSchemaNameLength()
  {
    debugCodeCall("getMaxSchemaNameLength");
    return 0;
  }
  
  public int getMaxProcedureNameLength()
  {
    debugCodeCall("getMaxProcedureNameLength");
    return 0;
  }
  
  public int getMaxCatalogNameLength()
  {
    debugCodeCall("getMaxCatalogNameLength");
    return 0;
  }
  
  public int getMaxRowSize()
  {
    debugCodeCall("getMaxRowSize");
    return 0;
  }
  
  public int getMaxStatementLength()
  {
    debugCodeCall("getMaxStatementLength");
    return 0;
  }
  
  public int getMaxStatements()
  {
    debugCodeCall("getMaxStatements");
    return 0;
  }
  
  public int getMaxTableNameLength()
  {
    debugCodeCall("getMaxTableNameLength");
    return 0;
  }
  
  public int getMaxTablesInSelect()
  {
    debugCodeCall("getMaxTablesInSelect");
    return 0;
  }
  
  public int getMaxUserNameLength()
  {
    debugCodeCall("getMaxUserNameLength");
    return 0;
  }
  
  public boolean supportsSavepoints()
  {
    debugCodeCall("supportsSavepoints");
    return true;
  }
  
  public boolean supportsNamedParameters()
  {
    debugCodeCall("supportsNamedParameters");
    return false;
  }
  
  public boolean supportsMultipleOpenResults()
  {
    debugCodeCall("supportsMultipleOpenResults");
    return true;
  }
  
  public boolean supportsGetGeneratedKeys()
  {
    debugCodeCall("supportsGetGeneratedKeys");
    return true;
  }
  
  public ResultSet getSuperTypes(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    throw unsupported("superTypes");
  }
  
  public ResultSet getSuperTables(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("getSuperTables(" + quote(paramString1) + ", " + quote(paramString2) + ", " + quote(paramString3) + ");");
      }
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT CATALOG_NAME TABLE_CAT, CATALOG_NAME TABLE_SCHEM, CATALOG_NAME TABLE_NAME, CATALOG_NAME SUPERTABLE_NAME FROM INFORMATION_SCHEMA.CATALOGS WHERE FALSE");
      
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet getAttributes(String paramString1, String paramString2, String paramString3, String paramString4)
    throws SQLException
  {
    throw unsupported("attributes");
  }
  
  public boolean supportsResultSetHoldability(int paramInt)
  {
    debugCodeCall("supportsResultSetHoldability", paramInt);
    return paramInt == 2;
  }
  
  public int getResultSetHoldability()
  {
    debugCodeCall("getResultSetHoldability");
    return 2;
  }
  
  public int getDatabaseMajorVersion()
  {
    debugCodeCall("getDatabaseMajorVersion");
    return 1;
  }
  
  public int getDatabaseMinorVersion()
  {
    debugCodeCall("getDatabaseMinorVersion");
    return 4;
  }
  
  public int getJDBCMajorVersion()
  {
    debugCodeCall("getJDBCMajorVersion");
    return 4;
  }
  
  public int getJDBCMinorVersion()
  {
    debugCodeCall("getJDBCMinorVersion");
    return 0;
  }
  
  public int getSQLStateType()
  {
    debugCodeCall("getSQLStateType");
    return 2;
  }
  
  public boolean locatorsUpdateCopy()
  {
    debugCodeCall("locatorsUpdateCopy");
    return false;
  }
  
  public boolean supportsStatementPooling()
  {
    debugCodeCall("supportsStatementPooling");
    return false;
  }
  
  private void checkClosed()
  {
    this.conn.checkClosed();
  }
  
  private static String getPattern(String paramString)
  {
    return paramString == null ? "%" : paramString;
  }
  
  private static String getSchemaPattern(String paramString)
  {
    return paramString.length() == 0 ? "PUBLIC" : paramString == null ? "%" : paramString;
  }
  
  private static String getCatalogPattern(String paramString)
  {
    return (paramString == null) || (paramString.length() == 0) ? "%" : paramString;
  }
  
  public RowIdLifetime getRowIdLifetime()
  {
    debugCodeCall("getRowIdLifetime");
    return RowIdLifetime.ROWID_UNSUPPORTED;
  }
  
  public ResultSet getSchemas(String paramString1, String paramString2)
    throws SQLException
  {
    try
    {
      debugCodeCall("getSchemas(String,String)");
      checkClosed();
      PreparedStatement localPreparedStatement = this.conn.prepareAutoCloseStatement("SELECT SCHEMA_NAME TABLE_SCHEM, CATALOG_NAME TABLE_CATALOG,  IS_DEFAULT FROM INFORMATION_SCHEMA.SCHEMATA WHERE CATALOG_NAME LIKE ? ESCAPE ? AND SCHEMA_NAME LIKE ? ESCAPE ? ORDER BY SCHEMA_NAME");
      
      localPreparedStatement.setString(1, getCatalogPattern(paramString1));
      localPreparedStatement.setString(2, "\\");
      localPreparedStatement.setString(3, getSchemaPattern(paramString2));
      localPreparedStatement.setString(4, "\\");
      return localPreparedStatement.executeQuery();
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean supportsStoredFunctionsUsingCallSyntax()
  {
    debugCodeCall("supportsStoredFunctionsUsingCallSyntax");
    return true;
  }
  
  public boolean autoCommitFailureClosesAllResultSets()
  {
    debugCodeCall("autoCommitFailureClosesAllResultSets");
    return false;
  }
  
  public ResultSet getClientInfoProperties()
    throws SQLException
  {
    return new SimpleResultSet();
  }
  
  public <T> T unwrap(Class<T> paramClass)
    throws SQLException
  {
    if (isWrapperFor(paramClass)) {
      return this;
    }
    throw DbException.getInvalidValueException("iface", paramClass);
  }
  
  public boolean isWrapperFor(Class<?> paramClass)
    throws SQLException
  {
    return (paramClass != null) && (paramClass.isAssignableFrom(getClass()));
  }
  
  public ResultSet getFunctionColumns(String paramString1, String paramString2, String paramString3, String paramString4)
    throws SQLException
  {
    throw unsupported("getFunctionColumns");
  }
  
  public ResultSet getFunctions(String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    throw unsupported("getFunctions");
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": " + this.conn;
  }
  
  private String getMode()
    throws SQLException
  {
    if (this.mode == null)
    {
      PreparedStatement localPreparedStatement = this.conn.prepareStatement("SELECT VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME=?");
      
      localPreparedStatement.setString(1, "MODE");
      ResultSet localResultSet = localPreparedStatement.executeQuery();
      localResultSet.next();
      this.mode = localResultSet.getString(1);
      localPreparedStatement.close();
    }
    return this.mode;
  }
}
