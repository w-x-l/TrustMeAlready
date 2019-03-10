package com.virb3.trustmealready;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedHelpers.*;

public class Main implements IXposedHookZygoteInit {

    private static final String SSL_CLASS_NAME = "com.android.org.conscrypt.TrustManagerImpl";
    private static final String SSL_METHOD_NAME = "checkTrustedRecursive";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("TrustMeAlready loaded!");

        for (Method method : findClass(SSL_CLASS_NAME, null).getDeclaredMethods()) {
            if (!checkSSLMethod(method)) {
                continue;
            }

            List<Object> params = new ArrayList<>();
            params.addAll(Arrays.asList(method.getParameterTypes()));
            params.add(new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return new ArrayList<X509Certificate>();
                }
            });

            XposedBridge.log("Hooking method:\n" + method.toString());
            findAndHookMethod(SSL_CLASS_NAME, null, SSL_METHOD_NAME, params.toArray());
        }
    }

    private boolean checkSSLMethod(Method method) {
        if (!method.getName().equals(SSL_METHOD_NAME)) {
            return false;
        }

        Type returnType = method.getGenericReturnType();
        if (!(returnType instanceof ParameterizedType)) {
            return false;
        }

        Type[] args = ((ParameterizedType) returnType).getActualTypeArguments();
        if (args.length != 1 || args[0] instanceof X509Certificate) {
            return false;
        }

        return true;
    }
}