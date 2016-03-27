package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchGuiConnecting
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GuiConnecting (%s)", new Object[] { Names.guiConnecting.getName() });
    
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
      if (Names.guiConnectingInit1.equals(name, desc))
      {
        LogUtil.startMethod(Names.guiConnectingInit1.getName() + " " + Names.guiConnectingInit1.getDesc(), new Object[0]);
        return new PatchGuiConnecting.PatchInitGui1(PatchGuiConnecting.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.guiConnectingInit2.equals(name, desc))
      {
        LogUtil.startMethod(Names.guiConnectingInit2.getName() + " " + Names.guiConnectingInit2.getDesc(), new Object[0]);
        return new PatchGuiConnecting.PatchInitGui2(PatchGuiConnecting.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchInitGui1
    extends MethodVisitor
  {
    public PatchInitGui1(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("init1", new Object[0]);
        this.mv.visitVarInsn(25, 3);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiConnecting", "(Ljava/lang/Object;)V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchInitGui2
    extends MethodVisitor
  {
    public PatchInitGui2(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("init2", new Object[0]);
        this.mv.visitTypeInsn(187, Names.serverData.getName());
        this.mv.visitInsn(89);
        this.mv.visitVarInsn(25, 3);
        this.mv.visitTypeInsn(187, "java/lang/StringBuilder");
        this.mv.visitInsn(89);
        this.mv.visitMethodInsn(183, "java/lang/StringBuilder", "<init>", "()V", false);
        this.mv.visitVarInsn(25, 3);
        this.mv.visitMethodInsn(182, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        this.mv.visitLdcInsn(":");
        this.mv.visitMethodInsn(182, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        this.mv.visitVarInsn(21, 4);
        this.mv.visitMethodInsn(182, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
        this.mv.visitMethodInsn(182, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        this.mv.visitInsn(3);
        this.mv.visitMethodInsn(183, Names.serverData.getName(), "<init>", "(Ljava/lang/String;Ljava/lang/String;Z)V", false);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiConnecting", "(Ljava/lang/Object;)V", false);
      }
      super.visitInsn(opcode);
    }
  }
}
