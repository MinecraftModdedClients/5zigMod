package org.h2.value;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import org.h2.message.DbException;
import org.h2.util.StringUtils;

public class ValueGeometry
  extends Value
{
  private final byte[] bytes;
  private final int hashCode;
  private Geometry geometry;
  
  private ValueGeometry(byte[] paramArrayOfByte, Geometry paramGeometry)
  {
    this.bytes = paramArrayOfByte;
    this.geometry = paramGeometry;
    this.hashCode = Arrays.hashCode(paramArrayOfByte);
  }
  
  public static ValueGeometry getFromGeometry(Object paramObject)
  {
    return get((Geometry)paramObject);
  }
  
  private static ValueGeometry get(Geometry paramGeometry)
  {
    byte[] arrayOfByte = convertToWKB(paramGeometry);
    return (ValueGeometry)Value.cache(new ValueGeometry(arrayOfByte, paramGeometry));
  }
  
  private static byte[] convertToWKB(Geometry paramGeometry)
  {
    boolean bool = paramGeometry.getSRID() != 0;
    int i = getDimensionCount(paramGeometry);
    WKBWriter localWKBWriter = new WKBWriter(i, bool);
    return localWKBWriter.write(paramGeometry);
  }
  
  private static int getDimensionCount(Geometry paramGeometry)
  {
    ZVisitor localZVisitor = new ZVisitor();
    paramGeometry.apply(localZVisitor);
    return localZVisitor.isFoundZ() ? 3 : 2;
  }
  
  public static ValueGeometry get(String paramString)
  {
    try
    {
      Geometry localGeometry = new WKTReader().read(paramString);
      return get(localGeometry);
    }
    catch (ParseException localParseException)
    {
      throw DbException.convert(localParseException);
    }
  }
  
  public static ValueGeometry get(String paramString, int paramInt)
  {
    try
    {
      GeometryFactory localGeometryFactory = new GeometryFactory(new PrecisionModel(), paramInt);
      Geometry localGeometry = new WKTReader(localGeometryFactory).read(paramString);
      return get(localGeometry);
    }
    catch (ParseException localParseException)
    {
      throw DbException.convert(localParseException);
    }
  }
  
  public static ValueGeometry get(byte[] paramArrayOfByte)
  {
    return (ValueGeometry)Value.cache(new ValueGeometry(paramArrayOfByte, null));
  }
  
  public Geometry getGeometry()
  {
    return (Geometry)getGeometryNoCopy().clone();
  }
  
  public Geometry getGeometryNoCopy()
  {
    if (this.geometry == null) {
      try
      {
        this.geometry = new WKBReader().read(this.bytes);
      }
      catch (ParseException localParseException)
      {
        throw DbException.convert(localParseException);
      }
    }
    return this.geometry;
  }
  
  public boolean intersectsBoundingBox(ValueGeometry paramValueGeometry)
  {
    return getGeometryNoCopy().getEnvelopeInternal().intersects(paramValueGeometry.getGeometryNoCopy().getEnvelopeInternal());
  }
  
  public Value getEnvelopeUnion(ValueGeometry paramValueGeometry)
  {
    GeometryFactory localGeometryFactory = new GeometryFactory();
    Envelope localEnvelope = new Envelope(getGeometryNoCopy().getEnvelopeInternal());
    localEnvelope.expandToInclude(paramValueGeometry.getGeometryNoCopy().getEnvelopeInternal());
    return get(localGeometryFactory.toGeometry(localEnvelope));
  }
  
  public int getType()
  {
    return 22;
  }
  
  public String getSQL()
  {
    return "X'" + StringUtils.convertBytesToHex(getBytesNoCopy()) + "'::Geometry";
  }
  
  protected int compareSecure(Value paramValue, CompareMode paramCompareMode)
  {
    Geometry localGeometry = ((ValueGeometry)paramValue).getGeometryNoCopy();
    return getGeometryNoCopy().compareTo(localGeometry);
  }
  
  public String getString()
  {
    return getWKT();
  }
  
  public long getPrecision()
  {
    return 0L;
  }
  
  public int hashCode()
  {
    return this.hashCode;
  }
  
  public Object getObject()
  {
    return getGeometry();
  }
  
  public byte[] getBytes()
  {
    return getWKB();
  }
  
  public byte[] getBytesNoCopy()
  {
    return getWKB();
  }
  
  public void set(PreparedStatement paramPreparedStatement, int paramInt)
    throws SQLException
  {
    paramPreparedStatement.setObject(paramInt, getGeometryNoCopy());
  }
  
  public int getDisplaySize()
  {
    return getWKT().length();
  }
  
  public int getMemory()
  {
    return getWKB().length * 20 + 24;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof ValueGeometry)) && (Arrays.equals(getWKB(), ((ValueGeometry)paramObject).getWKB()));
  }
  
  public String getWKT()
  {
    return new WKTWriter(3).write(getGeometryNoCopy());
  }
  
  public byte[] getWKB()
  {
    return this.bytes;
  }
  
  public Value convertTo(int paramInt)
  {
    if (paramInt == 19) {
      return this;
    }
    return super.convertTo(paramInt);
  }
  
  static class ZVisitor
    implements CoordinateSequenceFilter
  {
    boolean foundZ;
    
    public boolean isFoundZ()
    {
      return this.foundZ;
    }
    
    public void filter(CoordinateSequence paramCoordinateSequence, int paramInt)
    {
      if (!Double.isNaN(paramCoordinateSequence.getOrdinate(paramInt, 2))) {
        this.foundZ = true;
      }
    }
    
    public boolean isDone()
    {
      return this.foundZ;
    }
    
    public boolean isGeometryChanged()
    {
      return false;
    }
  }
}
