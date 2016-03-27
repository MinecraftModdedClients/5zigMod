package eu.the5zig.mod.server.hypixel.api;

import com.google.common.collect.Lists;
import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.manager.DataManager;
import eu.the5zig.mod.util.APIManager;
import eu.the5zig.util.io.FileUtils;
import eu.the5zig.util.io.http.HttpResponseCallback;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

public class HypixelAPIManager
  extends APIManager
{
  private final int MAX_REQUESTS_PER_MINUTE = 60;
  private UUID key;
  private final List<Long> requests = Lists.newArrayList();
  
  public HypixelAPIManager()
  {
    super("https://api.hypixel.net/");
    try
    {
      this.key = loadKey();
      The5zigMod.logger.debug("Loaded Hypixel API key: " + this.key);
    }
    catch (HypixelAPIMissingKeyException e)
    {
      The5zigMod.logger.debug("Could not load Hypixel API-key!");
    }
  }
  
  private UUID loadKey()
    throws HypixelAPIMissingKeyException
  {
    File keyFile = new File("the5zigmod/servers/hypixel/api_" + The5zigMod.getDataManager().getUniqueId() + ".key");
    if (!keyFile.exists()) {
      throw new HypixelAPIMissingKeyException();
    }
    try
    {
      return UUID.fromString(IOUtils.toString(keyFile.toURI()));
    }
    catch (IOException e)
    {
      throw new HypixelAPIMissingKeyException(e);
    }
  }
  
  public void setKey(UUID key)
  {
    this.key = key;
    try
    {
      File keyFile = new File("the5zigmod/servers/hypixel/api_" + The5zigMod.getDataManager().getUniqueId() + ".key");
      if ((keyFile.exists()) && (!keyFile.delete())) {
        throw new IOException("Could not delete existing Key File!");
      }
      if (!keyFile.createNewFile()) {
        throw new IOException("Could not create new Key File!");
      }
      FileUtils.writeFile(keyFile, key.toString());
    }
    catch (IOException e)
    {
      The5zigMod.logger.info("Could not save Key!", e);
    }
  }
  
  public void get(String endpoint, final HypixelAPICallback callback)
    throws HypixelAPIException
  {
    checkRequests();
    UUID key = getKey();
    try
    {
      get(endpoint + "&key=" + key, new HttpResponseCallback()
      {
        public void call(String response, int responseCode, Throwable throwable)
        {
          HypixelAPIResponse apiResponse = new HypixelAPIResponse(response);
          if ((throwable != null) || (responseCode != 200) || (!apiResponse.isSuccess()))
          {
            if (response != null) {
              callback.call(new HypixelAPIResponseException(responseCode, apiResponse.getCause()));
            } else {
              callback.call(new HypixelAPIResponseException(responseCode, null));
            }
          }
          else {
            try
            {
              callback.call(apiResponse);
            }
            catch (Exception e)
            {
              callback.call(new HypixelAPIResponseException(e));
            }
          }
        }
      });
    }
    catch (Exception e)
    {
      throw new HypixelAPIException(e);
    }
  }
  
  private void checkRequests()
    throws HypixelAPITooManyRequestsException
  {
    synchronized (this.requests)
    {
      for (Iterator<Long> iterator = this.requests.iterator(); iterator.hasNext();) {
        if (System.currentTimeMillis() - ((Long)iterator.next()).longValue() > 60000L) {
          iterator.remove();
        }
      }
      if (this.requests.size() >= 60) {
        throw new HypixelAPITooManyRequestsException();
      }
      this.requests.add(Long.valueOf(System.currentTimeMillis()));
    }
  }
  
  public UUID getKey()
    throws HypixelAPIMissingKeyException
  {
    if (!hasKey()) {
      throw new HypixelAPIMissingKeyException();
    }
    return this.key;
  }
  
  public boolean hasKey()
  {
    return this.key != null;
  }
}
