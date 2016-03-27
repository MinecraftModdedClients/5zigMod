package eu.the5zig.mod.util;

import eu.the5zig.mod.chat.network.NetworkManager;
import eu.the5zig.util.io.http.HttpClient;
import eu.the5zig.util.io.http.HttpResponseCallback;

public abstract class APIManager
{
  private final String BASE_URL;
  
  public APIManager(String baseURL)
  {
    this.BASE_URL = baseURL;
  }
  
  protected void get(String endpoint, HttpResponseCallback callback)
    throws Exception
  {
    HttpClient.get(this.BASE_URL + endpoint, NetworkManager.CLIENT_NIO_EVENTLOOP, callback);
  }
}
