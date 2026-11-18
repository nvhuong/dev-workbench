package com.nvhuong.devworkbench.plugin.api.service;

import com.nvhuong.devworkbench.model.ClientCertificate;

import javax.net.ssl.KeyManager;
import java.util.List;

public interface ClientCertificatePluginService {

    List<ClientCertificate> getAllCertificates();

    void addCertificate(ClientCertificate certificate);

    void updateCertificate(ClientCertificate certificate);

    void deleteCertificate(String id);

    boolean validateCertificatePaths(ClientCertificate certificate);

    KeyManager[] loadClientCertificateKeyManagers(String host, int port);
}
