package com.farao_community.farao.gridcapa.data_bridge.configuration;

public record FtpTimeouts(int dataTimeout, int defaultTimeout, int connectTimeout, long sessionWaitTimeout) {
}
