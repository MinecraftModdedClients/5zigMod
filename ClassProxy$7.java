import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.manager.SearchEntry;
import java.lang.reflect.Field;
import java.util.List;

final class ClassProxy$7
  extends SearchEntry
{
  ClassProxy$7(ITextfield x0, List x1)
  {
    super(x0, x1);
  }
  
  public boolean filter(String text, Object o)
  {
    bhn saveFormatComparator = (bhn)o;
    try
    {
      azl worldData = (azl)ClassProxy.access$000().get(saveFormatComparator);
      return (worldData.a().toLowerCase().contains(text.toLowerCase())) || (worldData.b().toLowerCase().contains(text.toLowerCase()));
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
}
