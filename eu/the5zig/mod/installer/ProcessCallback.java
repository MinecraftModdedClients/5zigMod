package eu.the5zig.mod.installer;

public abstract class ProcessCallback
{
  private int currentStage = 0;
  
  public void setStage(Stage stage)
  {
    message(Stage.values()[(this.currentStage = stage.ordinal())].getMessage() + " (" + (this.currentStage + 1) + "/" + Stage.values().length + ")");
  }
  
  public void setProgress(float progress)
  {
    float startPercentage = Stage.values()[this.currentStage].getStartPercentage();
    float endPercentage = this.currentStage == Stage.values().length - 1 ? 1.0F : Stage.values()[(this.currentStage + 1)].getStartPercentage();
    progress(startPercentage + (endPercentage - startPercentage) * progress);
  }
  
  protected abstract void progress(float paramFloat);
  
  protected abstract void message(String paramString);
}
