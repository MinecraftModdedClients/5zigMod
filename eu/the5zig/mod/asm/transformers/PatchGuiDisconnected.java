package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchGuiDisconnected
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GuiDisconnected (%s)", new Object[] { Names.guiDisconnected.getName() });
    
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
      if (Names.guiDisconnectedInit.equals(name, desc))
      {
        LogUtil.startMethod(Names.guiDisconnectedInit.getName() + " " + Names.guiDisconnectedInit.getDesc(), new Object[0]);
        return new PatchGuiDisconnected.PatchInitGui(PatchGuiDisconnected.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.drawScreen.equals(name, desc))
      {
        LogUtil.startMethod(Names.drawScreen.getName() + " " + Names.drawScreen.getDesc(), new Object[0]);
        return new PatchGuiDisconnected.PatchDrawGui(PatchGuiDisconnected.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchInitGui
    extends MethodVisitor
  {
    public PatchInitGui(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("init", new Object[0]);
        this.mv.visitVarInsn(25, 0);
        this.mv.visitFieldInsn(180, Names.guiDisconnected.getName(), "h", "L" + Names.guiScreen.getName() + ";");
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiDisconnectedInit", "(Ljava/lang/Object;)V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchDrawGui
    extends MethodVisitor
  {
    public PatchDrawGui(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("draw", new Object[0]);
        this.mv.visitVarInsn(25, 0);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiDisconnectedDraw", "(Ljava/lang/Object;)V", false);
      }
      super.visitInsn(opcode);
    }
  }
}
