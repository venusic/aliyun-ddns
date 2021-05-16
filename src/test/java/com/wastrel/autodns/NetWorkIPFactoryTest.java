package com.wastrel.autodns;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;

class NetWorkIPFactoryTest {

    @org.junit.jupiter.api.Test
    void getCanUseIp() throws IOException {
        Config config = DemoListDomains.parseConfig("config-example.json");
        String useIp = NetWorkIPFactory.getCanUseIp(config.ipServers);
        Assertions.assertNotNull(useIp, "");
    }
}
