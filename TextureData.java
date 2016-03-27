import java.util.List;

public class TextureData
{
  public String cape;
  public List<TextureData.Model> models;
  
  public class Model
  {
    public String itemName;
    public String render;
    public Integer modelId;
    public String model;
    public String texture;
    
    public Model() {}
  }
}
