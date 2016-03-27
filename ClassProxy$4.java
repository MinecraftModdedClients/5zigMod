import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.manager.SearchEntry;
import java.util.Comparator;
import java.util.List;

final class ClassProxy$4
  extends SearchEntry
{
  ClassProxy$4(ITextfield x0, List x1, Comparator x2)
  {
    super(x0, x1, x2);
  }
  
  public boolean filter(String text, Object o)
  {
    if (!(o instanceof bhb)) {
      return true;
    }
    bhb resourcePackListEntryFound = (bhb)o;
    return (resourcePackListEntryFound.l().d().toLowerCase().contains(text.toLowerCase())) || (resourcePackListEntryFound.l().e().toLowerCase().contains(text.toLowerCase()));
  }
  
  protected int getAddIndex()
  {
    return 1;
  }
}
