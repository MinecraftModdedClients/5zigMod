package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchNetworkManager
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
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
      if ((name.equals("exceptionCaught")) && (desc.equals("(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Throwable;)V")))
      {
        LogUtil.startMethod("exceptionCaught", new Object[0]);
        return new PatchNetworkManager.PatchException(PatchNetworkManager.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchException
    extends MethodVisitor
  {
    public PatchException(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      this.mv.visitVarInsn(25, 2);
      this.mv.visitMethodInsn(182, "java/lang/Throwable", "printStackTrace", "()V", false);
      super.visitCode();
    }
  }
}
