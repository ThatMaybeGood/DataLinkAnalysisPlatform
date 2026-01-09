package com.workflow.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class Application {

    public static void main(String[] args) {
        // 检查启动参数，支持 --mode=online/offline
        String mode = parseModeFromArgs(args);
        if (mode != null) {
            System.setProperty("app.mode", mode);
        }

        SpringApplication.run(Application.class, args);

        // 打印当前模式
        printModeInfo();
    }

    private static String parseModeFromArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--mode=")) {
                return arg.substring(7);
            }
        }
        return null;
    }

    private static void printModeInfo() {
        String mode = System.getProperty("app.mode", "online");
        System.out.println("========================================");
        System.out.println("工作流平台启动完成");
        System.out.println("当前运行模式: " + mode.toUpperCase());
        System.out.println("========================================");
    }
}