package eu.the5zig.mod.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.util.ExceptionCallback;
import eu.the5zig.util.io.http.HttpResponseCallback;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MojangAPIManager
  extends APIManager
{
  private final Map<String, String> uuidToName = Collections.synchronizedMap(new HashMap());
  
  public MojangAPIManager()
  {
    super("https://api.mojang.com/");
  }
  
  public void resolveUUID(final String uuid, final ExceptionCallback<String> callback)
  {
    if (this.uuidToName.containsKey(uuid)) {
      callback.call(this.uuidToName.get(uuid), null);
    }
    try
    {
      get("user/profiles/" + uuid + "/names", new HttpResponseCallback()
      {
        public void call(String response, int responseCode, Throwable throwable)
        {
          if (throwable != null)
          {
            callback.call(null, throwable);
            return;
          }
          JsonParser parser = new JsonParser();
          JsonElement parse = parser.parse(response);
          if (!parse.isJsonArray())
          {
            callback.call(null, new RuntimeException("Illegal Response Received!"));
            return;
          }
          MojangAPIManager.User user = (MojangAPIManager.User)The5zigMod.gson.fromJson(parse.getAsJsonArray().get(0), MojangAPIManager.User.class);
          MojangAPIManager.this.uuidToName.put(uuid, user.name);
          callback.call(user.name, null);
        }
      });
    }
    catch (Exception e)
    {
      callback.call(null, e);
    }
  }
  
  private class User
  {
    private String id;
    private String name;
    
    private User() {}
  }
}
