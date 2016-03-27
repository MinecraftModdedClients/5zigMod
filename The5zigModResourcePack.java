import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.Set;

public class The5zigModResourcePack
  extends bvx
{
  public static final Set<String> resourceDomains = ImmutableSet.of("the5zigmod");
  
  public The5zigModResourcePack()
  {
    super(new bvv()
    {
      public File a(kk kk)
      {
        return null;
      }
      
      public boolean b(kk kk)
      {
        return false;
      }
      
      public File a()
      {
        return null;
      }
    });
  }
  
  public Set<String> c()
  {
    return resourceDomains;
  }
  
  public String b()
  {
    return "The 5zig Mod";
  }
}
