import eu.the5zig.mod.util.IVariables.CapeCallback;
import java.awt.image.BufferedImage;

class Variables$1
  implements bnj
{
  Variables$1(Variables this$0, IVariables.CapeCallback paramCapeCallback, kk paramkk) {}
  
  public BufferedImage a(BufferedImage bufferedImage)
  {
    return this.val$callback.parseImage(bufferedImage);
  }
  
  public void a()
  {
    this.val$callback.callback(this.val$capeLocation);
  }
}
