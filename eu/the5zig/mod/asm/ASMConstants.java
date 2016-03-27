package eu.the5zig.mod.asm;

import org.objectweb.asm.MethodVisitor;

public class ASMConstants
{
  public static void addSystemOut(MethodVisitor mv, String message)
  {
    mv.visitFieldInsn(178, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitLdcInsn(message);
    mv.visitMethodInsn(182, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
  }
}
