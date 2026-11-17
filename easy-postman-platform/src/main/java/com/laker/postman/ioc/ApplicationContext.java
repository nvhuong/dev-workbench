package com.laker.postman.ioc;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * NhẹIOCthùng chứa
 * Chịu trách nhiệm quản lý tất cảBeanVí dụ, hỗ trợ chèn phụ thuộc
 */
@Slf4j
public class ApplicationContext {
    /**
     * Ví dụ Singleton
     */
    private static volatile ApplicationContext instance;

    /**
     * BeanXác định sổ đăng ký key: beanName, value: BeanDefinition
     */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    /**
     * BeanChỉ mục loại key: class type, value: beanName list
     */
    private final Map<Class<?>, List<String>> typeIndexMap = new ConcurrentHashMap<>();

    /**
     * ========== Cơ chế bộ đệm ba cấp (để giải quyết các phụ thuộc vòng tròn) ==========
     */

    /**
     * Bộ đệm cấp 1: Bộ đệm đối tượng Singleton
     * Lưu trữ được khởi tạo đầy đủBeanVí dụ
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    /**
     * Bộ đệm cấp hai: bộ đệm đối tượng đơn lẻ sớm
     * Lưu trữ những tiếp xúc trướcBeanSơ thẩm (được khởi tạo nhưng chưa được khởi tạo)
     */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();

    /**
     * Bộ đệm cấp 3: bộ đệm nhà máy đơn
     * Lưu trữBeanNhà máy để có được tiếp xúc sớmBeanVí dụ
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();

    /**
     * đang được tạo raBeanBộ sưu tập để gắn thẻBeanTạo trạng thái
     */
    private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet();

    /**
     * Lưu với@PreDestroyphương phápBeanVí dụ, thường được gọi khi vùng chứa được đóng lại
     */
    private final Map<Object, List<Method>> preDestroyMethods = new ConcurrentHashMap<>();

    private ApplicationContext() {
    }

    /**
     * LấyIOCVí dụ đơn lẻ của vùng chứa
     */
    public static ApplicationContext getInstance() {
        if (instance == null) {
            synchronized (ApplicationContext.class) {
                if (instance == null) {
                    instance = new ApplicationContext();
                }
            }
        }
        return instance;
    }

    /**
     * Quét tất cả các lớp trong gói được chỉ định và đăng ký chúng@ComponentLớp được chú thích
     */
    public void scan(String... basePackages) {
        for (String basePackage : basePackages) {
            try {
                scanPackage(basePackage);
            } catch (Exception e) {
                log.error("Failed to scan package: {}", basePackage, e);
            }
        }
    }

