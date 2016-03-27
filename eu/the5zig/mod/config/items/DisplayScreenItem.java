package eu.the5zig.mod.config.items;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import java.lang.reflect.Constructor;
import org.apache.logging.log4j.Logger;

public class DisplayScreenItem
  extends NonConfigItem
{
  private final Class<? extends Gui> gui;
  private final Class[] constructorParams;
  private final Object[] constructorArgs;
  
  public DisplayScreenItem(String key, String category, Class<? extends Gui> gui, Class[] constructorParams, Object[] constructorArgs)
  {
    super(key, category);
    this.gui = gui;
    Class[] construct;
    if ((!checkConstructor(construct = (Class[])Utils.concat(new Class[] { Gui.class }, constructorParams))) && 
      (!checkConstructor(construct = constructorParams))) {
      throw new RuntimeException("Could not find any matching constructor!");
    }
    this.constructorParams = construct;
    this.constructorArgs = constructorArgs;
  }
  
  private boolean checkConstructor(Class[] params)
  {
    try
    {
      this.gui.getConstructor(params);
      return true;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return false;
  }
  
  public DisplayScreenItem(String key, String category, Class<? extends Gui> gui)
  {
    this(key, category, gui, new Class[0], new Object[0]);
  }
  
  public void action()
  {
    try
    {
      Gui screen = (Gui)this.gui.getConstructor(this.constructorParams).newInstance(Utils.concat(Utils.asArray(new Gui[] { The5zigMod.getVars().getCurrentScreen() }), this.constructorArgs));
      The5zigMod.getVars().displayScreen(screen);
    }
    catch (Exception e)
    {
      The5zigMod.logger.error("Could not display GUI " + this.gui.getSimpleName() + "!", e);
    }
  }
}
