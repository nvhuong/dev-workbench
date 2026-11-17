package com.laker.postman.service.variable;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.variable.VariableType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Các nhà cung cấp chức năng tích hợp
 * <p>
 * Chịu trách nhiệm xử lý tất cả các hàm động được tích hợp sẵn như  $guid, $timestamp, $randomInt Đợi đã
 * <ul>
 *   <li>Mức độ ưu tiên thấp nhất</li>
 *   <li>Mỗi cuộc gọi tạo ra các giá trị ngẫu nhiên mới</li>
 * </ul>
 */
@Slf4j
public class BuiltInFunctionService implements VariableProvider {

    /**
     * Ví dụ Singleton
     */
    private static final BuiltInFunctionService INSTANCE = new BuiltInFunctionService();

    private static final Random RANDOM = new Random();

    /**
     * Danh sách tất cả các chức năng tích hợp được hỗ trợ
     */
    private static final String[] BUILT_IN_FUNCTIONS = {
            "$guid", "$uuid", "$randomUUID",
            "$timestamp", "$isoTimestamp",
            "$randomInt", "$randomAlphaNumeric", "$randomBoolean", "$randomTF",
            "$randomIP", "$randomEmail",
            "$randomFullName", "$randomFirstName", "$randomLastName",
            "$randomColor", "$randomDate", "$randomTime",
            "$randomUrl", "$randomPhoneNumber",
            "$randomCity", "$randomCountry", "$randomUserAgent",
            "$randomMD5", "$randomBase64"
    };

    /**
     * Nhà xây dựng riêng để ngăn chặn việc khởi tạo bên ngoài
     */
    private BuiltInFunctionService() {
    }

