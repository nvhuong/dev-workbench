package com.nvhuong.devworkbench.ioc.behavior;

import com.nvhuong.devworkbench.ioc.Autowired;
import com.nvhuong.devworkbench.ioc.Component;

@Component
public class MethodInjectedService {
    private MethodInjectedDependency dependency;

    @Autowired
    public void configure(MethodInjectedDependency dependency) {
        this.dependency = dependency;
    }

    public MethodInjectedDependency getDependency() {
        return dependency;
    }
}
