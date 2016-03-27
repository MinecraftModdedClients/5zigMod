import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.SliderCallback;
import eu.the5zig.util.Utils;

public class Slider
  extends Button
{
  private final String str;
  public float value;
  public boolean dragging;
  private SliderCallback sliderItem;
  
  public Slider(int id, int x, int y, SliderCallback sliderItem)
  {
    super(id, x, y, 150, 20, "");
    this.sliderItem = sliderItem;
    this.str = sliderItem.translate();
    this.value = normalizeValue(sliderItem.get());
    setLabel(this.str);
  }
  
  public void setLabel(String label)
  {
    this.value = normalizeValue(this.sliderItem.get());
    super.setLabel(label + ": " + getValue());
  }
  
  protected int a(boolean b)
  {
    return 0;
  }
  
  protected void b(bcf mc, int mouseX, int mouseY)
  {
    if (isVisible())
    {
      if (this.dragging)
      {
        this.value = ((mouseX - (getX() + 4)) / (getWidth() - 8));
        this.value = Utils.clamp(this.value, 0.0F, 1.0F);
        
        float v = denormalizeValue(this.value);
        this.sliderItem.set(v);
        this.value = normalizeValue(v);
        
        setLabel(this.str);
      }
      MinecraftFactory.getVars().bindTexture(a);
      GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
      b(getX() + (int)(this.value * (getWidth() - 8)), getY(), 0, 66, 4, 20);
      b(getX() + (int)(this.value * (getWidth() - 8)) + 4, getY(), 196, 66, 4, 20);
    }
  }
  
  public float denormalizeValue(float value)
  {
    return snapToStepClamp(this.sliderItem.getMinValue() + (this.sliderItem.getMaxValue() - this.sliderItem.getMinValue()) * Utils.clamp(value, 0.0F, 1.0F));
  }
  
  public float normalizeValue(float value)
  {
    return Utils.clamp((snapToStepClamp(value) - this.sliderItem.getMinValue()) / (this.sliderItem.getMaxValue() - this.sliderItem.getMinValue()), 0.0F, 1.0F);
  }
  
  public float snapToStepClamp(float value)
  {
    value = snapToStep(value);
    return Utils.clamp(value, this.sliderItem.getMinValue(), this.sliderItem.getMaxValue());
  }
  
  protected float snapToStep(float value)
  {
    if (this.sliderItem.getSteps() != -1) {
      value = this.sliderItem.getSteps() * Math.round(value / this.sliderItem.getSteps());
    }
    return value;
  }
  
  public boolean mouseClicked(int x, int y)
  {
    if (super.mouseClicked(x, y))
    {
      this.value = ((x - (this.h + 4)) / (this.f - 8));
      if (this.value < 0.0F) {
        this.value = 0.0F;
      }
      if (this.value > 1.0F) {
        this.value = 1.0F;
      }
      setLabel(this.str + getValue());
      this.dragging = true;
      return true;
    }
    return false;
  }
  
  private String getValue()
  {
    String customValue = this.sliderItem.getCustomValue(this.value);
    if (customValue != null) {
      return customValue;
    }
    return Math.round(this.value * (this.sliderItem.getMaxValue() - this.sliderItem.getMinValue()) + this.sliderItem.getMinValue()) + this.sliderItem.getSuffix();
  }
  
  public void mouseReleased(int x, int y)
  {
    if (this.dragging)
    {
      playClickSound();
      
      this.sliderItem.action();
    }
    this.dragging = false;
  }
}
