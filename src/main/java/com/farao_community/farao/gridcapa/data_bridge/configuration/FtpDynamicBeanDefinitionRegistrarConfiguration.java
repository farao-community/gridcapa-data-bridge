package com.farao_community.farao.gridcapa.data_bridge.configuration;

import com.farao_community.farao.gridcapa.data_bridge.sources.FtpDynamicBeanDefinitionRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "data-bridge.sources.ftp", name = "active", havingValue = "true")
public class FtpDynamicBeanDefinitionRegistrarConfiguration {

    @Bean
    public FtpDynamicBeanDefinitionRegistrar beanDefinitionRegistrar(DataBridgeConfiguration dataBridgeConfiguration) {
        return new FtpDynamicBeanDefinitionRegistrar(dataBridgeConfiguration);
    }
}
