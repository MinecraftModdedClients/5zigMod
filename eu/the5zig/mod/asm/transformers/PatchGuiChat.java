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

public class PatchGuiChat
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GuiChat (%s)", new Object[] { Names.guiChat.getName() });
    
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
      if (Names.handleMouseInput.equals(name, desc))
      {
        LogUtil.startMethod(Names.handleMouseInput.getName() + " " + Names.handleMouseInput.getDesc(), new Object[0]);
        return new PatchGuiChat.PatchHandleMouseInput(PatchGuiChat.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.mouseClicked.equals(name, desc))
      {
        LogUtil.startMethod(Names.mouseClicked.getName() + " " + Names.mouseClicked.getDesc(), new Object[0]);
        return new PatchGuiChat.PatchMouseClicked(PatchGuiChat.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.guiClosed.equals(name, desc))
      {
        LogUtil.startMethod(Names.guiClosed.getName() + " " + Names.guiClosed.getDesc(), new Object[0]);
        return new PatchGuiChat.PatchGuiClosed(PatchGuiChat.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.keyTyped.equals(name, desc))
      {
        LogUtil.startMethod(Names.keyTyped.getName() + " " + Names.keyTyped.getDesc(), new Object[0]);
        return new PatchGuiChat.PatchKeyTyped(PatchGuiChat.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.drawScreen.equals(name, desc))
      {
        LogUtil.startMethod(Names.drawScreen.getName() + " " + Names.drawScreen.getDesc(), new Object[0]);
        return new PatchGuiChat.PatchDrawScreen(PatchGuiChat.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchHandleMouseInput
    extends MethodVisitor
  {
    public PatchHandleMouseInput(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
    {
      super.visitMethodInsn(opcode, owner, name, desc, itf);
      if (Names.guiChatNew.getName().equals(owner))
      {
        LogUtil.log("handleMouseInput", new Object[0]);
        this.mv.visitVarInsn(21, 1);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onChatMouseInput", "(I)V", false);
      }
    }
  }
  
  public class PatchMouseClicked
    extends MethodVisitor
  {
    public PatchMouseClicked(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("mouseClicked", new Object[0]);
      this.mv.visitVarInsn(21, 1);
      this.mv.visitVarInsn(21, 2);
      this.mv.visitVarInsn(21, 3);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onChatMouseClicked", "(III)Z", false);
      Label l1 = new Label();
      this.mv.visitJumpInsn(153, l1);
      this.mv.visitInsn(177);
      this.mv.visitLabel(l1);
      this.mv.visitFrame(3, 0, null, 0, null);
    }
  }
  
  public class PatchGuiClosed
    extends MethodVisitor
  {
    public PatchGuiClosed(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("guiClosed", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onChatClosed", "()V", false);
    }
  }
  
  public class PatchKeyTyped
    extends MethodVisitor
  {
    public PatchKeyTyped(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("keyTyped", new Object[0]);
      this.mv.visitVarInsn(21, 2);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onChatKeyTyped", "(I)V", false);
    }
  }
  
  public class PatchDrawScreen
    extends MethodVisitor
  {
    public PatchDrawScreen(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("drawScreen", new Object[0]);
      this.mv.visitIntInsn(21, 1);
      this.mv.visitIntInsn(21, 2);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onChatDrawScreen", "(II)V", false);
    }
  }
}
