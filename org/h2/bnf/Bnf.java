package org.h2.bnf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.h2.bnf.context.DbContextRule;
import org.h2.tools.Csv;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class Bnf
{
  private final HashMap<String, RuleHead> ruleMap = New.hashMap();
  private String syntax;
  private String currentToken;
  private String[] tokens;
  private char firstChar;
  private int index;
  private Rule lastRepeat;
  private ArrayList<RuleHead> statements;
  private String currentTopic;
  
  public static Bnf getInstance(Reader paramReader)
    throws SQLException, IOException
  {
    Bnf localBnf = new Bnf();
    if (paramReader == null)
    {
      byte[] arrayOfByte = Utils.getResource("/org/h2/res/help.csv");
      paramReader = new InputStreamReader(new ByteArrayInputStream(arrayOfByte));
    }
    localBnf.parse(paramReader);
    return localBnf;
  }
  
  private void addFixedRule(String paramString, int paramInt)
  {
    RuleFixed localRuleFixed = new RuleFixed(paramInt);
    addRule(paramString, "Fixed", localRuleFixed);
  }
  
  private RuleHead addRule(String paramString1, String paramString2, Rule paramRule)
  {
    RuleHead localRuleHead = new RuleHead(paramString2, paramString1, paramRule);
    String str = StringUtils.toLowerEnglish(paramString1.trim().replace(' ', '_'));
    if (this.ruleMap.get(str) != null) {
      throw new AssertionError("already exists: " + paramString1);
    }
    this.ruleMap.put(str, localRuleHead);
    return localRuleHead;
  }
  
  private void parse(Reader paramReader)
    throws SQLException, IOException
  {
    Object localObject1 = null;
    this.statements = New.arrayList();
    Csv localCsv = new Csv();
    localCsv.setLineCommentCharacter('#');
    ResultSet localResultSet = localCsv.read(paramReader, null);
    while (localResultSet.next())
    {
      String str1 = localResultSet.getString("SECTION").trim();
      if (!str1.startsWith("System"))
      {
        String str2 = localResultSet.getString("TOPIC");
        this.syntax = localResultSet.getString("SYNTAX").trim();
        this.currentTopic = str1;
        this.tokens = tokenize();
        this.index = 0;
        Object localObject2 = parseRule();
        if (str1.startsWith("Command")) {
          localObject2 = new RuleList((Rule)localObject2, new RuleElement(";\n\n", this.currentTopic), false);
        }
        RuleHead localRuleHead = addRule(str2, str1, (Rule)localObject2);
        if (str1.startsWith("Function"))
        {
          if (localObject1 == null) {
            localObject1 = localObject2;
          } else {
            localObject1 = new RuleList((Rule)localObject2, (Rule)localObject1, true);
          }
        }
        else if (str1.startsWith("Commands")) {
          this.statements.add(localRuleHead);
        }
      }
    }
    addRule("@func@", "Function", (Rule)localObject1);
    addFixedRule("@ymd@", 0);
    addFixedRule("@hms@", 1);
    addFixedRule("@nanos@", 2);
    addFixedRule("anything_except_single_quote", 3);
    addFixedRule("anything_except_double_quote", 4);
    addFixedRule("anything_until_end_of_line", 5);
    addFixedRule("anything_until_end_comment", 6);
    addFixedRule("anything_except_two_dollar_signs", 8);
    addFixedRule("anything", 7);
    addFixedRule("@hex_start@", 10);
    addFixedRule("@concat@", 11);
    addFixedRule("@az_@", 12);
    addFixedRule("@af@", 13);
    addFixedRule("@digit@", 14);
    addFixedRule("@open_bracket@", 15);
    addFixedRule("@close_bracket@", 16);
  }
  
  public void visit(BnfVisitor paramBnfVisitor, String paramString)
  {
    this.syntax = paramString;
    this.tokens = tokenize();
    this.index = 0;
    Rule localRule = parseRule();
    localRule.setLinks(this.ruleMap);
    localRule.accept(paramBnfVisitor);
  }
  
  public static boolean startWithSpace(String paramString)
  {
    return (paramString.length() > 0) && (Character.isWhitespace(paramString.charAt(0)));
  }
  
  public static String getRuleMapKey(String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (char c : paramString.toCharArray()) {
      if (Character.isUpperCase(c)) {
        localStringBuilder.append('_').append(Character.toLowerCase(c));
      } else {
        localStringBuilder.append(c);
      }
    }
    return localStringBuilder.toString();
  }
  
  public RuleHead getRuleHead(String paramString)
  {
    return (RuleHead)this.ruleMap.get(paramString);
  }
  
  private Rule parseRule()
  {
    read();
    return parseOr();
  }
  
  private Rule parseOr()
  {
    Object localObject = parseList();
    if (this.firstChar == '|')
    {
      read();
      localObject = new RuleList((Rule)localObject, parseOr(), true);
    }
    this.lastRepeat = ((Rule)localObject);
    return (Rule)localObject;
  }
  
  private Rule parseList()
  {
    Object localObject = parseToken();
    if ((this.firstChar != '|') && (this.firstChar != ']') && (this.firstChar != '}') && (this.firstChar != 0)) {
      localObject = new RuleList((Rule)localObject, parseList(), false);
    }
    this.lastRepeat = ((Rule)localObject);
    return (Rule)localObject;
  }
  
  private Rule parseToken()
  {
    Object localObject;
    if (((this.firstChar >= 'A') && (this.firstChar <= 'Z')) || ((this.firstChar >= 'a') && (this.firstChar <= 'z')))
    {
      localObject = new RuleElement(this.currentToken, this.currentTopic);
    }
    else if (this.firstChar == '[')
    {
      read();
      Rule localRule = parseOr();
      localObject = new RuleOptional(localRule);
      if (this.firstChar != ']') {
        throw new AssertionError("expected ], got " + this.currentToken + " syntax:" + this.syntax);
      }
    }
    else if (this.firstChar == '{')
    {
      read();
      localObject = parseOr();
      if (this.firstChar != '}') {
        throw new AssertionError("expected }, got " + this.currentToken + " syntax:" + this.syntax);
      }
    }
    else if ("@commaDots@".equals(this.currentToken))
    {
      localObject = new RuleList(new RuleElement(",", this.currentTopic), this.lastRepeat, false);
      localObject = new RuleRepeat((Rule)localObject, true);
    }
    else if ("@dots@".equals(this.currentToken))
    {
      localObject = new RuleRepeat(this.lastRepeat, false);
    }
    else
    {
      localObject = new RuleElement(this.currentToken, this.currentTopic);
    }
    this.lastRepeat = ((Rule)localObject);
    read();
    return (Rule)localObject;
  }
  
  private void read()
  {
    if (this.index < this.tokens.length)
    {
      this.currentToken = this.tokens[(this.index++)];
      this.firstChar = this.currentToken.charAt(0);
    }
    else
    {
      this.currentToken = "";
      this.firstChar = '\000';
    }
  }
  
  private String[] tokenize()
  {
    ArrayList localArrayList = New.arrayList();
    this.syntax = StringUtils.replaceAll(this.syntax, "yyyy-MM-dd", "@ymd@");
    this.syntax = StringUtils.replaceAll(this.syntax, "hh:mm:ss", "@hms@");
    this.syntax = StringUtils.replaceAll(this.syntax, "nnnnnnnnn", "@nanos@");
    this.syntax = StringUtils.replaceAll(this.syntax, "function", "@func@");
    this.syntax = StringUtils.replaceAll(this.syntax, "0x", "@hexStart@");
    this.syntax = StringUtils.replaceAll(this.syntax, ",...", "@commaDots@");
    this.syntax = StringUtils.replaceAll(this.syntax, "...", "@dots@");
    this.syntax = StringUtils.replaceAll(this.syntax, "||", "@concat@");
    this.syntax = StringUtils.replaceAll(this.syntax, "a-z|_", "@az_@");
    this.syntax = StringUtils.replaceAll(this.syntax, "A-Z|_", "@az_@");
    this.syntax = StringUtils.replaceAll(this.syntax, "A-F", "@af@");
    this.syntax = StringUtils.replaceAll(this.syntax, "0-9", "@digit@");
    this.syntax = StringUtils.replaceAll(this.syntax, "'['", "@openBracket@");
    this.syntax = StringUtils.replaceAll(this.syntax, "']'", "@closeBracket@");
    StringTokenizer localStringTokenizer = getTokenizer(this.syntax);
    while (localStringTokenizer.hasMoreTokens())
    {
      String str = localStringTokenizer.nextToken();
      
      str = StringUtils.cache(str);
      if ((str.length() != 1) || 
        (" \r\n".indexOf(str.charAt(0)) < 0)) {
        localArrayList.add(str);
      }
    }
    return (String[])localArrayList.toArray(new String[localArrayList.size()]);
  }
  
  public HashMap<String, String> getNextTokenList(String paramString)
  {
    Sentence localSentence = new Sentence();
    localSentence.setQuery(paramString);
    try
    {
      for (RuleHead localRuleHead : this.statements) {
        if (localRuleHead.getSection().startsWith("Commands"))
        {
          localSentence.start();
          if (localRuleHead.getRule().autoComplete(localSentence)) {
            break;
          }
        }
      }
    }
    catch (IllegalStateException localIllegalStateException) {}
    return localSentence.getNext();
  }
  
  public void linkStatements()
  {
    for (RuleHead localRuleHead : this.ruleMap.values()) {
      localRuleHead.getRule().setLinks(this.ruleMap);
    }
  }
  
  public void updateTopic(String paramString, DbContextRule paramDbContextRule)
  {
    paramString = StringUtils.toLowerEnglish(paramString);
    RuleHead localRuleHead = (RuleHead)this.ruleMap.get(paramString);
    if (localRuleHead == null)
    {
      localRuleHead = new RuleHead("db", paramString, paramDbContextRule);
      this.ruleMap.put(paramString, localRuleHead);
      this.statements.add(localRuleHead);
    }
    else
    {
      localRuleHead.setRule(paramDbContextRule);
    }
  }
  
  public ArrayList<RuleHead> getStatements()
  {
    return this.statements;
  }
  
  public static StringTokenizer getTokenizer(String paramString)
  {
    return new StringTokenizer(paramString, " [](){}|.,\r\n<>:-+*/=<\">!'$", true);
  }
}
