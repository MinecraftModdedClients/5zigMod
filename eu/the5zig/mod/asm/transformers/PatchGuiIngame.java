package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchGuiIngame
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("GuiIngame (%s)", new Object[] { Names.guiIngame });
    
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
      if (Names.renderGameOverlay.equals(name, desc))
      {
        LogUtil.startMethod(Names.renderGameOverlay.getName() + " " + Names.renderGameOverlay.getDesc(), new Object[0]);
        return new PatchGuiIngame.PatchRenderGameOverlay(PatchGuiIngame.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.renderHotbar.equals(name, desc))
      {
        LogUtil.startMethod(Names.renderHotbar.getName() + " " + Names.renderHotbar.getDesc(), new Object[0]);
        return new PatchGuiIngame.PatchRenderHotbar(PatchGuiIngame.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.ingameTick.equals(name, desc))
      {
        LogUtil.startMethod(Names.ingameTick.getName() + " " + Names.ingameTick.getDesc(), new Object[0]);
        return new PatchGuiIngame.PatchTick(PatchGuiIngame.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.renderVignette.equals(name, desc))
      {
        LogUtil.startMethod(Names.renderVignette.getName() + " " + Names.renderVignette.getDesc(), new Object[0]);
        return new PatchGuiIngame.PatchVignette(PatchGuiIngame.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.renderFood.equals(name, desc))
      {
        LogUtil.startMethod(Names.renderFood.getName() + " " + Names.renderFood.getDesc(), new Object[0]);
        return new PatchGuiIngame.PatchFood(PatchGuiIngame.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchRenderGameOverlay
    extends MethodVisitor
  {
    private boolean patchChat = false;
    
    public PatchRenderGameOverlay(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("renderGameOverlay", new Object[0]);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onRenderGameOverlay", "()V", false);
      }
      super.visitInsn(opcode);
    }
    
    public void visitLdcInsn(Object o)
    {
      super.visitLdcInsn(o);
      if ("chat".equals(o)) {
        this.patchChat = true;
      }
    }
    
    public void visitMethodInsn(int i, String s, String s1, String s2, boolean b)
    {
      super.visitMethodInsn(i, s, s1, s2, b);
      if (this.patchChat)
      {
        LogUtil.log("drawChat", new Object[0]);
        this.mv.visitVarInsn(25, 0);
        this.mv.visitFieldInsn(180, Names.guiIngame.getName(), "m", "I");
        this.mv.visitMethodInsn(184, "BytecodeHook", "onDrawChat", "(I)V", false);
        this.patchChat = false;
      }
    }
  }
  
  public class PatchRenderHotbar
    extends MethodVisitor
  {
    public PatchRenderHotbar(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      LogUtil.log("renderHotbar", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onRenderHotbar", "()V", false);
      super.visitCode();
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
      LogUtil.log("tick", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onIngameTick", "()V", false);
    }
  }
  
  public class PatchVignette
    extends MethodVisitor
  {
    private int count = 0;
    
    public PatchVignette(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean ifc)
    {
      super.visitMethodInsn(opcode, owner, name, desc, ifc);
      if ((opcode == 184) && (owner.equals(Names.glStateManager.getName())) && (Names.glColor.equals(name, desc)) && (!ifc))
      {
        this.count += 1;
        if (this.count == 2)
        {
          LogUtil.log("vignette", new Object[0]);
          this.mv.visitMethodInsn(184, "BytecodeHook", "onRenderVignette", "()V", false);
        }
      }
    }
  }
  
  public class PatchFood
    extends MethodVisitor
  {
    private boolean hasVisitedLDC = false;
    
    public PatchFood(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean ifc)
    {
      super.visitMethodInsn(opcode, owner, name, desc, ifc);
      if (this.hasVisitedLDC)
      {
        LogUtil.log("saturation", new Object[0]);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onRenderFood", "()V", false);
        this.hasVisitedLDC = false;
      }
    }
    
    public void visitLdcInsn(Object o)
    {
      super.visitLdcInsn(o);
      if (((o instanceof String)) && ("food".equals(o))) {
        this.hasVisitedLDC = true;
      }
    }
  }
}
