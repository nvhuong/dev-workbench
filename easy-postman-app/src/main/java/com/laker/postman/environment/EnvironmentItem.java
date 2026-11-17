package com.laker.postman.environment;

import com.laker.postman.model.Environment;

import lombok.Getter;

import java.util.Objects;

/**
 * Lớp đóng gói mục môi trường, được sử dụng để hiển thị trong hộp thả xuống
 */
@Getter
public class EnvironmentItem {
    private final Environment environment;

    public EnvironmentItem(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EnvironmentItem that)) return false;
        return Objects.equals(getEnvironment().getId(), that.getEnvironment().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(environment.getId());
    }

    @Override
    public String toString() {
        return environment.getName();
    }


}
