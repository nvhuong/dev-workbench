package com.nvhuong.devworkbench.service.curl;

import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility facade for cURL import/export.
 */
@UtilityClass
public class CurlParser {

    /**
     * phân tích cURL Chuỗi lệnh là  CurlRequest đối tượng.
     *
     * @param curl cURL chuỗi lệnh
     * @return Sau khi phân tích CurlRequest đối tượng
     */
    public static CurlRequest parse(String curl) {
        if (curl == null || curl.trim().isEmpty()) {
            CurlRequest req = new CurlRequest();
            req.method = "GET";
            return req;
        }

        List<String> commands = splitCommands(curl);
        String command = commands.isEmpty() ? curl : commands.get(0);
        ShellCommand shellCommand = CurlCommandTokenizer.parseCommand(command);
        CurlCommandOptions options = CurlOptionParser.parse(shellCommand.argv());
        options.warnings.addAll(0, shellCommand.warnings());
        return CurlRequestAssembler.assemble(options);
    }

    /**
     * Việc phân tích cú pháp có chứa một hoặc nhiều mục cURL Văn bản của lệnh.
     *
     * @param curlText cURL Văn bản lệnh
     * @return Danh sách các yêu cầu được phân tích từng cái một
     */
    public static List<CurlRequest> parseMany(String curlText) {
        List<CurlRequest> requests = new ArrayList<>();
        for (String command : splitCommands(curlText)) {
            requests.add(parse(command));
        }
        return requests;
    }

    /**
     * Sẽ được dán theo đợt Bash cURL Văn bản được chia thành các lệnh độc lập.
     *
     * @param curlText cURL Văn bản lệnh
     * @return độc lập cURL Danh sách lệnh
     */
    public static List<String> splitCommands(String curlText) {
        return CurlCommandSplitter.split(curlText);
    }

    /**
     * Sẽ PreparedRequest Chuyển đổi sang  cURL chuỗi lệnh.
     */
    public static String toCurl(PreparedRequest preparedRequest) {
        return CurlRequestExporter.toCurl(preparedRequest);
    }

    /**
     * Sẽ thực sự được gửi bởi lớp mạng PreparedRequest Ảnh chụp nhanh được chuyển đổi thành cURL chuỗi lệnh.
     */
    public static String toActualCurl(PreparedRequest preparedRequest) {
        return CurlRequestExporter.toActualCurl(preparedRequest);
    }

    /**
     * phán xét PreparedRequest Liệu có lớp mạng nào thực sự gửi ảnh chụp nhanh hay không.
     */
    public static boolean canExportActualCurl(PreparedRequest preparedRequest) {
        return CurlRequestExporter.hasSentSnapshot(preparedRequest);
    }
}
