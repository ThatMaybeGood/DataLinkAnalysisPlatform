package com.workflow.platform.config;

import com.workflow.platform.component.ModeManager;
import com.workflow.platform.enums.ModeType;
import com.workflow.platform.service.WorkflowService;
import com.workflow.platform.service.impl.OnlineWorkflowServiceImpl;
import com.workflow.platform.service.impl.OfflineWorkflowServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ModeConfiguration {

    @Value("${app.mode:online}")
    private String currentMode;

    @Bean
    public ModeManager modeManager() {
        ModeType modeType = ModeType.fromCode(currentMode);
        return new ModeManager(modeType);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.mode", havingValue = "online")
    public WorkflowService onlineWorkflowService() {
//        return new OnlineWorkflowServiceImpl();
        return null;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.mode", havingValue = "offline")
    public WorkflowService offlineWorkflowService() {
//        return new OfflineWorkflowServiceImpl();
        return null;
    }
}