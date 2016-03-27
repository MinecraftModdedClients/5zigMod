import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.manager.SearchEntry;
import java.util.List;

final class ClassProxy$5
  extends SearchEntry
{
  ClassProxy$5(ITextfield x0, List x1)
  {
    super(x0, x1);
  }
  
  public boolean filter(String text, Object o)
  {
    bgu serverListEntry = (bgu)o;
    return (serverListEntry.a().a.toLowerCase().contains(text.toLowerCase())) || (serverListEntry.a().b.toLowerCase().contains(text.toLowerCase()));
  }
}
