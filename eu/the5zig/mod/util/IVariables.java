package eu.the5zig.mod.util;

import com.mojang.authlib.GameProfile;
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
import eu.the5zig.mod.gui.ingame.resource.IResourceManager;
import eu.the5zig.util.Callback;
import io.netty.buffer.ByteBuf;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.Proxy;
import java.util.List;

public abstract interface IVariables
{
  public abstract void drawString(String paramString, int paramInt1, int paramInt2, Object... paramVarArgs);
  
  public abstract void drawString(String paramString, int paramInt1, int paramInt2);
  
  public abstract void drawCenteredString(String paramString, int paramInt1, int paramInt2);
  
  public abstract void drawCenteredString(String paramString, int paramInt1, int paramInt2, int paramInt3);
  
  public abstract void drawString(String paramString, int paramInt1, int paramInt2, int paramInt3, Object... paramVarArgs);
  
  public abstract void drawString(String paramString, int paramInt1, int paramInt2, int paramInt3);
  
  public abstract void drawString(String paramString, int paramInt1, int paramInt2, int paramInt3, boolean paramBoolean);
  
  public abstract List<String> splitStringToWidth(String paramString, int paramInt);
  
  public abstract int getStringWidth(String paramString);
  
  public abstract String shortenToWidth(String paramString, int paramInt);
  
  public abstract IButton createButton(int paramInt1, int paramInt2, int paramInt3, String paramString);
  
  public abstract IButton createButton(int paramInt1, int paramInt2, int paramInt3, String paramString, boolean paramBoolean);
  
  public abstract IButton createButton(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, String paramString);
  
  public abstract IButton createButton(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, String paramString, boolean paramBoolean);
  
  public abstract IButton createStringButton(int paramInt1, int paramInt2, int paramInt3, String paramString);
  
  public abstract IButton createStringButton(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, String paramString);
  
  public abstract IButton createAudioButton(int paramInt1, int paramInt2, int paramInt3, AudioCallback paramAudioCallback);
  
  public abstract IButton createIconButton(IResourceLocation paramIResourceLocation, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5);
  
  public abstract ITextfield createTextfield(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5);
  
  public abstract ITextfield createTextfield(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6);
  
  public abstract IPlaceholderTextfield createTextfield(String paramString, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5);
  
  public abstract IPlaceholderTextfield createTextfield(String paramString, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6);
  
  public abstract <E extends Row> IGuiList<E> createGuiList(Clickable<E> paramClickable, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, List<E> paramList);
  
  @Deprecated
  public abstract <E extends Row> IGuiList<E> createGuiList(Clickable<E> paramClickable, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, List<E> paramList, int paramInt7);
  
  public abstract <E extends Row> IGuiList<E> createGuiListChat(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, int paramInt7, List<E> paramList, GuiListChatCallback paramGuiListChatCallback);
  
  public abstract IFileSelector createFileSelector(File paramFile, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, Callback<File> paramCallback);
  
  public abstract IButton createSlider(int paramInt1, int paramInt2, int paramInt3, SliderCallback paramSliderCallback);
  
  public abstract IColorSelector createColorSelector(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, String paramString, ColorSelectorCallback paramColorSelectorCallback);
  
  public abstract IOverlay newOverlay();
  
  public abstract void updateOverlayCount(int paramInt);
  
  public abstract void renderOverlay();
  
  public abstract IWrappedGui createWrappedGui(Object paramObject);
  
  public abstract IKeybinding createKeybinding(String paramString1, int paramInt, String paramString2);
  
  public abstract IGui2ndChat get2ndChat();
  
  public abstract boolean isChatOpened();
  
  public abstract void typeInChatGUI(String paramString);
  
  public abstract void registerKeybindings(List<IKeybinding> paramList);
  
  public abstract void playSound(String paramString, float paramFloat);
  
  public abstract void playSound(String paramString1, String paramString2, float paramFloat);
  
  public abstract int getFontHeight();
  
  public abstract Object getMinecraftServerData();
  
  public abstract void resetServer();
  
  public abstract String getServer();
  
  public abstract int getServerPlayers();
  
  public abstract boolean isTablistShown();
  
  public abstract void setFOV(float paramFloat);
  
  public abstract float getFOV();
  
  public abstract void setSmoothCamera(boolean paramBoolean);
  
  public abstract boolean isSmoothCamera();
  
  public abstract String translate(String paramString, Object... paramVarArgs);
  
  public abstract void displayScreen(Gui paramGui);
  
  public abstract void displayScreen(Object paramObject);
  
  public abstract void joinServer(String paramString, int paramInt);
  
  public abstract void joinServer(Object paramObject1, Object paramObject2);
  
  public abstract long getSystemTime();
  
  public abstract boolean isSpectatingSelf();
  
