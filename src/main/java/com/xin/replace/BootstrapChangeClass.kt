package com.xin.replace

import com.xin.StoreInfo
import com.xin.StoreInfo.commonClassLoader
import com.xin.base.GeneralClassAdapter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.lang.instrument.Instrumentation

/**
 *
 * @author linxixin@cvte.com
 */
fun handleClassLoader(name: String, classLoader: ClassLoader, parent: ClassLoader) {
    if (name == "common") {
        commonClassLoader = classLoader
        WebappLoaderChangeClass(StoreInfo.instrumentation, classLoader)
    }
}

class BootstrapChangeClass(instrumentation: Instrumentation, classLoader: ClassLoader) : BaseChangeClass(instrumentation, classLoader) {

    override fun getGeneralClassAdapter(): GeneralClassAdapter {
        return object : GeneralClassAdapter() {
            override fun visitMethod(access: Int, name: String, desc: String,
                                     signature: String, exceptions: Array<String>): MethodVisitor {
                val mv = cv.visitMethod(access, name, desc, signature, exceptions)

                // 当是sayName方法是做对应的修改
                return if (name == "createClassLoader" && desc == "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/ClassLoader;") {
                    ChangeMethodAdapter(mv)
                } else {
                    mv
                }
            }

            // 定义一个自己的方法访问类
            internal inner class ChangeMethodAdapter(mv: MethodVisitor) : MethodVisitor(ASM6, mv) {

                override fun visitInsn(opcode: Int) {
                    if (opcode == ARETURN) {
                        mv.visitVarInsn(ASTORE, 6)
                        mv.visitVarInsn(ALOAD, 1)
                        mv.visitVarInsn(ALOAD, 6)
                        mv.visitVarInsn(ALOAD, 2)
                        mv.visitMethodInsn(INVOKESTATIC, BootstrapChangeClass::class.java.name.replace(".", "/"), "handleClassLoader", "(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/ClassLoader;)V", false)
                        mv.visitVarInsn(ALOAD, 6)
                    }
                    mv.visitInsn(opcode)
                }
            }
        }
    }

    override fun getClassName(): String {
        return "org.apache.catalina.startup.Bootstrap"
    }

    override fun toString(): String {
        return "${getClassName()} 替换类"
    }
}
