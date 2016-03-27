package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchGuiOptions
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GuiOptions (%s)", new Object[] { Names.guiOptions.getName() });
    
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
      if (Names.initGui.equals(name, desc))
      {
        LogUtil.startMethod("InitGui " + Names.initGui.getName() + "(%s)", new Object[] { Names.initGui.getDesc() });
        return new PatchGuiOptions.PatchInitGui(PatchGuiOptions.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.actionPerformed.equals(name, desc))
      {
        LogUtil.startMethod("actionPerformed " + Names.actionPerformed.getName() + "(%s)", new Object[] { Names.actionPerformed.getDesc() });
        return new PatchGuiOptions.PatchActionPerformed(PatchGuiOptions.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
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
    
    public void visitCode()
    {
      LogUtil.log("Adding button", new Object[0]);
      this.mv.visitVarInsn(25, 0);
      this.mv.visitFieldInsn(180, Names.guiOptions.getName(), "n", "Ljava/util/List;");
      this.mv.visitVarInsn(25, 0);
      this.mv.visitMethodInsn(184, "BytecodeHook", "get5zigOptionButton", "(Ljava/lang/Object;)Leu/the5zig/mod/gui/elements/IButton;", false);
      this.mv.visitMethodInsn(185, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
      this.mv.visitInsn(87);
      super.visitCode();
    }
  }
  
  public class PatchActionPerformed
    extends MethodVisitor
  {
    public PatchActionPerformed(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      LogUtil.log("action performed proxy", new Object[0]);
      this.mv.visitVarInsn(25, 0);
      this.mv.visitVarInsn(25, 1);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onOptionsActionPerformed", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
      super.visitCode();
    }
  }
}
