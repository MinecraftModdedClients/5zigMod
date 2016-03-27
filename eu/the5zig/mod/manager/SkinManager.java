package eu.the5zig.mod.manager;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.util.Utils;
import eu.the5zig.util.io.FileUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import org.apache.logging.log4j.Logger;

public class SkinManager
{
  private final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Skin Download Thread #%d").build());
  private final File dir;
  private final String END = ".skin";
  private HashMap<String, Base64Skin> base64EncodedSkins = new HashMap();
  
  public SkinManager()
  {
    this.dir = new File("the5zigmod/skins");
    try
    {
      load();
    }
    catch (IOException e)
    {
      e.printStackTrace();
      The5zigMod.logger.warn("Could not load Skins!");
    }
  }
  
  private void load()
    throws IOException
  {
    File[] files = this.dir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files)
    {
      if (!file.getName().endsWith(".skin")) {
        return;
      }
      String name = file.getName().substring(0, file.getName().length() - ".skin".length());
      String base64 = FileUtils.readFile(file);
      this.base64EncodedSkins.put(name, new Base64Skin(base64, null));
    }
  }
  
  private void setBase64EncodedSkin(String uuid, String base64)
  {
    try
    {
      FileUtils.writeFile(new File(this.dir, uuid + ".skin"), base64);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    this.base64EncodedSkins.put(uuid, new Base64Skin(base64, null));
    ((Base64Skin)this.base64EncodedSkins.get(uuid)).setUpToDate();
  }
  
  public String getBase64EncodedSkin(UUID uniqueId)
  {
    String uuid = Utils.getUUIDWithoutDashes(uniqueId);
    if (this.base64EncodedSkins.containsKey(uuid))
    {
      if (!((Base64Skin)this.base64EncodedSkins.get(uuid)).isUpToDate())
      {
        ((Base64Skin)this.base64EncodedSkins.get(uuid)).setUpToDate();
        downloadBase64Skin(uuid);
      }
      return ((Base64Skin)this.base64EncodedSkins.get(uuid)).getBase64();
    }
    this.base64EncodedSkins.put(uuid, new Base64Skin(null, null));
    downloadBase64Skin(uuid);
    return null;
  }
  
  private void downloadBase64Skin(final String uuid)
  {
    this.executorService.submit(new Runnable()
    {
      public void run()
      {
        try
        {
          URL url = new URL("http://cravatar.eu/helmavatar/" + uuid + "/64");
          BufferedImage image = ImageIO.read(url);
          
          ByteBuf localByteBuf1 = Unpooled.buffer();
          ImageIO.write(image, "PNG", new ByteBufOutputStream(localByteBuf1));
          ByteBuf localByteBuf2 = Base64.encode(localByteBuf1);
          String imageDataString = localByteBuf2.toString(Charsets.UTF_8);
          SkinManager.this.setBase64EncodedSkin(uuid, imageDataString);
          The5zigMod.logger.debug("Got Base64 encoded skin for {}", new Object[] { uuid });
        }
        catch (Exception e)
        {
          The5zigMod.logger.warn("Could not get Base64 skin for " + uuid, e);
        }
      }
    });
  }
  
  private class Base64Skin
  {
    private String base64;
    private boolean upToDate;
    
    private Base64Skin(String base64)
    {
      this.base64 = base64;
      this.upToDate = false;
    }
    
    public boolean isUpToDate()
    {
      return this.upToDate;
    }
    
    public void setUpToDate()
    {
      this.upToDate = true;
    }
    
    public String getBase64()
    {
      return this.base64;
    }
  }
}
