package com.netfinworks.cloud.rpc.endpoint;

import com.netfinworks.cloud.rpc.JSONUtil;
import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
                try {
                    nameParam.put(paramNames.get(i), paramsTypes[i].newInstance());
                    nameParams.add(nameParam);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            nameMethod.put(method.getName(), nameParams);
        }
        return nameMethod;
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

    public List getRpcs() {
        return rpcs;
    }

    public void setRpcs(List rpcs) {
        this.rpcs = rpcs;
    }
}
