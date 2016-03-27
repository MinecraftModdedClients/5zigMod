package eu.the5zig.util.db;

public class DummySQLQuery<T>
  extends SQLQuery<T>
{
  public DummySQLQuery(Database database, Class<T> entity)
  {
    super(database, entity);
  }
  
  public SQLResult<T> query(String query, Object... fields)
  {
    return new SQLResult();
  }
}
