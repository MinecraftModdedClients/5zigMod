import java.lang.reflect.Field;
import java.util.Comparator;

final class ClassProxy$9
  implements Comparator
{
  public int compare(Object o1, Object o2)
  {
    bhn saveFormatComparator1 = (bhn)o1;
    bhn saveFormatComparator2 = (bhn)o2;
    try
    {
      azl worldData1 = (azl)ClassProxy.access$000().get(saveFormatComparator1);
      azl worldData2 = (azl)ClassProxy.access$000().get(saveFormatComparator2);
      return (int)(worldData2.e() - worldData1.e());
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
}
