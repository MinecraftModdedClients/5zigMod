package eu.the5zig.mod.installer;

import eu.the5zig.util.Callback;
import eu.the5zig.util.Utils;
import eu.the5zig.util.io.FileUtils;
import eu.the5zig.util.io.NotifiableFileCopier;
import eu.the5zig.util.io.NotifiableJarCopier;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import javax.swing.JOptionPane;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Installer
{
  public static String MODVERSION = "Unknown";
  public static String MCVERSION = "Unknown";
  public static String MODNAME = "The 5zig Mod";
  private final Callback<Float> progressCallback;
  private final Callback<String> progressNameCallback;
  public File[] otherMods;
  
  public Installer(Callback<Float> progressCallback, Callback<String> progressNameCallback)
  {
    this.progressCallback = progressCallback;
    this.progressNameCallback = progressNameCallback;
  }
  
  public static String getTitle()
  {
    return MODNAME + " v" + MODVERSION;
  }
  
  public static String getModVersionsName()
  {
    return MODNAME + "-" + MODVERSION + "_" + MCVERSION;
  }
  
  public void doInstall()
  {
    try
    {
      copyMinecraftVersion();
      updateVersionJson();
      updateLauncherJson();
    }
    catch (Throwable localThrowable) {}
  }
  
  private void copyMinecraftVersion()
    throws IOException, ParseException, URISyntaxException
  {
    File minecraftDir = InstallerUtils.getMinecraftDirectory();
    File versionsDir = new File(minecraftDir, "versions");
    File minecraftJarDir = new File(versionsDir, MCVERSION);
    if (!minecraftJarDir.exists())
    {
      System.err.println(minecraftJarDir);
      JOptionPane.showMessageDialog(null, "Minecraft version not found: " + MCVERSION + ".\nPlease start Minecraft " + MCVERSION + " manually via the Minecraft Launcher!", getTitle(), 0, Images.iconImage);
      
      System.exit(1);
      return;
    }
    File modJarDir = new File(versionsDir, getModVersionsName());
    if ((modJarDir.exists()) && (!FileUtils.deleteDirectory(modJarDir))) {
      throw new IOException("Could not delete directory at " + modJarDir);
    }
    if (!modJarDir.mkdirs()) {
      throw new IOException("Could not create directory at " + modJarDir);
    }
    File minecraftJarFile = new File(minecraftJarDir, MCVERSION + ".jar");
    File modJarFile = new File(modJarDir, getModVersionsName() + ".jar");
    
    File minecraftJsonFile = new File(minecraftJarDir, MCVERSION + ".json");
    File modJsonFile = new File(modJarDir, getModVersionsName() + ".json");
    NotifiableFileCopier.copy(new File[] { minecraftJsonFile }, modJsonFile);
    
    File fileSrc = new File(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
    
    File libraryDir = new File(minecraftDir, "libraries" + File.separator + "eu" + File.separator + "the5zig" + File.separator + "The5zigMod" + File.separator + MCVERSION + "_" + MODVERSION);
    if ((!libraryDir.exists()) && (!libraryDir.mkdirs())) {
      throw new IOException("Could not create directory at " + libraryDir);
    }
    File libraryFile = new File(libraryDir, "The5zigMod-" + MCVERSION + "_" + MODVERSION + ".jar");
    System.out.println();
    System.out.println("Installing " + MODNAME);
    this.progressNameCallback.call("Copying Mod Files");
    NotifiableFileCopier.copy(new Callback()
    {
      public void call(Float value)
      {
        Installer.this.progressCallback.call(value);
      }
    }, new File[] { fileSrc }, libraryFile);
    
    this.progressNameCallback.call("Copying Minecraft Jar");
    NotifiableFileCopier.copy(new Callback()
    {
      public void call(Float value)
      {
        Installer.this.progressCallback.call(value);
      }
    }, new File[] { minecraftJarFile }, modJarFile);
    if (this.otherMods != null)
    {
      System.out.println();
      System.out.println("Installing " + MODNAME + " with Other Mods (" + Arrays.toString(this.otherMods) + ")");
      File[] dest = new File[this.otherMods.length + 1];
      System.arraycopy(this.otherMods, 0, dest, 0, this.otherMods.length);
      dest[(dest.length - 1)] = minecraftJarFile;
      this.progressNameCallback.call("Copying Other Mods");
      NotifiableJarCopier.copy(dest, modJarFile, new Callback()
      {
        public void call(Float value)
        {
          Installer.this.progressCallback.call(value);
        }
      });
    }
  }
  
  private void updateVersionJson()
    throws IOException, ParseException
  {
    System.out.println();
    System.out.println("Updating JSON File of Mod-Versions-Directory");
    File minecraftDir = InstallerUtils.getMinecraftDirectory();
    File versionsDir = new File(minecraftDir, "versions");
    File modJarDir = new File(versionsDir, getModVersionsName());
    
    File fileJson = new File(modJarDir, getModVersionsName() + ".json");
    String json = Utils.loadJson(fileJson);
    JSONParser jp = new JSONParser();
    
    JSONObject root = (JSONObject)jp.parse(json);
    root.put("id", getModVersionsName());
    root.put("inheritsFrom", MCVERSION);
    root.put("mainClass", "net.minecraft.launchwrapper.Launch");
    root.put("minecraftArguments", root.get("minecraftArguments") + " --tweakClass eu.the5zig.mod.asm.ClassTweaker");
    
    JSONArray libraries = new JSONArray();
    
    JSONObject launchWrapper = new JSONObject();
    launchWrapper.put("name", "net.minecraft:launchwrapper:1.7");
    libraries.add(0, launchWrapper);
    JSONObject mod = new JSONObject();
    mod.put("name", "eu.the5zig:The5zigMod:" + MCVERSION + "_" + MODVERSION);
    libraries.add(0, mod);
    
    root.put("libraries", libraries);
    
    FileWriter fwJson = new FileWriter(fileJson);
    root.writeJSONString(fwJson);
    
    fwJson.flush();
    fwJson.close();
  }
  
  private void updateLauncherJson()
    throws IOException, ParseException
  {
    System.out.println();
    System.out.println("Updating JSON File of Minecraft Launcher Profiles");
    File minecraftDir = InstallerUtils.getMinecraftDirectory();
    File fileJson = new File(minecraftDir, "launcher_profiles.json");
    String json = Utils.loadJson(fileJson);
    JSONParser jp = new JSONParser();
    
    JSONObject root = (JSONObject)jp.parse(json);
    JSONObject profiles = (JSONObject)root.get("profiles");
    JSONObject prof = (JSONObject)profiles.get(getModVersionsName());
    if (prof == null)
    {
      prof = new JSONObject();
      prof.put("name", getModVersionsName());
      profiles.put(getModVersionsName(), prof);
    }
    prof.put("lastVersionId", getModVersionsName());
    root.put("selectedProfile", getModVersionsName());
    FileWriter fwJson = new FileWriter(fileJson);
    root.writeJSONString(fwJson);
    fwJson.flush();
    fwJson.close();
  }
}
