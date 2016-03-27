package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class PatchAbstractClientPlayer
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("AbstractClientPlayer (%s)", new Object[] { Names.abstractClientPlayer.getName() });
    
    ClassReader reader = new ClassReader(bytes);
    ClassWriter writer = new ClassWriter(reader, 3);
    writer.visitField(1, "capeLocation", "L" + Names.resourceLocation.getName() + ";", null, null).visitEnd();
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
      if (Names.abstractClientPlayerInit.equals(name, desc))
      {
        LogUtil.startMethod(Names.abstractClientPlayerInit.getName() + " " + Names.abstractClientPlayerInit.getDesc(), new Object[0]);
        return new PatchAbstractClientPlayer.PatchInit(PatchAbstractClientPlayer.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.getResourceLocation.equals(name, desc))
      {
        LogUtil.startMethod(Names.getResourceLocation.getName() + " " + Names.getResourceLocation.getDesc(), new Object[0]);
        return new PatchAbstractClientPlayer.PatchGetResourceLocation(PatchAbstractClientPlayer.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchInit
    extends MethodVisitor
  {
    public PatchInit(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("Init", new Object[0]);
        this.mv.visitVarInsn(25, 2);
        this.mv.visitVarInsn(25, 0);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onAbstractClientPlayerInit", "(Lcom/mojang/authlib/GameProfile;Ljava/lang/Object;)V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchGetResourceLocation
    extends MethodVisitor
  {
    public PatchGetResourceLocation(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitCode()
    {
      super.visitCode();
      LogUtil.log("capeLocation", new Object[0]);
      this.mv.visitVarInsn(25, 0);
      this.mv.visitFieldInsn(180, Names.abstractClientPlayer.getName(), "capeLocation", "L" + Names.resourceLocation.getName() + ";");
      Label l1 = new Label();
      this.mv.visitJumpInsn(198, l1);
      this.mv.visitVarInsn(25, 0);
      this.mv.visitFieldInsn(180, Names.abstractClientPlayer.getName(), "capeLocation", "L" + Names.resourceLocation.getName() + ";");
      this.mv.visitInsn(176);
      this.mv.visitLabel(l1);
    }
  }
}
