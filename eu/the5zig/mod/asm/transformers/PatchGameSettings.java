package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchGameSettings
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GameSettings (%s)", new Object[] { Names.gameSettings.getName() });
    
    ClassReader reader = new ClassReader(bytes);
    ClassWriter writer = new ClassWriter(reader, 3);
    ClassPatcher visitor = new ClassPatcher(writer);
    reader.accept(visitor, 0);
    LogUtil.endClass();
    return writer.toByteArray();
  }
  
  public class ClassPatcher
    extends ClassVisitor
  {
    public ClassPatcher(ClassVisitor visitor)
    {
      super(visitor);
    }
    
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
      if (Names.getKeyBinding.equals(name, desc))
      {
        LogUtil.startMethod(Names.getKeyBinding.getName() + "(%s)", new Object[] { Names.getKeyBinding.getDesc() });
        return new PatchGameSettings.PatchGetKeyBinding(PatchGameSettings.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchGetKeyBinding
    extends MethodVisitor
  {
    private boolean found = false;
    
    public PatchGetKeyBinding(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitLdcInsn(Object o)
    {
      if ((o instanceof Float))
      {
        Float value = (Float)o;
        if ((value.floatValue() == 100.0F) && (!this.found))
        {
          LogUtil.log("gamma", new Object[0]);
          this.mv.visitLdcInsn(Float.valueOf(1000.0F));
          this.found = true;
          return;
        }
      }
      super.visitLdcInsn(o);
    }
  }
}
