package com.netfinworks.cloud.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

public class Util {
    private static Logger log = LoggerFactory.getLogger(Util.class);

    /**
     * 自定义的URL映射类型
     */
    public static final String CUSTOM_URL_MAPPING_TYPE = "requestMappingHandlerMapping";
    /**
     * rpc 服务的地址前缀
     */
    public static final String RPC_PATH_PREFIX = "/jsonrpc/";

    public static LocalVariableTableParameterNameDiscoverer ParameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    //接口实例化时对应的实现类
    private static Map<Class, Object> infBeanMap = new HashMap<>();

    //添加基本类型
    static {
        infBeanMap.put(int.class, 0);
        infBeanMap.put(float.class, 0);
        infBeanMap.put(double.class, 0);
        infBeanMap.put(boolean.class, false);
        infBeanMap.put(short.class, 0);
        infBeanMap.put(long.class, 0);
        infBeanMap.put(char.class, ' ');
        infBeanMap.put(byte.class, 0);
        infBeanMap.put(List.class, new ArrayList<>());
        infBeanMap.put(Map.class, new HashMap<>());
        infBeanMap.put(Set.class, new HashSet<>());
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String DEFAULT_HOSTNAME = "0.0.0.0";

    static boolean hasNonNullObjectData(final ObjectNode node, final String key) {
        return hasNonNullData(node, key) && node.get(key).isObject();
    }

    static boolean hasNonNullData(final ObjectNode node, final String key) {
        return node.has(key) && !node.get(key).isNull();
    }

    static boolean hasNonNullTextualData(final ObjectNode node, final String key) {
        return hasNonNullData(node, key) && node.get(key).isTextual();
    }

    /**
     * 添加rpc服务地址前缀，并去掉重复的/
     *
     * @param path
     * @return
     */
    public static String addPrefixAndDistinct(String path) {
        return RPC_PATH_PREFIX.concat(path).replaceAll("/+", "/");
    }

    /**
     * 将完整的类路径转换成路径 即.变成/
     *
     * @param className 完整名称
     * @return 转换后的路径
     */
    public static String className2Path(String className) {
        return className.replaceAll("\\.", "/");
    }

    /**
     * 获取方法的参数名称列表
     *
     * @param method
     * @return
     */
    public static List<String> getMethodParamNames(Class bean, Method method) {
        try {
            method = bean.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        String[] paramNameArr = ParameterNameDiscoverer.getParameterNames(method);
        if (paramNameArr != null) {
            return Arrays.asList(paramNameArr);
        }
        List<String> paramNames = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        Class[] params = method.getParameterTypes();
        for (Parameter param : parameters) {
            paramNames.add(param.getName());
        }
        return paramNames;
    }

    /**
     * 生成方法的描述信息
     */
    public static String genMethodDesc(Method method, List<String> paramNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        sb.append("(");
        Class[] paramsTypes = method.getParameterTypes();
        for (int i = 0; i < paramsTypes.length; i++) {
            sb.append(paramsTypes[i].getSimpleName());
            sb.append(" ");
            sb.append(paramNames.get(i));
            sb.append(",");
        }
        //去掉最后一个逗号
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 根据类型获取对应的bean
     *
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T getBeanByType(Class<T> type) {
        Object param = null;
        try {
            param = infBeanMap.get(type);
            if (param == null) {
                param = type.newInstance();
            }
            return (T) param;
        } catch (Exception e) {
            log.error("can't newInstance of {}", type.getClass().getName(), e);
        }
        return null;
    }

    /**
     * 添加接口对应的实现Bean，获取参数属性信息时需要实例化参数
     *
     * @param inf
     * @param bean
     */
    public static void addBean4Inf(Class inf, Object bean) {
        infBeanMap.put(inf, bean);
    }

    /**
     * 获取接口对应的实现类的bean类型
     *
     * @param inf
     * @return
     */
    public static Class getBeanClassOfInf(Type inf) {
        Object bean = infBeanMap.get(inf);
        if (bean != null) {
            return bean.getClass();
        }
        return null;
    }


    public static void main(String[] args) {
        Class t = HttpServletResponse.class;

        System.out.println(ServletResponse.class.isAssignableFrom(ServletResponse.class));

    }
}
