package com.netfinworks.cloud.rpc.spring;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImplExporter;
import com.netfinworks.cloud.rpc.RpcService;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation goes on the <em>implementation</em> of the JSON-RPC service.  It defines any additional paths on
 * which the JSON-RPC service should be exported.  This can be used with the {@link AutoJsonRpcServiceImplExporter}
 * in order to automatically expose the JSON-RPC services in a spring based web application server.  Note that the
 * implementation should still continue to carry the {@link com.googlecode.jsonrpc4j.JsonRpcServer} annotation;
 * preferably on the service interface.
 */

@Target(TYPE)
@Retention(RUNTIME)
public @interface AutoRpcServiceImpl {

    /**
     * This value may contain a list of <em>additional</em> paths that the JSON-RPC service will be exposed on.
     * These are in addition to any which are defined on the {@link RpcService}
     * annotation preferably on the service interface.  This might be used, for example, where you still want
     * to expose a service on legacy paths for older clients.
     *
     * @return an array of additional paths
     */

    String[] additionalPaths() default {};

}
