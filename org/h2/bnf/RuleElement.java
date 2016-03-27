package org.h2.bnf;

import java.util.HashMap;
import org.h2.util.StringUtils;

public class RuleElement
  implements Rule
{
  private final boolean keyword;
  private final String name;
  private Rule link;
  private final int type;
  
  public RuleElement(String paramString1, String paramString2)
  {
    this.name = paramString1;
    this.keyword = ((paramString1.length() == 1) || (paramString1.equals(StringUtils.toUpperEnglish(paramString1))));
    
    paramString2 = StringUtils.toLowerEnglish(paramString2);
    this.type = (paramString2.startsWith("function") ? 2 : 1);
  }
  
  public void accept(BnfVisitor paramBnfVisitor)
  {
    paramBnfVisitor.visitRuleElement(this.keyword, this.name, this.link);
  }
  
  public void setLinks(HashMap<String, RuleHead> paramHashMap)
  {
    if (this.link != null) {
      this.link.setLinks(paramHashMap);
    }
    if (this.keyword) {
      return;
    }
    String str1 = Bnf.getRuleMapKey(this.name);
    for (int i = 0; i < str1.length(); i++)
    {
      String str2 = str1.substring(i);
      RuleHead localRuleHead = (RuleHead)paramHashMap.get(str2);
      if (localRuleHead != null)
      {
        this.link = localRuleHead.getRule();
        return;
      }
    }
    throw new AssertionError("Unknown " + this.name + "/" + str1);
  }
  
  public boolean autoComplete(Sentence paramSentence)
  {
    paramSentence.stopIfRequired();
    if (this.keyword)
    {
      String str1 = paramSentence.getQuery();
      String str2 = str1.trim();
      String str3 = paramSentence.getQueryUpper().trim();
      if (str3.startsWith(this.name))
      {
        str1 = str1.substring(this.name.length());
        while ((!"_".equals(this.name)) && (Bnf.startWithSpace(str1))) {
          str1 = str1.substring(1);
        }
        paramSentence.setQuery(str1);
        return true;
      }
      if (((str2.length() == 0) || (this.name.startsWith(str3))) && 
        (str2.length() < this.name.length())) {
        paramSentence.add(this.name, this.name.substring(str2.length()), this.type);
      }
      return false;
    }
    return this.link.autoComplete(paramSentence);
  }
}
