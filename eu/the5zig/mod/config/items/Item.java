package eu.the5zig.mod.config.items;

import com.google.gson.JsonObject;
import eu.the5zig.mod.I18n;
import eu.the5zig.util.Utils;
import eu.the5zig.util.minecraft.ChatColor;

public abstract class Item<T>
{
  private final String key;
  private final String category;
  private T value;
  private final T defaultValue;
  private boolean restricted = false;
  protected boolean changed = false;
  private String translationPrefix = "config";
  
  public Item(String key, String category, T defaultValue)
  {
    this.key = key;
    this.category = category;
    this.defaultValue = defaultValue;
    this.value = defaultValue;
  }
  
  public abstract void deserialize(JsonObject paramJsonObject);
  
  public abstract void serialize(JsonObject paramJsonObject);
  
  public abstract void next();
  
  public void action() {}
  
  public T get()
  {
    return (T)this.value;
  }
  
  public void set(T value)
  {
    this.changed = ((this.value != value) || (this.changed));
    this.value = value;
  }
  
  public String getKey()
  {
    return this.key;
  }
  
  public void reset()
  {
    set(this.defaultValue);
  }
  
  public String getCategory()
  {
    return this.category;
  }
  
  public void setChanged(boolean changed)
  {
    this.changed = changed;
  }
  
  public boolean hasChanged()
  {
    return this.changed;
  }
  
  public boolean isRestricted()
  {
    return this.restricted;
  }
  
  public void setRestricted(boolean restricted)
  {
    this.restricted = restricted;
  }
  
  public boolean isDefault()
  {
    return this.defaultValue.equals(get());
  }
  
  public String getTranslationPrefix()
  {
    return this.translationPrefix;
  }
  
  public void setTranslationPrefix(String translationPrefix)
  {
    this.translationPrefix = translationPrefix;
  }
  
  public String translateValue()
  {
    return String.valueOf(get());
  }
  
  public String translate()
  {
    return I18n.translate(new StringBuilder().append(this.translationPrefix).append(".").append(this.category).append(".").append(Utils.upperToDash(this.key)).toString()) + ": " + translateValue();
  }
  
  public String translateDescription()
  {
    return I18n.translate(this.translationPrefix + "." + this.category + "." + Utils.upperToDash(this.key) + ".desc");
  }
  
  public final String translateDefaultValue()
  {
    T current = get();
    boolean changed = hasChanged();
    set(this.defaultValue);
    String translated;
    String translated;
    if (((this instanceof SliderItem)) && (((SliderItem)this).getCustomValue(((Float)get()).floatValue()) != null))
    {
      translated = ((SliderItem)this).getCustomValue(((Float)get()).floatValue() - ((SliderItem)this).getMinValue());
    }
    else
    {
      translated = translateValue();
      if ((this instanceof SliderItem))
      {
        String suffix = ((SliderItem)this).getSuffix();
        if (suffix != null) {
          translated = translated + suffix;
        }
      }
    }
    set(current);
    this.changed = changed;
    return translated;
  }
  
  public String getHoverText()
  {
    String text = translateDescription();
    if (!(this instanceof NonConfigItem))
    {
      String defaultValue = translateDefaultValue();
      if (defaultValue != null) {
        text = text + "\n\n" + ChatColor.DARK_GRAY + I18n.translate("config.default") + ": " + defaultValue;
      }
    }
    return text;
  }
  
  public String toString()
  {
    return "Item{key='" + this.key + '\'' + ", value=" + this.value + ", defaultValue=" + this.defaultValue + ", category='" + this.category + '\'' + ", changed=" + this.changed + '}';
  }
}
