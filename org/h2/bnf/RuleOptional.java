package org.h2.bnf;

import java.util.HashMap;

public class RuleOptional
  implements Rule
{
  private final Rule rule;
  private boolean mapSet;
  
  public RuleOptional(Rule paramRule)
  {
    this.rule = paramRule;
  }
  
  public void accept(BnfVisitor paramBnfVisitor)
  {
    paramBnfVisitor.visitRuleOptional(this.rule);
  }
  
  public void setLinks(HashMap<String, RuleHead> paramHashMap)
  {
    if (!this.mapSet)
    {
      this.rule.setLinks(paramHashMap);
      this.mapSet = true;
    }
  }
  
  public boolean autoComplete(Sentence paramSentence)
  {
    paramSentence.stopIfRequired();
    this.rule.autoComplete(paramSentence);
    return true;
  }
}
