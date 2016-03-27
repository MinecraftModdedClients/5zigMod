package eu.the5zig.mod.gui;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.util.IVariables;

public class GuiCenteredTextfield
  extends Gui
{
  private final CenteredTextfieldCallback callback;
  private final String defaultText;
  private ITextfield textfield;
  private int minLength;
  private int maxStringLength;
  
  public GuiCenteredTextfield(Gui lastScreen, CenteredTextfieldCallback callback)
  {
    this(lastScreen, callback, 0, 100);
  }
  
  public GuiCenteredTextfield(Gui lastScreen, CenteredTextfieldCallback callback, String text)
  {
    this(lastScreen, callback, text, 0, 100);
  }
  
  public GuiCenteredTextfield(Gui lastScreen, CenteredTextfieldCallback callback, String text, int maxStringLength)
  {
    this(lastScreen, callback, text, 0, maxStringLength);
  }
  
  public GuiCenteredTextfield(Gui lastScreen, CenteredTextfieldCallback callback, int maxStringLength)
  {
    this(lastScreen, callback, 0, maxStringLength);
  }
  
  public GuiCenteredTextfield(Gui lastScreen, CenteredTextfieldCallback callback, int minLength, int maxStringLength)
  {
    this(lastScreen, callback, "", minLength, maxStringLength);
  }
  
  public GuiCenteredTextfield(Gui lastScreen, CenteredTextfieldCallback callback, String text, int minLength, int maxStringLength)
  {
    super(lastScreen);
    this.callback = callback;
    this.defaultText = text;
    this.minLength = minLength;
    this.maxStringLength = maxStringLength;
  }
  
  public void initGui()
  {
    addTextField(this.textfield = The5zigMod.getVars().createTextfield(1, getWidth() / 2 - 150, getHeight() / 6 + 80, 300, 20, this.maxStringLength));
    this.textfield.setText(this.defaultText);
    
    addButton(The5zigMod.getVars().createButton(1, getWidth() / 2 - 152, getHeight() / 6 + 140, 150, 20, The5zigMod.getVars().translate("gui.done", new Object[0]), false));
    addButton(The5zigMod.getVars().createButton(2, getWidth() / 2 + 2, getHeight() / 6 + 140, 150, 20, The5zigMod.getVars().translate("gui.cancel", new Object[0])));
  }
  
  protected void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawCenteredString(this.callback.title(), getWidth() / 2, getHeight() / 6);
  }
  
  protected void onKeyType(char character, int key)
  {
    getButtonById(1).setEnabled((this.textfield.getText().length() > this.minLength) && (!this.defaultText.equals(this.textfield.getText())));
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 1)
    {
      this.callback.onDone(this.textfield.getText());
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
    if (button.getId() == 2) {
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
  }
  
  public String getTitleKey()
  {
    return "input.title";
  }
}
