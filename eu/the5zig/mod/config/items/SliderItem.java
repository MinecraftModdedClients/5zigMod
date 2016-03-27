package eu.the5zig.mod.config.items;

import eu.the5zig.mod.I18n;
import eu.the5zig.util.Utils;

public class SliderItem
  extends FloatItem
{
  private String suffix;
  private final float minValue;
  private final float maxValue;
  private final int steps;
  
  public SliderItem(String key, String suffix, String category, float defaultValue, float minValue, float maxValue, int steps)
  {
    super(key, category, Float.valueOf(defaultValue));
    this.suffix = suffix;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.steps = steps;
  }
  
  public String translate()
  {
    return I18n.translate(getTranslationPrefix() + "." + getCategory() + "." + Utils.upperToDash(getKey()));
  }
  
  public float getMinValue()
  {
    return this.minValue;
  }
  
  public float getMaxValue()
  {
    return this.maxValue;
  }
  
  public int getSteps()
  {
    return this.steps;
  }
  
  public String getCustomValue(float value)
  {
    return null;
  }
  
  public String getSuffix()
  {
    return this.suffix;
  }
  
  public void setSuffix(String suffix)
  {
    this.suffix = suffix;
  }
  
  public void next() {}
}
