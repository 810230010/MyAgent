package com.huajia.agent;

import java.lang.instrument.Instrumentation;

public class MethodAgent {
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new CustomClassTransformer(agentArgs));
    }
}
