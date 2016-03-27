import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import eu.the5zig.mod.asm.Transformer;
import eu.the5zig.mod.gui.Gui;
import eu.the5zig.mod.gui.IOverlay;
import eu.the5zig.mod.gui.IWrappedGui;
import eu.the5zig.mod.gui.elements.Clickable;
import eu.the5zig.mod.gui.elements.IButton;
import eu.the5zig.mod.gui.elements.IColorSelector;
import eu.the5zig.mod.gui.elements.IFileSelector;
import eu.the5zig.mod.gui.elements.IGuiList;
import eu.the5zig.mod.gui.elements.IPlaceholderTextfield;
import eu.the5zig.mod.gui.elements.ITextfield;
import eu.the5zig.mod.gui.elements.Row;
import eu.the5zig.mod.gui.ingame.IGui2ndChat;
import eu.the5zig.mod.gui.ingame.PotionEffect;
import eu.the5zig.mod.gui.ingame.Scoreboard;
import eu.the5zig.mod.util.AudioCallback;
import eu.the5zig.mod.util.ClassProxyCallback;
import eu.the5zig.mod.util.ColorSelectorCallback;
import eu.the5zig.mod.util.GLUtil;
import eu.the5zig.mod.util.GuiListChatCallback;
import eu.the5zig.mod.util.IKeybinding;
import eu.the5zig.mod.util.IResourceLocation;
import eu.the5zig.mod.util.IVariables;
import eu.the5zig.mod.util.IVariables.CapeCallback;
import eu.the5zig.mod.util.IVariables.MouseOverObject;
import eu.the5zig.mod.util.IVariables.ObjectType;
import eu.the5zig.mod.util.ItemStack;
import eu.the5zig.mod.util.SliderCallback;
import eu.the5zig.util.Callback;
import eu.the5zig.util.Utils;
import io.netty.buffer.ByteBuf;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Logger;

