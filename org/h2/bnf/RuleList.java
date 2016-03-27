package org.h2.bnf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.h2.util.New;

public class RuleList
  implements Rule
{
  private final boolean or;
  private final ArrayList<Rule> list;
  private boolean mapSet;
  
  public RuleList(Rule paramRule1, Rule paramRule2, boolean paramBoolean)
  {
    this.list = New.arrayList();
    if (((paramRule1 instanceof RuleList)) && (((RuleList)paramRule1).or == paramBoolean)) {
      this.list.addAll(((RuleList)paramRule1).list);
    } else {
      this.list.add(paramRule1);
    }
    if (((paramRule2 instanceof RuleList)) && (((RuleList)paramRule2).or == paramBoolean)) {
      this.list.addAll(((RuleList)paramRule2).list);
    } else {
      this.list.add(paramRule2);
    }
    this.or = paramBoolean;
  }
  
  public void accept(BnfVisitor paramBnfVisitor)
  {
    paramBnfVisitor.visitRuleList(this.or, this.list);
  }
  
  public void setLinks(HashMap<String, RuleHead> paramHashMap)
  {
    if (!this.mapSet)
    {
      for (Rule localRule : this.list) {
        localRule.setLinks(paramHashMap);
      }
      this.mapSet = true;
    }
  }
  
  public boolean autoComplete(Sentence paramSentence)
  {
    paramSentence.stopIfRequired();
    String str = paramSentence.getQuery();
    Rule localRule;
    if (this.or)
    {
      for (localIterator = this.list.iterator(); localIterator.hasNext();)
      {
        localRule = (Rule)localIterator.next();
        paramSentence.setQuery(str);
        if (localRule.autoComplete(paramSentence)) {
          return true;
        }
      }
      return false;
    }
    for (Iterator localIterator = this.list.iterator(); localIterator.hasNext();)
    {
      localRule = (Rule)localIterator.next();
      if (!localRule.autoComplete(paramSentence))
      {
        paramSentence.setQuery(str);
        return false;
      }
    }
    return true;
  }
}
