package com.nvhuong.devworkbench.ioc.test;

import com.nvhuong.devworkbench.ioc.Autowired;
import com.nvhuong.devworkbench.ioc.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Kiểm tra sự phụ thuộc vòng tròn - ServiceA
 */
@Component
@Getter
@Setter
@ToString(exclude = "serviceB") // Loại trừ các trường tham chiếu vòng tròn để tránh toString Tràn ngăn xếp
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void doSomething() {
        System.out.println("ServiceA doing something, has ServiceB: " + (serviceB != null));
    }
}

