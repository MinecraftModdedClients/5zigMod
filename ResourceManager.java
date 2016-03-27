import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.asm.Transformer;
import eu.the5zig.mod.gui.ingame.resource.CapeResource;
import eu.the5zig.mod.gui.ingame.resource.IResourceManager;
import eu.the5zig.mod.gui.ingame.resource.ItemModelResource;
import eu.the5zig.mod.gui.ingame.resource.ItemModelResource.Render;
import eu.the5zig.mod.gui.ingame.resource.PlayerResource;
import eu.the5zig.mod.util.ClassProxyCallback;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.util.Utils;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.lwjgl.util.vector.Vector3f;
import sun.misc.BASE64Decoder;

public class ResourceManager
  implements IResourceManager
{
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("5zig Texture Downloader #%d").setDaemon(true)
    .build());
  private static final String BASE_URL = "http://5zig.eu/";
  private static final Gson gson = new Gson();
  private final Object guiCameraTransform;
  private final FaceBakery faceBakery = new FaceBakery();
  private UUID playerUUID;
  private PlayerResource ownPlayerResource;
  private final Cache<UUID, PlayerResource> playerResources = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.MINUTES).build();
  private final Cache<Integer, String> moduleIds = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES).build();
  
  public ResourceManager()
    throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
  {
    this.guiCameraTransform = Thread.currentThread().getContextClassLoader().loadClass(bos.class.getName() + (Transformer.FORGE ? "$TransformType" : "$b")).getDeclaredField(Transformer.FORGE ? "GUI" : "g").get(null);
  }
  
  public void loadPlayerTextures(GameProfile gameProfile, Object instance)
  {
    if (this.playerUUID == null) {
      this.playerUUID = Utils.getUUID(MinecraftFactory.getVars().getUUID());
    }
    if ((gameProfile == null) || (gameProfile.getId() == null)) {
      return;
    }
    PlayerResource playerResource = null;
    if (this.playerUUID.equals(gameProfile.getId()))
    {
      if (this.ownPlayerResource != null) {
        playerResource = this.ownPlayerResource;
      } else {
        playerResource = this.ownPlayerResource = loadPlayerResource(gameProfile, instance);
      }
    }
    else
    {
      playerResource = (PlayerResource)this.playerResources.getIfPresent(gameProfile.getId());
      if (playerResource != null) {
        MinecraftFactory.getClassProxyCallback().getLogger().debug("Loaded player resource textures from cache for player " + gameProfile.getName());
      } else {
        this.playerResources.put(gameProfile.getId(), loadPlayerResource(gameProfile, instance));
      }
    }
    if (playerResource == null) {
      return;
    }
    if (playerResource.getCapeResource() != null)
    {
      ((Variables)MinecraftFactory.getVars()).getTextureManager().a((kk)playerResource.getCapeResource().getResourceLocation(), 
        (SimpleTexture)playerResource.getCapeResource().getSimpleTexture());
      if (instance != null) {
        ((Variables)MinecraftFactory.getVars()).setCape((bmq)instance, (kk)playerResource.getCapeResource().getResourceLocation());
      }
    }
    if (playerResource.getItemModelResources() != null) {
      for (ItemModelResource itemModelResource : playerResource.getItemModelResources()) {
        ((Variables)MinecraftFactory.getVars()).getTextureManager().a((kk)itemModelResource.getResourceLocation(), (SimpleTexture)itemModelResource.getSimpleTexture());
      }
    }
  }
  
  private PlayerResource loadPlayerResource(final GameProfile gameProfile, final Object instance)
  {
    final MinecraftProfileTexture minecraftProfileTexture = new MinecraftProfileTexture("http://5zig.eu/textures/2/" + Utils.getUUIDWithoutDashes(gameProfile.getId()), new HashMap());
    final PlayerResource playerResource = new PlayerResource();
    
    final kk capeLocation = new kk("capes/" + gameProfile.getId() + ".png");
    final SimpleTexture capeTexture = new SimpleTexture();
    ((Variables)MinecraftFactory.getVars()).getTextureManager().a(capeLocation, capeTexture);
    
    EXECUTOR_SERVICE.execute(new Runnable()
    {
      public void run()
      {
        MinecraftFactory.getClassProxyCallback().getLogger().debug("Loading player resource textures from {} for player {}", new Object[] { minecraftProfileTexture.getUrl(), gameProfile.getName() });
        
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try
        {
          connection = (HttpURLConnection)new URL(minecraftProfileTexture.getUrl()).openConnection(MinecraftFactory.getVars().getProxy());
          connection.setDoInput(true);
          connection.setDoOutput(false);
          connection.connect();
          if (connection.getResponseCode() != 200) {
            return;
          }
          reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
          String line = reader.readLine();
          
          TextureData data = (TextureData)ResourceManager.gson.fromJson(line, TextureData.class);
          if ((data.cape != null) && (!data.cape.isEmpty())) {
            try
            {
              BufferedImage cape = ImageIO.read(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(data.cape)));
              capeTexture.setBufferedImage(cape);
              if (instance != null) {
                ((Variables)MinecraftFactory.getVars()).setCape((bmq)instance, capeLocation);
              }
              playerResource.setCapeResource(new CapeResource(capeLocation, capeTexture));
            }
            catch (Exception e)
            {
              MinecraftFactory.getClassProxyCallback().getLogger().error("Could not parse cape for " + gameProfile.getId(), e);
            }
          }
          if (data.models != null)
          {
            playerResource.setItemModelResources(Lists.newArrayList());
            List<TextureData.Model> models = data.models;
            for (TextureData.Model model : models) {
              if ((!StringUtils.isEmpty(model.itemName)) && (!StringUtils.isEmpty(model.texture))) {
                try
                {
                  ado item = (ado)ado.f.c(new kk(model.itemName));
                  if (item != null)
                  {
                    kk modelLocation = new kk("item-models/" + gameProfile.getId() + "/" + model.itemName + (StringUtils.isNotEmpty(model.render) ? "/" + model.render.toLowerCase() : "") + ".png");
                    
                    SimpleTexture modelTexture = new SimpleTexture();
                    ((Variables)MinecraftFactory.getVars()).getTextureManager().a(modelLocation, modelTexture);
                    
                    BufferedImage modelImage = ImageIO.read(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(model.texture)));
                    modelTexture.setBufferedImage(modelImage);
                    
                    String modelData = (model.modelId == null) || (model.modelId.intValue() == 0) ? model.model : ResourceManager.this.getModelIdData(model.modelId);
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
                      playerResource.getItemModelResources().add(new ItemModelResource(modelLocation, item, render, ResourceManager.this
                        .createModel(new String(new BASE64Decoder().decodeBuffer(modelData), Charsets.UTF_8)), modelTexture));
                    }
                  }
                }
                catch (Exception e)
                {
                  MinecraftFactory.getClassProxyCallback().getLogger().error("Could not parse item model for " + gameProfile.getId(), e);
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
    });
    return playerResource;
  }
  
  private String getModelIdData(final Integer modelId)
  {
    synchronized (this.moduleIds)
    {
      try
      {
        (String)this.moduleIds.get(modelId, new Callable()
        {
          public String call()
            throws Exception
          {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try
            {
              connection = (HttpURLConnection)new URL("http://5zig.eu/models/2/" + modelId).openConnection();
              String str;
              if (connection.getResponseCode() != 200) {
                return null;
              }
              reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
              return reader.readLine();
            }
            finally
            {
              IOUtils.closeQuietly(reader);
              if (connection != null) {
                connection.disconnect();
              }
            }
          }
        });
      }
      catch (ExecutionException e)
      {
        MinecraftFactory.getClassProxyCallback().getLogger().warn("Could not load model data with id " + modelId, e);
        return null;
      }
    }
  }
  
  public void updateOwnPlayerTextures()
  {
    this.ownPlayerResource = null;
    if (((Variables)MinecraftFactory.getVars()).getPlayer() != null) {
      ((Variables)MinecraftFactory.getVars()).setCape(((Variables)MinecraftFactory.getVars()).getPlayer(), null);
    }
    loadPlayerTextures(MinecraftFactory.getVars().getGameProfile(), ((Variables)MinecraftFactory.getVars()).getPlayer());
  }
  
  public Object getOwnCapeLocation()
  {
    if ((this.ownPlayerResource != null) && (this.ownPlayerResource.getCapeResource() != null)) {
      return this.ownPlayerResource.getCapeResource().getResourceLocation();
    }
    return null;
  }
  
  public void cleanupTextures()
  {
    MinecraftFactory.getClassProxyCallback().getLogger().debug("Cleaning up player textures...");
    this.playerResources.invalidateAll();
    this.ownPlayerResource = null;
  }
  
  private bxo createModel(String model)
  {
    bok modelBlock = bok.a(model);
    
    return bakeModel(modelBlock);
  }
  
  private bxo bakeModel(bok modelBlock)
  {
    bxv.a bakedModelBuilder = new bxv.a(modelBlock, modelBlock.g()).a(TextureAtlasSprite.MISSING_NO);
    for (Iterator localIterator1 = modelBlock.a().iterator(); localIterator1.hasNext();)
    {
      blockPart = (bog)localIterator1.next();
      for (cq face : blockPart.c.keySet())
      {
        boh blockPartFace = (boh)blockPart.c.get(face);
        if (blockPartFace.b == null) {
          bakedModelBuilder.a(this.faceBakery.a(blockPart.a, blockPart.b, blockPartFace, face, bxp.a, blockPart.d, false, blockPart.e));
        } else {
          bakedModelBuilder.a(bxp.a.a(blockPartFace.b), this.faceBakery.a(blockPart.a, blockPart.b, blockPartFace, face, bxp.a, blockPart.d, false, blockPart.e));
        }
      }
    }
    bog blockPart;
    return bakedModelBuilder.b();
  }
  
  private boolean shouldRender(ItemModelResource itemModelResource, zj entityPlayer, adq itemStack)
  {
    if (itemStack.b() == itemModelResource.getItem()) {
      if ((itemStack.b() == ads.f) && (entityPlayer.bR() != null))
      {
        int useCount = entityPlayer.cw() == 0 ? 0 : itemStack.l() - entityPlayer.cw();
        if (useCount >= 18)
        {
          if (itemModelResource.getRender() == ItemModelResource.Render.BOW_PULLING_2) {
            return true;
          }
        }
        else if (useCount > 13)
        {
          if (itemModelResource.getRender() == ItemModelResource.Render.BOW_PULLING_1) {
            return true;
          }
        }
        else if ((useCount > 0) && 
          (itemModelResource.getRender() == ItemModelResource.Render.BOW_PULLING_0)) {
          return true;
        }
      }
      else if ((itemStack.b() == ads.aY) && (entityPlayer.bP != null))
      {
        if (itemModelResource.getRender() == ItemModelResource.Render.FISHING_ROD_CAST) {
          return true;
        }
      }
      else if (itemModelResource.getRender() == null)
      {
        return true;
      }
    }
    return false;
  }
  
  public boolean renderInPersonMode(Object instance, Object itemStackObject, Object entityPlayerObject, Object cameraTransformTypeObject, boolean leftHand)
  {
    if (!MinecraftFactory.getClassProxyCallback().isRenderCustomModels()) {
      return false;
    }
    adq itemStack = (adq)itemStackObject;
    if (!(entityPlayerObject instanceof zj)) {
      return false;
    }
    zj entityPlayer = (zj)entityPlayerObject;
    bos.b cameraTransformType = (bos.b)cameraTransformTypeObject;
    UUID uuid = entityPlayer.bc();
    PlayerResource playerResource = this.playerUUID.equals(uuid) ? this.ownPlayerResource : (PlayerResource)this.playerResources.getIfPresent(uuid);
    if ((playerResource == null) || (playerResource.getItemModelResources() == null)) {
      return false;
    }
    for (ItemModelResource itemModelResource : playerResource.getItemModelResources()) {
      if (shouldRender(itemModelResource, entityPlayer, itemStack))
      {
        render(instance, itemStack, itemModelResource, cameraTransformType, leftHand);
        return true;
      }
    }
    return false;
  }
  
  private void render(Object instance, adq itemStack, ItemModelResource itemModel, bos.b transformType, boolean leftHand)
  {
    MinecraftFactory.getVars().bindTexture(itemModel.getResourceLocation());
    ((Variables)MinecraftFactory.getVars()).getTextureManager().b((kk)itemModel.getResourceLocation()).b(false, false);
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
    bni.D();
    bni.a(516, 0.1F);
    bni.m();
    bni.a(bni.r.l, bni.l.j, bni.r.e, bni.l.n);
    bni.G();
    bos itemCameraTransforms = ((bxo)itemModel.getBakedModel()).e();
    bos.a(itemCameraTransforms.b(transformType), leftHand);
    if (a(itemCameraTransforms.b(transformType))) {
      bni.a(bni.i.a);
    }
    ((brz)instance).a(itemStack, (bxo)itemModel.getBakedModel());
    bni.a(bni.i.b);
    bni.H();
    bni.E();
    bni.l();
    MinecraftFactory.getVars().bindTexture(itemModel.getResourceLocation());
    ((Variables)MinecraftFactory.getVars()).getTextureManager().b((kk)itemModel.getResourceLocation()).a();
  }
  
  private boolean a(bor var)
  {
    return (var.d.x < 0.0F ? 1 : 0) ^ (var.d.y < 0.0F ? 1 : 0) ^ (var.d.z < 0.0F ? 1 : 0);
  }
  
  public boolean renderInInventory(Object instance, Object itemStackObject, int x, int y)
  {
    if (!MinecraftFactory.getClassProxyCallback().isRenderCustomModels()) {
      return false;
    }
    adq itemStack = (adq)itemStackObject;
    if ((this.ownPlayerResource == null) || (this.ownPlayerResource.getItemModelResources() == null)) {
      return false;
    }
    for (ItemModelResource itemModel : this.ownPlayerResource.getItemModelResources())
    {
      bmt player = ((Variables)MinecraftFactory.getVars()).getPlayer();
      if ((shouldRender(itemModel, player, itemStack)) && ((itemModel.getRender() == null) || (player.br.h() == itemStack)))
      {
        bni.G();
        MinecraftFactory.getVars().bindTexture(itemModel.getResourceLocation());
        ((Variables)MinecraftFactory.getVars()).getTextureManager().b((kk)itemModel.getResourceLocation()).b(false, false);
        bni.D();
        bni.e();
        bni.a(516, 0.1F);
        bni.m();
        bni.a(bni.r.l, bni.l.j);
        bni.c(1.0F, 1.0F, 1.0F, 1.0F);
        a((brz)instance, x, y, ((bxo)itemModel.getBakedModel()).b());
        ((bxo)itemModel.getBakedModel()).e().a((bos.b)this.guiCameraTransform);
        ((brz)instance).a(itemStack, (bxo)itemModel.getBakedModel());
        bni.d();
        bni.E();
        bni.g();
        bni.H();
        MinecraftFactory.getVars().bindTexture(itemModel.getResourceLocation());
        ((Variables)MinecraftFactory.getVars()).getTextureManager().b((kk)itemModel.getResourceLocation()).a();
        
        return true;
      }
    }
    return false;
  }
  
  private void a(brz instance, int var, int var1, boolean var2)
  {
    bni.c(var, var1, 100.0F + instance.a);
    bni.c(8.0F, 8.0F, 0.0F);
    bni.b(1.0F, -1.0F, 1.0F);
    bni.b(16.0F, 16.0F, 16.0F);
    if (var2) {
      bni.f();
    } else {
      bni.g();
    }
  }
}
