package com.nvhuong.devworkbench.service.js.api;

import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.service.EnvironmentService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DevWorkbenchApiContextPersistenceTest {

    private String originalDataFilePath;
    private Path tempEnvFile;

    @BeforeMethod
    public void setUp() throws IOException {
        originalDataFilePath = EnvironmentService.getDataFilePath();
        tempEnvFile = Files.createTempFile("devworkbench-api-context-env", ".json");
        Files.writeString(tempEnvFile, "[]", StandardCharsets.UTF_8);
        EnvironmentService.setDataFilePath(tempEnvFile.toString());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        if (originalDataFilePath != null) {
            EnvironmentService.setDataFilePath(originalDataFilePath);
        }
        if (tempEnvFile != null) {
            Files.deleteIfExists(tempEnvFile);
        }
    }

    @Test
    public void standaloneEnvironmentMutationDoesNotPersistEnvironmentServiceData() throws IOException {
        Environment env = new Environment();
        env.setName("Test");

        DevWorkbenchApiContext pm = new DevWorkbenchApiContext(env);
        pm.environment.set("session_id", "value");

        assertEquals(env.get("session_id"), "value");
        assertTrue(readPersistedEnvironments().isEmpty());
    }

    @Test
    public void managedEnvironmentMutationPersistsEnvironmentServiceData() throws IOException {
        Environment env = new Environment("Managed");
        env.setId("managed-env");
        EnvironmentService.saveEnvironment(env);

        DevWorkbenchApiContext pm = new DevWorkbenchApiContext(env);
        pm.environment.set("token", "value");

        Environment persisted = readPersistedEnvironments().stream()
                .filter(candidate -> "managed-env".equals(candidate.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(persisted.get("token"), "value");
    }

    private List<Environment> readPersistedEnvironments() throws IOException {
        return JSONUtil.toList(Files.readString(tempEnvFile), Environment.class);
    }
}
