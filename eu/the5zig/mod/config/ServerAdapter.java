package eu.the5zig.mod.config;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.the5zig.mod.server.Server;
import java.lang.reflect.Type;
import java.util.List;

public class ServerAdapter
  implements JsonSerializer<List<Server>>, JsonDeserializer<List<Server>>
{
  private static final String CLASSNAME = "Name";
  private static final String VALUE = "Value";
  
  public JsonElement serialize(List<Server> servers, Type type, JsonSerializationContext context)
  {
    JsonArray array = new JsonArray();
    for (Server server : servers)
    {
      JsonObject retValue = new JsonObject();
      String className = server.getClass().getCanonicalName();
      retValue.addProperty("Name", className);
      JsonElement elem = context.serialize(server, server.getClass());
      retValue.add("Value", elem);
      array.add(retValue);
    }
    return array;
  }
  
  public List<Server> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context)
    throws JsonParseException
  {
    List<Server> servers = Lists.newArrayList();
    JsonArray array = jsonElement.getAsJsonArray();
    for (JsonElement element : array)
    {
      JsonObject jsonObject = element.getAsJsonObject();
      JsonPrimitive prim = (JsonPrimitive)jsonObject.get("Name");
      String className = prim.getAsString();
      try
      {
        clazz = Class.forName(className);
      }
      catch (ClassNotFoundException e)
      {
        Class<?> clazz;
        throw new JsonParseException(e);
      }
      Class<?> clazz;
      servers.add(context.deserialize(jsonObject.get("Value"), clazz));
    }
    return servers;
  }
}
