import java.awt.image.BufferedImage;
import java.io.IOException;

public class SimpleTexture
  extends bvd
{
  private boolean textureUploaded;
  private BufferedImage bufferedImage;
  
  public SimpleTexture()
  {
    super(null);
  }
  
  private void checkTextureUploaded()
  {
    if ((!this.textureUploaded) && 
      (this.bufferedImage != null))
    {
      bvk.a(super.b(), this.bufferedImage);
      this.textureUploaded = true;
    }
  }
  
  public void setBufferedImage(BufferedImage bufferedImage)
  {
    this.bufferedImage = bufferedImage;
  }
  
  public BufferedImage getBufferedImage()
  {
    return this.bufferedImage;
  }
  
  public int b()
  {
    checkTextureUploaded();
    return super.b();
  }
  
  public void a(bwg resourceManager)
    throws IOException
  {}
}
