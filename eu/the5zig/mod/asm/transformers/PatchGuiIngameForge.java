package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchGuiIngameForge
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GuiIngameForge (%s)", new Object[] { Names.guiIngameForge.getName() });
    
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
      if (Names.renderGameOverlayForge.equals(name, desc))
      {
        LogUtil.startMethod(Names.renderGameOverlayForge.getName() + " " + Names.renderGameOverlayForge.getDesc(), new Object[0]);
        return new PatchGuiIngameForge.PatchRenderGameOverlay(PatchGuiIngameForge.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.ingameTick.equals(name, desc))
      {
        LogUtil.startMethod(Names.ingameTick.getName() + " " + Names.ingameTick.getDesc(), new Object[0]);
        return new PatchGuiIngameForge.PatchTick(PatchGuiIngameForge.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.renderChatForge.equals(name, desc))
      {
        LogUtil.startMethod(Names.renderChatForge.getName() + " " + Names.renderChatForge.getDesc(), new Object[0]);
        return new PatchGuiIngameForge.PatchChat(PatchGuiIngameForge.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchRenderGameOverlay
    extends MethodVisitor
  {
    public PatchRenderGameOverlay(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("rendering mod", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onRenderGameOverlay", "()V", false);
    }
  }
  
  public class PatchTick
    extends MethodVisitor
  {
    public PatchTick(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("ingame tick", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onIngameTick", "()V", false);
    }
  }
  
  public class PatchChat
    extends MethodVisitor
  {
    public PatchChat(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("rendering chat", new Object[0]);
      this.mv.visitVarInsn(25, 0);
      this.mv.visitFieldInsn(180, Names.guiIngame.getName(), "m", "I");
      this.mv.visitMethodInsn(184, "BytecodeHook", "onDrawChat", "(I)V", false);
    }
  }
}
