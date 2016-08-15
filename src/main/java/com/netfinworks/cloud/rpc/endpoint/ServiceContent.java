package com.netfinworks.cloud.rpc.endpoint;

import com.netfinworks.cloud.rpc.JSONUtil;
import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author weichunhe
 *         Created on 2016/8/12.
 *         服务内容
 */
public class ServiceContent {
    private static Logger log = LoggerFactory.getLogger(ServiceContent.class);
    //rpc 服务列表
    private List rpcs = new ArrayList();

    private static ServiceContent content = new ServiceContent();

    private ServiceContent() {
    }

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

    public static ServiceContent newInstance() {
        return content;
    }

    /**
     * 添加rpc服务
     *
     * @param path rpc服务对应的路径
     * @param bean rpc服务实现类
     * @param inf  rpc服务接口类
     */
    public static void addRpcService(String path, Class bean, Class inf) {
        Map service = new HashMap();
        service.put(path, reflectMethodsAndParams(bean, inf));
        log.debug("rpc service detail,{}", JSONUtil.toJson(service));
        content.rpcs.add(service);
    }

    /**
     * 获取接口类中的方法及参数信息
     *
     * @param bean rpc服务实现类
     * @param inf  接口类
     * @return
     */
    private static Map reflectMethodsAndParams(Class bean, Class inf) {
        Method[] methods = inf.getDeclaredMethods();
        Map<String, List> nameMethod = new HashMap<>();
        for (Method method : methods) {
            Class[] paramsTypes = method.getParameterTypes();
            List<Map<String, Object>> nameParams = new ArrayList<>();
            List<String> paramNames = getMethodParamNames(bean, method);

            for (int i = 0; i < paramsTypes.length; i++) {
                Map<String, Object> nameParam = new HashedMap();
                Object param = null;
                try {
                    param = infBeanMap.get(paramsTypes[i]);
                    if (param == null) {
                        param = paramsTypes[i].newInstance();
                    }
                } catch (Exception e) {
                    log.error("can't newInstance of {}", paramsTypes[i].getClass().getName(), e);
                }
                nameParam.put(paramNames.get(i), param);
                nameParams.add(nameParam);

            }
            nameMethod.put(method.getName(), nameParams);
        }
        return nameMethod;
    }

    //生成方法的描述信息
    private static String getMethodDesc(Method method, List<String> paramNames) {
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
     * 获取方法的参数名称列表
     *
     * @param method
     * @return
     */
    private static List<String> getMethodParamNames(Class bean, Method method) {
        try {
            method = bean.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] paramNameArr = u.getParameterNames(method);
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

    public List getRpcs() {
        return rpcs;
    }

    public void setRpcs(List rpcs) {
        this.rpcs = rpcs;
    }
}
