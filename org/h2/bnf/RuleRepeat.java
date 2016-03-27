package org.h2.bnf;

import java.util.HashMap;

public class RuleRepeat
  implements Rule
{
  private final Rule rule;
  private final boolean comma;
  
  public RuleRepeat(Rule paramRule, boolean paramBoolean)
  {
    this.rule = paramRule;
    this.comma = paramBoolean;
  }
  
  public void accept(BnfVisitor paramBnfVisitor)
  {
    paramBnfVisitor.visitRuleRepeat(this.comma, this.rule);
  }
  
  public void setLinks(HashMap<String, RuleHead> paramHashMap) {}
  
  public boolean autoComplete(Sentence paramSentence)
  {
    paramSentence.stopIfRequired();
    while (this.rule.autoComplete(paramSentence)) {}
    return true;
  }
}
