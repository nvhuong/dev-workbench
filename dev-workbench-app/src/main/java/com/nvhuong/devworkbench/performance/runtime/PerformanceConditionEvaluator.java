package com.nvhuong.devworkbench.performance.runtime;

import com.nvhuong.devworkbench.performance.core.controller.ConditionData;
import com.nvhuong.devworkbench.performance.core.controller.ConditionExpressionEvaluator;
import com.nvhuong.devworkbench.performance.core.controller.WhileData;
import com.nvhuong.devworkbench.performance.core.plan.PerformanceConditionController;
import com.nvhuong.devworkbench.performance.core.plan.PerformanceWhileController;
import com.nvhuong.devworkbench.service.variable.ExecutionContextScope;
import com.nvhuong.devworkbench.service.variable.ExecutionVariableContext;
import com.nvhuong.devworkbench.service.variable.VariableResolver;

final class PerformanceConditionEvaluator {

    private PerformanceConditionEvaluator() {
    }

    static boolean evaluate(PerformanceConditionController conditionController,
                            ExecutionVariableContext iterationContext) {
        try (ExecutionContextScope ignored = ExecutionContextScope.open(iterationContext)) {
            ConditionData conditionData = conditionController.getConditionData();
            return ConditionExpressionEvaluator.evaluate(
                    conditionData == null ? null : conditionData.expression,
                    APP_VARIABLE_LOOKUP
            );
        }
    }

    static boolean evaluate(PerformanceWhileController whileController,
                            ExecutionVariableContext iterationContext) {
        try (ExecutionContextScope ignored = ExecutionContextScope.open(iterationContext)) {
            WhileData whileData = whileController.getWhileData();
            return ConditionExpressionEvaluator.evaluate(
                    whileData == null ? null : whileData.expression,
                    APP_VARIABLE_LOOKUP
            );
        }
    }

    private static final ConditionExpressionEvaluator.VariableLookup APP_VARIABLE_LOOKUP =
            new ConditionExpressionEvaluator.VariableLookup() {
                @Override
                public String resolve(String variableName) {
                    return VariableResolver.resolveVariable(variableName);
                }

                @Override
                public boolean isDefined(String variableName) {
                    return VariableResolver.isVariableDefined(variableName);
                }
            };
}
