package eu.the5zig.mod.manager;

import com.google.common.collect.Lists;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.listener.Listener;
import java.util.Collections;
import java.util.List;
import org.lwjgl.input.Keyboard;

public class SearchManager
  extends Listener
{
  private List<SearchEntry> entries = Lists.newArrayList();
  
  public void onGuiClose()
  {
    Keyboard.enableRepeatEvents(false);
  }
  
  public void addSearch(SearchEntry searchEntry, SearchEntry... searchEntries)
  {
    for (SearchEntry entry : this.entries) {
      entry.reset();
    }
    this.entries.clear();
    Keyboard.enableRepeatEvents(true);
    this.entries.add(searchEntry);
    Collections.addAll(this.entries, searchEntries);
  }
  
  public void draw()
  {
    for (SearchEntry entry : this.entries) {
      entry.draw();
    }
  }
  
  public void keyTyped(char character, int code)
  {
    boolean atLeastOneFocused = false;
    for (SearchEntry entry : this.entries) {
      if (entry.getTextfield().isFocused())
      {
        atLeastOneFocused = true;
        break;
      }
    }
    for (SearchEntry entry : this.entries) {
      if (((!entry.isVisible()) || (!entry.getTextfield().isFocused())) && (!atLeastOneFocused) && (!entry.isAlwaysVisible()))
      {
        entry.setVisible(true);
        if (!entry.keyTyped(character, code)) {
          entry.setVisible(false);
        } else {
          entry.setLastInteractTime(System.currentTimeMillis());
        }
      }
      else if ((entry.isVisible()) && 
        (entry.keyTyped(character, code)))
      {
        entry.setLastInteractTime(System.currentTimeMillis());
      }
    }
  }
  
  public void mouseClicked(int mouseX, int mouseY, int button)
  {
    for (SearchEntry entry : this.entries) {
      entry.getTextfield().mouseClicked(mouseX, mouseY, button);
    }
  }
  
  public void onTick()
  {
    for (SearchEntry entry : this.entries) {
      if ((!entry.getTextfield().isFocused()) && (entry.getTextfield().getText().isEmpty()) && (System.currentTimeMillis() - entry.getLastInteractTime() > 5000L) && 
        (!entry.isAlwaysVisible())) {
        entry.setVisible(false);
      }
    }
  }
}
