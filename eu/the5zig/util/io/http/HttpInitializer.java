package eu.the5zig.util.io.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLEngine;

public class HttpInitializer
  extends ChannelInitializer<Channel>
{
  private final HttpResponseCallback callback;
  private final boolean ssl;
  private final String host;
  private final int port;
  
  public HttpInitializer(HttpResponseCallback callback, boolean ssl, String host, int port)
  {
    this.callback = callback;
    this.ssl = ssl;
    this.host = host;
    this.port = port;
  }
  
  protected void initChannel(Channel ch)
    throws Exception
  {
    ch.pipeline().addLast("timeout", new ReadTimeoutHandler(5000L, TimeUnit.MILLISECONDS));
    if (this.ssl)
    {
      SSLEngine engine = SslContext.newClientContext().newEngine(ch.alloc(), this.host, this.port);
      
      ch.pipeline().addLast("ssl", new SslHandler(engine));
    }
    ch.pipeline().addLast("http", new HttpClientCodec());
    ch.pipeline().addLast("handler", new HttpHandler(this.callback));
  }
}
