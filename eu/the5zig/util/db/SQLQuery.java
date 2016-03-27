package eu.the5zig.util.db;

import eu.the5zig.util.db.exceptions.NoConnectionException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.logging.log4j.Logger;

public class SQLQuery<T>
{
  protected Database database;
  protected Class<T> entityClass;
  
  public SQLQuery(Database database, Class<T> entity)
  {
    this.database = database;
    this.entityClass = entity;
  }
  
  public SQLResult<T> query(String query, Object... fields)
  {
    try
    {
      connection = this.database.getConnection();
    }
    catch (NoConnectionException e)
    {
      Connection connection;
      this.database.getLogger().debug(e);
      return new SQLResult();
    }
    Connection connection;
    if (connection == null) {
      throw new RuntimeException("Connection is Null!");
    }
    PreparedStatement st = null;
    ResultSet rs = null;
    try
    {
      st = connection.prepareStatement(query);
      for (int i = 0; i < fields.length; i++) {
        st.setObject(i + 1, fields[i]);
      }
      rs = st.executeQuery();
      SQLResult<T> result = new SQLResult();
      while (rs.next())
      {
        metaData = rs.getMetaData();
        try
        {
          T entity = this.entityClass.newInstance();
          int columns = metaData.getColumnCount();
          for (int i = 1; i <= columns; i++)
          {
            try
            {
              field = entity.getClass().getDeclaredField(metaData.getColumnName(i));
            }
            catch (NoSuchFieldException e)
            {
              Field field;
              continue;
            }
            Field field;
            Object value = rs.getObject(i);
            if ((field != null) && (value != null))
            {
              field.setAccessible(true);
              if (UUID.class.isAssignableFrom(field.getType())) {
                field.set(entity, UUID.fromString(rs.getString(i)));
              } else if (Boolean.TYPE.isAssignableFrom(field.getType())) {
                field.set(entity, Boolean.valueOf(rs.getBoolean(i)));
              } else {
                field.set(entity, value);
              }
              field.setAccessible(false);
            }
          }
          result.add(entity);
        }
        catch (InstantiationException e)
        {
          this.database.getLogger().warn("Failed to Instantiate a Field via Reflection", e);
        }
        catch (IllegalAccessException e)
        {
          this.database.getLogger().warn("Failed to Access a Field via Reflection", e);
        }
      }
      return result;
    }
    catch (SQLException e)
    {
      ResultSetMetaData metaData;
      this.database.getLogger().warn("Could not Execute MySQL Update " + query, e);
      return new SQLResult();
    }
    finally
    {
      this.database.closeResources(rs, st);
    }
  }
}
