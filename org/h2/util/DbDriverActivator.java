package org.h2.util;

import org.h2.Driver;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class DbDriverActivator
  implements BundleActivator
{
  private static final String DATASOURCE_FACTORY_CLASS = "org.osgi.service.jdbc.DataSourceFactory";
  
  public void start(BundleContext paramBundleContext)
  {
    Driver localDriver = Driver.load();
    try
    {
      JdbcUtils.loadUserClass("org.osgi.service.jdbc.DataSourceFactory");
    }
    catch (Exception localException)
    {
      return;
    }
    OsgiDataSourceFactory.registerService(paramBundleContext, localDriver);
  }
  
  public void stop(BundleContext paramBundleContext) {}
}
