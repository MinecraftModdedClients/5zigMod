package eu.the5zig.mod.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.util.Callback;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public abstract class SearchEntry
{
  private final ITextfield textfield;
  private final List entries;
  private Comparator comparator;
  private Callback enterCallback;
  private final List entriesRemoved = Lists.newArrayList();
  private final HashMap<Object, Integer> entryIndexMap = Maps.newHashMap();
  private boolean visible = false;
  private long lastInteractTime;
  private boolean alwaysVisible = false;
  
  public SearchEntry(ITextfield textfield, List list)
  {
    this(textfield, list, null);
  }
  
  public SearchEntry(ITextfield textfield, List list, Comparator comparator)
  {
    this(textfield, list, comparator, null);
  }
  
  public SearchEntry(ITextfield textfield, List list, Comparator comparator, Callback enterCallback)
  {
    this.textfield = textfield;
    this.entries = list;
    this.comparator = comparator;
    this.enterCallback = enterCallback;
    if (comparator == null) {
      for (Object o : this.entries) {
        this.entryIndexMap.put(o, Integer.valueOf(this.entries.indexOf(o)));
      }
    }
  }
  
  public void setComparator(Comparator comparator)
  {
    this.comparator = comparator;
  }
  
  public void setEnterCallback(Callback enterCallback)
  {
    this.enterCallback = enterCallback;
  }
  
  public boolean isAlwaysVisible()
  {
    return this.alwaysVisible;
  }
  
  public void setAlwaysVisible(boolean alwaysVisible)
  {
    this.alwaysVisible = alwaysVisible;
    if (alwaysVisible) {
      this.visible = true;
    }
  }
  
  public void draw()
  {
    if (!this.visible) {
      return;
    }
    this.textfield.draw();
    
    String text = this.textfield.getText();
    List addList = Lists.newArrayList();
    List removeList = Lists.newArrayList();
    for (Object o : this.entries) {
      if ((!this.entriesRemoved.contains(o)) && (!filter(text, o))) {
        removeList.add(o);
      }
    }
    for (Object o : this.entriesRemoved) {
      if ((!this.entries.contains(o)) && (filter(text, o))) {
        addList.add(o);
      }
    }
    this.entriesRemoved.addAll(removeList);
    this.entriesRemoved.removeAll(addList);
    this.entries.removeAll(removeList);
    for (Object o : addList)
    {
      int addIndex = getAddIndex();
      if (addIndex < 0)
      {
        this.entries.add(o);
      }
      else
      {
        while (addIndex > this.entries.size() - 1) {
          addIndex--;
        }
        this.entries.add(addIndex, o);
      }
    }
    if (!addList.isEmpty()) {
      sort();
    }
  }
  
  public void reset()
  {
    this.entries.addAll(this.entriesRemoved);
    this.textfield.setText("");
    sort();
    this.entriesRemoved.clear();
  }
  
  private void sort()
  {
    if (this.comparator != null) {
      Collections.sort(this.entries, this.comparator);
    } else if (this.entries.size() == this.entryIndexMap.size()) {
      Collections.sort(this.entries, new Comparator()
      {
        public int compare(Object o1, Object o2)
        {
          Integer integer1 = (Integer)SearchEntry.this.entryIndexMap.get(o1);
          Integer integer2 = (Integer)SearchEntry.this.entryIndexMap.get(o2);
          if ((integer1 == null) && (integer2 == null)) {
            return 0;
          }
          if (integer1 == null) {
            return 1;
          }
          if (integer2 == null) {
            return -1;
          }
          return integer1.compareTo(integer2);
        }
      });
    }
  }
  
  public boolean keyTyped(char character, int code)
  {
    if ((code == 28) && (this.enterCallback != null) && (!this.entries.isEmpty()) && (!this.textfield.getText().isEmpty())) {
      this.enterCallback.call(this.entries.get(0));
    }
    return getTextfield().keyTyped(character, code);
  }
  
  public void setVisible(boolean visible)
  {
    this.visible = visible;
    if (visible) {
      this.textfield.setFocused(true);
    } else {
      this.textfield.setFocused(false);
    }
  }
  
  public boolean isVisible()
  {
    return this.visible;
  }
  
  public void setLastInteractTime(long lastInteractTime)
  {
    this.lastInteractTime = lastInteractTime;
  }
  
  public long getLastInteractTime()
  {
    return this.lastInteractTime;
  }
  
  public ITextfield getTextfield()
  {
    return this.textfield;
  }
  
  public abstract boolean filter(String paramString, Object paramObject);
  
  protected int getAddIndex()
  {
    return -1;
  }
}
