package eu.the5zig.mod.installer;

import java.io.File;

public class UpdateInstaller
  extends InstallerNew
{
  public UpdateInstaller(String modVersion, String minecraftVersion, String currentModVersion, File sourceFile)
    throws MinecraftNotFoundException
  {
    super(new File("").getAbsoluteFile(), modVersion, minecraftVersion);
    if (currentModVersion != null)
    {
      File oldModJarDirectory = new File(this.versionsDirectory, getVersionName(currentModVersion));
      File oldModJarFile = new File(oldModJarDirectory, getVersionName(currentModVersion) + ".jar");
      File oldModJsonFile = new File(oldModJarDirectory, getVersionName(currentModVersion) + ".json");
      if ((oldModJarFile.exists()) && (oldModJsonFile.exists()))
      {
        this.minecraftJarDirectory = oldModJarDirectory;
        this.minecraftJarFile = oldModJarFile;
      }
      File oldOtherModsLibraryDirectory = new File(this.librariesDirectory, "eu" + File.separator + "the5zig" + File.separator + "Mods" + File.separator + minecraftVersion + "_" + currentModVersion);
      
      File oldOtherModsLibraryFile = new File(oldOtherModsLibraryDirectory, "Mods-" + minecraftVersion + "_" + currentModVersion + ".jar");
      if (oldOtherModsLibraryFile.exists()) {
        this.otherMods = new File[] { oldOtherModsLibraryFile };
      }
    }
    this.sourceFile = sourceFile;
  }
  
  protected void applyOptifinePatch() {}
}