    /**
     * Nhận một ví dụ đơn lẻ
     */
    public static BuiltInFunctionService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        return generate(key);
    }

    @Override
    public boolean has(String key) {
        return isBuiltInFunction(key);
    }

    @Override
    public Map<String, String> getAll() {
        return getAllFunctionsWithDescriptions();
    }

    @Override
    public int getPriority() {
        return VariableType.BUILT_IN.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.BUILT_IN;
    }

    // ==================== Các phương thức dành riêng cho các hàm dựng sẵn ====================

    /**
     * Xác định xem đó có phải là chức năng tích hợp hay không
     *
     * @param name tên chức năng
     * @return Cho dù đó là một chức năng tích hợp
     */
    public boolean isBuiltInFunction(String name) {
        if (name == null) {
            return false;
        }
        for (String func : BUILT_IN_FUNCTIONS) {
            if (func.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tạo ra giá trị của hàm dựng sẵn
     *
     * @param funcName tên chức năng
     * @return Giá trị được tạo, trả về nếu hàm không tồn tại null
     */
    public String generate(String funcName) {
        if (funcName == null) {
            return null;
        }

        String result;
        switch (funcName) {
            case "$guid":
            case "$uuid":
            case "$randomUUID":
                result = UUID.randomUUID().toString();
                break;

            case "$timestamp":
                result = String.valueOf(System.currentTimeMillis() / 1000);
                break;

            case "$isoTimestamp":
                result = Instant.now().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                break;

            case "$randomInt":
                result = String.valueOf(RANDOM.nextInt(1001)); // 0-1000
                break;

            case "$randomAlphaNumeric":
                result = generateRandomAlphaNumeric(10);
                break;

            case "$randomBoolean":
                result = String.valueOf(RANDOM.nextBoolean());
                break;

            case "$randomTF":
                result = RANDOM.nextBoolean() ? "T" : "F";
                break;

            case "$randomIP":
                result = RANDOM.nextInt(256) + "." + RANDOM.nextInt(256) + "." +
                        RANDOM.nextInt(256) + "." + RANDOM.nextInt(256);
                break;

            case "$randomEmail":
                result = generateRandomAlphaNumeric(8).toLowerCase() + "@example.com";
                break;

            case "$randomFullName":
                result = getRandomFirstName() + " " + getRandomLastName();
                break;

            case "$randomFirstName":
                result = getRandomFirstName();
                break;

            case "$randomLastName":
                result = getRandomLastName();
                break;

            case "$randomColor":
                result = String.format("#%06x", RANDOM.nextInt(0xffffff + 1));
                break;

            case "$randomDate":
                LocalDate date = LocalDate.now().minusDays(RANDOM.nextInt(365));
                result = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                break;

            case "$randomTime":
                LocalTime time = LocalTime.of(RANDOM.nextInt(24), RANDOM.nextInt(60), RANDOM.nextInt(60));
                result = time.format(DateTimeFormatter.ISO_LOCAL_TIME);
                break;

            case "$randomUrl":
                result = "https://example" + RANDOM.nextInt(100) + ".com/" +
                        generateRandomAlphaNumeric(8).toLowerCase();
                break;

            case "$randomPhoneNumber":
                result = String.format("+1-%03d-%03d-%04d",
                        RANDOM.nextInt(1000), RANDOM.nextInt(1000), RANDOM.nextInt(10000));
                break;

            case "$randomCity":
                result = getRandomCity();
                break;

            case "$randomCountry":
                result = getRandomCountry();
                break;

            case "$randomUserAgent":
                result = getRandomUserAgent();
                break;

            case "$randomMD5":
                result = generateMD5Hash(generateRandomAlphaNumeric(16));
                break;

            case "$randomBase64":
                result = Base64.getEncoder().encodeToString(generateRandomAlphaNumeric(12).getBytes());
                break;

            default:
                log.warn("Unknown built-in function: {}", funcName);
                return null;
        }

        log.debug("Generated value for {}: {}", funcName, result);
        return result;
    }

    /**
     * Nhận tất cả các chức năng tích hợp và mô tả của chúng
     *
     * @return Map<tên chức năng, Mô tả>
     */
    public Map<String, String> getAllFunctionsWithDescriptions() {
        Map<String, String> functions = new LinkedHashMap<>();
        for (String func : BUILT_IN_FUNCTIONS) {
            functions.put(func, getDescription(func));
        }
        return functions;
    }

    /**
     * Nhận mô tả về chức năng tích hợp
     *
     * @param funcName tên chức năng
     * @return Mô tả chức năng
     */
    private static String getDescription(String funcName) {
        try {
            return I18nUtil.getMessage("builtin.var." + funcName.substring(1));
        } catch (Exception e) {
            // Nếu không tìm thấy văn bản quốc tế hóa, mô tả mặc định sẽ được trả về
            return "Built-in function: " + funcName;
        }
    }

    // ==================== Các phương pháp trợ giúp ====================

    private static String generateRandomAlphaNumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String getRandomFirstName() {
        String[] names = {"John", "Jane", "Michael", "Emily", "David", "Sarah",
                "James", "Emma", "Robert", "Olivia", "William", "Sophia",
                "Daniel", "Isabella", "Matthew", "Mia"};
        return names[RANDOM.nextInt(names.length)];
    }

    private static String getRandomLastName() {
        String[] names = {"Smith", "Johnson", "Williams", "Brown", "Jones",
                "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
                "Anderson", "Taylor", "Thomas", "Moore", "Jackson"};
        return names[RANDOM.nextInt(names.length)];
    }

    private static String getRandomCity() {
        String[] cities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
                "Philadelphia", "San Antonio", "San Diego", "Dallas", "Austin",
                "London", "Paris", "Tokyo", "Beijing", "Shanghai"};
        return cities[RANDOM.nextInt(cities.length)];
    }

    private static String getRandomCountry() {
        String[] countries = {"United States", "United Kingdom", "Canada", "Australia", "Germany",
                "France", "Japan", "China", "Brazil", "India",
                "Mexico", "Spain", "Italy", "South Korea", "Netherlands"};
        return countries[RANDOM.nextInt(countries.length)];
    }

    private static String getRandomUserAgent() {
        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        };
        return userAgents[RANDOM.nextInt(userAgents.length)];
    }

    private static String generateMD5Hash(String input) {
        return DigestUtils.md5Hex(input);
    }

}