public class Variables
  implements IVariables
{
  private static Field forgeChatField;
  private final brz itemRenderer;
  private bcx scaledResolution;
  
  static
  {
    if (Transformer.FORGE) {
      try
      {
        forgeChatField = Thread.currentThread().getContextClassLoader().loadClass(Names.guiChat.getName()).getDeclaredField("field_146415_a");
        forgeChatField.setAccessible(true);
      }
      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }
  }
  
  private IGui2ndChat gui2ndChat = new Gui2ndChat();
  private final ResourceManager resourceManager;
  
  public Variables()
  {
    this.itemRenderer = getMinecraft().ad();
    updateScaledResolution();
    try
    {
      this.resourceManager = new ResourceManager();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public void drawString(String string, int x, int y, Object... format)
  {
    drawString(string, x, y, 16777215, format);
  }
  
  public void drawString(String string, int x, int y)
  {
    drawString(string, x, y, 16777215);
  }
  
  public void drawCenteredString(String string, int x, int y)
  {
    drawCenteredString(string, x, y, 16777215);
  }
  
  public void drawCenteredString(String string, int x, int y, int color)
  {
    drawString(string, x - getStringWidth(string) / 2, y, color);
  }
  
  public void drawString(String string, int x, int y, int color, Object... format)
  {
    drawString(String.format(string, format), x, y, color);
  }
  
  public void drawString(String string, int x, int y, int color)
  {
    drawString(string, x, y, color, true);
  }
  
  public void drawString(String string, int x, int y, int color, boolean withShadow)
  {
    getFontrenderer().a(string, x, y, color, withShadow);
  }
  
  public List<String> splitStringToWidth(String string, int width)
  {
    Validate.isTrue(width > 0);
    if (string == null) {
      return Collections.emptyList();
    }
    if (string.isEmpty()) {
      return Collections.singletonList("");
    }
    return getFontrenderer().c(string, width);
  }
  
  public int getStringWidth(String string)
  {
    return getFontrenderer().a(string);
  }
  
  public String shortenToWidth(String string, int width)
  {
    if (StringUtils.isEmpty(string)) {
      return string;
    }
    Validate.isTrue(width > 0);
    
    boolean changed = false;
    while (getStringWidth(string) > width + getStringWidth("..."))
    {
      string = string.substring(0, string.length() - 1);
      changed = true;
    }
    if (changed) {
      string = string + "...";
    }
    return string;
  }
  
  public IButton createButton(int id, int x, int y, String label)
  {
    return new Button(id, x, y, label);
  }
  
  public IButton createButton(int id, int x, int y, String label, boolean enabled)
  {
    return new Button(id, x, y, label, enabled);
  }
  
  public IButton createButton(int id, int x, int y, int width, int height, String label)
  {
    return new Button(id, x, y, width, height, label);
  }
  
  public IButton createButton(int id, int x, int y, int width, int height, String label, boolean enabled)
  {
    return new Button(id, x, y, width, height, label, enabled);
  }
  
  public IButton createStringButton(int id, int x, int y, String label)
  {
    return new StringButton(id, x, y, label);
  }
  
  public IButton createStringButton(int id, int x, int y, int width, int height, String label)
  {
    return new StringButton(id, x, y, width, height, label);
  }
  
  public IButton createAudioButton(int id, int x, int y, AudioCallback callback)
  {
    return new AudioButton(id, x, y, callback);
  }
  
  public IButton createIconButton(IResourceLocation resourceLocation, int u, int v, int id, int x, int y)
  {
    return new IconButton(resourceLocation, u, v, id, x, y);
  }
  
  public ITextfield createTextfield(int id, int x, int y, int width, int height)
  {
    return new Textfield(id, x, y, width, height);
  }
  
  public ITextfield createTextfield(int id, int x, int y, int width, int height, int maxStringLength)
  {
    return new Textfield(id, x, y, width, height, maxStringLength);
  }
  
  public IPlaceholderTextfield createTextfield(String placeholder, int id, int x, int y, int width, int height)
  {
    return new PlaceholderTextfield(placeholder, id, x, y, width, height);
  }
  
  public IPlaceholderTextfield createTextfield(String placeholder, int id, int x, int y, int width, int height, int maxStringLength)
  {
    return new PlaceholderTextfield(placeholder, id, x, y, width, height, maxStringLength);
  }
  
  public <E extends Row> IGuiList<E> createGuiList(Clickable<E> clickable, int width, int height, int top, int bottom, int left, int right, List<E> rows)
  {
    return new GuiList(clickable, width, height, top, bottom, left, right, rows);
  }
  
  public <E extends Row> IGuiList<E> createGuiList(Clickable<E> clickable, int width, int height, int top, int bottom, int left, int right, List<E> rows, int padding)
  {
    return new GuiList(clickable, width, height, top, bottom, left, right, rows, padding);
  }
  
  public <E extends Row> IGuiList<E> createGuiListChat(int width, int height, int top, int bottom, int left, int right, int scrollx, List<E> rows, GuiListChatCallback callback)
  {
    return new GuiListChat(width, height, top, bottom, left, right, scrollx, rows, callback);
  }
  
  public IFileSelector createFileSelector(File currentDir, int width, int height, int left, int right, int top, int bottom, Callback<File> callback)
  {
    return new FileSelector(currentDir, width, height, left, right, top, bottom, callback);
  }
  
  public IButton createSlider(int id, int x, int y, SliderCallback sliderCallback)
  {
    return new Slider(id, x, y, sliderCallback);
  }
  
  public IColorSelector createColorSelector(int id, int x, int y, int width, int height, String label, ColorSelectorCallback callback)
  {
    return new ColorSelector(id, x, y, width, height, label, callback);
  }
  
  public IOverlay newOverlay()
  {
    return new Overlay();
  }
  
  public void updateOverlayCount(int count)
  {
    Overlay.updateOverlayCount(count);
  }
  
  public IWrappedGui createWrappedGui(Object lastScreen)
  {
    return new WrappedGui((bfb)lastScreen);
  }
  
  public IKeybinding createKeybinding(String description, int keyCode, String category)
  {
    return new Keybinding(description, keyCode, category);
  }
  
  public IGui2ndChat get2ndChat()
  {
    return this.gui2ndChat;
  }
  
  public boolean isChatOpened()
  {
    return getMinecraftScreen() instanceof bed;
  }
  
  public void typeInChatGUI(String text)
  {
    if (!isChatOpened()) {
      return;
    }
    bee chatGUI = (bee)getMinecraftScreen();
    bdd chatField;
    if (Transformer.FORGE) {
      try
      {
        chatField = (bdd)forgeChatField.get(chatGUI);
      }
      catch (Exception e)
      {
        bdd chatField;
        throw new RuntimeException(e);
      }
    } else {
      chatField = chatGUI.a;
    }
    chatField.a(chatField.b() + text);
  }
  
  public void registerKeybindings(List<IKeybinding> keybindings)
  {
    bcc[] currentKeybindings = getGameSettings().al;
    bcc[] customKeybindings = new bcc[keybindings.size()];
    for (int i = 0; i < keybindings.size(); i++) {
      customKeybindings[i] = ((bcc)keybindings.get(i));
    }
    getGameSettings().al = ((bcc[])Utils.concat(currentKeybindings, customKeybindings));
    
    getGameSettings().a();
  }
  
  public void playSound(String sound, float pitch)
  {
    playSound("minecraft", sound, pitch);
  }
  
  public void playSound(String domain, String sound, float pitch)
  {
    getMinecraft().U().a(bye.a(new nf(new ResourceLocation(domain, sound)), pitch));
  }
  
  public int getFontHeight()
  {
    return getFontrenderer().a;
  }
  
  public bkx getServerData()
  {
    return getMinecraft().C();
  }
  
  public Object getMinecraftServerData()
  {
    return getServerData();
  }
  
  public void resetServer()
  {
    getMinecraft().a((bkx)null);
  }
  
  public String getServer()
  {
    if (getServerData() == null) {
      return null;
    }
    return getServerData().b;
  }
  
  public int getServerPlayers()
  {
    return getPlayer().d.d().size();
  }
  
  public boolean isTablistShown()
  {
    return (getGameSettings().ad.e()) && ((!getMinecraft().E()) || (getServerPlayers() > 1));
  }
  
  public void setFOV(float fov)
  {
    getGameSettings().aw = fov;
  }
  
  public float getFOV()
  {
    return getGameSettings().aw;
  }
  
  public void setSmoothCamera(boolean smoothCamera)
  {
    getGameSettings().au = smoothCamera;
  }
  
  public boolean isSmoothCamera()
  {
    return getGameSettings().au;
  }
  
  public String translate(String location, Object... values)
  {
    return bwo.a(location, values);
  }
  
  public void displayScreen(Gui gui)
  {
    if (gui == null) {
      displayScreen((bfb)null);
    } else {
      displayScreen(gui.getHandle());
    }
  }
  
  public void displayScreen(Object gui)
  {
    getMinecraft().a((bfb)gui);
  }
  
  public void joinServer(String host, int port)
  {
    if ((getServerData() != null) && (getWorld() != null))
    {
      getWorld().H();
      getMinecraft().a((bku)null);
    }
    MinecraftFactory.getClassProxyCallback().resetServer();
    displayScreen(new bei((bfb)getMinecraftScreen(), getMinecraft(), new bkx(host, host + ":" + port, false)));
  }
  
  public void joinServer(Object parentScreen, Object serverData)
  {
    if (serverData == null) {
      return;
    }
    if (getWorld() != null)
    {
      getWorld().H();
      getMinecraft().a((bku)null);
    }
    MinecraftFactory.getClassProxyCallback().resetServer();
    displayScreen(new bei((bfb)parentScreen, getMinecraft(), (bkx)serverData));
  }
  
  public long getSystemTime()
  {
    return bcf.I();
  }
  
  public bcf getMinecraft()
  {
    return bcf.z();
  }
  
  public bct getFontrenderer()
  {
    return getMinecraft().k;
  }
  
  public bch getGameSettings()
  {
    return getMinecraft().u;
  }
  
  public bmt getPlayer()
  {
    return getMinecraft().h;
  }
  
  public bku getWorld()
  {
    return getMinecraft().f;
  }
  
  public bcu getGuiIngame()
  {
    return getMinecraft().r;
  }
  
  public boolean isSpectatingSelf()
  {
    return getSpectatingEntity() instanceof zj;
  }
  
  public rr getSpectatingEntity()
  {
    return getMinecraft().aa();
  }
  
  public aau getOpenContainer()
  {
    return getPlayer().bs;
  }
  
  public String getOpenContainerTitle()
  {
    if (!(getOpenContainer() instanceof abb)) {
      return null;
    }
    return ((abb)getOpenContainer()).e().h_();
  }
  
  public void closeContainer()
  {
    getPlayer().q();
  }
  
  public String getSession()
  {
    return getMinecraft().K().d();
  }
  
  public String getUsername()
  {
    return getMinecraft().K().c();
  }
  
  public String getUUID()
  {
    return getMinecraft().K().b();
  }
  
  public Proxy getProxy()
  {
    return getMinecraft().M();
  }
  
  public GameProfile getGameProfile()
  {
    return getMinecraft().K().e();
  }
  
  public String getFPS()
  {
    return getMinecraft().D.split(" fps")[0];
  }
  
  public boolean isPlayerNull()
  {
    return getPlayer() == null;
  }
  
  public boolean isTerrainLoading()
  {
    return getMinecraftScreen() instanceof bfa;
  }
  
  public double getPlayerPosX()
  {
    return getSpectatingEntity().p;
  }
  
  public double getPlayerPosY()
  {
    return getSpectatingEntity().bl().b;
  }
  
  public double getPlayerPosZ()
  {
    return getSpectatingEntity().r;
  }
  
  public float getPlayerRotationYaw()
  {
    return getSpectatingEntity().x;
  }
  
  public boolean isFancyGraphicsEnabled()
  {
    return bcf.w();
  }
  
  public String getBiome()
  {
    cj localdt = new cj(getPlayerPosX(), getPlayerPosY(), getPlayerPosZ());
    if (!getWorld().e(localdt)) {
      return null;
    }
    ase localObject = getMinecraft().f.f(localdt);
    return localObject.a(localdt, getMinecraft().f.A()).l();
  }
  
  public int getLightLevel()
  {
    cj localdt = new cj(getPlayerPosX(), getPlayerPosY(), getPlayerPosZ());
    if (!getWorld().e(localdt)) {
      return 0;
    }
    ase localObject = getMinecraft().f.f(localdt);
    return localObject.a(localdt, 0);
  }
  
  public String getEntityCount()
  {
    return getMinecraft().g.h().split(" |,")[1];
  }
  
  public float getSaturation()
  {
    return isSpectatingSelf() ? ((zj)getSpectatingEntity()).cS().e() : 0.0F;
  }
  
  public float getHealth(Object entity)
  {
    if (!(entity instanceof sa)) {
      return -1.0F;
    }
    return ((sa)entity).bQ();
  }
  
  public int getAir()
  {
    return getPlayer().aP();
  }
  
  public PotionEffect getPotionForVignette()
  {
    for (rl potionEffect : getActivePlayerPotionEffects())
    {
      rk potion = getPotionByEffect(potionEffect);
      if (potion.i()) {
        return wrapPotionEffect(potionEffect);
      }
    }
    for (rl potionEffect : getActivePlayerPotionEffects())
    {
      rk potion = getPotionByEffect(potionEffect);
      if (!potion.i()) {
        return wrapPotionEffect(potionEffect);
      }
    }
    return null;
  }
  
  public List<PotionEffect> getActivePotionEffects()
  {
    List<PotionEffect> result = new ArrayList(getActivePlayerPotionEffects().size());
    for (rl potionEffect : getActivePlayerPotionEffects()) {
      result.add(wrapPotionEffect(potionEffect));
    }
    Collections.sort(result);
    return result;
  }
  
  private PotionEffect wrapPotionEffect(rl potionEffect)
  {
    return new PotionEffect(potionEffect.f(), potionEffect.b(), rj.a(potionEffect, 1.0F), potionEffect.c() + 1, potionEffect.a().d(), potionEffect.a().i(), potionEffect.e());
  }
  
  public int getPotionEffectIndicatorHeight()
  {
    int result = 0;
    
    boolean hasGood = false;
    boolean hasBad = false;
    for (PotionEffect potionEffect : getActivePotionEffects()) {
      if ((potionEffect.getIconIndex() >= 0) && (potionEffect.hasParticles())) {
        if (potionEffect.isGood()) {
          hasGood = true;
        } else {
          hasBad = true;
        }
      }
    }
    if ((hasGood) && (hasBad)) {
      return 52;
    }
    if ((hasGood) || (hasBad)) {
      return 26;
    }
    return result;
  }
  
  public ItemStack getItemInMainHand()
  {
    return getPlayer().cb() == null ? null : new WrappedItemStack(getPlayer().cb());
  }
  
  public ItemStack getItemInOffHand()
  {
    return getPlayer().cc() == null ? null : new WrappedItemStack(getPlayer().cc());
  }
  
  public ItemStack getItemInArmorSlot(int slot)
  {
    return getArmorItemBySlot(slot) == null ? null : new WrappedItemStack(getArmorItemBySlot(slot));
  }
  
  public Collection<rl> getActivePlayerPotionEffects()
  {
    return getPlayer().bO();
  }
  
  public rk getPotionByEffect(rl potionEffect)
  {
    return potionEffect.a();
  }
  
  public adq getArmorItemBySlot(int slot)
  {
    return getPlayer().br.g(slot);
  }
  
  public ItemStack getItemByName(String resourceName)
  {
    return new WrappedItemStack(new adq(ado.d(resourceName)));
  }
  
  public ItemStack getItemByName(String resourceName, int amount)
  {
    return new WrappedItemStack(new adq(ado.d(resourceName), amount));
  }
  
  public int getItemCount(String key)
  {
    int count = 0;
    for (adq itemStack : getPlayer().br.a) {
      if (itemStack != null) {
        if (key.equals(itemStack.a())) {
          count += itemStack.b;
        }
      }
    }
    return count;
  }
  
  public void renderItem(adq itemStack, int x, int y)
  {
    bcd.c();
    GLUtil.enableBlend();
    GLUtil.tryBlendFuncSeparate(770, 771, 1, 0);
    this.itemRenderer.b(itemStack, x, y);
    this.itemRenderer.a(getFontrenderer(), itemStack, x, y);
    GLUtil.disableBlend();
    bcd.a();
  }
  
  public void updateScaledResolution()
  {
    this.scaledResolution = new bcx(getMinecraft());
  }
  
  public int getWidth()
  {
    return getMinecraft().d;
  }
  
  public int getHeight()
  {
    return getMinecraft().e;
  }
  
  public int getScaledWidth()
  {
    return this.scaledResolution.a();
  }
  
  public int getScaledHeight()
  {
    return this.scaledResolution.b();
  }
  
  public int getScaleFactor()
  {
    return this.scaledResolution.e();
  }
  
  public void drawIngameTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height)
  {
    if (getGuiIngame() != null) {
      getGuiIngame().b(x, y, textureX, textureY, width, height);
    }
  }
  
  public boolean showDebugScreen()
  {
    return getGameSettings().aq;
  }
  
  public boolean enableEverythingIsScrewedUpMode()
  {
    return (!isSpectatingSelf()) || (getPlayerController().a());
  }
  
  public boolean shouldDrawHUD()
  {
    return getPlayerController().b();
  }
  
  public int[] getHotbarKeys()
  {
    int[] result = new int[9];
    bcc[] hotbarBindings = getGameSettings().ak;
    for (int i = 0; i < hotbarBindings.length; i++) {
      result[i] = hotbarBindings[i].i();
    }
    return result;
  }
  
  private bkt getPlayerController()
  {
    return getMinecraft().c;
  }
  
  public Gui getCurrentScreen()
  {
    if ((getMinecraft().m == null) || (!(getMinecraft().m instanceof GuiHandle))) {
      return null;
    }
    return ((GuiHandle)getMinecraft().m).getChild();
  }
  
  public Object getMinecraftScreen()
  {
    return getMinecraft().m;
  }
  
  public void messagePlayer(String message)
  {
    messagePlayer(ChatComponentBuilder.fromLegacyText(message));
  }
  
  public void messagePlayer(eu chatComponent)
  {
    getPlayer().a(chatComponent);
  }
  
  public void sendMessage(String message)
  {
    getPlayer().g(message);
  }
  
  public boolean hasNetworkManager()
  {
    return getNetworkManager() != null;
  }
  
  public void sendCustomPayload(String channel, ByteBuf payload)
  {
    if (getNetworkManager() != null) {
      getNetworkManager().a(new iq(channel, new em(payload)));
    }
  }
  
  private ek getNetworkManager()
  {
    return getMinecraft().v() != null ? getMinecraft().v().a() : null;
  }
  
  public IResourceLocation createResourceLocation(String resourcePath)
  {
    return new ResourceLocation(resourcePath);
  }
  
  public IResourceLocation createResourceLocation(String resourceDomain, String resourcePath)
  {
    return new ResourceLocation(resourceDomain, resourcePath);
  }
  
  public Object bindDynamicImage(String name, BufferedImage image)
  {
    return getTextureManager().a(name, new bux(image));
  }
  
  public void bindTexture(Object resourceLocation)
  {
    getTextureManager().a((kk)resourceLocation);
  }
  
  public void deleteTexture(Object resourceLocation)
  {
    getTextureManager().c((kk)resourceLocation);
  }
  
  public Object loadTexture(Object resourceLocation, int width, int height)
  {
    bux dynamicImage = new bux(width, height);
    getTextureManager().a((kk)resourceLocation, dynamicImage);
    return dynamicImage;
  }
  
  public Object getTexture(Object resourceLocation)
  {
    return getTextureManager().b((kk)resourceLocation);
  }
  
  public void renderDynamicImage(Object dynamicImage, BufferedImage image)
  {
    image.getRGB(0, 0, image.getWidth(), image.getHeight(), ((bux)dynamicImage).e(), 0, image.getWidth());
    ((bux)dynamicImage).d();
  }
  
  public void renderPotionIcon(int index)
  {
    getGuiIngame().b(0, 0, index % 8 * 18, 198 + index / 8 * 18, 18, 18);
  }
  
  public bvi getTextureManager()
  {
    return getMinecraft().N();
  }
  
  public void bindCape(GameProfile gameProfile, final IVariables.CapeCallback callback, File location)
  {
    MinecraftProfileTexture minecraftProfileTexture = new MinecraftProfileTexture(String.format("http://5zig.eu/modcapes/%s.png", new Object[] { Utils.getUUIDWithoutDashes(gameProfile.getId()) }), new HashMap());
    
    final kk capeLocation = new kk("capes/" + minecraftProfileTexture.getHash());
    buy threadDownloadImage = new buy(location, minecraftProfileTexture.getUrl(), null, new bnj()
    {
      public BufferedImage a(BufferedImage bufferedImage)
      {
        return callback.parseImage(bufferedImage);
      }
      
      public void a()
      {
        callback.callback(capeLocation);
      }
    });
    getTextureManager().a(capeLocation, threadDownloadImage);
  }
  
  public void bindPlayerCape(GameProfile gameProfile, final bmq player)
  {
    bindCape(gameProfile, new IVariables.CapeCallback()
    {
      public void callback(Object capeLocation)
      {
        Variables.this.setCape(player, (kk)capeLocation);
      }
    }, null);
  }
  
  public void bindCape(GameProfile gameProfile)
  {
    bindPlayerCape(gameProfile, getPlayer());
  }
  
  public void setOwnCape(Object resourceLocation)
  {
    setCape(getPlayer(), (kk)resourceLocation);
  }
  
  public void setCape(bmq player, kk capeLocation)
  {
    try
    {
      player.getClass().getField("capeLocation").set(player, capeLocation);
    }
    catch (Exception e)
    {
      MinecraftFactory.getClassProxyCallback().getLogger().error("Could not set capeLocation Field via reflection in AbstractClientPlayer. Did something went wrong with patching that class?", e);
    }
  }
  
  public void renderTextureOverlay(int x1, int x2, int y1, int y2)
  {
    bnu var4 = bnu.a();
    bmz var5 = var4.c();
    bindTexture(bcu.b);
    GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
    var5.a(7, bvp.i);
    var5.b(x1, y2, 0.0D).a(0.0D, y2 / 32.0F).b(64, 64, 64, 255).d();
    var5.b(x1 + x2, y2, 0.0D).a(x2 / 32.0F, y2 / 32.0F).b(64, 64, 64, 255).d();
    var5.b(x1 + x2, y1, 0.0D).a(x2 / 32.0F, y1 / 32.0F).b(64, 64, 64, 255).d();
    var5.b(x1, y1, 0.0D).a(0.0D, y1 / 32.0F).b(64, 64, 64, 255).d();
    var4.b();
  }
  
  public void patchGamma()
  {
    try
    {
      Class enumGameSettings = Thread.currentThread().getContextClassLoader().loadClass(Names.gameOption.getName());
      Field gamma = Transformer.FORGE ? enumGameSettings.getDeclaredField("GAMMA") : enumGameSettings.getDeclaredField("d");
      Object e = gamma.get(null);
      Method a = Transformer.FORGE ? e.getClass().getDeclaredMethod("func_148263_a", new Class[] { Float.TYPE }) : e.getClass().getDeclaredMethod("a", new Class[] { Float.TYPE });
      a.invoke(e, new Object[] { Float.valueOf(10.0F) });
      MinecraftFactory.getClassProxyCallback().getLogger().info("Changed max gamma to 10.0f");
    }
    catch (Exception e)
    {
      MinecraftFactory.getClassProxyCallback().getLogger().warn("Could not set max gamma", e);
    }
  }
  
  public void setIngameFocus()
  {
    getMinecraft().o();
  }
  
  public IVariables.MouseOverObject calculateMouseOverDistance(double maxDistance)
  {
    if ((getSpectatingEntity() == null) || (getWorld() == null)) {
      return null;
    }
    bbi objectMouseOver = getSpectatingEntity().a(maxDistance, 1.0F);
    double var3 = maxDistance;
    bbj entityPosition = getSpectatingEntity().g(1.0F);
    if (objectMouseOver != null) {
      var3 = objectMouseOver.c.f(entityPosition);
    }
    bbj look = getSpectatingEntity().f(1.0F);
    bbj mostFarPoint = entityPosition.b(look.b * maxDistance, look.c * maxDistance, look.d * maxDistance);
    rr pointedEntity = null;
    bbj hitVector = null;
    List<rr> entitiesWithinAABBExcludingEntity = getWorld().a(getSpectatingEntity(), 
      getSpectatingEntity().bl().a(look.b * maxDistance, look.c * maxDistance, look.d * maxDistance).b(1.0D, 1.0D, 1.0D), Predicates.and(rv.e, new Predicate()
      {
        public boolean apply(rr entity)
        {
          return (entity != null) && (entity.ap());
        }
      }));
    double distance = var3;
    for (rr entity : entitiesWithinAABBExcludingEntity)
    {
      bbh axisAlignedBB = entity.bl().g(entity.aA());
      bbi intercept = axisAlignedBB.a(entityPosition, mostFarPoint);
      if (axisAlignedBB.a(entityPosition))
      {
        if (distance >= 0.0D)
        {
          pointedEntity = entity;
          hitVector = intercept == null ? entityPosition : intercept.c;
          distance = 0.0D;
        }
      }
      else if (intercept != null)
      {
        double distanceToHitVec = entityPosition.f(intercept.c);
        if ((distanceToHitVec < distance) || (distance == 0.0D)) {
          if (entity == getSpectatingEntity().bv())
          {
            if (distance == 0.0D)
            {
              pointedEntity = entity;
              hitVector = intercept.c;
            }
          }
          else
          {
            pointedEntity = entity;
            hitVector = intercept.c;
            distance = distanceToHitVec;
          }
        }
      }
    }
    if ((pointedEntity != null) && ((distance < var3) || (objectMouseOver == null))) {
      objectMouseOver = new bbi(pointedEntity, hitVector);
    }
    if (objectMouseOver == null) {
      return null;
    }
    IVariables.ObjectType type;
    IVariables.ObjectType type;
    switch (objectMouseOver.a)
    {
    case a: 
      return null;
    case b: 
      type = IVariables.ObjectType.BLOCK;
      break;
    case c: 
      type = IVariables.ObjectType.ENTITY;
      break;
    default: 
      return null;
    }
    IVariables.ObjectType type;
    return new IVariables.MouseOverObject(type, type == IVariables.ObjectType.ENTITY ? pointedEntity : null, distance);
  }
  
  public Scoreboard getScoreboard()
  {
    if (getWorld() == null) {
      return null;
    }
    bbp scoreboard = getWorld().ad();
    if (scoreboard == null) {
      return null;
    }
    bbl objective = scoreboard.a(1);
    if (objective == null) {
      return null;
    }
    String displayName = objective.d();
    Collection<bbn> scores = scoreboard.i(objective);
    HashMap<Integer, String> lines = Maps.newHashMap();
    for (bbn score : scores) {
      lines.put(Integer.valueOf(score.c()), score.e());
    }
    return new Scoreboard(displayName, lines);
  }
  
  public ResourceManager getResourceManager()
  {
    return this.resourceManager;
  }
  
  public void shutdown()
  {
    getMinecraft().h();
  }
  
  public void renderOverlay() {}
}