    /**
     * Quét gói
     */
    private void scanPackage(String basePackage) throws Exception {
        String packagePath = basePackage.replace('.', '/');
        ClassLoader classLoader = resolveClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                String filePath = resource.getFile();
                scanFile(new File(filePath), basePackage);
            } else if ("jar".equals(protocol)) {
                scanJar(resource, packagePath);
            }
        }
    }

    /**
     * quétJARlớp trong tập tin
     */
    private void scanJar(URL resource, String packagePath) {
        try {
            JarURLConnection jarConnection = (JarURLConnection) resource.openConnection();
            JarFile jarFile = jarConnection.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Chỉ xử lý.classtệp và lớp trong đường dẫn gói đã chỉ định
                if (entryName.endsWith(".class") && entryName.startsWith(packagePath)) {
                    String className = entryName.replace('/', '.').replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className, false, resolveClassLoader());
                        if (clazz.isAnnotationPresent(Component.class)) {
                            registerBean(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        log.warn("Failed to load class: {}", className);
                    } catch (Exception e) {
                        log.error("Error processing class: {}", className, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan JAR: {}", resource, e);
        }
    }

    /**
     * Quét các tập tin đệ quy
     */
    private void scanFile(File file, String packageName) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        scanFile(f, packageName + "." + f.getName());
                    } else if (f.getName().endsWith(".class")) {
                        String className = packageName + "." + f.getName().replace(".class", "");
                        try {
                            Class<?> clazz = Class.forName(className, false, resolveClassLoader());
                            if (clazz.isAnnotationPresent(Component.class)) {
                                registerBean(clazz);
                            }
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            log.warn("Failed to load class: {}", className);
                        }
                    }
                }
            }
        }
    }

    /**
     * Đăng kýBean
     */
    private void registerBean(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        String beanName = component.value();

        if (beanName.isEmpty()) {
            // Theo mặc định, chữ cái đầu tiên của tên lớp là chữ thườngbeantên
            beanName = getBeanName(clazz);
        }
        BeanDefinition existingDefinition = beanDefinitionMap.get(beanName);
        if (existingDefinition != null) {
            if (existingDefinition.getBeanClass().equals(clazz)) {
                log.debug("Skipped duplicate bean registration: {} -> {}", beanName, clazz.getName());
                return;
            }
            throw new BeanException("Duplicate bean name '" + beanName + "' for classes "
                    + existingDefinition.getBeanClass().getName() + " and " + clazz.getName());
        }

        // Phạm vi kiểm tra: Mặc định là singleton trừ khi được chỉ định rõ ràng prototype
        boolean singleton = true;
        if (clazz.isAnnotationPresent(Scope.class)) {
            Scope scope = clazz.getAnnotation(Scope.class);
            singleton = !Scope.PROTOTYPE.equals(scope.value());
        }

        BeanDefinition beanDefinition = new BeanDefinition(beanName, clazz, singleton);

        beanDefinitionMap.put(beanName, beanDefinition);

        // Tạo một chỉ mục loại
        indexType(clazz, beanName);

        // Lập chỉ mục đồng thời tất cả các giao diện và lớp cha
        indexInterfaces(clazz, beanName);

        log.debug("Registered bean: {} -> {} (singleton={})", beanName, clazz.getName(), singleton);
    }

    private ClassLoader resolveClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        ClassLoader localClassLoader = ApplicationContext.class.getClassLoader();
        if (localClassLoader != null) {
            return localClassLoader;
        }
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * Giao diện chỉ mục và lớp cha
     */
    private void indexInterfaces(Class<?> clazz, String beanName) {
        // Giao diện chỉ mục
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> iface : interfaces) {
            indexType(iface, beanName);
        }

        // Chỉ mục lớp cha mẹ
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            indexType(superClass, beanName);
            indexInterfaces(superClass, beanName);
        }
    }

    private void indexType(Class<?> type, String beanName) {
        typeIndexMap.compute(type, (ignored, beanNames) -> {
            List<String> indexedBeanNames = beanNames != null ? beanNames : new ArrayList<>();
            if (!indexedBeanNames.contains(beanName)) {
                indexedBeanNames.add(beanName);
            }
            return indexedBeanNames;
        });
    }

    /**
     * Đăng ký thủ côngBeanVí dụ
     */
    public void registerBean(String beanName, Object bean) {
        BeanDefinition beanDefinition = new BeanDefinition(beanName, bean.getClass(), true);
        beanDefinitionMap.put(beanName, beanDefinition);

        // Trực tiếp vào bộ đệm cấp đầu tiên (đã hoàn tấtBeanVí dụ)
        singletonObjects.put(beanName, bean);

        // Tạo một chỉ mục loại
        indexType(bean.getClass(), beanName);
        indexInterfaces(bean.getClass(), beanName);

        log.debug("Registered bean instance: {} -> {}", beanName, bean.getClass().getName());
    }

    /**
     * Nhận nó theo tênBean
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new NoSuchBeanException(beanName);
        }

        return (T) getBean(beanDefinition);
    }

    /**
     * Nhận dựa trên loạiBean
     */
    public <T> T getBean(Class<T> requiredType) {
        List<String> beanNames = typeIndexMap.get(requiredType);

        if (beanNames == null || beanNames.isEmpty()) {
            throw new NoSuchBeanException(requiredType);
        }

        if (beanNames.size() > 1) {
            throw new BeanException("Multiple beans of type '" + requiredType.getName() + "' found: " + beanNames);
        }

        return getBean(beanNames.get(0));
    }

    /**
     * theoBeanDefinitionLấyBeanInstance (hỗ trợ bộ đệm cấp 3)
     */
    private Object getBean(BeanDefinition beanDefinition) {
        String beanName = beanDefinition.getName();

        if (beanDefinition.isSingleton()) {
            // Sử dụng bộ đệm cấp ba để lấy singletonBean
            return getSingleton(beanName, () -> createBean(beanDefinition));
        } else {
            // Các trường hợp không đơn lẻ được tạo trực tiếp
            return createBean(beanDefinition);
        }
    }

    /**
     * Lấy đĩa đơnBean（Phương pháp lõi bộ đệm cấp 3)
     *
     * @param beanName         Beantên
     * @param singletonFactory Beannhà máy
     * @return BeanVí dụ
     */
    private Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
        // 1. Trước tiên hãy lấy cái được khởi tạo đầy đủ từ bộ đệm cấp đầu tiênBean
        Object singletonObject = singletonObjects.get(beanName);

        // 2. Nếu bộ đệm cấp đầu tiên không tồn tại vàBeanĐang được tạo (cho biết có sự phụ thuộc vòng tròn)
        if (singletonObject == null && singletonsCurrentlyInCreation.contains(beanName)) {
            // Khóa đảm bảo an toàn cho sợi
            synchronized (this.singletonObjects) {
                // Kiểm tra lại
                singletonObject = singletonObjects.get(beanName);
                if (singletonObject == null) {
                    // Nhận tài liệu tham khảo sớm từ bộ đệm L2
                    singletonObject = earlySingletonObjects.get(beanName);

                    // 3. Nếu bộ đệm cấp hai không có nó, nó sẽ được lấy từ bộ đệm cấp ba
                    if (singletonObject == null) {
                        ObjectFactory<?> factory = singletonFactories.get(beanName);
                        if (factory != null) {
                            try {
                                // Nhận tài liệu tham khảo sớm từ nhà máy
                                singletonObject = factory.getObject();
                                // Đặt nó vào bộ đệm cấp hai và xóa bộ đệm cấp ba
                                earlySingletonObjects.put(beanName, singletonObject);
                                singletonFactories.remove(beanName);
                                log.debug("Resolved circular dependency for bean: {}", beanName);
                            } catch (Exception e) {
                                throw new BeanCreationException(beanName, "Failed to get early reference", e);
                            }
                        }
                    }
                }
            }
        }

        if (singletonObject == null && singletonsCurrentlyInCreation.contains(beanName)) {
            throw new BeanCreationException(beanName,
                    "Circular constructor dependency cannot be resolved before bean instantiation is complete");
        }

        // 4. Nếu tất cả bộ đệm đều bị thiếu, hãy tạo bộ đệm mớiBean
        if (singletonObject == null) {
            // Sử dụng khóa đồng bộ hóa để đảm bảo tính đơn lẻBeanChỉ được tạo một lần
            synchronized (this.singletonObjects) {
                // Kiểm tra lại bộ đệm (có thể các luồng khác đã tạo nó)
                singletonObject = singletonObjects.get(beanName);
                if (singletonObject == null) {
                    // đánh dấuBeanĐang tạo
                    singletonsCurrentlyInCreation.add(beanName);

                    try {
                        // Tạo bằng cách gọi phương thức xuất xưởngBean
                        singletonObject = singletonFactory.getObject();

                        // Đã xóa khỏi bộ đệm cấp hai nếu có
                        earlySingletonObjects.remove(beanName);
                        // Đã xóa khỏi bộ đệm L3 nếu có
                        singletonFactories.remove(beanName);
                        // Đưa vào bộ đệm cấp đầu tiên
                        singletonObjects.put(beanName, singletonObject);

                        log.debug("Created and cached singleton bean: {}", beanName);
                    } catch (Exception e) {
                        // Tạo không thành công, trạng thái dọn dẹp
                        singletonsCurrentlyInCreation.remove(beanName);
                        earlySingletonObjects.remove(beanName);
                        singletonFactories.remove(beanName);
                        throw new BeanCreationException(beanName, e);
                    } finally {
                        // Khi tạo xong, hãy xóa dấu tạo
                        singletonsCurrentlyInCreation.remove(beanName);
                    }
                }
            }
        }

        return singletonObject;
    }

    /**
     * TạoBeanVí dụ (hỗ trợ bộ đệm cấp ba để giải quyết các phụ thuộc vòng tròn)
     */
    private Object createBean(BeanDefinition beanDefinition) {
        String beanName = beanDefinition.getName();
        Class<?> beanClass = beanDefinition.getBeanClass();

        try {
            Object instance = instantiateBean(beanClass);

            if (beanDefinition.isSingleton()) {
                final Object finalInstance = instance;
                singletonFactories.put(beanName, () -> getEarlyBeanReference(beanName, finalInstance));
                log.debug("Added bean factory to third-level cache for: {}", beanName);
            }

            injectFields(instance);
            injectMethods(instance);

            invokePostConstruct(instance);
            invokeInitializingBean(instance);

            registerPreDestroyMethods(instance);

            log.debug("Created bean: {}", beanName);
            return instance;

        } catch (BeanCreationException e) {
            // Chỉ cần ném lại BeanCreationException
            throw e;
        } catch (Exception e) {
            throw new BeanCreationException(beanName, e);
        }
    }

    /**
     * Phương pháp tiêm
     */
    private void injectMethods(Object instance) {
        Class<?> clazz = instance.getClass();

        while (clazz != null && clazz != Object.class) {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(Autowired.class)) {
                    Autowired autowired = method.getAnnotation(Autowired.class);
                    try {
                        Object[] arguments = resolveMethodArguments(method);
                        method.setAccessible(true);
                        method.invoke(instance, arguments);
                        log.debug("Injected method '{}' in bean '{}'",
                                method.getName(), instance.getClass().getSimpleName());
                    } catch (NoSuchBeanException e) {
                        if (autowired.required()) {
                            throw new BeanCreationException(
                                    instance.getClass().getSimpleName(),
                                    "Failed to inject required method '" + method.getName() + "': " + e.getMessage(),
                                    e
                            );
                        } else {
                            log.debug("Skipped optional method injection: {}.{}", clazz.getSimpleName(), method.getName());
                        }
                    } catch (Exception e) {
                        throw new BeanCreationException(
                                instance.getClass().getSimpleName(),
                                "Failed to inject method '" + method.getName() + "'",
                                e
                        );
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    private Object[] resolveMethodArguments(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            arguments[i] = getBean(parameterTypes[i]);
        }
        return arguments;
    }

    /**
     * Khởi tạoBean（sử dụng hàm tạo)
     * Các chiến lược sau đây được hỗ trợ:
     * 1. Nếu vậy@AutowiredCác hàm tạo có chú thích được ưu tiên
     * 2. Nếu chỉ có một hàm tạo (có tham số), hàm tạo này sẽ tự động được sử dụng để chèn phần phụ thuộc
     * 3. Nếu có nhiều hàm tạo, hãy thử sử dụng hàm tạo không có đối số
     * 4. Nếu không có hàm tạo không tham số và không có@AutowiredHàm tạo có chú thích sẽ đưa ra một ngoại lệ
     */
    private Object instantiateBean(Class<?> beanClass) throws Exception {
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        Constructor<?> targetConstructor = null;

        // Chiến lược1: Tìm ban nhạc@Autowiredhàm tạo của
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                targetConstructor = constructor;
                log.debug("Found @Autowired constructor for {}", beanClass.getSimpleName());
                break;
            }
        }

        // Chiến lược2: Nếu không@AutowiredHàm tạo có chú thích, nhưng chỉ có một hàm tạo (có tham số), được sử dụng tự động
        if (targetConstructor == null && constructors.length == 1 && constructors[0].getParameterCount() > 0) {
            targetConstructor = constructors[0];
            log.debug("Using single constructor with parameters for {}", beanClass.getSimpleName());
        }

        // Sử dụng hàm tạo được tìm thấy để chèn phần phụ thuộc
        if (targetConstructor != null) {
            Class<?>[] paramTypes = targetConstructor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                try {
                    params[i] = getBean(paramTypes[i]);
                    log.debug("Resolved constructor parameter {} for {}: {}",
                            i, beanClass.getSimpleName(), paramTypes[i].getSimpleName());
                } catch (NoSuchBeanException e) {
                    throw new BeanCreationException(
                            beanClass.getSimpleName(),
                            "Failed to resolve constructor parameter [" + i + "] of type '" +
                                    paramTypes[i].getName() + "': " + e.getMessage(),
                            e
                    );
                }
            }

            targetConstructor.setAccessible(true);
            return targetConstructor.newInstance(params);
        }

        // Chiến lược3: Sử dụng hàm tạo không có đối số
        try {
            Constructor<?> defaultConstructor = beanClass.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
            log.debug("Using default no-arg constructor for {}", beanClass.getSimpleName());
            return defaultConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            // Chiến lược4: Nếu không tìm thấy hàm tạo phù hợp, một ngoại lệ sẽ được đưa ra
            throw new BeanCreationException(
                    beanClass.getSimpleName(),
                    "No suitable constructor found. Please provide either: " +
                            "1) a no-arg constructor, " +
                            "2) a single constructor with parameters, or " +
                            "3) a constructor annotated with @Autowired",
                    e
            );
        }
    }

    /**
     * Nhận nó sớmBeanTài liệu tham khảo (được sử dụng để giải quyết các phụ thuộc vòng tròn)
     * Những gì phương thức này trả về được khởi tạo nhưng không được khởi tạo đầy đủBean
     *
     * @param beanName Beantên
     * @param bean     BeanVí dụ
     * @return Những ngày đầuBeanTrích dẫn
     */
    private Object getEarlyBeanReference(String beanName, Object bean) {
        // Ở đây một đối tượng proxy có thể được trả về (nếu cầnAOP）
        // Hiện tại đối tượng ban đầu được trả lại trực tiếp
        log.debug("Getting early reference for bean: {}", beanName);
        return bean;
    }

    /**
     * Tiêm hiện trường
     */
    private void injectFields(Object instance) {
        Class<?> clazz = instance.getClass();

        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Autowired autowired = field.getAnnotation(Autowired.class);

                    try {
                        Object dependency = getBean(field.getType());
                        field.setAccessible(true);
                        field.set(instance, dependency);
                        log.debug("Injected field '{}' in bean '{}'", field.getName(), instance.getClass().getSimpleName());
                    } catch (NoSuchBeanException e) {
                        if (autowired.required()) {
                            throw new BeanCreationException(
                                    instance.getClass().getSimpleName(),
                                    "Failed to inject required field '" + field.getName() + "': " + e.getMessage(),
                                    e
                            );
                        } else {
                            log.debug("Skipped optional field injection: {}.{}", clazz.getSimpleName(), field.getName());
                        }
                    } catch (Exception e) {
                        throw new BeanCreationException(
                                instance.getClass().getSimpleName(),
                                "Failed to inject field '" + field.getName() + "'",
                                e
                        );
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * gọi@PostConstructphương pháp
     */
    private void invokePostConstruct(Object instance) {
        Class<?> clazz = instance.getClass();

        while (clazz != null && clazz != Object.class) {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    // Xác minh chữ ký phương thức
                    if (method.getParameterCount() != 0) {
                        throw new BeanCreationException(
                                instance.getClass().getSimpleName(),
                                "@PostConstruct method '" + method.getName() + "' must have no parameters"
                        );
                    }

                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                        log.debug("Invoked @PostConstruct method: {}.{}", clazz.getSimpleName(), method.getName());
                    } catch (Exception e) {
                        throw new BeanCreationException(
                                instance.getClass().getSimpleName(),
                                "Failed to invoke @PostConstruct method '" + method.getName() + "'",
                                e
                        );
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * gọiInitializingBeangiao diệnafterPropertiesSetphương pháp
     */
    private void invokeInitializingBean(Object instance) {
        if (instance instanceof InitializingBean initializingBean) {
            try {
                initializingBean.afterPropertiesSet();
                log.debug("Invoked InitializingBean.afterPropertiesSet() for: {}",
                        instance.getClass().getSimpleName());
            } catch (Exception e) {
                throw new BeanCreationException(
                        instance.getClass().getSimpleName(),
                        "Failed to invoke afterPropertiesSet()",
                        e
                );
            }
        }
    }

    /**
     * Đăng ký@PreDestroyphương pháp
     */
    private void registerPreDestroyMethods(Object instance) {
        List<Method> destroyMethods = new ArrayList<>();
        Class<?> clazz = instance.getClass();

        while (clazz != null && clazz != Object.class) {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    if (method.getParameterCount() != 0) {
                        throw new BeanCreationException(
                                instance.getClass().getSimpleName(),
                                "@PreDestroy method '" + method.getName() + "' must have no parameters"
                        );
                    }

                    method.setAccessible(true);
                    destroyMethods.add(method);
                }
            }

            clazz = clazz.getSuperclass();
        }

        if (!destroyMethods.isEmpty()) {
            preDestroyMethods.put(instance, destroyMethods);
        }
    }

    /**
     * Tiêu diệt tất cảBean，gọi@PreDestroyphương pháp vàDisposableBeangiao diện
     */
    public void destroy() {
        // 1. Hãy gọi tất cả trước@PreDestroyphương pháp
        for (Map.Entry<Object, List<Method>> entry : preDestroyMethods.entrySet()) {
            Object instance = entry.getKey();
            List<Method> methods = entry.getValue();

            for (Method method : methods) {
                try {
                    method.invoke(instance);
                    log.debug("Invoked @PreDestroy method: {}.{}",
                            instance.getClass().getSimpleName(), method.getName());
                } catch (Exception e) {
                    log.error("Failed to invoke @PreDestroy method: {}", method.getName(), e);
                }
            }
        }

        // 2. Sau đó gọiDisposableBeangiao diệndestroyphương pháp
        for (Object instance : singletonObjects.values()) {
            if (instance instanceof DisposableBean disposableBean) {
                try {
                    disposableBean.destroy();
                    log.debug("Invoked DisposableBean.destroy() for: {}",
                            instance.getClass().getSimpleName());
                } catch (Exception e) {
                    log.error("Failed to invoke DisposableBean.destroy() for: {}",
                            instance.getClass().getName(), e);
                }
            }
        }

        preDestroyMethods.clear();
        log.info("All beans destroyed");
    }

    /**
     * LấyBeanTên (chữ cái đầu tiên của tên lớp là chữ thường)
     */
    private String getBeanName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * Nhận tất cả chúngBeantên
     */
    public Set<String> getBeanNames() {
        return new HashSet<>(beanDefinitionMap.keySet());
    }

    /**
     * Kiểm tra xem tên được chỉ định có được bao gồm hay khôngBean
     */
    public boolean containsBean(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }

    /**
     * Kiểm tra chỉ địnhBeanCho dù đó là một singleton
     */
    public boolean isSingleton(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        return beanDefinition != null && beanDefinition.isSingleton();
    }

    /**
     * Làm trống thùng chứa
     */
    public void clear() {
        // Phá hủy mọi thứ trước tiênBean
        destroy();

        beanDefinitionMap.clear();
        typeIndexMap.clear();
        singletonObjects.clear();
        earlySingletonObjects.clear();
        singletonFactories.clear();
        singletonsCurrentlyInCreation.clear();
        log.info("ApplicationContext cleared");
    }

}
