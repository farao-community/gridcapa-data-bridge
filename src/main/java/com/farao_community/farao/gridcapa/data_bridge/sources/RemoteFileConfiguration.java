package com.farao_community.farao.gridcapa.data_bridge.sources;

import com.farao_community.farao.gridcapa.data_bridge.model.DataBridge;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
@ConfigurationProperties("data-bridge")
@ConstructorBinding
@Data
public class RemoteFileConfiguration {
    private List<DataBridge> bridges;
    private String zoneId;

    @PostConstruct
    public void setZoneIdForBridge() {
        bridges.stream().forEach(b -> b.setZoneId(zoneId));
    }

}

