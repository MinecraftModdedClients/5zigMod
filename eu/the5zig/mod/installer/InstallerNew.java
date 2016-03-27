package eu.the5zig.mod.installer;

import eu.the5zig.mod.util.InputStreamReaderThread;
import eu.the5zig.util.Callback;
import eu.the5zig.util.Utils;
import eu.the5zig.util.io.NotifiableFileCopier;
import eu.the5zig.util.io.NotifiableJarCopier;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class InstallerNew
{
  protected final String modVersion;
  protected final String minecraftVersion;
  protected File minecraftDirectory;
  protected File versionsDirectory;
  protected File librariesDirectory;
  protected File minecraftJarDirectory;
  protected File minecraftJarFile;
  protected File minecraftJsonFile;
  protected File modJarDirectory;
  protected File modJarFile;
  protected File modJsonFile;
  protected File modLibraryDirectory;
  protected File modLibraryFile;
  protected File otherModsLibraryDirectory;
  protected File otherModsLibraryFile;
  protected File sourceFile;
  protected File[] otherMods;
  
  public InstallerNew(File installDirectory, String modVersion, String minecraftVersion)
    throws MinecraftNotFoundException
  {
    this.modVersion = modVersion;
    this.minecraftVersion = minecraftVersion;
    
    this.minecraftDirectory = installDirectory;
    this.versionsDirectory = new File(this.minecraftDirectory, "versions");
    this.librariesDirectory = new File(this.minecraftDirectory, "libraries");
    if ((!this.versionsDirectory.exists()) || (!this.librariesDirectory.exists())) {
      throw new MinecraftNotFoundException();
    }
    this.minecraftJarDirectory = new File(this.versionsDirectory, minecraftVersion);
    this.minecraftJarFile = new File(this.minecraftJarDirectory, minecraftVersion + ".jar");
    this.minecraftJsonFile = new File(this.minecraftJarDirectory, minecraftVersion + ".json");
    if ((!this.minecraftJarFile.exists()) || (!this.minecraftJsonFile.exists())) {
      throw new MinecraftNotFoundException();
    }
    this.modJarDirectory = new File(this.versionsDirectory, getVersionName());
    this.modJarFile = new File(this.modJarDirectory, getVersionName() + ".jar");
    this.modJsonFile = new File(this.modJarDirectory, getVersionName() + ".json");
    this.modLibraryDirectory = new File(this.librariesDirectory, "eu" + File.separator + "the5zig" + File.separator + "The5zigMod" + File.separator + minecraftVersion + "_" + modVersion);
    this.modLibraryFile = new File(this.modLibraryDirectory, "The5zigMod-" + minecraftVersion + "_" + modVersion + ".jar");
    this.otherModsLibraryDirectory = new File(this.librariesDirectory, "eu" + File.separator + "the5zig" + File.separator + "Mods" + File.separator + minecraftVersion + "_" + modVersion);
    this.otherModsLibraryFile = new File(this.otherModsLibraryDirectory, "Mods-" + minecraftVersion + "_" + modVersion + ".jar");
    
    this.sourceFile = Utils.getRunningJar();
  }
  
  public void setOtherMods(File[] otherMods)
  {
    this.otherMods = otherMods;
  }
  
  public void install()
    throws Exception
  {
    install(null);
  }
  
  public void install(ProcessCallback callback)
    throws Exception
  {
    if ((this.otherMods == null) || (this.otherMods.length == 0))
    {
      Stage.COPY_MINECRAFT.setStartPercentage(0.6F);
      Stage.APPLY_OPTIFINE_PATCHES.setStartPercentage(0.95F);
      Stage.COPY_OTHER_MODS.setStartPercentage(0.95F);
    }
    extractSourcesToLib(callback);
    copyMinecraftVersion(callback);
    copyOtherModsIntoMinecraftJar(callback);
    updateLauncherJson(callback);
    if (callback != null) {
      callback.message("Done.");
    }
    if (callback != null) {
      callback.progress(1.0F);
    }
  }
  
  protected void extractSourcesToLib(final ProcessCallback callback)
    throws IOException
  {
    if ((!this.modLibraryDirectory.exists()) && (!this.modLibraryDirectory.mkdirs())) {
      throw new IOException("Could not create a new Mod Library File!");
    }
    if (this.sourceFile == null) {
      throw new RuntimeException("Could not find locating of running Jar!");
    }
    if (this.sourceFile.equals(this.modLibraryFile)) {
      return;
    }
    if (callback != null) {
      callback.setStage(Stage.EXTRACT_SOURCES);
    }
    NotifiableFileCopier.copy(new Callback()
    {
      public void call(Float f)
      {
        if (callback != null) {
          callback.setProgress(f.floatValue());
        }
      }
    }, new File[] { this.sourceFile }, this.modLibraryFile);
  }
  
  protected void copyMinecraftVersion(final ProcessCallback callback)
    throws IOException, ParseException
  {
    if ((!this.modJarDirectory.exists()) && (!this.modJarDirectory.mkdirs())) {
      throw new IOException("Could not create a new Minecraft Version Directory!");
    }
    if (callback != null) {
      callback.setStage(Stage.COPY_MINECRAFT);
    }
    if (!this.minecraftJarFile.equals(this.modJarFile))
    {
      NotifiableFileCopier.copy(new Callback()
      {
        public void call(Float f)
        {
          if (callback != null) {
            callback.setProgress(f.floatValue());
          }
        }
      }, new File[] { this.minecraftJarFile }, this.modJarFile);
      
      NotifiableFileCopier.copy(new Callback()
      {
        public void call(Float f)
        {
          if (callback != null) {
            callback.setProgress(f.floatValue());
          }
        }
      }, new File[] { this.minecraftJsonFile }, this.modJsonFile);
    }
    updateMinecraftJson();
  }
  
  protected void copyOtherModsIntoMinecraftJar(final ProcessCallback callback)
    throws IOException
  {
    if ((this.otherMods == null) || (this.otherMods.length == 0)) {
      return;
    }
    if (((!this.otherModsLibraryDirectory.exists()) && (!this.otherModsLibraryDirectory.mkdirs())) || ((!this.otherModsLibraryFile.exists()) && (!this.otherModsLibraryFile.createNewFile()))) {
      throw new IOException("Could not create mod directory or file!");
    }
    if (callback != null) {
      callback.setStage(Stage.APPLY_OPTIFINE_PATCHES);
    }
    applyOptifinePatch();
    if (callback != null) {
      callback.setStage(Stage.COPY_OTHER_MODS);
    }
    NotifiableJarCopier.copy(this.otherMods, this.otherModsLibraryFile, new Callback()
    {
      public void call(Float f)
      {
        if (callback != null) {
          callback.setProgress(f.floatValue());
        }
      }
    });
  }
  
  protected void applyOptifinePatch()
  {
    int i = 0;
    for (int otherModsLength = this.otherMods.length; i < otherModsLength; i++)
    {
      File otherMod = this.otherMods[i];
      try
      {
        JarFile jarFile = new JarFile(otherMod);
        ZipEntry entry = jarFile.getEntry("optifine/Patcher.class");
        jarFile.close();
        if (entry != null)
        {
          System.out.println("Found Optifine!");
          File temp = File.createTempFile(otherMod.getName(), null);
          temp.deleteOnExit();
          
          String javaHome = "\"" + System.getProperty("java.home") + "\\bin\\java\"";
          
          String command = javaHome + " -cp \"" + otherMod.getAbsolutePath() + "\" optifine.Patcher \"" + this.minecraftJarFile.getAbsolutePath() + "\" \"" + otherMod.getAbsolutePath() + "\" \"" + temp.getAbsolutePath() + "\"";
          System.out.println("Starting Optifine patch Process: " + command);
          ProcessBuilder builder = new ProcessBuilder(new String[] { command });
          Process process = builder.start();
          Thread input = new InputStreamReaderThread("Input", process.getInputStream());
          input.start();
          Thread error = new InputStreamReaderThread("Error", process.getErrorStream());
          error.start();
          int exitCode = process.waitFor();
          System.out.println("Optifine process exited with code " + exitCode);
          if (exitCode != 0) {
            throw new RuntimeException("Could not patch Optifine file! Exit code=" + exitCode);
          }
          this.otherMods[i] = temp;
          input.join();
          error.join();
        }
      }
      catch (Exception e)
      {
        throw new RuntimeException("Could not patch Optifine file!", e);
      }
    }
  }
  
  protected void updateMinecraftJson()
    throws ParseException, IOException
  {
    String json = Utils.loadJson(this.modJsonFile);
    
    JSONParser jp = new JSONParser();
    
    JSONObject root = (JSONObject)jp.parse(json);
    root.put("id", getVersionName());
    root.put("mainClass", "net.minecraft.launchwrapper.Launch");
    root.put("minecraftArguments", root.get("minecraftArguments") + " --tweakClass eu.the5zig.mod.asm.ClassTweaker");
    
    JSONArray libraries = (JSONArray)root.get("libraries");
    for (Iterator iterator = libraries.iterator(); iterator.hasNext();)
    {
      Object o = iterator.next();
      JSONObject library = (JSONObject)o;
      String name = library.get("name").toString();
      if ((name.startsWith("net.minecraft:launchwrapper:")) || (name.startsWith("eu.the5zig:The5zigMod:")) || (name.startsWith("eu.the5zig:mods:"))) {
        iterator.remove();
      }
    }
    JSONObject launchWrapper = new JSONObject();
    launchWrapper.put("name", "net.minecraft:launchwrapper:1.7");
    libraries.add(0, launchWrapper);
    JSONObject mod = new JSONObject();
    mod.put("name", "eu.the5zig:The5zigMod:" + this.minecraftVersion + "_" + this.modVersion);
    libraries.add(0, mod);
    if ((this.otherMods != null) && (this.otherMods.length > 0))
    {
      JSONObject mods2 = new JSONObject();
      mods2.put("name", "eu.the5zig:Mods:" + this.minecraftVersion + "_" + this.modVersion);
      libraries.add(1, mods2);
    }
    root.put("libraries", libraries);
    
    writeToFile(root, this.modJsonFile);
  }
  
  protected void updateLauncherJson(ProcessCallback callback)
    throws ParseException, IOException
  {
    if (callback != null) {
      callback.setStage(Stage.UPDATE_LAUNCHER_FILES);
    }
    File fileJson = new File(this.minecraftDirectory, "launcher_profiles.json");
    String json = Utils.loadJson(fileJson);
    JSONParser jp = new JSONParser();
    
    JSONObject root = (JSONObject)jp.parse(json);
    JSONObject profiles = (JSONObject)root.get("profiles");
    JSONObject prof = (JSONObject)profiles.get(getVersionName());
    if (prof == null)
    {
      prof = new JSONObject();
      prof.put("name", getVersionName());
      profiles.put(getVersionName(), prof);
    }
    prof.put("lastVersionId", getVersionName());
    root.put("selectedProfile", getVersionName());
    
    writeToFile(root, fileJson);
  }
  
  protected void writeToFile(JSONObject object, File file)
    throws IOException
  {
    FileWriter fwJson = null;
    try
    {
      fwJson = new FileWriter(file);
      object.writeJSONString(fwJson);
      fwJson.flush();
    }
    finally
    {
      if (fwJson != null) {
        fwJson.close();
      }
    }
  }
  
  public String getVersionName()
  {
    return getVersionName(this.minecraftVersion);
  }
  
  public static String getVersionName(String minecraftVersion)
  {
    return minecraftVersion + " - 5zig Mod";
  }
}
