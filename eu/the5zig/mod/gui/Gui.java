package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.util.ClassProxyCallback;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.ReflectionUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.input.Keyboard;

public abstract class Gui
{
  private static final Class<?> handleClass;
  private static final Constructor<?> handleConstructor;
  private static final Method drawModalRectWithCustomSizedTexture;
  private static final Method drawRect;
  public Gui lastScreen;
  private IGuiHandle handle;
  
  static
  {
    try
    {
      handleClass = Class.forName("GuiHandle");
      handleConstructor = handleClass.getConstructor(new Class[] { Gui.class });
      
      drawModalRectWithCustomSizedTexture = handleClass.getMethod("drawModalRectWithCustomSizedTexture", new Class[] { Integer.TYPE, Integer.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE, Integer.TYPE, Float.TYPE, Float.TYPE });
      
      drawRect = handleClass.getMethod("drawRect", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE });
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  
  protected HashMap<Integer, IButton> buttonIdLookup = Maps.newHashMap();
  protected HashMap<Integer, ITextfield> textFieldIdLookup = Maps.newHashMap();
  protected List<IButton> buttons = Lists.newArrayList();
  protected List<ITextfield> textfields = Lists.newArrayList();
  protected List<IGuiList> guiLists = Lists.newArrayList();
  
  public Gui(Gui lastScreen)
  {
    this();
    this.lastScreen = lastScreen;
  }
  
  public Gui()
  {
    try
    {
      this.handle = ((IGuiHandle)handleConstructor.newInstance(new Object[] { this }));
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public void initGui0()
  {
    this.buttons.clear();
    this.textfields.clear();
    this.guiLists.clear();
    this.buttonIdLookup.clear();
    this.textFieldIdLookup.clear();
    initGui();
    if (this.textfields.size() > 0) {
      ((ITextfield)this.textfields.get(0)).setSelected(true);
    }
    tick();
  }
  
  public void addDoneButton()
  {
    addButton(MinecraftFactory.getVars().createButton(200, getWidth() / 2 - 100, getHeight() / 6 + 168, MinecraftFactory.getVars().translate("gui.done", new Object[0])));
  }
  
  public void addBottomDoneButton()
  {
    addButton(MinecraftFactory.getVars().createButton(200, getWidth() / 2 - 100, getHeight() - 32, MinecraftFactory.getVars().translate("gui.done", new Object[0])));
  }
  
  public void addCancelButton()
  {
    addButton(MinecraftFactory.getVars().createButton(200, getWidth() / 2 - 100, getHeight() / 6 + 168, MinecraftFactory.getVars().translate("gui.cancel", new Object[0])));
  }
  
  public void addTextField(ITextfield textfield)
  {
    Keyboard.enableRepeatEvents(true);
    this.textfields.add(textfield);
    this.textFieldIdLookup.put(Integer.valueOf(textfield.getId()), textfield);
  }
  
  public void addButton(IButton button)
  {
    this.buttons.add(button);
    this.buttonIdLookup.put(Integer.valueOf(button.getId()), button);
  }
  
  public void removeButton(IButton button)
  {
    this.buttons.remove(button);
    this.buttonIdLookup.remove(Integer.valueOf(button.getId()));
  }
  
  public void addGuiList(IGuiList guiList)
  {
    this.guiLists.add(guiList);
  }
  
  public void actionPerformed0(IButton button)
  {
    if (!button.isEnabled()) {
      return;
    }
    actionPerformed(button);
    if (button.getId() == 200) {
      MinecraftFactory.getVars().displayScreen(this.lastScreen);
    }
  }
  
  public void drawScreen0(int mouseX, int mouseY, float partialTicks)
  {
    this.handle.drawDefaultBackground();
    if (!this.guiLists.isEmpty()) {
      drawMenuBackground();
    }
    for (IGuiList guiList : this.guiLists) {
      guiList.drawScreen(mouseX, mouseY, partialTicks);
    }
    drawScreen(mouseX, mouseY, partialTicks);
    if (getTitleName() != null) {
      drawCenteredString(getTitleName(), getWidth() / 2, 15);
    } else {
      drawCenteredString("The 5zig Mod - " + MinecraftFactory.getClassProxyCallback().translate(getTitleKey(), new Object[0]), getWidth() / 2, 15);
    }
    for (IButton button : this.buttons) {
      button.draw(mouseX, mouseY);
    }
    for (ITextfield textfield : this.textfields) {
      textfield.draw();
    }
  }
  
  public void tick0()
  {
    for (IButton button : this.buttons) {
      button.tick();
    }
    for (ITextfield textfield : this.textfields) {
      textfield.tick();
    }
    tick();
  }
  
  public void handleMouseInput0()
  {
    for (IGuiList guiList : this.guiLists) {
      guiList.handleMouseInput();
    }
    handleMouseInput();
  }
  
  public void guiClosed0()
  {
    Keyboard.enableRepeatEvents(false);
    for (IButton button : this.buttons) {
      button.guiClosed();
    }
    guiClosed();
  }
  
  public void keyTyped0(char character, int key)
  {
    if (key == 1) {
      onEscapeType();
    }
    for (ITextfield t : this.textfields) {
      if (t.isFocused()) {
        t.keyTyped(character, key);
      }
    }
    if ((key == 28) || (key == 156)) {
      if (getSelectedTextField() != null) {
        actionPerformed0((IButton)getButtonList().get(0));
      }
    }
    if (key == 15)
    {
      ITextfield curField = getSelectedTextField();
      if ((curField == null) && (this.textfields.size() > 0))
      {
        ((ITextfield)this.textfields.get(0)).setSelected(true);
      }
      else if (curField != null)
      {
        int id = this.textfields.indexOf(curField);
        int next = (id + 1) % this.textfields.size();
        curField.setSelected(false);
        ((ITextfield)this.textfields.get(next)).setSelected(true);
      }
    }
    onKeyType(character, key);
  }
  
  protected void onEscapeType()
  {
    MinecraftFactory.getVars().displayScreen(null);
    if (MinecraftFactory.getVars().getCurrentScreen() == null) {
      MinecraftFactory.getVars().setIngameFocus();
    }
  }
  
  public void mouseClicked0(int x, int y, int button)
  {
    if (button == 0) {
      for (IButton b : this.buttons) {
        if (b.mouseClicked(x, y))
        {
          b.playClickSound();
          actionPerformed0(b);
        }
      }
    }
    for (ITextfield textfield : this.textfields) {
      textfield.mouseClicked(x, y, button);
    }
    for (IGuiList guiList : this.guiLists) {
      guiList.mouseClicked(x, y);
    }
    mouseClicked(x, y, button);
  }
  
  public void mouseReleased0(int x, int y, int state)
  {
    for (IButton button : this.buttons) {
      button.mouseReleased(x, y);
    }
    for (IGuiList guiList : this.guiLists) {
      guiList.mouseReleased(x, y, state);
    }
    mouseReleased(x, y, state);
  }
  
  public ITextfield getSelectedTextField()
  {
    if (this.textfields.size() == 0) {
      return null;
    }
    for (ITextfield textfield : this.textfields) {
      if (textfield.isFocused()) {
        return textfield;
      }
    }
    return null;
  }
  
  protected void drawMenuBackground()
  {
    this.handle.drawMenuBackground();
  }
  
  public String getTitleName()
  {
    return null;
  }
  
  public String getTitleKey()
  {
    return "config.main.title";
  }
  
  public static void drawCenteredString(String string, int x, int y)
  {
    drawCenteredString(string, x, y, 16777215);
  }
  
  public static void drawCenteredString(String string, int x, int y, int color)
  {
    MinecraftFactory.getVars().drawCenteredString(string, x, y, color);
  }
  
  public IButton getButtonById(int id)
  {
    return (IButton)this.buttonIdLookup.get(Integer.valueOf(id));
  }
  
  public ITextfield getTextfieldById(int id)
  {
    return (ITextfield)this.textFieldIdLookup.get(Integer.valueOf(id));
  }
  
  public List<IButton> getButtonList()
  {
    return this.buttons;
  }
  
  public int getWidth()
  {
    return this.handle.getWidth();
  }
  
  public int getHeight()
  {
    return this.handle.getHeight();
  }
  
  public void setResolution(int width, int height)
  {
    this.handle.setResolution(width, height);
  }
  
  public IGuiHandle getHandle()
  {
    return this.handle;
  }
  
  public void drawTexturedModalRect(int x, int y, int texX, int texY, int width, int height)
  {
    this.handle.drawTexturedModalRect(x, y, texX, texY, width, height);
  }
  
  public void drawHoveringText(List<String> lines, int x, int y)
  {
    this.handle.drawHoveringText(lines, x, y);
  }
  
  public static void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight)
  {
    ReflectionUtil.invoke(drawModalRectWithCustomSizedTexture, new Object[] { Integer.valueOf(x), Integer.valueOf(y), Float.valueOf(u), Float.valueOf(v), Integer.valueOf(width), Integer.valueOf(height), Float.valueOf(textureWidth), Float.valueOf(textureHeight) });
  }
  
  public static void drawRect(int left, int top, int right, int bottom, int color)
  {
    ReflectionUtil.invoke(drawRect, new Object[] { Integer.valueOf(left), Integer.valueOf(top), Integer.valueOf(right), Integer.valueOf(bottom), Integer.valueOf(color) });
  }
  
  public static void drawRectOutline(int left, int top, int right, int bottom, int color)
  {
    drawRect(left - 1, top - 1, right + 1, top, color);
    drawRect(right, top, right + 1, bottom, color);
    drawRect(left - 1, bottom, right + 1, bottom + 1, color);
    drawRect(left - 1, top, left, bottom, color);
  }
  
  public static void drawRectInline(int left, int top, int right, int bottom, int color)
  {
    drawRect(left, top, right, top + 1, color);
    drawRect(right - 1, top, right, bottom, color);
    drawRect(left, bottom - 1, right, bottom, color);
    drawRect(left, top, left + 1, bottom, color);
  }
  
  public static void drawScaledCenteredString(String string, int x, int y, float scale)
  {
    GLUtil.pushMatrix();
    GLUtil.translate(x, y, 1.0F);
    GLUtil.scale(scale, scale, scale);
    MinecraftFactory.getVars().drawCenteredString(string, 0, 0);
    GLUtil.popMatrix();
  }
  
  public static void drawScaledString(String string, int x, int y, float scale)
  {
    GLUtil.pushMatrix();
    GLUtil.translate(x, y, 1.0F);
    GLUtil.scale(scale, scale, scale);
    MinecraftFactory.getVars().drawString(string, 0, 0);
    GLUtil.popMatrix();
  }
  
  public abstract void initGui();
  
  protected abstract void actionPerformed(IButton paramIButton);
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks) {}
  
  protected void tick() {}
  
  protected void handleMouseInput() {}
  
  protected void guiClosed() {}
  
  protected void onKeyType(char character, int key) {}
  
  protected void mouseClicked(int x, int y, int button) {}
  
  protected void mouseReleased(int x, int y, int state) {}
}
