package org.h2.bnf;

public class RuleHead
{
  private final String section;
  private final String topic;
  private Rule rule;
  
  RuleHead(String paramString1, String paramString2, Rule paramRule)
  {
    this.section = paramString1;
    this.topic = paramString2;
    this.rule = paramRule;
  }
  
  public String getTopic()
  {
    return this.topic;
  }
  
  public Rule getRule()
  {
    return this.rule;
  }
  
  void setRule(Rule paramRule)
  {
    this.rule = paramRule;
  }
  
  public String getSection()
  {
    return this.section;
  }
}
