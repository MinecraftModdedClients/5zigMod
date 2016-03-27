package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchGuiScreen
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GuiScreen (%s)", new Object[] { Names.guiScreen.getName() });
    
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
      if (Names.isCtrlKeyDown.equals(name, desc))
      {
        LogUtil.startMethod(Names.isCtrlKeyDown.getName() + "(%s)", new Object[] { Names.isCtrlKeyDown.getDesc() });
        return new PatchGuiScreen.PatchIsCtrlKeyDown(PatchGuiScreen.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchIsCtrlKeyDown
    extends MethodVisitor
  {
    public PatchIsCtrlKeyDown(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitIntInsn(int opcode, int i)
    {
      if ((opcode == 17) && (i == 219))
      {
        LogUtil.log("219", new Object[0]);
        this.mv.visitIntInsn(16, 29);
        return;
      }
      if ((opcode == 17) && (i == 220))
      {
        LogUtil.log("220", new Object[0]);
        this.mv.visitIntInsn(17, 157);
        return;
      }
      super.visitIntInsn(opcode, i);
    }
  }
}
