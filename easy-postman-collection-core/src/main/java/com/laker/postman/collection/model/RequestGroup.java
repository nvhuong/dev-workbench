package com.laker.postman.collection.model;

import com.laker.postman.model.Variable;
import com.laker.postman.request.model.AuthApiKeyPlacement;
import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.HttpHeader;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RequestGroup Lớp đại diện cho một mục cấu hình yêu cầu nhóm
 * Chứa tên nhóm, xác thực, tập lệnh và thông tin khác
 * Xác thực cấp nhóm và tập lệnh được kế thừa bởi các yêu cầu bên dưới chúng
 */
@Getter
@Setter
public class RequestGroup implements Serializable {
    private String id = ""; // Mã định danh duy nhất
    private String name = ""; // Tên nhóm
    private String description = ""; // Mô tả nhóm
    private String authType = AuthType.INHERIT.getConstant(); // Loại xác thực (none/apiKey/basic/bearer/digest/inherit），Các nhóm kế thừa xác thực gốc theo mặc định
    private String authUsername = ""; // Basic/DigestTên người dùng
    private String authPassword = ""; // Basic/Digestmật khẩu
    private String authToken = "";    // Bearer Token
    private String authApiKeyName = ""; // API Key tên
    private String authApiKeyValue = ""; // API Key giá trị
    private String authApiKeyPlacement = AuthApiKeyPlacement.HEADER.getConstant(); // API Key Thêm vị trí
    // Pre-script (thực thi trước khi có yêu cầu)
    private String prescript = "";
    // Postscript (được thực hiện sau khi phản hồi)
    private String postscript = "";
    // Tiêu đề yêu cầu công khai (sẽ được kế thừa bởi các yêu cầu phụ)
    private List<HttpHeader> headers = new ArrayList<>();
    // Các biến cấp độ nhóm (sẽ được kế thừa bởi các yêu cầu phụ)
    private List<Variable> variables = new ArrayList<>();

    public RequestGroup() {
        this.id = UUID.randomUUID().toString();
    }

    public RequestGroup(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    /**
     * Xác định xem nhóm có cấu hình xác thực hay không
     * cần phải được loại trừ cùng một lúc NONE và INHERIT Nhập, chỉ khi xác thực thực tế được định cấu hình (Basic/Bearer/Digest v.v.) trước khi quay lại true
     */
    public boolean hasAuth() {
        return authType != null
                && !AuthType.NONE.getConstant().equals(authType)
                && !AuthType.INHERIT.getConstant().equals(authType);
    }

    /**
     * Xác định xem nhóm có kịch bản trước hay không
     */
    public boolean hasPreScript() {
        return prescript != null && !prescript.trim().isEmpty();
    }

    /**
     * Xác định xem nhóm có post-script hay không
     */
    public boolean hasPostScript() {
        return postscript != null && !postscript.trim().isEmpty();
    }

    /**
     * Xác định xem nhóm có tiêu đề yêu cầu chung hay không
     */
    public boolean hasHeaders() {
        return headers != null && !headers.isEmpty();
    }

    /**
     * Xác định xem nhóm có biến hay không
     */
    public boolean hasVariables() {
        return variables != null && !variables.isEmpty();
    }
}
