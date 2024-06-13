/*
 * Copyright (c) 2021 BarrelMC Team
 * This project is licensed under the MIT License
 */

package org.barrelmc.barrel.config;

import lombok.Getter;
import lombok.Setter;

public class Config {

    @Setter
    @Getter
    public String bindAddress;

    @Setter
    @Getter
    public Integer port;

    @Setter
    @Getter
    public String motd;

    @Setter
    @Getter
    public String bedrockAddress;

    @Setter
    @Getter
    public Integer bedrockPort;

    @Setter
    @Getter
    public String auth;
    
    @Setter
    @Getter
    public Integer maxplayers;

    @Setter
    @Getter
    public String icon;
    
    @Setter
    @Getter
    public String shutdownMessage;
    
    @Setter
    @Getter
    public Boolean premiumPlayerJava;

    @Setter
    @Getter
    public Boolean useJavaId;

    @Setter
    @Getter
    public Integer configVersion;
}
