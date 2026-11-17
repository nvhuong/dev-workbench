package com.laker.postman.plugin.git.internal;

import cn.hutool.core.io.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSHNhà cung cấp xác thực
 * sử dụng Apache MINA SSHD hoàn thànhSSHXác thực khóa
 */
@Slf4j
public class SshCredentialsProvider implements TransportConfigCallback {

    private final String privateKeyPath;
    private final String passphrase;

    // Bộ nhớ đệmSSHNhà máy phiên để tránh tạo phiên bản mới mỗi lần
    // Key: privateKeyPath, Value: SshSessionFactory
    private static final Map<String, SshSessionFactory> SESSION_FACTORY_CACHE = new ConcurrentHashMap<>();

    // Được sử dụng để dọn dẹp tài nguyên khi đóng ứng dụng
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Cleaning up SSH session factories...");
            SESSION_FACTORY_CACHE.values().forEach(factory -> {
                if (factory instanceof SshdSessionFactory sshdFactory) {
                    try {
                        sshdFactory.close();
                    } catch (Exception e) {
                        log.debug("Error closing SSH session factory during shutdown", e);
                    }
                }
            });
            SESSION_FACTORY_CACHE.clear();
        }, "SSH-Cleanup-Thread"));
    }

    public SshCredentialsProvider(String privateKeyPath, String passphrase) {
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
    }

    @Override
    public void configure(Transport transport) {
        if (transport instanceof SshTransport sshTransport) {
            sshTransport.setSshSessionFactory(getOrCreateSshSessionFactory());
        }
    }

    /**
     * Nhận hoặc tạoSSHNhà máy phiên (có bộ đệm)
     * Điều này có thể tránh được mọi lúcGitHoạt động tạo ra những cái mớiexecutorNhóm chủ đề
     */
    @SneakyThrows
    private SshSessionFactory getOrCreateSshSessionFactory() {
        return SESSION_FACTORY_CACHE.computeIfAbsent(privateKeyPath, key -> {
            try {
                String privateKeyContent = FileUtil.readString(new File(privateKeyPath), StandardCharsets.UTF_8);
                Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(null,
                        null,
                        new ByteArrayInputStream(privateKeyContent.getBytes()),
                        (session, resourceKey, retryIndex) -> passphrase);

                return new SshdSessionFactoryBuilder()
//                      .setPreferredAuthentications("publickey") // Không bắt buộc Sử dụng xác thực khóa công khai
                        .setDefaultKeysProvider(ignoredSshDirBecauseWeUseAnInMemorySetOfKeyPairs -> keyPairs) // Đặt nhà cung cấp khóa
                        .setHomeDirectory(FS.DETECTED.userHome()) //Đặt thư mục chính của người dùng ảnh hưởng.ssh/known_hosts Đợi đã
                        .setSshDirectory(new File(FS.DETECTED.userHome(), ".ssh")) //cài đặt.sshThư mục
                        .build(null);
            } catch (Exception e) {
                log.error("Failed to create SSH session factory for key: {}", privateKeyPath, e);
                throw new RuntimeException("Failed to create SSH session factory", e);
            }
        });
    }

    /**
     * Xóa bộ đệm cho khóa riêng được chỉ định
     * Khi người dùng thay đổiSSHPhương pháp này có thể được gọi khi lấy được khóa
     */
    public static void clearCache(String privateKeyPath) {
        SshSessionFactory factory = SESSION_FACTORY_CACHE.remove(privateKeyPath);
        if (factory instanceof SshdSessionFactory sshdFactory) {
            try {
                sshdFactory.close();
            } catch (Exception e) {
                log.debug("Error closing SSH session factory", e);
            }
        }
    }
}
