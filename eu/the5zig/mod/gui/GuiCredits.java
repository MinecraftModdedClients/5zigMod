package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.BasicRow;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.LinkedProperties;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class GuiCredits
  extends Gui
{
  private static final LinkedHashMap<String, List<String>> credits = ;
  
  static
  {
    try
    {
      LinkedProperties properties = new LinkedProperties();
      properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("credits.txt"));
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        credits.put("credits." + entry.getKey(), Arrays.asList(String.valueOf(entry.getValue()).split(", ")));
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
      credits.put("Error", Collections.singletonList(e.getMessage()));
    }
  }
  
  private List<Row> rows = Lists.newArrayList();
  
  public GuiCredits(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    addButton(The5zigMod.getVars().createButton(200, getWidth() / 2 - 100, getHeight() - 35, The5zigMod.getVars().translate("gui.done", new Object[0])));
    
    this.rows.clear();
    for (Iterator localIterator = credits.entrySet().iterator(); localIterator.hasNext();)
    {
      entry = (Map.Entry)localIterator.next();
      for (int i = 0; i < ((List)entry.getValue()).size(); i++)
      {
        final String credit = (String)((List)entry.getValue()).get(i);
        if (i == 0) {
          this.rows.add(new Row()
          {
            public int getLineHeight()
            {
              return 12;
            }
            
            public void draw(int x, int y)
            {
              The5zigMod.getVars().drawString(I18n.translate((String)entry.getKey()) + ": ", GuiCredits.this.getWidth() / 2 - 100, y + 2);
              The5zigMod.getVars().drawString(credit, GuiCredits.this.getWidth() / 2, y + 2);
            }
          });
        } else {
          this.rows.add(new Row()
          {
            public int getLineHeight()
            {
              return 12;
            }
            
            public void draw(int x, int y)
            {
              The5zigMod.getVars().drawString(credit, GuiCredits.this.getWidth() / 2, y + 2);
            }
          });
        }
        if (i + 1 == ((List)entry.getValue()).size()) {
          this.rows.add(new BasicRow(""));
        }
      }
    }
    final Map.Entry<String, List<String>> entry;
    Object thankYouList = The5zigMod.getVars().splitStringToWidth(I18n.translate("credits.thank_you"), 220);
    for (Object object : (List)thankYouList)
    {
      final String line = String.valueOf(object);
      this.rows.add(new Row()
      {
        public int getLineHeight()
        {
          return 12;
        }
        
        public void draw(int x, int y)
        {
          Gui.drawCenteredString(line, GuiCredits.this.getWidth() / 2, y + 2);
        }
      });
    }
    IGuiList guiList = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 50, getHeight() - 50, 0, getWidth(), this.rows);
    guiList.setLeftbound(true);
    guiList.setDrawSelection(false);
    guiList.setScrollX(getWidth() / 2 + 120);
    addGuiList(guiList);
  }
  
  public String getTitleKey()
  {
    return "credits.title";
  }
  
  protected void actionPerformed(IButton button) {}
}
