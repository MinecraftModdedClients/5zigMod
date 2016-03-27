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

public class PatchGuiSelectWorld
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GuiSelectWorld (%s)", new Object[] { Names.guiSelectWorld.getName() });
    
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
    private boolean patchedGuiClosedMethod = false;
    private boolean patchedKeyTypedMethod = false;
    private boolean patchedMouseClickedMethod = false;
    
    public ClassPatcher(ClassVisitor visitor)
    {
      super(visitor);
    }
    
    public void visitEnd()
    {
      if (!this.patchedGuiClosedMethod)
      {
        LogUtil.startMethod(Names.guiClosed.getName() + " " + Names.guiClosed.getDesc(), new Object[0]);
        MethodVisitor mv = this.cv.visitMethod(1, Names.guiClosed.getName(), Names.guiClosed.getDesc(), null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitMethodInsn(184, "BytecodeHook", "onGuiSelectWorldClosed", "()V", false);
        mv.visitInsn(177);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + Names.guiSelectWorld.getName() + ";", null, l0, l2, 0);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
      }
      if (!this.patchedKeyTypedMethod)
      {
        LogUtil.startMethod(Names.keyTyped.getName() + " " + Names.keyTyped.getDesc(), new Object[0]);
        MethodVisitor mv = this.cv.visitMethod(1, Names.keyTyped.getName(), Names.keyTyped.getDesc(), null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(148, l0);
        mv.visitVarInsn(25, 0);
        mv.visitVarInsn(21, 1);
        mv.visitVarInsn(21, 2);
        mv.visitMethodInsn(183, Names.guiScreen.getName(), Names.keyTyped.getName(), Names.keyTyped.getDesc(), false);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(149, l1);
        mv.visitVarInsn(21, 1);
        mv.visitVarInsn(21, 2);
        mv.visitMethodInsn(184, "BytecodeHook", "onGuiSelectWorldKey", "(CI)V", false);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(150, l2);
        mv.visitInsn(177);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "L" + Names.guiSelectWorld.getName() + ";", null, l0, l3, 0);
        mv.visitLocalVariable("c", "C", null, l0, l3, 1);
        mv.visitLocalVariable("i", "I", null, l0, l3, 2);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
      }
      if (!this.patchedMouseClickedMethod)
      {
        LogUtil.startMethod(Names.mouseClicked.getName() + " " + Names.mouseClicked.getDesc(), new Object[0]);
        MethodVisitor mv = this.cv.visitMethod(4, Names.mouseClicked.getName(), Names.mouseClicked.getDesc(), null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(67, l0);
        mv.visitVarInsn(25, 0);
        mv.visitVarInsn(21, 1);
        mv.visitVarInsn(21, 2);
        mv.visitVarInsn(21, 3);
        mv.visitMethodInsn(183, Names.guiScreen.getName(), "a", "(III)V", false);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(68, l1);
        mv.visitVarInsn(21, 1);
        mv.visitVarInsn(21, 2);
        mv.visitVarInsn(21, 3);
        mv.visitMethodInsn(184, "BytecodeHook", "onGuiSelectWorldMouseClicked", "(III)V", false);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(69, l2);
        mv.visitInsn(177);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "L" + Names.guiSelectWorld.getName() + ";", null, l0, l3, 0);
        mv.visitLocalVariable("i", "I", null, l0, l3, 1);
        mv.visitLocalVariable("i1", "I", null, l0, l3, 2);
        mv.visitLocalVariable("i2", "I", null, l0, l3, 3);
        mv.visitMaxs(4, 4);
        mv.visitEnd();
      }
      super.visitEnd();
    }
    
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
      if (Names.initGui.equals(name, desc))
      {
        LogUtil.startMethod(Names.initGui.getName() + " " + Names.initGui.getDesc(), new Object[0]);
        return new PatchGuiSelectWorld.PatchInitGui(PatchGuiSelectWorld.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.guiClosed.equals(name, desc))
      {
        this.patchedGuiClosedMethod = true;
        LogUtil.startMethod(Names.guiClosed.getName() + " " + Names.guiClosed.getDesc(), new Object[0]);
        return new PatchGuiSelectWorld.PatchGuiClosed(PatchGuiSelectWorld.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.drawScreen.equals(name, desc))
      {
        LogUtil.startMethod(Names.drawScreen.getName() + " " + Names.drawScreen.getDesc(), new Object[0]);
        return new PatchGuiSelectWorld.PatchDrawScreen(PatchGuiSelectWorld.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.keyTyped.equals(name, desc))
      {
        this.patchedKeyTypedMethod = true;
        LogUtil.startMethod(Names.keyTyped.getName() + " " + Names.keyTyped.getDesc(), new Object[0]);
        return new PatchGuiSelectWorld.PatchKeyTyped(PatchGuiSelectWorld.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.mouseClicked.equals(name, desc))
      {
        this.patchedMouseClickedMethod = true;
        LogUtil.startMethod(Names.mouseClicked.getName() + " " + Names.mouseClicked.getDesc(), new Object[0]);
        return new PatchGuiSelectWorld.PatchMouseClicked(PatchGuiSelectWorld.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
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
        this.mv.visitVarInsn(25, 0);
        this.mv.visitFieldInsn(180, Names.guiSelectWorld.getName(), "u", "Lbho;");
        this.mv.visitMethodInsn(184, "java/util/Collections", "singletonList", "(Ljava/lang/Object;)Ljava/util/List;", false);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiSelectWorldInit", "(Ljava/lang/Object;Ljava/util/List;)V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchGuiClosed
    extends MethodVisitor
  {
    public PatchGuiClosed(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("guiclosed", new Object[0]);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiSelectWorldClosed", "()V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchDrawScreen
    extends MethodVisitor
  {
    public PatchDrawScreen(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("drawscreen", new Object[0]);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiSelectWorldDraw", "()V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchKeyTyped
    extends MethodVisitor
  {
    public PatchKeyTyped(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("keytyped", new Object[0]);
        this.mv.visitVarInsn(21, 1);
        this.mv.visitVarInsn(21, 2);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiSelectWorldKey", "(CI)V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchMouseClicked
    extends MethodVisitor
  {
    public PatchMouseClicked(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("mouse clicked", new Object[0]);
        this.mv.visitVarInsn(21, 1);
        this.mv.visitVarInsn(21, 2);
        this.mv.visitVarInsn(21, 3);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onGuiSelectWorldMouseClicked", "(III)V", false);
      }
      super.visitInsn(opcode);
    }
  }
}
