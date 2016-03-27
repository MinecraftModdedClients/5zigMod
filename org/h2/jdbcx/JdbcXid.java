package org.h2.jdbcx;

import java.util.StringTokenizer;
import javax.transaction.xa.Xid;
import org.h2.message.DbException;
import org.h2.message.TraceObject;
import org.h2.util.StringUtils;

public class JdbcXid
  extends TraceObject
  implements Xid
{
  private static final String PREFIX = "XID";
  private final int formatId;
  private final byte[] branchQualifier;
  private final byte[] globalTransactionId;
  
  JdbcXid(JdbcDataSourceFactory paramJdbcDataSourceFactory, int paramInt, String paramString)
  {
    setTrace(paramJdbcDataSourceFactory.getTrace(), 15, paramInt);
    try
    {
      StringTokenizer localStringTokenizer = new StringTokenizer(paramString, "_");
      String str = localStringTokenizer.nextToken();
      if (!"XID".equals(str)) {
        throw DbException.get(90101, paramString);
      }
      this.formatId = Integer.parseInt(localStringTokenizer.nextToken());
      this.branchQualifier = StringUtils.convertHexToBytes(localStringTokenizer.nextToken());
      this.globalTransactionId = StringUtils.convertHexToBytes(localStringTokenizer.nextToken());
    }
    catch (RuntimeException localRuntimeException)
    {
      throw DbException.get(90101, paramString);
    }
  }
  
  public static String toString(Xid paramXid)
  {
    StringBuilder localStringBuilder = new StringBuilder("XID");
    localStringBuilder.append('_').append(paramXid.getFormatId()).append('_').append(StringUtils.convertBytesToHex(paramXid.getBranchQualifier())).append('_').append(StringUtils.convertBytesToHex(paramXid.getGlobalTransactionId()));
    
    return localStringBuilder.toString();
  }
  
  public int getFormatId()
  {
    debugCodeCall("getFormatId");
    return this.formatId;
  }
  
  public byte[] getBranchQualifier()
  {
    debugCodeCall("getBranchQualifier");
    return this.branchQualifier;
  }
  
  public byte[] getGlobalTransactionId()
  {
    debugCodeCall("getGlobalTransactionId");
    return this.globalTransactionId;
  }
}
