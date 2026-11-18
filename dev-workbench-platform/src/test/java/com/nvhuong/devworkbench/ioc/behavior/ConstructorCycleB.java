package com.nvhuong.devworkbench.ioc.behavior;

import com.nvhuong.devworkbench.ioc.Component;

@Component
public class ConstructorCycleB {
    public ConstructorCycleB(ConstructorCycleA constructorCycleA) {
    }
}
