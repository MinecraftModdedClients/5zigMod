package org.h2.expression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.message.DbException;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

public class CompareLike
  extends Condition
{
  private static final int MATCH = 0;
  private static final int ONE = 1;
  private static final int ANY = 2;
  private final CompareMode compareMode;
  private final String defaultEscape;
  private Expression left;
  private Expression right;
  private Expression escape;
  private boolean isInit;
  private char[] patternChars;
  private String patternString;
  private int[] patternTypes;
  private int patternLength;
  private final boolean regexp;
  private Pattern patternRegexp;
  private boolean ignoreCase;
  private boolean fastCompare;
  private boolean invalidPattern;
  
  public CompareLike(Database paramDatabase, Expression paramExpression1, Expression paramExpression2, Expression paramExpression3, boolean paramBoolean)
  {
    this(paramDatabase.getCompareMode(), paramDatabase.getSettings().defaultEscape, paramExpression1, paramExpression2, paramExpression3, paramBoolean);
  }
  
  public CompareLike(CompareMode paramCompareMode, String paramString, Expression paramExpression1, Expression paramExpression2, Expression paramExpression3, boolean paramBoolean)
  {
    this.compareMode = paramCompareMode;
    this.defaultEscape = paramString;
    this.regexp = paramBoolean;
    this.left = paramExpression1;
    this.right = paramExpression2;
    this.escape = paramExpression3;
  }
  
  private static Character getEscapeChar(String paramString)
  {
    return (paramString == null) || (paramString.length() == 0) ? null : Character.valueOf(paramString.charAt(0));
  }
  
  public String getSQL()
  {
    String str;
    if (this.regexp)
    {
      str = this.left.getSQL() + " REGEXP " + this.right.getSQL();
    }
    else
    {
      str = this.left.getSQL() + " LIKE " + this.right.getSQL();
      if (this.escape != null) {
        str = str + " ESCAPE " + this.escape.getSQL();
      }
    }
    return "(" + str + ")";
  }
  
  public Expression optimize(Session paramSession)
  {
    this.left = this.left.optimize(paramSession);
    this.right = this.right.optimize(paramSession);
    if (this.left.getType() == 14) {
      this.ignoreCase = true;
    }
    Value localValue1;
    if (this.left.isValueSet())
    {
      localValue1 = this.left.getValue(paramSession);
      if (localValue1 == ValueNull.INSTANCE) {
        return ValueExpression.getNull();
      }
    }
    if (this.escape != null) {
      this.escape = this.escape.optimize(paramSession);
    }
    if ((this.right.isValueSet()) && ((this.escape == null) || (this.escape.isValueSet())))
    {
      if (this.left.isValueSet()) {
        return ValueExpression.get(getValue(paramSession));
      }
      localValue1 = this.right.getValue(paramSession);
      if (localValue1 == ValueNull.INSTANCE) {
        return ValueExpression.getNull();
      }
      Value localValue2 = this.escape == null ? null : this.escape.getValue(paramSession);
      if (localValue2 == ValueNull.INSTANCE) {
        return ValueExpression.getNull();
      }
      String str = localValue1.getString();
      initPattern(str, getEscapeChar(localValue2));
      if (this.invalidPattern) {
        return ValueExpression.getNull();
      }
      if ("%".equals(str)) {
        return new Comparison(paramSession, 7, this.left, null).optimize(paramSession);
      }
      if (isFullMatch())
      {
        Value localValue3 = ValueString.get(this.patternString);
        ValueExpression localValueExpression = ValueExpression.get(localValue3);
        return new Comparison(paramSession, 0, this.left, localValueExpression).optimize(paramSession);
      }
      this.isInit = true;
    }
    return this;
  }
  
  private Character getEscapeChar(Value paramValue)
  {
    if (paramValue == null) {
      return getEscapeChar(this.defaultEscape);
    }
    String str = paramValue.getString();
    Character localCharacter;
    if (str == null)
    {
      localCharacter = getEscapeChar(this.defaultEscape);
    }
    else if (str.length() == 0)
    {
      localCharacter = null;
    }
    else
    {
      if (str.length() > 1) {
        throw DbException.get(22025, str);
      }
      localCharacter = Character.valueOf(str.charAt(0));
    }
    return localCharacter;
  }
  
  public void createIndexConditions(Session paramSession, TableFilter paramTableFilter)
  {
    if (this.regexp) {
      return;
    }
    if (!(this.left instanceof ExpressionColumn)) {
      return;
    }
    ExpressionColumn localExpressionColumn = (ExpressionColumn)this.left;
    if (paramTableFilter != localExpressionColumn.getTableFilter()) {
      return;
    }
    if (!this.right.isEverything(ExpressionVisitor.INDEPENDENT_VISITOR)) {
      return;
    }
    if ((this.escape != null) && (!this.escape.isEverything(ExpressionVisitor.INDEPENDENT_VISITOR))) {
      return;
    }
    String str1 = this.right.getValue(paramSession).getString();
    Value localValue = this.escape == null ? null : this.escape.getValue(paramSession);
    if (localValue == ValueNull.INSTANCE) {
      DbException.throwInternalError();
    }
    initPattern(str1, getEscapeChar(localValue));
    if (this.invalidPattern) {
      return;
    }
    if ((this.patternLength <= 0) || (this.patternTypes[0] != 0)) {
      return;
    }
    int i = localExpressionColumn.getColumn().getType();
    if ((i != 13) && (i != 14) && (i != 21)) {
      return;
    }
    int j = 0;
    StringBuilder localStringBuilder = new StringBuilder();
    while ((j < this.patternLength) && (this.patternTypes[j] == 0)) {
      localStringBuilder.append(this.patternChars[(j++)]);
    }
    String str2 = localStringBuilder.toString();
    if (j == this.patternLength)
    {
      paramTableFilter.addIndexCondition(IndexCondition.get(0, localExpressionColumn, ValueExpression.get(ValueString.get(str2))));
    }
    else if (str2.length() > 0)
    {
      paramTableFilter.addIndexCondition(IndexCondition.get(1, localExpressionColumn, ValueExpression.get(ValueString.get(str2))));
      
      int k = str2.charAt(str2.length() - 1);
      for (int m = 1; m < 2000; m++)
      {
        String str3 = str2.substring(0, str2.length() - 1) + (char)(k + m);
        if (this.compareMode.compareString(str2, str3, this.ignoreCase) == -1)
        {
          paramTableFilter.addIndexCondition(IndexCondition.get(4, localExpressionColumn, ValueExpression.get(ValueString.get(str3))));
          
          break;
        }
      }
    }
  }
  
  public Value getValue(Session paramSession)
  {
    Value localValue1 = this.left.getValue(paramSession);
    if (localValue1 == ValueNull.INSTANCE) {
      return localValue1;
    }
    if (!this.isInit)
    {
      localObject = this.right.getValue(paramSession);
      if (localObject == ValueNull.INSTANCE) {
        return (Value)localObject;
      }
      String str = ((Value)localObject).getString();
      Value localValue2 = this.escape == null ? null : this.escape.getValue(paramSession);
      if (localValue2 == ValueNull.INSTANCE) {
        return ValueNull.INSTANCE;
      }
      initPattern(str, getEscapeChar(localValue2));
    }
    if (this.invalidPattern) {
      return ValueNull.INSTANCE;
    }
    Object localObject = localValue1.getString();
    boolean bool;
    if (this.regexp) {
      bool = this.patternRegexp.matcher((CharSequence)localObject).find();
    } else {
      bool = compareAt((String)localObject, 0, 0, ((String)localObject).length(), this.patternChars, this.patternTypes);
    }
    return ValueBoolean.get(bool);
  }
  
  private boolean compare(char[] paramArrayOfChar, String paramString, int paramInt1, int paramInt2)
  {
    return (paramArrayOfChar[paramInt1] == paramString.charAt(paramInt2)) || ((!this.fastCompare) && (this.compareMode.equalsChars(this.patternString, paramInt1, paramString, paramInt2, this.ignoreCase)));
  }
  
  private boolean compareAt(String paramString, int paramInt1, int paramInt2, int paramInt3, char[] paramArrayOfChar, int[] paramArrayOfInt)
  {
    for (; paramInt1 < this.patternLength; paramInt1++) {
      switch (paramArrayOfInt[paramInt1])
      {
      case 0: 
        if ((paramInt2 >= paramInt3) || (!compare(paramArrayOfChar, paramString, paramInt1, paramInt2++))) {
          return false;
        }
        break;
      case 1: 
        if (paramInt2++ >= paramInt3) {
          return false;
        }
        break;
      case 2: 
        paramInt1++;
        if (paramInt1 >= this.patternLength) {
          return true;
        }
        while (paramInt2 < paramInt3)
        {
          if ((compare(paramArrayOfChar, paramString, paramInt1, paramInt2)) && (compareAt(paramString, paramInt1, paramInt2, paramInt3, paramArrayOfChar, paramArrayOfInt))) {
            return true;
          }
          paramInt2++;
        }
        return false;
      default: 
        DbException.throwInternalError();
      }
    }
    return paramInt2 == paramInt3;
  }
  
  public boolean test(String paramString1, String paramString2, char paramChar)
  {
    initPattern(paramString1, Character.valueOf(paramChar));
    if (this.invalidPattern) {
      return false;
    }
    return compareAt(paramString2, 0, 0, paramString2.length(), this.patternChars, this.patternTypes);
  }
  
  private void initPattern(String paramString, Character paramCharacter)
  {
    if ((this.compareMode.getName().equals("OFF")) && (!this.ignoreCase)) {
      this.fastCompare = true;
    }
    if (this.regexp)
    {
      this.patternString = paramString;
      try
      {
        if (this.ignoreCase) {
          this.patternRegexp = Pattern.compile(paramString, 2);
        } else {
          this.patternRegexp = Pattern.compile(paramString);
        }
      }
      catch (PatternSyntaxException localPatternSyntaxException)
      {
        throw DbException.get(22025, localPatternSyntaxException, new String[] { paramString });
      }
      return;
    }
    this.patternLength = 0;
    if (paramString == null)
    {
      this.patternTypes = null;
      this.patternChars = null;
      return;
    }
    int i = paramString.length();
    this.patternChars = new char[i];
    this.patternTypes = new int[i];
    int j = 0;
    for (int k = 0; k < i; k++)
    {
      int m = paramString.charAt(k);
      int n;
      if ((paramCharacter != null) && (paramCharacter.charValue() == m))
      {
        if (k >= i - 1)
        {
          this.invalidPattern = true;
          return;
        }
        m = paramString.charAt(++k);
        n = 0;
        j = 0;
      }
      else if (m == 37)
      {
        if (j != 0) {
          continue;
        }
        n = 2;
        j = 1;
      }
      else if (m == 95)
      {
        n = 1;
      }
      else
      {
        n = 0;
        j = 0;
      }
      this.patternTypes[this.patternLength] = n;
      this.patternChars[(this.patternLength++)] = m;
    }
    for (k = 0; k < this.patternLength - 1; k++) {
      if ((this.patternTypes[k] == 2) && (this.patternTypes[(k + 1)] == 1))
      {
        this.patternTypes[k] = 1;
        this.patternTypes[(k + 1)] = 2;
      }
    }
    this.patternString = new String(this.patternChars, 0, this.patternLength);
  }
  
  private boolean isFullMatch()
  {
    if (this.patternTypes == null) {
      return false;
    }
    for (int k : this.patternTypes) {
      if (k != 0) {
        return false;
      }
    }
    return true;
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.left.mapColumns(paramColumnResolver, paramInt);
    this.right.mapColumns(paramColumnResolver, paramInt);
    if (this.escape != null) {
      this.escape.mapColumns(paramColumnResolver, paramInt);
    }
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.left.setEvaluatable(paramTableFilter, paramBoolean);
    this.right.setEvaluatable(paramTableFilter, paramBoolean);
    if (this.escape != null) {
      this.escape.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.left.updateAggregate(paramSession);
    this.right.updateAggregate(paramSession);
    if (this.escape != null) {
      this.escape.updateAggregate(paramSession);
    }
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return (this.left.isEverything(paramExpressionVisitor)) && (this.right.isEverything(paramExpressionVisitor)) && ((this.escape == null) || (this.escape.isEverything(paramExpressionVisitor)));
  }
  
  public int getCost()
  {
    return this.left.getCost() + this.right.getCost() + 3;
  }
}
