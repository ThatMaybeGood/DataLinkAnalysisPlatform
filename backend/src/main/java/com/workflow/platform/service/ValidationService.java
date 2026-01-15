package com.workflow.platform.service;

import com.workflow.platform.model.entity.ValidationRuleEntity;
import java.util.List;
public interface ValidationService {
    boolean validate(Object object);

    List<ValidationRuleEntity> getRulesByWorkflowId(String workflowId);

    void addRule(String workflowId, ValidationRuleEntity rule);


    void deleteRule(String ruleId);

    void updateRule(String ruleId, ValidationRuleEntity updatedRule);

    ValidationRuleEntity getRuleById(String ruleId);


}
