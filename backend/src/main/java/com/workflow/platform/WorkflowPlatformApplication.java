package com.workflow.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 工作流平台主启动类
 * 支持通过配置文件、环境变量或命令行参数切换在线/离线模式
 */
@SpringBootApplication
@EnableConfigurationProperties
public class WorkflowPlatformApplication {

	public static void main(String[] args) {
		// 1. 解析命令行参数，支持 --mode=online/offline
		String mode = parseModeFromArgs(args);
		if (mode != null) {
			System.setProperty("app.mode", mode);
			System.out.println("通过命令行参数设置模式为: " + mode);
		}

		// 2. 启动Spring Boot应用
		SpringApplication app = new SpringApplication(WorkflowPlatformApplication.class);

		// 3. 打印启动信息
		app.addListeners(event -> {
			if (event instanceof org.springframework.boot.context.event.ApplicationReadyEvent) {
				printStartupInfo();
			}
		});

		app.run(args);
	}

	/**
	 * 从命令行参数解析模式设置
	 * 
	 * @param args 命令行参数
	 * @return 模式字符串（online/offline）
	 */
	private static String parseModeFromArgs(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("--mode=")) {
				String mode = arg.substring(7).toLowerCase();
				if ("online".equals(mode) || "offline".equals(mode)) {
					return mode;
				}
			}
		}
		return null;
	}

	/**
	 * 打印启动信息
	 */
	private static void printStartupInfo() {
		String mode = System.getProperty("app.mode", "online");
		System.out.println("╔══════════════════════════════════════════════════════════╗");
		System.out.println("║                   工作流平台启动完成                     ║");
		System.out.println("╠══════════════════════════════════════════════════════════╣");
		System.out.println("║ 当前模式: " + padRight(mode.toUpperCase(), 46) + "║");
		System.out.println("║ 服务地址: http://localhost:8080/api                     ║");
		System.out.println("║ API文档: http://localhost:8080/api/swagger-ui.html      ║");
		System.out.println("║ 健康检查: http://localhost:8080/api/actuator/health     ║");
		System.out.println("╚══════════════════════════════════════════════════════════╝");

		// 打印模式特定信息
		if ("offline".equals(mode)) {
			System.out.println("\n📂 离线模式说明:");
			System.out.println("   • 数据存储在本地文件系统中");
			System.out.println("   • 位置: ./data/ 目录");
			System.out.println("   • 支持工作流导入/导出功能");
		} else {
			System.out.println("\n🌐 在线模式说明:");
			System.out.println("   • 数据存储在MySQL数据库中");
			System.out.println("   • 支持多用户实时协作");
			System.out.println("   • 支持用户权限管理");
			System.out.println("\n🚀 工作流平台已成功启动，祝您使用愉快！");
		}
	}

	/**
	 * 字符串右填充工具方法
	 */
	private static String padRight(String s, int n) {
		return String.format("%-" + n + "s", s);
	}

}

//
//
//
// package com.workflow.platform;
//
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import
// org.springframework.boot.context.properties.EnableConfigurationProperties;
// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.scheduling.annotation.EnableAsync;
// import org.springframework.scheduling.annotation.EnableScheduling;
// import
// org.springframework.transaction.annotation.EnableTransactionManagement;
//
/// **
// * 工作流可视化平台 - 主启动类
// */
// @SpringBootApplication
// @EnableConfigurationProperties
// @EnableTransactionManagement
// @EnableCaching
// @EnableAsync
// @EnableScheduling
// public class Application {
//
// public static void main(String[] args) {
// SpringApplication application = new SpringApplication(Application.class);
//
// // 添加初始化监听器
// application.addListeners(new ApplicationStartupListener());
//
// // 运行应用
// application.run(args);
//
// System.out.println("==========================================");
// System.out.println("🚀 工作流可视化平台启动成功!");
// System.out.println("📊 当前模式: " + System.getProperty("app.mode", "online"));
// System.out.println("🌐 服务地址: http://localhost:8080");
// System.out.println("📝 API文档: http://localhost:8080/swagger-ui.html");
// System.out.println("==========================================");
// }
// }