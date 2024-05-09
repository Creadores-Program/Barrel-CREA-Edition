/*
 *  ____                                 _
 * | __ )    __ _   _ __   _ __    ___  | |
 * |  _ \   / _` | | '__| | '__|  / _ \ | |
 * | |_) | | (_| | | |    | |    |  __/ | |
 * |____/   \__,_| |_|    |_|     \___| |_|
 *
 * Copyright (c) 2021 BarrelMC Team
 * This project is licensed under the MIT License
 */

package org.barrelmc.barrel;

import org.barrelmc.barrel.network.converter.BlockConverter;
import org.barrelmc.barrel.server.ProxyServer;
import org.barrelmc.barrel.utils.Logger;

public class Barrel {

    public static String DATA_PATH = System.getProperty("user.dir") + "/";

    public static void main(String[] args) {
        System.out.println("Starting Barrel Proxy software CREA Edition");
        BlockConverter.init();
        new ProxyServer(DATA_PATH, new Logger("§6BarrelMC"));
    }
}
