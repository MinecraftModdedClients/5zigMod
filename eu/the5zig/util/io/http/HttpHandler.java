package eu.the5zig.util.io.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import java.nio.charset.Charset;

public class HttpHandler
  extends SimpleChannelInboundHandler<HttpObject>
{
  private final HttpResponseCallback callback;
  private final StringBuilder buffer = new StringBuilder();
  private int responseCode = 200;
  
  public HttpHandler(HttpResponseCallback callback)
  {
    this.callback = callback;
  }
  
  /* Error */
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 6	eu/the5zig/util/io/http/HttpHandler:callback	Leu/the5zig/util/io/http/HttpResponseCallback;
    //   4: aconst_null
    //   5: aload_0
    //   6: getfield 5	eu/the5zig/util/io/http/HttpHandler:responseCode	I
    //   9: aload_2
    //   10: invokeinterface 7 4 0
    //   15: aload_1
    //   16: invokeinterface 8 1 0
    //   21: invokeinterface 9 1 0
    //   26: pop
    //   27: goto +18 -> 45
    //   30: astore_3
    //   31: aload_1
    //   32: invokeinterface 8 1 0
    //   37: invokeinterface 9 1 0
    //   42: pop
    //   43: aload_3
    //   44: athrow
    //   45: return
    // Line number table:
    //   Java source line #26	-> byte code offset #0
    //   Java source line #28	-> byte code offset #15
    //   Java source line #29	-> byte code offset #27
    //   Java source line #28	-> byte code offset #30
    //   Java source line #30	-> byte code offset #45
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	46	0	this	HttpHandler
    //   0	46	1	ctx	ChannelHandlerContext
    //   0	46	2	cause	Throwable
    //   30	14	3	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   0	15	30	finally
  }
  
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
    throws Exception
  {
    if ((msg instanceof HttpResponse))
    {
      HttpResponse response = (HttpResponse)msg;
      this.responseCode = response.getStatus().code();
      if (this.responseCode == HttpResponseStatus.NO_CONTENT.code())
      {
        done(ctx);
        return;
      }
    }
    if ((msg instanceof HttpContent))
    {
      HttpContent content = (HttpContent)msg;
      this.buffer.append(content.content().toString(Charset.forName("UTF-8")));
      if ((msg instanceof LastHttpContent)) {
        done(ctx);
      }
    }
  }
  
  /* Error */
  private void done(ChannelHandlerContext ctx)
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 6	eu/the5zig/util/io/http/HttpHandler:callback	Leu/the5zig/util/io/http/HttpResponseCallback;
    //   4: aload_0
    //   5: getfield 4	eu/the5zig/util/io/http/HttpHandler:buffer	Ljava/lang/StringBuilder;
    //   8: invokevirtual 22	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   11: aload_0
    //   12: getfield 5	eu/the5zig/util/io/http/HttpHandler:responseCode	I
    //   15: aconst_null
    //   16: invokeinterface 7 4 0
    //   21: aload_1
    //   22: invokeinterface 8 1 0
    //   27: invokeinterface 9 1 0
    //   32: pop
    //   33: goto +18 -> 51
    //   36: astore_2
    //   37: aload_1
    //   38: invokeinterface 8 1 0
    //   43: invokeinterface 9 1 0
    //   48: pop
    //   49: aload_2
    //   50: athrow
    //   51: return
    // Line number table:
    //   Java source line #59	-> byte code offset #0
    //   Java source line #61	-> byte code offset #21
    //   Java source line #62	-> byte code offset #33
    //   Java source line #61	-> byte code offset #36
    //   Java source line #63	-> byte code offset #51
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	52	0	this	HttpHandler
    //   0	52	1	ctx	ChannelHandlerContext
    //   36	14	2	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   0	21	36	finally
  }
}
