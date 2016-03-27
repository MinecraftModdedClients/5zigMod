package org.h2.engine;

import java.util.HashMap;
import org.h2.util.New;
import org.h2.util.StringUtils;

public class Mode
{
  static final String REGULAR = "REGULAR";
  private static final HashMap<String, Mode> MODES = ;
  public boolean aliasColumnName;
  public boolean convertInsertNullToZero;
  public boolean convertOnlyToSmallerScale;
  public boolean indexDefinitionInCreateTable;
  public boolean lowerCaseIdentifiers;
  public boolean nullConcatIsNull;
  public boolean squareBracketQuotedNames;
  public boolean supportOffsetFetch = true;
  public boolean systemColumns;
  public boolean uniqueIndexSingleNull;
  public boolean uniqueIndexSingleNullExceptAllColumnsAreNull;
  public boolean treatEmptyStringsAsNull;
  public boolean sysDummy1;
  public boolean allowPlusForStringConcat;
  public boolean logIsLogBase10;
  public boolean serialColumnIsNotPK;
  public boolean swapConvertFunctionParameters;
  public boolean isolationLevelInSelectOrInsertStatement;
  public boolean onDuplicateKeyUpdate;
  private final String name;
  
  static
  {
    Mode localMode = new Mode("REGULAR");
    localMode.nullConcatIsNull = true;
    add(localMode);
    
    localMode = new Mode("DB2");
    localMode.aliasColumnName = true;
    localMode.supportOffsetFetch = true;
    localMode.sysDummy1 = true;
    localMode.isolationLevelInSelectOrInsertStatement = true;
    add(localMode);
    
    localMode = new Mode("Derby");
    localMode.aliasColumnName = true;
    localMode.uniqueIndexSingleNull = true;
    localMode.supportOffsetFetch = true;
    localMode.sysDummy1 = true;
    localMode.isolationLevelInSelectOrInsertStatement = true;
    add(localMode);
    
    localMode = new Mode("HSQLDB");
    localMode.aliasColumnName = true;
    localMode.convertOnlyToSmallerScale = true;
    localMode.nullConcatIsNull = true;
    localMode.uniqueIndexSingleNull = true;
    localMode.allowPlusForStringConcat = true;
    add(localMode);
    
    localMode = new Mode("MSSQLServer");
    localMode.aliasColumnName = true;
    localMode.squareBracketQuotedNames = true;
    localMode.uniqueIndexSingleNull = true;
    localMode.allowPlusForStringConcat = true;
    localMode.swapConvertFunctionParameters = true;
    add(localMode);
    
    localMode = new Mode("MySQL");
    localMode.convertInsertNullToZero = true;
    localMode.indexDefinitionInCreateTable = true;
    localMode.lowerCaseIdentifiers = true;
    localMode.onDuplicateKeyUpdate = true;
    add(localMode);
    
    localMode = new Mode("Oracle");
    localMode.aliasColumnName = true;
    localMode.convertOnlyToSmallerScale = true;
    localMode.uniqueIndexSingleNullExceptAllColumnsAreNull = true;
    localMode.treatEmptyStringsAsNull = true;
    add(localMode);
    
    localMode = new Mode("PostgreSQL");
    localMode.aliasColumnName = true;
    localMode.nullConcatIsNull = true;
    localMode.supportOffsetFetch = true;
    localMode.systemColumns = true;
    localMode.logIsLogBase10 = true;
    localMode.serialColumnIsNotPK = true;
    add(localMode);
  }
  
  private Mode(String paramString)
  {
    this.name = paramString;
  }
  
  private static void add(Mode paramMode)
  {
    MODES.put(StringUtils.toUpperEnglish(paramMode.name), paramMode);
  }
  
  public static Mode getInstance(String paramString)
  {
    return (Mode)MODES.get(StringUtils.toUpperEnglish(paramString));
  }
  
  public String getName()
  {
    return this.name;
  }
}
