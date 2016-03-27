package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class PatchRenderItem
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("RenderItem (%s)", new Object[] { Names.renderItem.getName() });
    
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
      if (Names.renderItemPerson.equals(name, desc))
      {
        LogUtil.startMethod(Names.renderItemPerson.getName() + "(%s)", new Object[] { Names.renderItemPerson.getDesc() });
        return new PatchRenderItem.PatchRenderPerson(PatchRenderItem.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.renderItemInventory.equals(name, desc))
      {
        LogUtil.startMethod(Names.renderItemInventory.getName() + "(%s)", new Object[] { Names.renderItemInventory.getDesc() });
        return new PatchRenderItem.PatchRenderInventory(PatchRenderItem.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchRenderPerson
    extends MethodVisitor
  {
    public PatchRenderPerson(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("renderItemPerson", new Object[0]);
      this.mv.visitVarInsn(25, 0);
      this.mv.visitVarInsn(25, 1);
      this.mv.visitVarInsn(25, 2);
      this.mv.visitVarInsn(25, 3);
      this.mv.visitVarInsn(21, 4);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onRenderItemPerson", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Z)Z", false);
      Label label = new Label();
      this.mv.visitJumpInsn(153, label);
      this.mv.visitInsn(177);
      this.mv.visitLabel(label);
      this.mv.visitFrame(3, 0, null, 0, null);
    }
  }
  
  public class PatchRenderInventory
    extends MethodVisitor
  {
    public PatchRenderInventory(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("renderItemInventory", new Object[0]);
      this.mv.visitVarInsn(25, 0);
      this.mv.visitVarInsn(25, 1);
      this.mv.visitVarInsn(21, 2);
      this.mv.visitVarInsn(21, 3);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onRenderItemInventory", "(Ljava/lang/Object;Ljava/lang/Object;II)Z", false);
      Label label = new Label();
      this.mv.visitJumpInsn(153, label);
      this.mv.visitInsn(177);
      this.mv.visitLabel(label);
      this.mv.visitFrame(3, 0, null, 0, null);
    }
  }
}
