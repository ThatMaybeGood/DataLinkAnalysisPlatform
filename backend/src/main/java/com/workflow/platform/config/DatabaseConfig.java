package com.workflow.platform.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.workflow.platform.repository", entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
public class DatabaseConfig {

	@Value("${spring.jpa.hibernate.ddl-auto:update}")
	private String ddlAuto;

	@Value("${spring.jpa.show-sql:false}")
	private boolean showSql;

	// -------------------------------------------------------------------
	// 1. 数据源定义：根据 Profile 激活不同的数据源
	// -------------------------------------------------------------------

	/**
	 * 在线模式数据源：读取 spring.datasource 的配置 (MySQL)
	 */
	@Bean
	@Primary
	@Profile("!offline") // 非 offline 模式才加载
	// @ConfigurationProperties("spring.datasource.hikari")
	public DataSource dataSource() {
		return new HikariDataSource();
	}

	/**
	 * 离线模式数据源：手动配置 H2，不读取 application.yml 的 MySQL 配置
	 */
	@Bean
	@Primary
	@Profile("offline") // 只有 offline 模式加载
	public DataSource offlineDataSource() {
		HikariDataSource ds = new HikariDataSource();
		ds.setDriverClassName("org.h2.Driver");
		// 使用内存模式，并开启 MySQL 兼容模式
		ds.setJdbcUrl("jdbc:h2:mem:workflow_db;DB_CLOSE_DELAY=-1;MODE=MySQL");
		ds.setUsername("sa");
		ds.setPassword("");
		ds.setPoolName("H2-Offline-Pool");
		ds.setMaximumPoolSize(5);
		return ds;
	}

	// -------------------------------------------------------------------
	// 2. JPA 实体管理器：动态处理方言和缓存
	// -------------------------------------------------------------------

	@Bean
	@Primary
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			DataSource dataSource,
			@Value("${spring.profiles.active:online}") String activeProfile) {

		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource);
		em.setPackagesToScan("com.workflow.platform.model.entity");

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);

		Map<String, Object> properties = new HashMap<>();
		properties.put("hibernate.hbm2ddl.auto", ddlAuto);
		properties.put("hibernate.show_sql", showSql);
		properties.put("hibernate.format_sql", true);

		// 核心：如果是离线模式，强制关闭二级缓存和 EhCache
		if ("offline".equalsIgnoreCase(activeProfile)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			properties.put("hibernate.cache.use_second_level_cache", "false");
			properties.put("hibernate.cache.use_query_cache", "false");
			// 解决你之前的 RegionFactory 报错：强制指定不使用缓存工厂
			properties.put("hibernate.cache.region.factory_class",
					"org.hibernate.cache.internal.NoCachingRegionFactory");
		} else {
			properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
			properties.put("hibernate.cache.use_second_level_cache", "true");
			properties.put("hibernate.cache.use_query_cache", "true");
			properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
		}

		em.setJpaPropertyMap(properties);
		return em;
	}

	@Bean
	@Primary
	public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
		return transactionManager;
	}
}