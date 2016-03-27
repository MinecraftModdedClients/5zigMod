import java.util.Comparator;

final class ClassProxy$1
  implements Comparator
{
  public int compare(Object o1, Object o2)
  {
    if ((!(o1 instanceof bhb)) || (!(o2 instanceof bhb))) {
      return 0;
    }
    bhb resourcePackListEntryFound1 = (bhb)o1;
    bhb resourcePackListEntryFound2 = (bhb)o2;
    return resourcePackListEntryFound1.l().d().toLowerCase().compareTo(resourcePackListEntryFound2.l().d().toLowerCase());
  }
}
