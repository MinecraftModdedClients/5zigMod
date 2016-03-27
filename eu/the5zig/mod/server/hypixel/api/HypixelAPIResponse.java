package eu.the5zig.mod.server.hypixel.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HypixelAPIResponse
{
  private JsonObject response;
  private String raw;
  private boolean success;
  private String cause;
  
  public HypixelAPIResponse(String response)
  {
    if (response == null)
    {
      this.success = false;
      return;
    }
    this.raw = response;
    JsonParser parser = new JsonParser();
    JsonElement element = parser.parse(response);
    if (!element.isJsonObject()) {
      throw new IllegalArgumentException();
    }
    this.response = element.getAsJsonObject();
    
    this.success = this.response.get("success").getAsBoolean();
    if (!this.success) {
      this.cause = this.response.get("cause").getAsString();
    }
  }
  
  public boolean isSuccess()
  {
    return this.success;
  }
  
  public String getCause()
  {
    return this.cause;
  }
  
  public JsonObject data()
  {
    return this.response;
  }
  
  public String getRaw()
  {
    return this.raw;
  }
  
  public JsonElement getElement(String path)
  {
    return getElement(path, this.response);
  }
  
  public static JsonElement getElement(String path, JsonElement element)
  {
    String[] tree = path.split("\\.");
    JsonElement current = element;
    for (String part : tree)
    {
      if (current.isJsonNull()) {
        return null;
      }
      if ((current.isJsonArray()) || (current.isJsonPrimitive())) {
        return current;
      }
      current = current.getAsJsonObject().get(part);
    }
    return current;
  }
}
