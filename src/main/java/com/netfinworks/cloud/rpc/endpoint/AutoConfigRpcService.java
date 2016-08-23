package com.netfinworks.cloud.rpc.endpoint;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author weichunhe
 *         Created on 2016/8/12.
 */
@Configuration
public class AutoConfigRpcService {

    @Bean
    public RpcServiceEndpoint serviceEndpoint(ApplicationContext context) {
        return new RpcServiceEndpoint(context);
    }
}
