package com.laker.postman.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV Lớp công cụ xử lý dữ liệu
 */
@Slf4j
@UtilityClass
public class CsvDataUtil {

    public record CsvTextData(List<String> headers, List<List<String>> rows) {
    }

    /**
     * Đọc CSV Dữ liệu tập tin
     *
     * @param csvFile CSV tập tin
     * @return Danh sách dữ liệu, được sử dụng cho từng hàng dữ liệu Map bày tỏ
     */
    public static List<Map<String, String>> readCsvData(File csvFile) {
        if (csvFile == null || !csvFile.exists() || !csvFile.isFile()) {
            log.warn("CSV Tệp không tồn tại hoặc không hợp lệ: {}", csvFile);
            return new ArrayList<>();
        }

        try {
            String content = FileUtil.readString(csvFile, StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                log.warn("CSV Nội dung tập tin trống: {}", csvFile.getAbsolutePath());
                return new ArrayList<>();
            }

            CsvTextData csvTextData = parseCsvText(content);
            List<String> headers = csvTextData.headers();
            List<List<String>> rows = csvTextData.rows();
            if (headers.isEmpty()) {
                log.warn("CSV Tệp không có dòng dữ liệu: {}", csvFile.getAbsolutePath());
                return new ArrayList<>();
            }

            List<Map<String, String>> dataList = new ArrayList<>();

            for (List<String> row : rows) {
                // sử dụngLinkedHashMapGiữ thứ tự các cột
                Map<String, String> rowData = new LinkedHashMap<>();

                for (int j = 0; j < headers.size() && j < row.size(); j++) {
                    String header = headers.get(j).trim();
                    String value = row.get(j);
                    rowData.put(header, value);
                }

                dataList.add(rowData);
            }

            log.debug("Đọc thành công CSV tập tin: {}, Số lượng hàng dữ liệu: {}", csvFile.getAbsolutePath(), dataList.size());
            return dataList;

        } catch (Exception e) {
            log.error("Đọc CSV Tệp không thành công: {}", csvFile.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Xác minh CSV Định dạng tập tin
     *
     * @param csvFile CSV tập tin
     * @return Thông tin kết quả xác minh
     */
    public static String validateCsvFile(File csvFile) {
        if (csvFile == null || !csvFile.exists()) {
            return I18nUtil.getMessage(MessageKeys.CSV_FILE_NOT_EXIST);
        }

        if (!csvFile.isFile()) {
            return I18nUtil.getMessage(MessageKeys.CSV_FILE_NOT_VALID);
        }

        if (!csvFile.getName().toLowerCase().endsWith(".csv")) {
            return I18nUtil.getMessage(MessageKeys.CSV_FILE_NOT_CSV);
        }

        try {
            List<Map<String, String>> data = readCsvData(csvFile);
            if (data.isEmpty()) {
                return I18nUtil.getMessage(MessageKeys.CSV_NO_VALID_DATA);
            }

            return "";
        } catch (Exception e) {
            return I18nUtil.getMessage(MessageKeys.CSV_LOAD_FAILED, e.getMessage());
        }
    }

    /**
     * Lấy CSV Thông tin tiêu đề cột của tập tin
     *
     * @param csvFile CSV tập tin
     * @return Danh sách các tiêu đề cột
     */
    public static List<String> getCsvHeaders(File csvFile) {
        try {
            String content = FileUtil.readString(csvFile, StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                return new ArrayList<>();
            }

            return parseCsvText(content).headers();
        } catch (Exception e) {
            log.error("Lấy CSV Tiêu đề cột không thành công: {}", csvFile.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Phân tách bằng dấu phẩy chuẩn CSV văn bản.
     * Hàng đầu tiên đóng vai trò là tiêu đề cột và các hàng tiếp theo đóng vai trò là hàng dữ liệu.
     */
    public static CsvTextData parseCsvText(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new CsvTextData(new ArrayList<>(), new ArrayList<>());
        }

        CsvReader reader = CsvUtil.getReader();
        CsvData csvData = reader.readFromStr(content);
        List<CsvRow> parsedRows = csvData.getRows();

        if (parsedRows.isEmpty()) {
            return new CsvTextData(new ArrayList<>(), new ArrayList<>());
        }

        List<String> headers = new ArrayList<>(parsedRows.get(0));
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < parsedRows.size(); i++) {
            rows.add(new ArrayList<>(parsedRows.get(i)));
        }
        return new CsvTextData(headers, rows);
    }
}
