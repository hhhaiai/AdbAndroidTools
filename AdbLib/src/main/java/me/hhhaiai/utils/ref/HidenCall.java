package me.hhhaiai.utils.ref;


import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 最小剪辑版黑灰名单避免方案.支持JDK 7、8编译的环境.
 * 用法使用一个接口方案: MinRefUtils.unseal(Context context)
 */
public final class HidenCall {
    private static final String TAG = "sanbo." + HidenCall.class.getSimpleName();
    // 对应 Executable.artMethod 等价于 JNI artmethod 指针
    private static long methodOffset;
    // 方法数对应，与JNI Class uint64_t methods_;
    private static long methodsOffset;
    // art方法的大小
    private static long artMethodSize;
    private static long artMethodBias;
    private static Field artMethod = null;
    //确保只注册一次
    private static boolean isInit = false;

    static {
        try {
            if (Build.VERSION.SDK_INT > 27) {
                if (hasArtMethod()) {
                    // 这个也涉及浅灰名单。
                    methodOffset = Unsafe.objectFieldOffset(getField(Method.class, "artMethod"));
                    //Class.java methods
                    // android 7+版本才有这个变量
                    methodsOffset = Unsafe.objectFieldOffset(getField(HideCall.class, "methods"));

                    Method mA = getMethod(HideCall.class, "fun1");
                    Method mB = getMethod(HideCall.class, "fun2");
                    long aAddr = getMethodAddress(mA);
                    long bAddr = getMethodAddress(mB);
                    long aMethods = Unsafe.getLong(HideCall.class, methodsOffset);
                    artMethodSize = bAddr - aAddr;
                    artMethodBias = aAddr - aMethods - artMethodSize;

                    i("获取所有参数结束,详情如下: "
                            + "\r\n    methodOffset (Executable artMethod):              " + methodOffset
                            + "\r\n    methodsOffset (Class  long methods):              " + methodsOffset
                            + "\r\n    artMethodSize (addressA - addressB):              " + artMethodSize
                            + "\r\n    aMethods                           :              " + aMethods
                            + "\r\n    artMethodBias (addressA-aMethods-artMethodSize):  " + artMethodBias
                    );

                    exemptAll();
                } else {
                    e("static{} artMethod is null!");
                }
            }


        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    static boolean checkArgsForInvokeMethod(Class[] params, Object[] args) {
        if (params.length != args.length) return false;
        for (int i = 0; i < params.length; ++i) {
            if (params[i].isPrimitive()) {
                if (params[i] == int.class && !(args[i] instanceof Integer)) return false;
                else if (params[i] == byte.class && !(args[i] instanceof Byte)) return false;
                else if (params[i] == char.class && !(args[i] instanceof Character)) return false;
                else if (params[i] == boolean.class && !(args[i] instanceof Boolean)) return false;
                else if (params[i] == double.class && !(args[i] instanceof Double)) return false;
                else if (params[i] == float.class && !(args[i] instanceof Float)) return false;
                else if (params[i] == long.class && !(args[i] instanceof Long)) return false;
                else if (params[i] == short.class && !(args[i] instanceof Short)) return false;
            } else if (args[i] != null && !params[i].isInstance(args[i])) return false;
        }
        return true;
    }


    /**
     * invoke a restrict method named {@code methodName} of the given class {@code clazz} with this object {@code thiz} and arguments {@code args}
     *
     * @param clazz      the class call the method on (this parameter is required because this method cannot call inherit method)
     * @param thiz       this object, which can be {@code null} if the target method is static
     * @param methodName the method name
     * @param args       arguments to call the method with name {@code methodName}
     * @return the return value of the method
     * @see Method#invoke(Object, Object...)
     */
    public static Object invoke(Class<?> clazz, Object thiz, String methodName, Object... args) throws Exception {
        if (thiz != null && !clazz.isInstance(thiz)) {
            throw new IllegalArgumentException("this object is not an instance of the given class");
        }
        //实际上如果只是为了简单调用这个函数，完全可以直接随便找一个 Method 并用 Unsafe 去设置它的 artMethod，然后直接调用 Method.invoke 即可。
        // 因为从 Method.invoke 的 native 代码可以看到，调用过程仅使用 Method 的 artMethod 成员。
        Method stub = HideCall.class.getDeclaredMethod("invoke", Object[].class);
        stub.setAccessible(true);
        long methods = Unsafe.getLong(clazz, methodsOffset);
        if (methods == 0) {
            throw new NoSuchMethodException("(invoke)Cannot find matching method");
        }
        int numMethods = Unsafe.getInt(methods);
        d("[invoke] " + clazz.getName() + " " + methodName + "()  has " + numMethods + " methods");
        for (int i = 0; i < numMethods; i++) {
            //第I个元素  base + i * size  + 偏差值？
            long method = methods + i * artMethodSize + artMethodBias;
            Unsafe.putLong(stub, methodOffset, method);
            //////////Just for long begin////////////
//            debugLog(clazz, stub);
            //////////Just for long end////////////
            if (methodName.equals(stub.getName())) {
                Class<?>[] params = stub.getParameterTypes();
                if (checkArgsForInvokeMethod(params, args))
                    return stub.invoke(thiz, args);
            }
        }
        throw new NoSuchMethodException("invoke Cannot find matching method");
    }

//    private static void debugLog(Class<?> clazz, Method stub) {
//        StringBuffer sb = new StringBuffer();
//        sb.append("[invoke获取方法]  ").append(clazz.getName()).append(".").append(stub.getName());
//        Class<?>[] parameterTypes = stub.getParameterTypes();
//        if (parameterTypes != null && parameterTypes.length > 0) {
//            sb.append(java.util.Arrays.asList(parameterTypes).toString());
//        }
//        v(sb.toString());
//    }


    public static boolean setHiddenApiExemptions(String... signaturePrefixes) {
        try {
            Object runtime = invoke(Class.forName("dalvik.system.VMRuntime"), null, "getRuntime");
            invoke(Class.forName("dalvik.system.VMRuntime"), runtime, "setHiddenApiExemptions", (Object) signaturePrefixes);
            return true;
        } catch (Throwable e) {
            e("setHiddenApiExemptions\r\n" + Log.getStackTraceString(e));
            return false;
        }
    }

    public static int unseal() {
        if (Build.VERSION.SDK_INT < 28) {
            // Below Android P, ignore
            return 0;
        }

        // try exempt API first.
        if (exemptAll()) {
            return 0;
        }
        return -1;
    }


    private static boolean exemptAll() {
        if (!isInit) {
            isInit = setHiddenApiExemptions(new String[]{
                    "L"
//                , "Landroid/" "Lcom/", "Ljava/", "Ldalvik/", "Llibcore/", "Lsun/", "Lhuawei/"
            });
        }
        return isInit;
    }


    /**
     * 内部类仅用于内部替换
     */
    static final class HideCall {
        private transient int accessFlags; //
        private transient int classFlags; //
        private transient ClassLoader classLoader; //
        private transient int classSize; //
        private transient int clinitThreadId; //
        private transient Class<?> componentType; //
        private transient short copiedMethodsOffset; //
        private transient Object dexCache; //
        private transient int dexClassDefIndex; //
        private volatile transient int dexTypeIndex;//
        // 方法原型：private transient ClassExt extData;
        private transient Object extData; //
        private transient long iFields; //
        private transient Object[] ifTable; //
        private transient long methods; //
        private transient String name;//
        private transient int numReferenceInstanceFields; //
        private transient int numReferenceStaticFields; //
        private transient int objectSize; //
        private transient int objectSizeAllocFastPath; //
        private transient int primitiveType; //
        private transient int referenceInstanceOffsets; //
        private transient long sFields; //
        private transient int status; //
        private transient Class<?> superClass; //
        private transient short virtualMethodsOffset; //
        private transient Object vtable;//

        private static void fun1() {
        }

        private static void fun2() {
        }

        private static Object invoke(Object... args) {
            throw new IllegalStateException("Failed to invoke the method");
        }

        private HideCall(Object... args) {
            throw new IllegalStateException("Failed to new a instance");
        }
    }

    static class Unsafe {

        private static Object unsafeObj;
        private static Class unsafeClass;

        public static boolean isOK() {
            return unsafeObj != null && unsafeClass != null;
        }

        static {
            try {
                if (unsafeClass == null) {
                    unsafeClass = Class.forName("sun.misc.Unsafe");
                }
                //private static final Unsafe theUnsafe = THE_ONE;
                unsafeObj = getFieldValue(unsafeClass, "theUnsafe", null);
                if (unsafeObj == null) {
                    //private static final Unsafe THE_ONE = new Unsafe();
                    unsafeObj = getFieldValue(unsafeClass, "THE_ONE", null);
                }
                if (unsafeObj == null) {
                    unsafeObj = call(unsafeClass, "getUnsafe", null, null, null);
                }
            } catch (Throwable e) {
                e(e);
            }
        }

        public static long objectFieldOffset(Field field) {
            if (field == null) {
                return 0L;
            }
            Object result = call(unsafeClass, "objectFieldOffset", unsafeObj, new Class[]{Field.class}, new Object[]{field});
            return result != null && (result.getClass() == Long.class || result.getClass() == long.class) ? ((Long) result).longValue() : 0L;
        }

        public static long getLong(Object obj, long offset) {
            Object result = call(unsafeClass, "getLongVolatile", unsafeObj, new Class[]{Object.class, long.class}, new Object[]{obj, offset});
            if (result == null) {
                result = call(unsafeClass, "getLong", unsafeObj, new Class[]{Object.class, long.class}, new Object[]{obj, offset});
            }
            return result != null && (result.getClass() == Long.class || result.getClass() == long.class) ? ((Long) result).longValue() : 0L;
        }


        public static void putLong(Object obj, long offset, long newValue) {
            try {
                Method method = getMethod(unsafeClass, "putLongVolatile", Object.class, long.class, long.class);
                if (method == null) {
                    method = getMethod(unsafeClass, "putLong", Object.class, long.class, long.class);
                }
                if (method != null) {
                    method.setAccessible(true);
                    method.invoke(unsafeObj, obj, offset, newValue);
                }
            } catch (Throwable e) {
                e(e);
            }
        }

        public static int getInt(long offset) {
            Object result = call(unsafeClass, "getInt", unsafeObj, new Class[]{long.class}, new Object[]{offset});
            return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
        }


        public static int arrayIndexScale(Class clazz) {
            Object result = call(unsafeClass, "arrayIndexScale", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
            if (result == null) {
                if (Build.VERSION.SDK_INT > 20) {
                    result = call(unsafeClass, "getArrayIndexScaleForComponentType", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
                } else {
                    // 4.x
                    result = call(unsafeClass, "arrayIndexScale0", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
                }
            }
            return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
        }

        public static int arrayBaseOffset(Class clazz) {
            Object result = call(unsafeClass, "arrayBaseOffset", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
            if (result == null) {
                result = call(unsafeClass, "getArrayBaseOffsetForComponentType", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
            }
            return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
        }

        public static int getInt(Object obj, long offset) {
            Object result = call(unsafeClass, "getIntVolatile", unsafeObj, new Class[]{Object.class, long.class}, new Object[]{obj, offset});
            if (result == null) {
                result = call(unsafeClass, "getInt", unsafeObj, new Class[]{Object.class, long.class}, new Object[]{obj, offset});
            }
            return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
        }


        public static long toAddress(Object obj) {
            try {
                Object[] array = new Object[]{obj};
                //返回数组中一个元素占用的大小
                if (arrayIndexScale(Object[].class) == 8) {
                    return getLong(array, arrayBaseOffset(Object[].class));
                } else {
                    return 0xffffffffL & getInt(array, arrayBaseOffset(Object[].class));
                }
            } catch (Throwable e) {
                e(e);
            }
            return 0L;
        }
    }



    public static long getMethodAddress(Method method) {
        try {
            if (method == null) {
                return 0L;
            }
            if (hasArtMethod()) {
                Object mirrorMethod = getFieldValue(artMethod, method);
                if (mirrorMethod.getClass().equals(Long.class)) {
                    return (Long) mirrorMethod;
                }
                return Unsafe.toAddress(mirrorMethod);
            }
        } catch (Throwable e) {
            e(e);
        }
        return 0L;
    }

    //java.lang.reflect.AccessibleObject
    //  安卓4.0.1 ---> 变量private int slot;
    //  安卓4.0.2 ---> 变量private int slot;
    //  安卓4.0.3 ---> 变量private int slot;
    //  安卓4.0.4 ---> 变量private int slot;
    //  安卓4.1.1 ---> 变量private int slot;
    //  安卓4.1.2 ---> 变量private int slot;
    //  安卓4.2 ---> 变量private int slot;
    //  安卓4.3 ---> 变量private int slot;
    // java.lang.reflect.AbstractMethod
    //  安卓4.4 ---> 变量protected final ArtMethod artMethod; ArtMethod中包含很多其他变量
    //  安卓5.0 ---> 变量protected final ArtMethod artMethod; ArtMethod中包含很多其他变量
    //  安卓5.1 ---> 变量protected final ArtMethod artMethod; ArtMethod中包含很多其他变量
    //  安卓6 ---> 变量protected long artMethod; 包含很多其他变量
    //  安卓7 ---> 变量protected long artMethod; 包含很多其他变量  {libcore/libart/src/main/java/java/lang/reflect/AbstractMethod.java}
    // java.lang.reflect.Executable-->变量 protected long artMethod; 包含很对变量
    // android 8 ---> 变量  private long artMethod;
    // android 9 ---> 变量  private long artMethod;
    // android 10 ---> 变量  private long artMethod;
    // android 11 ---> 变量  private long artMethod;
    // android 12 ---> 变量  private long artMethod;
    // android 13 ---> 变量  private long artMethod;
    private static boolean hasArtMethod() {
        if (artMethod == null) {
            artMethod = getField(Method.class, "artMethod");
        }
        return artMethod != null;
    }

    public static Object getFieldValue(Field filed, Object instance) {
        try {
            if (filed == null) {
                return null;
            }
            if (!filed.isAccessible()) {
                filed.setAccessible(true);
            }
            return filed.get(instance);
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    public static Object getFieldValue(Class<?> clazz, String fieldName, Object instance) {
        try {
            Field addr = getField(clazz, fieldName);
            if (addr != null) {
                return addr.get(instance);
            }
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    public static Object call(Class<?> clazz, String methodName, Object receiver, Class[]
            types, Object[] params) {
        if (clazz == null || TextUtils.isEmpty(methodName)) {
            return null;
        }
        try {
            if (types == null || params == null) {
                Method method = getMethod(clazz, methodName);
                if (method != null) {
                    return method.invoke(receiver);
                }
            } else {
                Method method = getMethod(clazz, methodName, types);
                if (method != null) {
                    return method.invoke(receiver, params);
                }
            }

        } catch (Throwable throwable) {
            e(throwable);
        }
        return null;
    }

    public static Field getField(Class topClass, String fieldName) {
        Field field = null;
        while (topClass != null && topClass != Object.class) {
            try {
                field = topClass.getDeclaredField(fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (Exception e) {
            }
            topClass = topClass.getSuperclass();
        }
        return field;
    }

    public static Method getMethod(Class<?> topClass, String methodName, Class<?>... types) {
        if (topClass == null || TextUtils.isEmpty(methodName)) {
            return null;
        }
        Method method = null;
        while (topClass != Object.class) {
            try {
                method = topClass.getDeclaredMethod(methodName, types);

                if (method != null) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (Throwable e) {
            }
            topClass = topClass.getSuperclass();
        }
        return method;
    }

    private static void v(String s) {
        Log.println(Log.VERBOSE, TAG, s);
    }

    private static void d(String s) {
        Log.println(Log.DEBUG, TAG, s);
    }

    private static void i(String s) {
        Log.println(Log.INFO, TAG, s);
    }

    private static void e(String s) {
        Log.println(Log.ERROR, TAG, s);
    }

    private static void e(Throwable e) {
        Log.println(Log.ERROR, TAG, Log.getStackTraceString(e));
    }

}
