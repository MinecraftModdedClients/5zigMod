package eu.the5zig.mod.gui;

import com.google.common.collect.Lists;
import eu.the5zig.mod.I18n;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.minecraft.ChatColor;
import java.util.List;
import java.util.Locale;

public class GuiLanguage
  extends GuiOptions
{
  private List<LanguageRow> languages = Lists.newArrayList();
  private IGuiList languageSlot;
  private int tickCount = 20;
  
  public GuiLanguage(Gui lastScreen)
  {
    super(lastScreen);
  }
  
  public void initGui()
  {
    this.languages.clear();
    addButton(The5zigMod.getVars().createButton(100, getWidth() / 2 - 100, getHeight() - 40, The5zigMod.getVars().translate("gui.done", new Object[0])));
    for (Locale locale : I18n.getLanguages()) {
      this.languages.add(new LanguageRow(locale));
    }
    this.languageSlot = The5zigMod.getVars().createGuiList(null, getWidth(), getHeight(), 32, getHeight() - 64, 0, getWidth(), this.languages);
    this.languageSlot.setRowWidth(200);
    this.languageSlot.setScrollX(getWidth() / 2 + 150);
    this.languageSlot.setSelectedId(I18n.getLanguages().indexOf(I18n.getCurrentLanguage()));
    addGuiList(this.languageSlot);
  }
  
  protected void actionPerformed(IButton button)
  {
    if (button.getId() == 100)
    {
      selectLanguage(this.languageSlot.getSelectedId());
      The5zigMod.getVars().displayScreen(this.lastScreen);
    }
  }
  
  private void selectLanguage(int id)
  {
    if ((id < 0) || (id >= this.languages.size())) {
      return;
    }
    LanguageRow language = (LanguageRow)this.languages.get(id);
    I18n.setLanguage(language.getLocale());
    The5zigMod.getOverlayMessage().displayMessageAndSplit(ChatColor.YELLOW + I18n.translate("language.select", new Object[] { I18n.translate("author") }));
  }
  
  protected void tick()
  {
    this.tickCount += 1;
    if (this.tickCount >= 20)
    {
      this.tickCount = 0;
      if (I18n.loadLocales())
      {
        this.languages.clear();
        for (Locale locale : I18n.getLanguages()) {
          this.languages.add(new LanguageRow(locale));
        }
        this.languageSlot.setSelectedId(I18n.getLanguages().indexOf(I18n.getCurrentLanguage()));
      }
    }
  }
  
  public String getTitleKey()
  {
    return "language.title";
  }
  
  class LanguageRow
    implements Row
  {
    private Locale locale;
    
    public LanguageRow(Locale locale)
    {
      this.locale = locale;
    }
    
    public int getLineHeight()
    {
      return 18;
    }
    
    public void draw(int x, int y)
    {
      Gui.drawCenteredString(String.format("%s (%s)", new Object[] { this.locale.getDisplayLanguage(this.locale), this.locale.getDisplayCountry(this.locale) }), GuiLanguage.this.getWidth() / 2, y + 2);
    }
    
    public Locale getLocale()
    {
      return this.locale;
    }
    
    public String toString()
    {
      return this.locale.toString();
    }
  }
}
