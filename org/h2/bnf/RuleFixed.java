package org.h2.bnf;

import java.util.HashMap;

public class RuleFixed
  implements Rule
{
  public static final int YMD = 0;
  public static final int HMS = 1;
  public static final int NANOS = 2;
  public static final int ANY_EXCEPT_SINGLE_QUOTE = 3;
  public static final int ANY_EXCEPT_DOUBLE_QUOTE = 4;
  public static final int ANY_UNTIL_EOL = 5;
  public static final int ANY_UNTIL_END = 6;
  public static final int ANY_WORD = 7;
  public static final int ANY_EXCEPT_2_DOLLAR = 8;
  public static final int HEX_START = 10;
  public static final int CONCAT = 11;
  public static final int AZ_UNDERSCORE = 12;
  public static final int AF = 13;
  public static final int DIGIT = 14;
  public static final int OPEN_BRACKET = 15;
  public static final int CLOSE_BRACKET = 16;
  private final int type;
  
  RuleFixed(int paramInt)
  {
    this.type = paramInt;
  }
  
  public void accept(BnfVisitor paramBnfVisitor)
  {
    paramBnfVisitor.visitRuleFixed(this.type);
  }
  
  public void setLinks(HashMap<String, RuleHead> paramHashMap) {}
  
  public boolean autoComplete(Sentence paramSentence)
  {
    paramSentence.stopIfRequired();
    String str1 = paramSentence.getQuery();
    String str2 = str1;
    switch (this.type)
    {
    case 0: 
      while ((str2.length() > 0) && ("0123456789-".indexOf(str2.charAt(0)) >= 0)) {
        str2 = str2.substring(1);
      }
      if (str2.length() == 0) {
        paramSentence.add("2006-01-01", "1", 1);
      }
      break;
    case 1: 
      while ((str2.length() > 0) && ("0123456789:".indexOf(str2.charAt(0)) >= 0)) {
        str2 = str2.substring(1);
      }
      if (str2.length() == 0) {
        paramSentence.add("12:00:00", "1", 1);
      }
      break;
    case 2: 
      while ((str2.length() > 0) && (Character.isDigit(str2.charAt(0)))) {
        str2 = str2.substring(1);
      }
      if (str2.length() == 0) {
        paramSentence.add("nanoseconds", "0", 1);
      }
      break;
    case 3: 
      for (;;)
      {
        if ((str2.length() > 0) && (str2.charAt(0) != '\''))
        {
          str2 = str2.substring(1);
        }
        else
        {
          if (!str2.startsWith("''")) {
            break;
          }
          str2 = str2.substring(2);
        }
      }
      if (str2.length() == 0)
      {
        paramSentence.add("anything", "Hello World", 1);
        paramSentence.add("'", "'", 1);
      }
      break;
    case 8: 
      while ((str2.length() > 0) && (!str2.startsWith("$$"))) {
        str2 = str2.substring(1);
      }
      if (str2.length() == 0)
      {
        paramSentence.add("anything", "Hello World", 1);
        paramSentence.add("$$", "$$", 1);
      }
      break;
    case 4: 
      for (;;)
      {
        if ((str2.length() > 0) && (str2.charAt(0) != '"'))
        {
          str2 = str2.substring(1);
        }
        else
        {
          if (!str2.startsWith("\"\"")) {
            break;
          }
          str2 = str2.substring(2);
        }
      }
      if (str2.length() == 0)
      {
        paramSentence.add("anything", "identifier", 1);
        paramSentence.add("\"", "\"", 1);
      }
      break;
    case 7: 
      while ((str2.length() > 0) && (!Bnf.startWithSpace(str2))) {
        str2 = str2.substring(1);
      }
      if (str2.length() == 0) {
        paramSentence.add("anything", "anything", 1);
      }
      break;
    case 10: 
      if ((str2.startsWith("0X")) || (str2.startsWith("0x"))) {
        str2 = str2.substring(2);
      } else if ("0".equals(str2)) {
        paramSentence.add("0x", "x", 1);
      } else if (str2.length() == 0) {
        paramSentence.add("0x", "0x", 1);
      }
      break;
    case 11: 
      if (str2.equals("|")) {
        paramSentence.add("||", "|", 1);
      } else if (str2.startsWith("||")) {
        str2 = str2.substring(2);
      } else if (str2.length() == 0) {
        paramSentence.add("||", "||", 1);
      }
      break;
    case 12: 
      if ((str2.length() > 0) && ((Character.isLetter(str2.charAt(0))) || (str2.charAt(0) == '_'))) {
        str2 = str2.substring(1);
      }
      if (str2.length() == 0) {
        paramSentence.add("character", "A", 1);
      }
      break;
    case 13: 
      if (str2.length() > 0)
      {
        int i = Character.toUpperCase(str2.charAt(0));
        if ((i >= 65) && (i <= 70)) {
          str2 = str2.substring(1);
        }
      }
      if (str2.length() == 0) {
        paramSentence.add("hex character", "0A", 1);
      }
      break;
    case 14: 
      if ((str2.length() > 0) && (Character.isDigit(str2.charAt(0)))) {
        str2 = str2.substring(1);
      }
      if (str2.length() == 0) {
        paramSentence.add("digit", "1", 1);
      }
      break;
    case 15: 
      if (str2.length() == 0) {
        paramSentence.add("[", "[", 1);
      } else if (str2.charAt(0) == '[') {
        str2 = str2.substring(1);
      }
      break;
    case 16: 
      if (str2.length() == 0) {
        paramSentence.add("]", "]", 1);
      } else if (str2.charAt(0) == ']') {
        str2 = str2.substring(1);
      }
      break;
    case 5: 
    case 6: 
    case 9: 
    default: 
      throw new AssertionError("type=" + this.type);
    }
    if (!str2.equals(str1))
    {
      while (Bnf.startWithSpace(str2)) {
        str2 = str2.substring(1);
      }
      paramSentence.setQuery(str2);
      return true;
    }
    return false;
  }
}
