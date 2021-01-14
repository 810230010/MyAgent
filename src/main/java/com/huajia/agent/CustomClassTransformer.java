package com.huajia.agent;

import com.huajia.common.Spy;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

// 用来打印方法调用时长
public class CustomClassTransformer implements ClassFileTransformer {
    private String targetPackage;

    public CustomClassTransformer(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        System.out.println(className);

        if(StringUtils.isEmpty(className) || StringUtils.isEmpty(targetPackage)) {
            System.out.println("className或者targetPackage为空");
            return classfileBuffer;
        }

        final ClassPool classPool = ClassPool.getDefault();
        try {
            className = className.replace("/", ".");

            final CtClass ctClass = classPool.get(className);
            String packageName = ctClass.getPackageName();
            String replace = packageName.replace("/", ".");
            if(!replace.startsWith(targetPackage)) return classfileBuffer;

            CtMethod[] methods = ctClass.getDeclaredMethods();
            for(CtMethod oldMethod : methods) {
                if(oldMethod.getAnnotation(Spy.class) == null) continue;

                CtMethod delegatedMethod = CtNewMethod.copy(oldMethod, ctClass, null);
                String oldMethodName = oldMethod.getName() + "$agent";
                oldMethod.setName(oldMethodName);

                if(oldMethod.getReturnType().equals(CtClass.voidType)) {
                    delegatedMethod.setBody(String.format(voidSource, delegatedMethod.getName()));
                }else {
                    delegatedMethod.setBody(String.format(source, delegatedMethod.getName()));
                }
                System.out.println("add delegate method");
                ctClass.addMethod(delegatedMethod);
            }
            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classfileBuffer;
    }

    //有返回值得方法
    final static String source = "{ long begin = System.currentTimeMillis();\n" +
            "        Object result;\n" +
            "        try {\n" +
            "            result = ($w)%s$agent($$);\n" + //s% 将参数传递到下一个方法，然后使用 s% 传递的参数进行替换操作, $w 表示的是在进行return的时候会强制的进行类型转换
            "        } finally {\n" +
            "            long end = System.currentTimeMillis();\n" +
            "            System.out.println(end - begin);\n" +
            "        }\n" +
            "        return ($r) result;}";

    //没有返回值的方法
    final static String voidSource = "{long begin = System.currentTimeMillis();\n" +
            "        try {\n" +
            "            %s$agent($$);\n" +
            "        } finally {\n" +
            "            long end = System.currentTimeMillis();\n" +
            "            System.out.println(end - begin);\n" +
            "        }}";
}
