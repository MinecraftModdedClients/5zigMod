package org.h2.bnf.context;

import java.util.HashMap;
import java.util.HashSet;
import org.h2.bnf.Bnf;
import org.h2.bnf.BnfVisitor;
import org.h2.bnf.Rule;
import org.h2.bnf.RuleElement;
import org.h2.bnf.RuleHead;
import org.h2.bnf.RuleList;
import org.h2.bnf.Sentence;
import org.h2.command.Parser;
import org.h2.message.DbException;
import org.h2.util.StringUtils;

public class DbContextRule
  implements Rule
{
  public static final int COLUMN = 0;
  public static final int TABLE = 1;
  public static final int TABLE_ALIAS = 2;
  public static final int NEW_TABLE_ALIAS = 3;
  public static final int COLUMN_ALIAS = 4;
  public static final int SCHEMA = 5;
  public static final int PROCEDURE = 6;
  private final DbContents contents;
  private final int type;
  private String columnType;
  
  public DbContextRule(DbContents paramDbContents, int paramInt)
  {
    this.contents = paramDbContents;
    this.type = paramInt;
  }
  
  public void setColumnType(String paramString)
  {
    this.columnType = paramString;
  }
  
  public void setLinks(HashMap<String, RuleHead> paramHashMap) {}
  
  public void accept(BnfVisitor paramBnfVisitor) {}
  
  public boolean autoComplete(Sentence paramSentence)
  {
    String str1 = paramSentence.getQuery();Object localObject1 = str1;
    String str2 = paramSentence.getQueryUpper();
    Object localObject2;
    Object localObject3;
    Object localObject5;
    Object localObject10;
    String str3;
    String str4;
    Object localObject4;
    switch (this.type)
    {
    case 5: 
      localObject2 = this.contents.getSchemas();
      localObject3 = null;
      localObject5 = null;
      for (Object localObject8 : localObject2)
      {
        localObject10 = StringUtils.toUpperEnglish(((DbSchema)localObject8).name);
        if (str2.startsWith((String)localObject10))
        {
          if ((localObject3 == null) || (((String)localObject10).length() > ((String)localObject3).length()))
          {
            localObject3 = localObject10;
            localObject5 = localObject8;
          }
        }
        else if (((((String)localObject1).length() == 0) || (((String)localObject10).startsWith(str2))) && 
          (((String)localObject1).length() < ((String)localObject10).length()))
        {
          paramSentence.add((String)localObject10, ((String)localObject10).substring(((String)localObject1).length()), this.type);
          paramSentence.add(((DbSchema)localObject8).quotedName + ".", ((DbSchema)localObject8).quotedName.substring(((String)localObject1).length()) + ".", 0);
        }
      }
      if (localObject3 != null)
      {
        paramSentence.setLastMatchedSchema((DbSchema)localObject5);
        localObject1 = ((String)localObject1).substring(((String)localObject3).length());
      }
      break;
    case 1: 
      localObject2 = paramSentence.getLastMatchedSchema();
      if (localObject2 == null) {
        localObject2 = this.contents.getDefaultSchema();
      }
      localObject3 = ((DbSchema)localObject2).getTables();
      localObject5 = null;
      ??? = null;
      for (localObject10 : localObject3)
      {
        str3 = str2;
        str4 = StringUtils.toUpperEnglish(((DbTableOrView)localObject10).getName());
        if (((DbTableOrView)localObject10).getQuotedName().length() > str4.length())
        {
          str4 = ((DbTableOrView)localObject10).getQuotedName();
          str3 = str1;
        }
        if (str3.startsWith(str4))
        {
          if ((localObject5 == null) || (str4.length() > ((String)localObject5).length()))
          {
            localObject5 = str4;
            ??? = localObject10;
          }
        }
        else if (((((String)localObject1).length() == 0) || (str4.startsWith(str3))) && 
          (((String)localObject1).length() < str4.length())) {
          paramSentence.add(((DbTableOrView)localObject10).getQuotedName(), ((DbTableOrView)localObject10).getQuotedName().substring(((String)localObject1).length()), 0);
        }
      }
      if (localObject5 != null)
      {
        paramSentence.setLastMatchedTable((DbTableOrView)???);
        paramSentence.addTable((DbTableOrView)???);
        localObject1 = ((String)localObject1).substring(((String)localObject5).length());
      }
      break;
    case 3: 
      localObject1 = autoCompleteTableAlias(paramSentence, true);
      break;
    case 2: 
      localObject1 = autoCompleteTableAlias(paramSentence, false);
      break;
    case 4: 
      int i = 0;
      if (str1.indexOf(' ') >= 0)
      {
        for (; i < str2.length(); i++)
        {
          char c = str2.charAt(i);
          if ((c != '_') && (!Character.isLetterOrDigit(c))) {
            break;
          }
        }
        if (i != 0)
        {
          localObject4 = str2.substring(0, i);
          if (!Parser.isKeyword((String)localObject4, true)) {
            localObject1 = ((String)localObject1).substring(((String)localObject4).length());
          }
        }
      }
      break;
    case 0: 
      HashSet localHashSet = paramSentence.getTables();
      localObject4 = null;
      localObject5 = paramSentence.getLastMatchedTable();
      Object localObject9;
      if ((localObject5 != null) && (((DbTableOrView)localObject5).getColumns() != null)) {
        for (localObject9 : ((DbTableOrView)localObject5).getColumns())
        {
          localObject10 = str2;
          str3 = StringUtils.toUpperEnglish(((DbColumn)localObject9).getName());
          if (((DbColumn)localObject9).getQuotedName().length() > str3.length())
          {
            str3 = ((DbColumn)localObject9).getQuotedName();
            localObject10 = str1;
          }
          if ((((String)localObject10).startsWith(str3)) && ((this.columnType == null) || (((DbColumn)localObject9).getDataType().contains(this.columnType))))
          {
            str4 = ((String)localObject1).substring(str3.length());
            if ((localObject4 == null) || (str4.length() < ((String)localObject4).length())) {
              localObject4 = str4;
            } else if (((((String)localObject1).length() == 0) || (str3.startsWith((String)localObject10))) && 
              (((String)localObject1).length() < str3.length())) {
              paramSentence.add(((DbColumn)localObject9).getName(), ((DbColumn)localObject9).getName().substring(((String)localObject1).length()), 0);
            }
          }
        }
      }
      for (localObject9 : this.contents.getSchemas()) {
        for (Object localObject11 : ((DbSchema)localObject9).getTables()) {
          if ((localObject11 == localObject5) || (localHashSet == null) || (localHashSet.contains(localObject11))) {
            if ((localObject11 != null) && (((DbTableOrView)localObject11).getColumns() != null)) {
              for (DbColumn localDbColumn : ((DbTableOrView)localObject11).getColumns())
              {
                String str5 = StringUtils.toUpperEnglish(localDbColumn.getName());
                if ((this.columnType == null) || (localDbColumn.getDataType().contains(this.columnType))) {
                  if (str2.startsWith(str5))
                  {
                    String str6 = ((String)localObject1).substring(str5.length());
                    if ((localObject4 == null) || (str6.length() < ((String)localObject4).length())) {
                      localObject4 = str6;
                    }
                  }
                  else if (((((String)localObject1).length() == 0) || (str5.startsWith(str2))) && 
                    (((String)localObject1).length() < str5.length()))
                  {
                    paramSentence.add(localDbColumn.getName(), localDbColumn.getName().substring(((String)localObject1).length()), 0);
                  }
                }
              }
            }
          }
        }
      }
      if (localObject4 != null) {
        localObject1 = localObject4;
      }
      break;
    case 6: 
      autoCompleteProcedure(paramSentence);
      break;
    default: 
      throw DbException.throwInternalError("type=" + this.type);
    }
    if (!((String)localObject1).equals(str1))
    {
      while (Bnf.startWithSpace((String)localObject1)) {
        localObject1 = ((String)localObject1).substring(1);
      }
      paramSentence.setQuery((String)localObject1);
      return true;
    }
    return false;
  }
  
  private void autoCompleteProcedure(Sentence paramSentence)
  {
    DbSchema localDbSchema = paramSentence.getLastMatchedSchema();
    if (localDbSchema == null) {
      localDbSchema = this.contents.getDefaultSchema();
    }
    String str1 = paramSentence.getQueryUpper();
    String str2 = str1;
    if (str1.contains("(")) {
      str2 = str1.substring(0, str1.indexOf('(')).trim();
    }
    RuleElement localRuleElement1 = new RuleElement("(", "Function");
    RuleElement localRuleElement2 = new RuleElement(")", "Function");
    RuleElement localRuleElement3 = new RuleElement(",", "Function");
    for (DbProcedure localDbProcedure : localDbSchema.getProcedures())
    {
      String str3 = localDbProcedure.getName();
      if (str3.startsWith(str2))
      {
        RuleElement localRuleElement4 = new RuleElement(str3, "Function");
        
        RuleList localRuleList = new RuleList(localRuleElement4, localRuleElement1, false);
        if (str1.contains("("))
        {
          for (DbColumn localDbColumn : localDbProcedure.getParameters())
          {
            if (localDbColumn.getPosition() > 1) {
              localRuleList = new RuleList(localRuleList, localRuleElement3, false);
            }
            DbContextRule localDbContextRule = new DbContextRule(this.contents, 0);
            
            String str4 = localDbColumn.getDataType();
            if (str4.contains("(")) {
              str4 = str4.substring(0, str4.indexOf('('));
            }
            localDbContextRule.setColumnType(str4);
            localRuleList = new RuleList(localRuleList, localDbContextRule, false);
          }
          localRuleList = new RuleList(localRuleList, localRuleElement2, false);
        }
        localRuleList.autoComplete(paramSentence);
      }
    }
  }
  
  private static String autoCompleteTableAlias(Sentence paramSentence, boolean paramBoolean)
  {
    String str1 = paramSentence.getQuery();
    String str2 = paramSentence.getQueryUpper();
    for (int i = 0; i < str2.length(); i++)
    {
      char c = str2.charAt(i);
      if ((c != '_') && (!Character.isLetterOrDigit(c))) {
        break;
      }
    }
    if (i == 0) {
      return str1;
    }
    String str3 = str2.substring(0, i);
    if (("SET".equals(str3)) || (Parser.isKeyword(str3, true))) {
      return str1;
    }
    if (paramBoolean) {
      paramSentence.addAlias(str3, paramSentence.getLastTable());
    }
    HashMap localHashMap = paramSentence.getAliases();
    if (((localHashMap != null) && (localHashMap.containsKey(str3))) || (paramSentence.getLastTable() == null))
    {
      if ((paramBoolean) && (str1.length() == str3.length())) {
        return str1;
      }
      str1 = str1.substring(str3.length());
      if (str1.length() == 0) {
        paramSentence.add(str3 + ".", ".", 0);
      }
      return str1;
    }
    HashSet localHashSet = paramSentence.getTables();
    if (localHashSet != null)
    {
      Object localObject = null;
      for (DbTableOrView localDbTableOrView : localHashSet)
      {
        String str4 = StringUtils.toUpperEnglish(localDbTableOrView.getName());
        if ((str3.startsWith(str4)) && ((localObject == null) || (str4.length() > ((String)localObject).length())))
        {
          paramSentence.setLastMatchedTable(localDbTableOrView);
          localObject = str4;
        }
        else if ((str1.length() == 0) || (str4.startsWith(str3)))
        {
          paramSentence.add(str4 + ".", str4.substring(str1.length()) + ".", 0);
        }
      }
      if (localObject != null)
      {
        str1 = str1.substring(((String)localObject).length());
        if (str1.length() == 0) {
          paramSentence.add(str3 + ".", ".", 0);
        }
        return str1;
      }
    }
    return str1;
  }
}
