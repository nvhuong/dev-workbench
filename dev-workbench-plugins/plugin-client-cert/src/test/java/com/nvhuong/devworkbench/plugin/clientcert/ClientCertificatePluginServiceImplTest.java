package com.nvhuong.devworkbench.plugin.clientcert;

import cn.hutool.core.io.FileUtil;
import com.nvhuong.devworkbench.model.ClientCertificate;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ClientCertificatePluginServiceImplTest {

    /**
     * Configurations without an id (hand-written JSON or files from older versions) made update/delete throw a
     * NullPointerException, so such entries could never be edited or removed from the settings page.
     */
    @Test public void entriesWithoutIdCanBeUpdatedAndDeleted() throws Exception {
        File config = Files.createTempFile("client-certs", ".json").toFile();
        config.deleteOnExit();
        FileUtil.writeUtf8String("[{\"name\":\"legacy\",\"host\":\"api.example.com\",\"certType\":\"PFX\"},"
                + "{\"name\":\"other\",\"host\":\"b.example.com\",\"certType\":\"PFX\"}]", config);

        ClientCertificatePluginServiceImpl service = new ClientCertificatePluginServiceImpl(config);
        List<ClientCertificate> loaded = service.getAllCertificates();
        assertEquals(loaded.size(), 2);
        loaded.forEach(certificate -> assertNotNull(certificate.getId(), "loaded entries get an id"));
        assertTrue(FileUtil.readUtf8String(config).contains(loaded.get(0).getId()), "assigned ids are persisted");

        ClientCertificate legacy = loaded.get(0);
        legacy.setHost("new.example.com");
        service.updateCertificate(legacy);
        assertEquals(new ClientCertificatePluginServiceImpl(config).getAllCertificates().get(0).getHost(), "new.example.com");

        service.deleteCertificate(legacy.getId());
        service.deleteCertificate(null);
        List<ClientCertificate> remaining = new ClientCertificatePluginServiceImpl(config).getAllCertificates();
        assertEquals(remaining.size(), 1);
        assertEquals(remaining.get(0).getName(), "other");
        assertFalse(remaining.get(0).getId().isBlank());
    }
}
