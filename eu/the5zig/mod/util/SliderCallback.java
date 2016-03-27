package eu.the5zig.mod.util;

public abstract interface SliderCallback
{
  public abstract String translate();
  
  public abstract float get();
  
  public abstract void set(float paramFloat);
  
  public abstract float getMinValue();
  
  public abstract float getMaxValue();
  
  public abstract int getSteps();
  
  public abstract String getCustomValue(float paramFloat);
  
  public abstract String getSuffix();
  
  public abstract void action();
}
