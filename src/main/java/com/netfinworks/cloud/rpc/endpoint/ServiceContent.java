package com.netfinworks.cloud.rpc.endpoint;

import com.netfinworks.cloud.rpc.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @param inf  rpc服务接口类
     */
    public static void addRpcService(String path, Class inf) {
        Map service = new HashMap();
        service.put(path, reflectMethodsAndParams(inf));
        log.debug("rpc service detail,{}", JSONUtil.toJson(service));
        content.rpcs.add(service);
    }

    /**
     * 获取接口类中的方法及参数信息
     *
     * @param inf 接口类
     * @return
     */
    private static Map reflectMethodsAndParams(Class inf) {
        Method[] methods = inf.getDeclaredMethods();
        Map nameParam = new HashMap<>();
        for (Method method : methods) {
            Class[] paramsTypes = method.getParameterTypes();
            List params = new ArrayList();
            for (Class paramType : paramsTypes) {
                try {
                    params.add(paramType.newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            nameParam.put(method.getName(), params);
        }
        return nameParam;
    }

    public List getRpcs() {
        return rpcs;
    }

    public void setRpcs(List rpcs) {
        this.rpcs = rpcs;
    }
}
