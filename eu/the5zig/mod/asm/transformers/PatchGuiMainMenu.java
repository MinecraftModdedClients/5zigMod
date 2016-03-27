package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import eu.the5zig.mod.asm.Transformer;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchGuiMainMenu
  implements IClassTransformer
{
  public byte[] transform(String s, String arg, byte[] bytes)
  {
    LogUtil.startClass("GuiMainMenu (%s)", new Object[] { "ayb" });
    
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
      if (Names.insertSingleMultiplayerButton.equals(name, desc))
      {
        LogUtil.startMethod(Names.insertSingleMultiplayerButton.getName() + "(%s)", new Object[] { Names.insertSingleMultiplayerButton.getDesc() });
        return new PatchGuiMainMenu.PatchInsertSingleMultiplayer(PatchGuiMainMenu.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.actionPerformed.equals(name, desc))
      {
        LogUtil.startMethod(Names.actionPerformed.getName() + "(%s)", new Object[] { Names.actionPerformed.getDesc() });
        return new PatchGuiMainMenu.PatchActionPerformed(PatchGuiMainMenu.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if ((Names._static.equals(name, desc)) && (access == 8))
      {
        LogUtil.startMethod(Names._static.getName() + "(%s)", new Object[] { Names._static.getDesc() });
        return new PatchGuiMainMenu.PatchStatic(PatchGuiMainMenu.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchInsertSingleMultiplayer
    extends MethodVisitor
  {
    public PatchInsertSingleMultiplayer(MethodVisitor visitor)
    {
      super(visitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("Adding 'Last Server' Button... ", new Object[0]);
        this.mv.visitVarInsn(25, 0);
        this.mv.visitVarInsn(21, 1);
        this.mv.visitVarInsn(21, 2);
        this.mv.visitInsn(Transformer.FORGE ? 4 : 3);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onInsertSingleMultiplayerButton", "(Ljava/lang/Object;IIZ)V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchActionPerformed
    extends MethodVisitor
  {
    public PatchActionPerformed(MethodVisitor visitor)
    {
      super(visitor);
    }
    
    public void visitCode()
    {
      LogUtil.log("Adding Proxy access", new Object[0]);
      this.mv.visitVarInsn(25, 1);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onMainActionPerformed", "(Ljava/lang/Object;)V", false);
      super.visitCode();
    }
  }
  
  public class PatchStatic
    extends MethodVisitor
  {
    public PatchStatic(MethodVisitor visitor)
    {
      super(visitor);
    }
    
    public void visitCode()
    {
      LogUtil.log("Adding The 5zig Mod", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onMainStatic", "()V", false);
      super.visitCode();
    }
  }
}
