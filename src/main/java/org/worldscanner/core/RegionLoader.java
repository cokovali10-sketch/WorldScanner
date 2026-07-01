package org.worldscanner.core;

import java.io.File;

public class RegionLoader {

    public static void load(String path) {
        File regionFile = new File(path);
        if (!regionFile.exists()) {
            throw new IllegalArgumentException("Region file does not exist: " + path);
        }
        System.out.println("Region file detected: " + regionFile.getName());
        System.out.println("File size: " + regionFile.length() + " bytes");
    }

}