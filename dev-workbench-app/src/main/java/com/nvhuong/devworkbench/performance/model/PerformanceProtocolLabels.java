package com.nvhuong.devworkbench.performance.model;

import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;


import com.nvhuong.devworkbench.util.I18nUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceProtocolLabels {
    public String displayName(PerformanceProtocol protocol) {
        return I18nUtil.getMessage(protocol.getMessageKey());
    }
}
