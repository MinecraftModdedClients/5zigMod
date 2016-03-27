package eu.the5zig.mod.installer;

public enum Stage
{
  EXTRACT_SOURCES(0.0F, "Extracting Sources to Library File."),  COPY_MINECRAFT(0.25F, "Copying Minecraft Version."),  APPLY_OPTIFINE_PATCHES(0.5F, "Trying to apply Optifine patches."),  COPY_OTHER_MODS(0.55F, "Installing other Mods."),  UPDATE_LAUNCHER_FILES(0.95F, "Updating launcher files.");
  
  private float startPercentage;
  private String message;
  
  private Stage(float startPercentage, String message)
  {
    this.startPercentage = startPercentage;
    this.message = message;
  }
  
  public float getStartPercentage()
  {
    return this.startPercentage;
  }
  
  public void setStartPercentage(float startPercentage)
  {
    this.startPercentage = startPercentage;
  }
  
  public String getMessage()
  {
    return this.message;
  }
}
