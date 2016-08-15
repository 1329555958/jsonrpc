package com.netfinworks.cloud.rpc.endpoint;

import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author weichunhe
 *         Created on 2016/8/12.
 */
public class RpcServiceEndpoint extends AbstractEndpoint<ServiceContent> {

    public RpcServiceEndpoint() {
        super("jsonrpc");
    }

    @Override
    public ServiceContent invoke() {
        return ServiceContent.newInstance();
    }

}
