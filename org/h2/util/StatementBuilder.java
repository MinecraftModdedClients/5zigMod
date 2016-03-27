package org.h2.util;

public class StatementBuilder
{
  private final StringBuilder builder = new StringBuilder();
  private int index;
  
  public StatementBuilder() {}
  
  public StatementBuilder(String paramString)
  {
    this.builder.append(paramString);
  }
  
  public StatementBuilder append(String paramString)
  {
    this.builder.append(paramString);
    return this;
  }
  
  public StatementBuilder append(char paramChar)
  {
    this.builder.append(paramChar);
    return this;
  }
  
  public StatementBuilder append(long paramLong)
  {
    this.builder.append(paramLong);
    return this;
  }
  
  public StatementBuilder resetCount()
  {
    this.index = 0;
    return this;
  }
  
  public void appendOnlyFirst(String paramString)
  {
    if (this.index == 0) {
      this.builder.append(paramString);
    }
  }
  
  public void appendExceptFirst(String paramString)
  {
    if (this.index++ > 0) {
      this.builder.append(paramString);
    }
  }
  
  public String toString()
  {
    return this.builder.toString();
  }
  
  public int length()
  {
    return this.builder.length();
  }
}
