package com.automq.s3shell.sdk.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Properties;

public class S3PropUtil {
    public static final String BROKER_PROPS_PATH = "template/broker.properties";
    public static final String CONTROLLER_PROPS_PATH = "template/controller.properties";
    public static final String SERVER_PROPS_PATH = "template/server.properties";

    public static void persist(Properties props, String fileName) throws IOException {
        File directory = new File("generated");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Can't create directory " + directory.getAbsolutePath());
        }

        String targetPath = "generated/" + fileName;
        File file = new File(targetPath);
        try (PrintWriter pw = new PrintWriter(file, Charset.forName("utf-8"))) {
            for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                pw.println(key + "=" + props.getProperty(key));
            }
        }
    }

    public static Properties loadTemplateProps(String propsPath) throws IOException {
        try (var in = S3PropUtil.class.getClassLoader().getResourceAsStream(propsPath)) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                return props;
            } else {
                throw new IOException(String.format("Can not find resource file under path: %s", propsPath));
            }
        }
    }
}
