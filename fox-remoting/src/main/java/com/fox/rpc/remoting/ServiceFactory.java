package com.fox.rpc.remoting;

import com.fox.rpc.SpiServiceLoader;
import com.fox.rpc.registry.RemotingServiceRegistry;
import com.fox.rpc.remoting.invoker.api.ServiceProxy;
import com.fox.rpc.remoting.invoker.config.InvokerConfig;
import com.fox.rpc.remoting.invoker.proxy.ServiceProxyLoader;
import com.fox.rpc.remoting.provider.api.Server;
import com.fox.rpc.remoting.provider.config.ProviderConfig;
import com.fox.rpc.remoting.provider.config.ServerConfig;
import com.fox.rpc.remoting.provider.process.RequestProcessor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Created by shenwenbo on 16/8/6.
 */
public class ServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceFactory.class);

    static ServiceProxy serviceProxy = ServiceProxyLoader.getServiceProxy();

    public static <T> T getService(InvokerConfig invokerConfig)  {
        return serviceProxy.getProxy(invokerConfig);
    }

    public static void addService(List<ProviderConfig<?>> providerConfigList) {
        publishService(providerConfigList);
        List<Server> servers = SpiServiceLoader.getExtensionList(Server.class);
        ServerConfig serverConfig=null;
        if (CollectionUtils.isNotEmpty(providerConfigList)) {
            serverConfig=providerConfigList.get(0).getServerConfig();
            for (Server server:servers) {
                RequestProcessor requestProcessor=null;
                if (!server.isStarted()) {
                    try {
                        requestProcessor=server.star(serverConfig);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //将服务添加到线程池服务处理器中;
                for (ProviderConfig config:providerConfigList) {
                    requestProcessor.addService(config);
                }

            }
        } else {
            LOGGER.error("serverConfig is  null");
        }
    }

    public static void  publishService(List<ProviderConfig<?>> providerConfigList){
        RemotingServiceRegistry remotingServiceRegistry=SpiServiceLoader.newExtension(RemotingServiceRegistry.class);
        if (providerConfigList != null) {
            for (ProviderConfig config:providerConfigList ) {
                remotingServiceRegistry.register(config.getServiceName(),config.getServerConfig().getIp()+":"+config.getServerConfig().getPort());
                LOGGER.debug("register service: {} => {}", config.getServiceName());
            }
        } else {
            LOGGER.error("register center fail");
        }
    }
}
