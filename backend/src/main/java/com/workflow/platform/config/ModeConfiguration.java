package com.workflow.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.workflow.platform.component.ModeManager;
import com.workflow.platform.enums.ModeType;
import com.workflow.platform.service.WorkflowService;

@Configuration
public class ModeConfiguration {

	@Value("${app.mode:online}")
	private String currentMode;

	@Bean
	public ModeManager modeManager() {
		ModeType modeType = ModeType.fromCode(currentMode);
		return new ModeManager(modeType);
	}

}