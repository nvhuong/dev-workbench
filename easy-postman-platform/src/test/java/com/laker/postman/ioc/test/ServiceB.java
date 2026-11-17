package com.laker.postman.ioc.test;

import com.laker.postman.ioc.Autowired;
import com.laker.postman.ioc.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Kiểm tra sự phụ thuộc vòng tròn - ServiceB
 */
@Component
@Getter
@Setter
@ToString(exclude = "serviceA") // Loại trừ các trường tham chiếu vòng tròn để tránh toString Tràn ngăn xếp
public class ServiceB {

    @Autowired
    private ServiceA serviceA;

    public void doSomething() {
        System.out.println("ServiceB doing something, has ServiceA: " + (serviceA != null));
    }
}

