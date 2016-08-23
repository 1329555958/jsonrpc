package com.netfinworks.cloud.rpc.endpoint;

import com.netfinworks.cloud.rpc.JSONUtil;
import com.netfinworks.cloud.rpc.Util;
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
            List<String> paramNames = Util.getMethodParamNames(bean, method);

            for (int i = 0; i < paramsTypes.length; i++) {
                Map<String, Object> nameParam = new HashedMap();

                nameParam.put(paramNames.get(i), Util.getBeanByType(paramsTypes[i]));
                nameParams.add(nameParam);

            }
            nameMethod.put(method.getName(), nameParams);
        }
        return nameMethod;
    }

    public List getRpcs() {
        return rpcs;
    }

    public void setRpcs(List rpcs) {
        this.rpcs = rpcs;
    }
}
