package com.netfinworks.cloud.rpc.endpoint;

import com.netfinworks.cloud.rpc.JSONUtil;
import com.netfinworks.cloud.rpc.Util;
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

/**
 * @author weichunhe
 *         Created on 2016/8/12.
 *         服务内容
 */
public class ServiceContent {
    private static Logger log = LoggerFactory.getLogger(ServiceContent.class);
    //rpc 服务列表
    private List rpcs = new ArrayList();
    // 自定义的http服务列表
    private Map<String, List<Map>> https;

    private ApplicationContext context;

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

    //-------------------------------http--------------------------------------------------

    protected void extractMethodMappings(ApplicationContext applicationContext,
                                         Map<String, List<Map>> result) {
        if (applicationContext != null) {
            for (Map.Entry<String, AbstractHandlerMethodMapping> bean : applicationContext
                    .getBeansOfType(AbstractHandlerMethodMapping.class).entrySet()) {
                @SuppressWarnings("unchecked")
                Map<?, HandlerMethod> methods = bean.getValue().getHandlerMethods();
                if (!Util.CUSTOM_URL_MAPPING_TYPE.equals(bean.getKey())) {
                    continue;
                }
                for (Map.Entry<?, HandlerMethod> method : methods.entrySet()) {
                    RequestMappingInfo info = (RequestMappingInfo) method.getKey();
                    Map<String, Object> methodMap = new HashedMap();
                    Map<String, Object> value = new LinkedHashMap<String, Object>();

                    methodMap.put("urls", info.getPatternsCondition().getPatterns());
                    methodMap.put("methods", info.getMethodsCondition().getMethods());
                    List<String> paramNames = Arrays.asList(Util.ParameterNameDiscoverer.getParameterNames(method.getValue().getMethod()));
                    List<Class<?>> paramTypes = Arrays.asList(method.getValue().getMethod().getParameterTypes());

                    for (int i = 0; i < paramNames.size(); i++) {
                        //过滤掉request response
                        if (ServletResponse.class.isAssignableFrom(paramTypes.get(i)) || ServletRequest.class.isAssignableFrom(paramTypes.get(i))) {
                            continue;
                        }
                        value.put(paramNames.get(i), Util.getBeanByType(paramTypes.get(i)));
                    }
                    methodMap.put("params", value);
                    Map map = new HashedMap();
                    map.put(Util.genMethodDesc(method.getValue().getMethod(), paramNames), methodMap);

                    addMethod(result, method.getValue().getBeanType(), map);
                }
            }
        }
    }

    /**
     * 按照Controller归类,Controller 类型作为key，value是map(方法描述作为key,参数信息作为value)
     *
     * @param result
     * @param beanType
     * @param method
     */
    private void addMethod(Map<String, List<Map>> result, Class beanType, Map method) {
        List<Map> methods = result.get(beanType.getName());
        if (methods == null) {
            methods = new ArrayList<Map>();
            result.put(beanType.getName(), methods);
        }

        methods.add(method);
    }


    public List getRpcs() {
        return rpcs;
    }

    public void setRpcs(List rpcs) {
        this.rpcs = rpcs;
    }

    public Map<String, List<Map>> getHttps() {
        https = new LinkedHashMap<String, List<Map>>();
        extractMethodMappings(this.context, https);
        return https;
    }

    public void setHttps(Map<String, List<Map>> https) {
        this.https = https;
    }

    public void setContext(ApplicationContext context) {
        this.context = context;
    }
}
