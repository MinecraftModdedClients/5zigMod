import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.ingame.resource.CapeResource;
import eu.the5zig.mod.gui.ingame.resource.ItemModelResource;
import eu.the5zig.mod.gui.ingame.resource.ItemModelResource.Render;
import eu.the5zig.mod.gui.ingame.resource.PlayerResource;
import eu.the5zig.mod.util.ClassProxyCallback;
import eu.the5zig.mod.util.IVariables;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import sun.misc.BASE64Decoder;

class ResourceManager$1
  implements Runnable
{
  ResourceManager$1(ResourceManager this$0, MinecraftProfileTexture paramMinecraftProfileTexture, GameProfile paramGameProfile, SimpleTexture paramSimpleTexture, Object paramObject, kk paramkk, PlayerResource paramPlayerResource) {}
  
  public void run()
  {
    MinecraftFactory.getClassProxyCallback().getLogger().debug("Loading player resource textures from {} for player {}", new Object[] { this.val$minecraftProfileTexture.getUrl(), this.val$gameProfile.getName() });
    
    HttpURLConnection connection = null;
    BufferedReader reader = null;
    try
    {
      connection = (HttpURLConnection)new URL(this.val$minecraftProfileTexture.getUrl()).openConnection(MinecraftFactory.getVars().getProxy());
      connection.setDoInput(true);
      connection.setDoOutput(false);
      connection.connect();
      if (connection.getResponseCode() != 200) {
        return;
      }
      reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line = reader.readLine();
      
      TextureData data = (TextureData)ResourceManager.access$000().fromJson(line, TextureData.class);
      if ((data.cape != null) && (!data.cape.isEmpty())) {
        try
        {
          BufferedImage cape = ImageIO.read(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(data.cape)));
          this.val$capeTexture.setBufferedImage(cape);
          if (this.val$instance != null) {
            ((Variables)MinecraftFactory.getVars()).setCape((bmq)this.val$instance, this.val$capeLocation);
          }
          this.val$playerResource.setCapeResource(new CapeResource(this.val$capeLocation, this.val$capeTexture));
        }
        catch (Exception e)
        {
          MinecraftFactory.getClassProxyCallback().getLogger().error("Could not parse cape for " + this.val$gameProfile.getId(), e);
        }
      }
      if (data.models != null)
      {
        this.val$playerResource.setItemModelResources(Lists.newArrayList());
        List<TextureData.Model> models = data.models;
        for (TextureData.Model model : models) {
          if ((!StringUtils.isEmpty(model.itemName)) && (!StringUtils.isEmpty(model.texture))) {
            try
            {
              ado item = (ado)ado.f.c(new kk(model.itemName));
              if (item != null)
              {
                kk modelLocation = new kk("item-models/" + this.val$gameProfile.getId() + "/" + model.itemName + (StringUtils.isNotEmpty(model.render) ? "/" + model.render.toLowerCase() : "") + ".png");
                
                SimpleTexture modelTexture = new SimpleTexture();
                ((Variables)MinecraftFactory.getVars()).getTextureManager().a(modelLocation, modelTexture);
                
                BufferedImage modelImage = ImageIO.read(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(model.texture)));
                modelTexture.setBufferedImage(modelImage);
                
                String modelData = (model.modelId == null) || (model.modelId.intValue() == 0) ? model.model : ResourceManager.access$100(this.this$0, model.modelId);
                ItemModelResource.Render render = null;
                if (StringUtils.isNotEmpty(model.render)) {
                  try
                  {
                    render = ItemModelResource.Render.valueOf(model.render);
                  }
                  catch (IllegalArgumentException e)
                  {
                    MinecraftFactory.getClassProxyCallback().getLogger().warn("Could not parse render type for model with item " + model.itemName + "!", e);
                  }
                }
                if (StringUtils.isNotEmpty(modelData)) {
                  this.val$playerResource.getItemModelResources().add(new ItemModelResource(modelLocation, item, render, 
                    ResourceManager.access$200(this.this$0, new String(new BASE64Decoder().decodeBuffer(modelData), Charsets.UTF_8)), modelTexture));
                }
              }
            }
            catch (Exception e)
            {
              MinecraftFactory.getClassProxyCallback().getLogger().error("Could not parse item model for " + this.val$gameProfile.getId(), e);
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      MinecraftFactory.getClassProxyCallback().getLogger().error("Couldn't download http texture", e);
    }
    finally
    {
      IOUtils.closeQuietly(reader);
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
