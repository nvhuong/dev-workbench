package com.nvhuong.devworkbench.service.curl;

import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;

import java.util.List;

/**
 * Cấu trúc dữ liệu tạm thời, được sử dụng để phân tích cú pháp curl Đặt hàng
 */
public class CurlRequest {
    public String url;
    public String method;
    public List<HttpHeader> headersList;
    public String body;
    public boolean binaryBody;
    public List<HttpParam> paramsList;
    // Được sử dụng để lưu trữ dữ liệu biểu mẫu được phân tích cú pháp (multipart/form-data)
    public List<HttpFormData> formDataList;
    // để lưu trữ application/x-www-form-urlencoded loại dữ liệu
    public List<HttpFormUrlencoded> urlencodedList;
    public boolean followRedirects = false; // Có nên theo chuyển hướng hay không
    public String authType;
    public String authUsername;
    public String authPassword;
    public List<CurlParseWarning> warnings;
}