  public abstract String getOpenContainerTitle();
  
  public abstract void closeContainer();
  
  public abstract String getSession();
  
  public abstract String getUsername();
  
  public abstract String getUUID();
  
  public abstract Proxy getProxy();
  
  public abstract GameProfile getGameProfile();
  
  public abstract String getFPS();
  
  public abstract boolean isPlayerNull();
  
  public abstract boolean isTerrainLoading();
  
  public abstract double getPlayerPosX();
  
  public abstract double getPlayerPosY();
  
  public abstract double getPlayerPosZ();
  
  public abstract float getPlayerRotationYaw();
  
  public abstract boolean isFancyGraphicsEnabled();
  
  public abstract String getBiome();
  
  public abstract int getLightLevel();
  
  public abstract String getEntityCount();
  
  public abstract float getSaturation();
  
  public abstract float getHealth(Object paramObject);
  
  public abstract int getAir();
  
  public abstract PotionEffect getPotionForVignette();
  
  public abstract List<PotionEffect> getActivePotionEffects();
  
  public abstract int getPotionEffectIndicatorHeight();
  
  public abstract ItemStack getItemInMainHand();
  
  public abstract ItemStack getItemInOffHand();
  
  public abstract ItemStack getItemInArmorSlot(int paramInt);
  
  public abstract ItemStack getItemByName(String paramString);
  
  public abstract ItemStack getItemByName(String paramString, int paramInt);
  
  public abstract int getItemCount(String paramString);
  
  public abstract void updateScaledResolution();
  
  public abstract int getWidth();
  
  public abstract int getHeight();
  
  public abstract int getScaledWidth();
  
  public abstract int getScaledHeight();
  
  public abstract int getScaleFactor();
  
  public abstract boolean showDebugScreen();
  
  public abstract boolean enableEverythingIsScrewedUpMode();
  
  public abstract boolean shouldDrawHUD();
  
  public abstract int[] getHotbarKeys();
  
  public abstract void drawIngameTexturedModalRect(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6);
  
  public abstract void messagePlayer(String paramString);
  
  public abstract void sendMessage(String paramString);
  
  public abstract boolean hasNetworkManager();
  
  public abstract void sendCustomPayload(String paramString, ByteBuf paramByteBuf);
  
  public abstract Gui getCurrentScreen();
  
  public abstract Object getMinecraftScreen();
  
  public abstract Object bindDynamicImage(String paramString, BufferedImage paramBufferedImage);
  
  public abstract IResourceLocation createResourceLocation(String paramString);
  
  public abstract IResourceLocation createResourceLocation(String paramString1, String paramString2);
  
  public abstract void bindTexture(Object paramObject);
  
  public abstract void deleteTexture(Object paramObject);
  
  public abstract Object loadTexture(Object paramObject, int paramInt1, int paramInt2);
  
  public abstract Object getTexture(Object paramObject);
  
  public abstract void renderDynamicImage(Object paramObject, BufferedImage paramBufferedImage);
  
  public abstract void renderPotionIcon(int paramInt);
  
  public abstract void bindCape(GameProfile paramGameProfile, CapeCallback paramCapeCallback, File paramFile);
  
  public abstract void bindCape(GameProfile paramGameProfile);
  
  public abstract void setOwnCape(Object paramObject);
  
  public abstract void renderTextureOverlay(int paramInt1, int paramInt2, int paramInt3, int paramInt4);
  
  public abstract void patchGamma();
  
  public abstract void setIngameFocus();
  
  public abstract MouseOverObject calculateMouseOverDistance(double paramDouble);
  
  public abstract Scoreboard getScoreboard();
  
  public abstract IResourceManager getResourceManager();
  
  public abstract void shutdown();
  
  public static abstract class CapeCallback
  {
    public BufferedImage parseImage(BufferedImage image)
    {
      return image;
    }
    
    public abstract void callback(Object paramObject);
  }
  
  public static class MouseOverObject
  {
    private IVariables.ObjectType type;
    private Object object;
    private double distance;
    
    public MouseOverObject(IVariables.ObjectType type, Object object, double distance)
    {
      this.type = type;
      this.object = object;
      this.distance = distance;
    }
    
    public IVariables.ObjectType getType()
    {
      return this.type;
    }
    
    public void setType(IVariables.ObjectType type)
    {
      this.type = type;
    }
    
    public Object getObject()
    {
      return this.object;
    }
    
    public void setObject(Object object)
    {
      this.object = object;
    }
    
    public double getDistance()
    {
      return this.distance;
    }
    
    public void setDistance(double distance)
    {
      this.distance = distance;
    }
  }
  
  public static enum ObjectType
  {
    MISS,  BLOCK,  ENTITY;
    
    private ObjectType() {}
  }
}
