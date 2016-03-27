package eu.the5zig.mod.gui;

import com.google.common.collect.Maps;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Callback;
import eu.the5zig.util.Utils;
import java.util.HashMap;
import org.apache.commons.lang3.Validate;

public abstract class GuiOptions
  extends Gui
{
  private boolean guiInitialized = false;
  private int optionButtonCount;
  private HashMap<Integer, Callback<IButton>> callbacks = Maps.newHashMap();
  
  public GuiOptions(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    this.guiInitialized = true;
    this.optionButtonCount = 0;
    this.callbacks.clear();
    addDoneButton();
  }
  
  protected void actionPerformed(IButton button)
  {
    if (this.callbacks.containsKey(Integer.valueOf(button.getId()))) {
      ((Callback)this.callbacks.get(Integer.valueOf(button.getId()))).call(button);
    }
  }
  
  protected boolean isInt(String str)
  {
    return Utils.isInt(str);
  }
  
  protected int addOptionButton(int id, int row, boolean left, String label, boolean enabled, int offset, Callback<IButton> actionPerformed)
  {
    Validate.isTrue(this.guiInitialized, "Gui hasn't been initialized yet!", new Object[0]);
    
    addButton(The5zigMod.getVars().createButton(id, getWidth() / 2 + (left ? 65381 : 5), getHeight() / 6 + row * 24 + offset - 6, 150, 20, label, enabled));
    if (actionPerformed != null) {
      this.callbacks.put(Integer.valueOf(id), actionPerformed);
    }
    return this.optionButtonCount++;
  }
  
  protected int addOptionButton(String label, boolean enabled, int idOffset, int offset, Callback<IButton> actionPerformed)
  {
    return addOptionButton(this.optionButtonCount + idOffset, this.optionButtonCount / 2, this.optionButtonCount % 2 == 0, label, enabled, offset, actionPerformed);
  }
  
  protected int addOptionButton(String label, int idOffset, int offset, Callback<IButton> actionPerformed)
  {
    return addOptionButton(label, true, idOffset, offset, actionPerformed);
  }
  
  protected int addOptionButton(String label, int offset, Callback<IButton> actionPerformed)
  {
    return addOptionButton(label, 0, offset, actionPerformed);
  }
  
  protected int addOptionButton(String label, boolean enabled, Callback<IButton> actionPerformed)
  {
    return addOptionButton(label, enabled, 0, 0, actionPerformed);
  }
  
  protected int addOptionButton(String label, Callback<IButton> actionPerformed)
  {
    return addOptionButton(label, 0, 0, actionPerformed);
  }
}
