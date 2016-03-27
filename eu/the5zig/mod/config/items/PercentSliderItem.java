package eu.the5zig.mod.config.items;

public class PercentSliderItem
  extends SliderItem
{
  public PercentSliderItem(String key, String category, Float defaultValue, Float minValue, Float maxValue, int steps)
  {
    super(key, "%", category, defaultValue.floatValue(), minValue.floatValue(), maxValue.floatValue(), steps);
  }
  
  public String getCustomValue(float value)
  {
    return Math.round((value * (getMaxValue() - getMinValue()) + getMinValue()) * 100.0F) + getSuffix();
  }
}
