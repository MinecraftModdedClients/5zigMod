package org.h2.jdbcx;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.h2.Driver;
import org.h2.engine.SysProperties;
import org.h2.message.Trace;
import org.h2.message.TraceSystem;

public class JdbcDataSourceFactory
  implements ObjectFactory
{
  private static TraceSystem cachedTraceSystem;
  private final Trace trace;
  
  static
  {
    Driver.load();
  }
  
  public JdbcDataSourceFactory()
  {
    this.trace = getTraceSystem().getTrace("JDBCX");
  }
  
  public synchronized Object getObjectInstance(Object paramObject, Name paramName, Context paramContext, Hashtable<?, ?> paramHashtable)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("getObjectInstance obj={0} name={1} nameCtx={2} environment={3}", new Object[] { paramObject, paramName, paramContext, paramHashtable });
    }
    if ((paramObject instanceof Reference))
    {
      Reference localReference = (Reference)paramObject;
      if (localReference.getClassName().equals(JdbcDataSource.class.getName()))
      {
        JdbcDataSource localJdbcDataSource = new JdbcDataSource();
        localJdbcDataSource.setURL((String)localReference.get("url").getContent());
        localJdbcDataSource.setUser((String)localReference.get("user").getContent());
        localJdbcDataSource.setPassword((String)localReference.get("password").getContent());
        localJdbcDataSource.setDescription((String)localReference.get("description").getContent());
        String str = (String)localReference.get("loginTimeout").getContent();
        localJdbcDataSource.setLoginTimeout(Integer.parseInt(str));
        return localJdbcDataSource;
      }
    }
    return null;
  }
  
  public static TraceSystem getTraceSystem()
  {
    synchronized (JdbcDataSourceFactory.class)
    {
      if (cachedTraceSystem == null)
      {
        cachedTraceSystem = new TraceSystem(SysProperties.CLIENT_TRACE_DIRECTORY + "h2datasource" + ".trace.db");
        
        cachedTraceSystem.setLevelFile(SysProperties.DATASOURCE_TRACE_LEVEL);
      }
      return cachedTraceSystem;
    }
  }
  
  Trace getTrace()
  {
    return this.trace;
  }
}
