package eu.the5zig.mod.asm.transformers;

import eu.the5zig.mod.asm.LogUtil;
import eu.the5zig.mod.asm.Name;
import eu.the5zig.mod.asm.Names;
import eu.the5zig.util.minecraft.ChatColor;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class PatchNetHandlerPlayClient
  implements IClassTransformer
{
  public byte[] transform(String s, String s1, byte[] bytes)
  {
    LogUtil.startClass("NetHandlerPlayClient (%s)", new Object[] { Names.netHandlerPlayClient.getName() });
    
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
      if (Names.handleCustomPayload.equals(name, desc))
      {
        LogUtil.startMethod(Names.handleCustomPayload.getName() + "(%s)", new Object[] { Names.handleCustomPayload.getDesc() });
        return new PatchNetHandlerPlayClient.PatchHandleCustomPayload(PatchNetHandlerPlayClient.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.handlePacketPlayerListHeaderFooter.equals(name, desc))
      {
        LogUtil.startMethod(Names.handlePacketPlayerListHeaderFooter.getName() + "(%s)", new Object[] { Names.handlePacketPlayerListHeaderFooter.getDesc() });
        return new PatchNetHandlerPlayClient.PatchHandlePlayerListHeaderFooter(PatchNetHandlerPlayClient.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.handlePacketChat.equals(name, desc))
      {
        LogUtil.startMethod(Names.handlePacketChat.getName() + "(%s)", new Object[] { Names.handlePacketChat.getDesc() });
        return new PatchNetHandlerPlayClient.PatchHandlePacketChat(PatchNetHandlerPlayClient.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.handlePacketSetSlot.equals(name, desc))
      {
        LogUtil.startMethod(Names.handlePacketSetSlot.getName() + "(%s)", new Object[] { Names.handlePacketSetSlot.getDesc() });
        return new PatchNetHandlerPlayClient.PatchHandlePacketSetSlot(PatchNetHandlerPlayClient.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.handlePacketPlayerInfo.equals(name, desc))
      {
        LogUtil.startMethod(Names.handlePacketPlayerInfo.getName() + "(%s)", new Object[] { Names.handlePacketPlayerInfo.getDesc() });
        return new PatchNetHandlerPlayClient.PatchHandlePacketPlayerInfo(PatchNetHandlerPlayClient.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      if (Names.handlePacketTitle.equals(name, desc))
      {
        LogUtil.startMethod(Names.handlePacketTitle.getName() + "(%s)", new Object[] { Names.handlePacketTitle.getDesc() });
        return new PatchNetHandlerPlayClient.PatchHandlePacketTitle(PatchNetHandlerPlayClient.this, this.cv.visitMethod(access, name, desc, signature, exceptions));
      }
      LogUtil.endMethod();
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }
  
  public class PatchHandleCustomPayload
    extends MethodVisitor
  {
    public PatchHandleCustomPayload(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("payload", new Object[0]);
        this.mv.visitVarInsn(25, 1);
        this.mv.visitMethodInsn(182, Names.packetPayload.getName(), "a", "()Ljava/lang/String;", false);
        this.mv.visitVarInsn(25, 1);
        this.mv.visitMethodInsn(182, Names.packetPayload.getName(), "b", "()L" + Names.packetBuffer.getName() + ";", false);
        this.mv.visitMethodInsn(184, "BytecodeHook", "packetBufferToByteBuf", "(Ljava/lang/Object;)Lio/netty/buffer/ByteBuf;", false);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onCustomPayload", "(Ljava/lang/String;Lio/netty/buffer/ByteBuf;)V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchHandlePlayerListHeaderFooter
    extends MethodVisitor
  {
    public PatchHandlePlayerListHeaderFooter(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        LogUtil.log("playerListHeaderFooter", new Object[0]);
        this.mv.visitTypeInsn(187, "eu/the5zig/mod/util/TabList");
        this.mv.visitInsn(89);
        this.mv.visitVarInsn(25, 1);
        this.mv.visitMethodInsn(182, Names.packetHeaderFooter.getName(), "a", "()L" + Names.chatComponent.getName() + ";", false);
        this.mv.visitMethodInsn(185, Names.chatComponent.getName(), "d", "()Ljava/lang/String;", true);
        this.mv.visitVarInsn(25, 1);
        this.mv.visitMethodInsn(182, Names.packetHeaderFooter.getName(), "b", "()L" + Names.chatComponent.getName() + ";", false);
        this.mv.visitMethodInsn(185, Names.chatComponent.getName(), "d", "()Ljava/lang/String;", true);
        this.mv.visitMethodInsn(183, "eu/the5zig/mod/util/TabList", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onPlayerListHeaderFooter", "(Leu/the5zig/mod/util/TabList;)V", false);
      }
      super.visitInsn(opcode);
    }
  }
  
  public class PatchHandlePacketChat
    extends MethodVisitor
  {
    private int getField = 0;
    
    public PatchHandlePacketChat(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitFieldInsn(int opcode, String owner, String name, String desc)
    {
      if ((opcode == 180) && (owner.equals(Names.netHandlerPlayClient.getName())) && (name.equals("f")) && (desc.equals("L" + Names.minecraft.getName() + ";")))
      {
        if (this.getField == 1)
        {
          LogUtil.log("adding actionBar Proxy", new Object[0]);
          
          this.mv.visitInsn(87);
          this.mv.visitVarInsn(25, 1);
          this.mv.visitMethodInsn(182, Names.packetChat.getName(), "a", "()L" + Names.chatComponent.getName() + ";", false);
          this.mv.visitMethodInsn(185, Names.chatComponent.getName(), "d", "()Ljava/lang/String;", true);
          this.mv.visitLdcInsn(ChatColor.RESET.toString());
          this.mv.visitLdcInsn("");
          this.mv.visitMethodInsn(182, "java/lang/String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false);
          this.mv.visitMethodInsn(184, "BytecodeHook", "onActionBar", "(Ljava/lang/String;)Z", false);
          Label l2 = new Label();
          this.mv.visitJumpInsn(153, l2);
          this.mv.visitInsn(177);
          this.mv.visitLabel(l2);
          this.mv.visitFrame(3, 0, null, 0, null);
          this.mv.visitVarInsn(25, 0);
        }
        if (this.getField == 2)
        {
          LogUtil.log("adding onServerChat Proxy", new Object[0]);
          
          this.mv.visitInsn(87);
          this.mv.visitVarInsn(25, 1);
          this.mv.visitMethodInsn(182, Names.packetChat.getName(), "a", "()L" + Names.chatComponent.getName() + ";", false);
          this.mv.visitMethodInsn(185, Names.chatComponent.getName(), "d", "()Ljava/lang/String;", true);
          this.mv.visitLdcInsn(ChatColor.RESET.toString());
          this.mv.visitLdcInsn("");
          this.mv.visitMethodInsn(182, "java/lang/String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false);
          this.mv.visitVarInsn(25, 1);
          this.mv.visitMethodInsn(182, Names.packetChat.getName(), "a", "()L" + Names.chatComponent.getName() + ";", false);
          this.mv.visitMethodInsn(184, "BytecodeHook", "onChat", "(Ljava/lang/String;Ljava/lang/Object;)Z", false);
          Label l2 = new Label();
          this.mv.visitJumpInsn(153, l2);
          this.mv.visitInsn(177);
          this.mv.visitLabel(l2);
          this.mv.visitFrame(3, 0, null, 0, null);
          this.mv.visitVarInsn(25, 0);
        }
        this.getField += 1;
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }
  
  public class PatchHandlePacketSetSlot
    extends MethodVisitor
  {
    private int count = 0;
    
    public PatchHandlePacketSetSlot(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
    {
      super.visitMethodInsn(opcode, owner, name, desc, itf);
      if ((opcode == 182) && (owner.equals(Names.openContainer.getName())) && (name.equals("a")) && (desc.equals("(IL" + Names.itemStack.getName() + ";)V")) && (!itf))
      {
        if (this.count == 1)
        {
          LogUtil.log("handleInventorySetSlot at c=" + this.count, new Object[0]);
          this.mv.visitVarInsn(25, 1);
          this.mv.visitMethodInsn(182, Names.packetSetSlot.getName(), "b", "()I", false);
          this.mv.visitTypeInsn(187, "WrappedItemStack");
          this.mv.visitInsn(89);
          this.mv.visitVarInsn(25, 1);
          this.mv.visitMethodInsn(182, Names.packetSetSlot.getName(), "c", "()L" + Names.itemStack.getName() + ";", false);
          this.mv.visitMethodInsn(183, "WrappedItemStack", "<init>", "(L" + Names.itemStack.getName() + ";)V", false);
          this.mv.visitMethodInsn(184, "BytecodeHook", "onSetSlot", "(ILeu/the5zig/mod/util/ItemStack;)V", false);
        }
        this.count += 1;
      }
    }
  }
  
  public class PatchHandlePacketPlayerInfo
    extends MethodVisitor
  {
    public PatchHandlePacketPlayerInfo(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitJumpInsn(int opcode, Label label)
    {
      super.visitJumpInsn(opcode, label);
      if (opcode == 198)
      {
        LogUtil.log("Ping", new Object[0]);
        this.mv.visitVarInsn(25, 1);
        this.mv.visitMethodInsn(182, Names.packetPlayerInfo.getName(), "b", "()L" + Names.packetPlayerInfo.getName() + "$a;", false);
        this.mv.visitVarInsn(25, 3);
        this.mv.visitMethodInsn(182, Names.packetPlayerInfo.getName() + "$b", "b", "()I", false);
        this.mv.visitVarInsn(25, 3);
        this.mv.visitMethodInsn(182, Names.packetPlayerInfo.getName() + "$b", "a", "()Lcom/mojang/authlib/GameProfile;", false);
        this.mv.visitMethodInsn(184, "BytecodeHook", "onPlayerInfo", "(Ljava/lang/Object;ILcom/mojang/authlib/GameProfile;)V", false);
      }
    }
  }
  
  public class PatchHandlePacketTitle
    extends MethodVisitor
  {
    private int returnCount;
    
    public PatchHandlePacketTitle(MethodVisitor methodVisitor)
    {
      super(methodVisitor);
    }
    
    public void visitInsn(int opcode)
    {
      if (opcode == 177)
      {
        this.returnCount += 1;
        if (this.returnCount == 1)
        {
          this.mv.visitInsn(1);
          this.mv.visitInsn(1);
          this.mv.visitMethodInsn(184, "BytecodeHook", "onTitle", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        }
        else if (this.returnCount == 2)
        {
          this.mv.visitVarInsn(25, 3);
          this.mv.visitVarInsn(25, 4);
          this.mv.visitMethodInsn(184, "BytecodeHook", "onTitle", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        }
      }
      super.visitInsn(opcode);
    }
  }
}
