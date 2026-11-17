package com.laker.postman.ioc.test;

import com.laker.postman.ioc.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;

import static org.testng.Assert.*;

/**
 * Kiểm tra phụ thuộc vòng tròn - sử dụng TestNG
 * Kiểm tra xem bộ đệm cấp ba có thể giải quyết chính xác vấn đề phụ thuộc vòng tròn hay không
 */
@Slf4j
public class CircularDependencyTest {

    private ApplicationContext context;

    @BeforeMethod
    public void setUp() {
        log.info("========== Khởi tạo thử nghiệm ==========");
        context = ApplicationContext.getInstance();
        context.clear();
    }

    @AfterMethod
    public void tearDown() {
        log.info("========== Kiểm tra dọn dẹp ==========");
        if (context != null) {
            context.clear();
        }
    }

    @Test(description = "Hoàn thành thử nghiệm quy trình giải quyết phụ thuộc vòng tròn")
    public void testCompleteCircularDependencyResolution() {
        log.info("========== Bắt đầu thử nghiệm độ phân giải phụ thuộc vòng tròn (sử dụng bộ đệm L3) ==========");

        // 1. Quét gói và đăng kýBean
        log.info("1. Quét và đăng kýBean...");
        context.scan("com.laker.postman.ioc.test");

        // 2. LấyServiceA（Điều này sẽ kích hoạt sự phụ thuộc vòng tròn)
        log.info("\n2. LấyServiceA（sẽ kích hoạt độ phân giải phụ thuộc vòng tròn)...");
        ServiceA serviceA = context.getBean(ServiceA.class);
        assertNotNull(serviceA, "ServiceA should not be null");
        log.info("✅ Đã nhận được thành công ServiceA: {}", serviceA);

        // 3. LấyServiceB
        log.info("\n3. LấyServiceB...");
        ServiceB serviceB = context.getBean(ServiceB.class);
        assertNotNull(serviceB, "ServiceB should not be null");
        log.info("✅ Đã nhận được thành công ServiceB: {}", serviceB);

        // 4. Xác minh rằng các phần phụ thuộc vòng tròn được chèn chính xác
        log.info("\n4. Xác minh nội dung phụ thuộc vòng tròn...");
        assertNotNull(serviceA.getServiceB(), "ServiceA.serviceB should not be null");
        assertNotNull(serviceB.getServiceA(), "ServiceB.serviceA should not be null");
        log.info("✅ ServiceA.serviceB Không null");
        log.info("✅ ServiceB.serviceA Không null");

        // 5. Xác minh đó là cùng một phiên bản (singleton)
        log.info("\n5. Xác minh mẫu đơn...");
        assertSame(serviceA.getServiceB(), serviceB, "ServiceA.serviceB should be the same instance as ServiceB");
        assertSame(serviceB.getServiceA(), serviceA, "ServiceB.serviceA should be the same instance as ServiceA");
        log.info("ServiceA.serviceB == ServiceB: true");
        log.info("ServiceB.serviceA == ServiceA: true");
        log.info("✅ Xác minh chế độ Singleton đã được thông qua! .");

        // 6. Kiểm tra xem chức năng có bình thường không
        log.info("\n6. Bài kiểm traBeanChức năng...");
        serviceA.doSomething();
        serviceB.doSomething();

        // 7. Thử nghiệm nhiều lần mua lại
        log.info("\n7. Kiểm tra để có được điều tương tự nhiều lầnBean...");
        ServiceA serviceA2 = context.getBean("serviceA");
        ServiceB serviceB2 = context.getBean("serviceB");

        assertSame(serviceA, serviceA2, "Multiple gets should return the same ServiceA instance");
        assertSame(serviceB, serviceB2, "Multiple gets should return the same ServiceB instance");
        log.info("Liệu nhiều lần truy xuất có trả về cùng một phiên bản hay không: true");
        log.info("✅ Đã vượt qua bài kiểm tra nhiều lần! .");

        log.info("\n========== 🎉 Tất cả các bài kiểm tra phụ thuộc vòng tròn đã vượt qua! Bộ đệm cấp 3 đang hoạt động tốt! . ==========");
        log.info("\nTóm tắt:");
        log.info("✅ Đã giải quyết thành công ServiceA và ServiceB sự phụ thuộc theo chu kỳ giữa");
        log.info("✅ SingletonBeanChỉ có một phiên bản được tạo ra");
        log.info("✅ Việc tiêm phụ thuộc được thực hiện chính xác");
        log.info("✅ Cơ chế bộ đệm cấp ba hoạt động bình thường");
    }

    @Test(description = "Bài kiểm traBeanTiêm không có tác dụngnull", priority = 1)
    public void testBeanInjectionNotNull() {
        log.info("========== Bài kiểm traBeanTiêm không có tác dụngnull ==========");

        context.scan("com.laker.postman.ioc.test");

        ServiceA serviceA = context.getBean(ServiceA.class);
        ServiceB serviceB = context.getBean(ServiceB.class);

        assertNotNull(serviceA.getServiceB(), "ServiceA's serviceB field should be injected");
        assertNotNull(serviceB.getServiceA(), "ServiceB's serviceA field should be injected");

        log.info("✅ Tất cả xác minh nội dung phụ thuộc đã được thông qua! .");
    }

    @Test(description = "Kiểm tra tính nhất quán đơn lẻ", priority = 2)
    public void testSingletonConsistency() {
        log.info("========== Kiểm tra tính nhất quán đơn lẻ ==========");

        context.scan("com.laker.postman.ioc.test");

        ServiceA serviceA = context.getBean(ServiceA.class);
        ServiceB serviceB = context.getBean(ServiceB.class);

        // Xác minh tính nhất quán tham chiếu vòng tròn
        assertSame(serviceA.getServiceB(), serviceB, "ServiceA's serviceB should reference the same ServiceB instance");
        assertSame(serviceB.getServiceA(), serviceA, "ServiceB's serviceA should reference the same ServiceA instance");

        // Xác minh tài liệu tham khảo sâu
        assertSame(serviceA, serviceA.getServiceB().getServiceA(), "Deep circular reference should work");
        assertSame(serviceB, serviceB.getServiceA().getServiceB(), "Deep circular reference should work");

        log.info("✅ Xác minh tính nhất quán của Singleton đã được thông qua! .");
    }

    @Test(description = "Bài kiểm traBeanChức năng bình thường", priority = 3)
    public void testBeanFunctionality() {
        log.info("========== Bài kiểm traBeanChức năng bình thường ==========");

        context.scan("com.laker.postman.ioc.test");

        ServiceA serviceA = context.getBean(ServiceA.class);
        ServiceB serviceB = context.getBean(ServiceB.class);

        // Khi gọi một phương thức, không được phép ném ra ngoại lệ
        try {
            serviceA.doSomething();
            serviceB.doSomething();
            log.info("✅ BeanKiểm tra chức năng đã thông qua! .");
        } catch (Exception e) {
            fail("Bean methods should not throw exception: " + e.getMessage());
        }
    }
}

