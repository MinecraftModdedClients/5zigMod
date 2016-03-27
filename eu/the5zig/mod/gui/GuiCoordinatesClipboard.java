package eu.the5zig.mod.gui;

import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.Vector2i;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

public class GuiCoordinatesClipboard
  extends GuiOptions
{
  private List<?> description;
  private static final Pattern COORDINATE_PATTERN = Pattern.compile("(-?[0-9]+) -?[0-9]+ (-?[0-9]+)|(-?[0-9]+) (-?[0-9]+)");
  private boolean pressedPaste = false;
  
  public GuiCoordinatesClipboard(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    Keyboard.enableRepeatEvents(true);
    
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 + 5, getHeight() / 6 + 168, 150, 20, The5zigMod.getVars().translate("gui.done", new Object[0])));
    
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 - 155, getHeight() / 6 + 168, 150, 20, The5zigMod.getVars().translate("gui.cancel", new Object[0])));
    
    addTextField(The5zigMod.getVars().createTextfield(1, getWidth() / 2 - 70, 60, 50, 20));
    addTextField(The5zigMod.getVars().createTextfield(2, getWidth() / 2 + 20, 60, 50, 20));
    
    this.description = The5zigMod.getVars().splitStringToWidth(I18n.translate("coordinate_clipboard.description"), getWidth() - 150);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 100)
    {
      if ((getTextfieldById(1).getText().length() > 0) && (getTextfieldById(2).getText().length() > 0)) {
        The5zigMod.getDataManager().setCoordinatesClipboard(new Vector2i(Integer.parseInt(getTextfieldById(1).getText()), Integer.parseInt(getTextfieldById(2).getText())));
      } else {
        The5zigMod.getDataManager().setCoordinatesClipboard(null);
      }
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
  }
  
  public void m()
  {
    Keyboard.enableRepeatEvents(false);
  }
  
  protected void tick()
  {
    getButtonById(100).setEnabled(((((ITextfield)this.textfields.get(0)).getText().length() > 0) && (((ITextfield)this.textfields.get(1)).getText().length() > 0) && (isInt(((ITextfield)this.textfields.get(0)).getText())) && 
      (isInt(((ITextfield)this.textfields.get(1)).getText()))) || ((((ITextfield)this.textfields.get(0)).getText().length() == 0) && (((ITextfield)this.textfields.get(1)).getText().length() == 0)));
    if ((Keyboard.isKeyDown(29)) && (Keyboard.isKeyDown(47)) && (!this.pressedPaste))
    {
      this.pressedPaste = true;
      try
      {
        String clipboard = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        if ((clipboard != null) && (!clipboard.isEmpty()))
        {
          Matcher matcher = COORDINATE_PATTERN.matcher(clipboard);
          if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i += 2)
            {
              String x = matcher.group(i);
              String z = matcher.group(i + 1);
              if ((x != null) && (z != null))
              {
                getTextfieldById(1).setText("");
                getTextfieldById(1).setText(x);
                getTextfieldById(2).setText("");
                getTextfieldById(2).setText(z);
              }
            }
          }
        }
      }
      catch (Exception e)
      {
        The5zigMod.logger.warn("Could not paste clipboard contents!", e);
      }
    }
    else if ((!Keyboard.isKeyDown(29)) || (Keyboard.isKeyDown(47)))
    {
      this.pressedPaste = false;
    }
  }
  
  public String getTitleKey()
  {
    return "coordinate_clipboard.title";
  }
  
  public void drawScreen(int mouseX, int mouseY, float partialTicks)
  {
    drawCenteredString("X:", getWidth() / 2 - 80, 67);
    drawCenteredString("Z:", getWidth() / 2 + 10, 67);
    
    int y = 130;
    for (Iterator<?> it = this.description.iterator(); it.hasNext(); y += The5zigMod.getVars().getFontHeight()) {
      The5zigMod.getVars().drawString((String)it.next(), 75, y);
    }
  }
}
