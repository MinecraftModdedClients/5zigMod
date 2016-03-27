package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class PatchMinecraft
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("Minecraft (%s)", new Object[] { Names.minecraft.getName() });
    
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
      if (Names.startGame.equals(name, desc))
      {
        LogUtil.startMethod(Names.startGame.getName() + "(%s)", new Object[] { Names.startGame.getDesc() });
        return new PatchMinecraft.PatchStartGame(PatchMinecraft.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.dispatchKeypresses.equals(name, desc))
      {
        LogUtil.startMethod(Names.dispatchKeypresses.getName() + "(%s)", new Object[] { Names.dispatchKeypresses.getDesc() });
        return new PatchMinecraft.PatchDispatchKeypresses(PatchMinecraft.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.shutdown.equals(name, desc))
      {
        LogUtil.startMethod(Names.shutdown.getName() + "(%s)", new Object[] { Names.shutdown.getDesc() });
        return new PatchMinecraft.PatchShutdown(PatchMinecraft.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.displayCrashReport.equals(name, desc))
      {
        LogUtil.startMethod(Names.displayCrashReport.getName() + "(%s)", new Object[] { Names.displayCrashReport.getDesc() });
        return new PatchMinecraft.PatchDisplayCrashReport(PatchMinecraft.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.tick.equals(name, desc))
      {
        LogUtil.startMethod(Names.tick.getName() + "(%s)", new Object[] { Names.tick.getDesc() });
        return new PatchMinecraft.PatchTick(PatchMinecraft.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.leftClickMouse.equals(name, desc))
      {
        LogUtil.startMethod(Names.leftClickMouse.getName() + "(%s)", new Object[] { Names.leftClickMouse.getDesc() });
        return new PatchMinecraft.PatchLeftClickMouse(PatchMinecraft.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchStartGame
    extends MethodVisitor
  {
    public PatchStartGame(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      LogUtil.log("add resource pack", new Object[0]);
      this.mv.visitVarInsn(25, 0);
      this.mv.visitFieldInsn(180, Names.minecraft.getName(), Names.resourcePacks.getName(), "Ljava/util/List;");
      this.mv.visitTypeInsn(187, "The5zigModResourcePack");
      this.mv.visitInsn(89);
      this.mv.visitMethodInsn(183, "The5zigModResourcePack", "<init>", "()V", false);
      this.mv.visitMethodInsn(185, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
      this.mv.visitInsn(87);
      super.visitCode();
    }
  }
  
  public class PatchDispatchKeypresses
    extends MethodVisitor
  {
    public PatchDispatchKeypresses(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      LogUtil.log("dispatchKeypresses", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onDispatchKeyPresses", "()V", false);
      super.visitCode();
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean ifc)
    {
      super.visitMethodInsn(opcode, owner, name, desc, ifc);
      if ((opcode == 184) && ("org/lwjgl/input/Keyboard".equals(owner)) && ("getEventCharacter".equals(name)) && ("()C".equals(desc)) && (!ifc))
      {
        LogUtil.log("fix keys", new Object[0]);
        this.mv.visitIntInsn(17, 256);
        this.mv.visitInsn(96);
      }
    }
  }
  
  public class PatchShutdown
    extends MethodVisitor
  {
    public PatchShutdown(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      LogUtil.log("shutdown", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onShutdown", "()V", false);
      super.visitCode();
    }
  }
  
  public class PatchDisplayCrashReport
    extends MethodVisitor
  {
    private int count;
    
    public PatchDisplayCrashReport(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      this.mv.visitVarInsn(25, 1);
      this.mv.visitMethodInsn(184, "BytecodeHook", "appendCrashCategory", "(Ljava/lang/Object;)V", false);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 2)
      {
        this.count += 1;
        if (this.count == 1)
        {
          LogUtil.log("display crash report #" + this.count, new Object[0]);
          this.mv.visitVarInsn(25, 1);
          this.mv.visitMethodInsn(182, Names.crashReport.getName(), "b", "()Ljava/lang/Throwable;", false);
          this.mv.visitVarInsn(25, 1);
          this.mv.visitMethodInsn(182, Names.crashReport.getName(), "f", "()Ljava/io/File;", false);
          this.mv.visitMethodInsn(184, "BytecodeHook", "onDisplayCrashReport", "(Ljava/lang/Throwable;Ljava/io/File;)V", false);
        }
        else if (this.count == 2)
        {
          this.mv.visitVarInsn(25, 1);
          this.mv.visitMethodInsn(182, Names.crashReport.getName(), "b", "()Ljava/lang/Throwable;", false);
          this.mv.visitVarInsn(25, 3);
          this.mv.visitMethodInsn(182, "java/io/File", "getAbsoluteFile", "()Ljava/io/File;", false);
          this.mv.visitMethodInsn(184, "BytecodeHook", "onDisplayCrashReport", "(Ljava/lang/Throwable;Ljava/io/File;)V", false);
        }
      }
      super.visitInsn(opcode);
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
      this.mv.visitMethodInsn(184, "BytecodeHook", "onRealTick", "()V", false);
    }
  }
  
  public class PatchLeftClickMouse
    extends MethodVisitor
  {
    public PatchLeftClickMouse(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("left click mouse", new Object[0]);
      this.mv.visitMethodInsn(184, "BytecodeHook", "onLeftClickMouse", "()V", false);
    }
  }
}
