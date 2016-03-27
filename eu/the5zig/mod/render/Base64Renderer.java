package eu.the5zig.mod.render;

import com.google.common.base.Charsets;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IResourceLocation;
import eu.the5zig.mod.util.IVariables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public class Base64Renderer
{
  private int height = 64;
  private int width = 64;
  private int y;
  private int x;
  private IResourceLocation resourceLocation;
  private Object dynamicImage;
  private String base64String;
  
  public void renderImage()
  {
    renderImage(this.x, this.y, this.width, this.height);
  }
  
  public void renderImage(int x, int y, int width, int height)
  {
    renderImage(x, y, width, height, 1.0F, 1.0F, 1.0F, 1.0F);
  }
  
  public void renderImage(int x, int y, int width, int height, float r, float g, float b, float a)
  {
    if (this.dynamicImage == null) {
      render(x, y, width, height, The5zigMod.STEVE, r, g, b, a);
    } else {
      render(x, y, width, height, this.resourceLocation, r, b, g, a);
    }
  }
  
  private void render(int x, int y, int width, int height, IResourceLocation resource, float r, float g, float b, float a)
  {
    The5zigMod.getVars().bindTexture(resource);
    
    GLUtil.color(r, g, b, a);
    GLUtil.disableBlend();
    Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, width, height, width, height);
  }
  
  public String getBase64String()
  {
    return this.base64String;
  }
  
  public void setBase64String(String base64String, String resourceLocation)
  {
    this.base64String = base64String;
    this.resourceLocation = The5zigMod.getVars().createResourceLocation(resourceLocation);
    this.dynamicImage = The5zigMod.getVars().getTexture(this.resourceLocation);
    prepareImage();
  }
  
  public void reset()
  {
    this.base64String = null;
    this.resourceLocation = null;
    this.dynamicImage = null;
    prepareImage();
  }
  
  private void prepareImage()
  {
    if (this.base64String == null)
    {
      delete(this.resourceLocation);
      this.dynamicImage = null;
      return;
    }
    ByteBuf localByteBuf1 = Unpooled.copiedBuffer(this.base64String, Charsets.UTF_8);
    ByteBuf localByteBuf2 = Base64.decode(localByteBuf1);
    try
    {
      BufferedImage localBufferedImage = read(new ByteBufInputStream(localByteBuf2));
      Validate.validState(localBufferedImage.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
      Validate.validState(localBufferedImage.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
      delete(this.resourceLocation);
      this.dynamicImage = null;
      return;
    }
    finally
    {
      localByteBuf1.release();
      localByteBuf2.release();
    }
    BufferedImage localBufferedImage;
    if (this.dynamicImage == null) {
      this.dynamicImage = The5zigMod.getVars().loadTexture(this.resourceLocation, this.width, this.height);
    }
    The5zigMod.getVars().renderDynamicImage(this.dynamicImage, localBufferedImage);
  }
  
  private static BufferedImage read(InputStream byteBuf)
    throws IOException
  {
    try
    {
      return ImageIO.read(byteBuf);
    }
    finally
    {
      IOUtils.closeQuietly(byteBuf);
    }
  }
  
  private void delete(IResourceLocation resource)
  {
    The5zigMod.getVars().deleteTexture(resource);
  }
  
  public int getX()
  {
    return this.x;
  }
  
  public void setX(int x)
  {
    this.x = x;
  }
  
  public int getY()
  {
    return this.y;
  }
  
  public void setY(int y)
  {
    this.y = y;
  }
  
  public int getWidth()
  {
    return this.width;
  }
  
  public void setWidth(int width)
  {
    this.width = width;
  }
  
  public int getHeight()
  {
    return this.height;
  }
  
  public void setHeight(int height)
  {
    this.height = height;
  }
  
  public void setWidthAndHeight(int size)
  {
    this.width = size;
    this.height = size;
  }
}
