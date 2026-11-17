package com.laker.postman.collection.model;

import com.laker.postman.model.Environment;
import com.laker.postman.request.model.HttpRequestItem;


import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * thống nhất Collection Phân tích kết quả
 * Hỗ trợ nhóm và yêu cầu theo cấu trúc cây
 * Kết quả được phân tích cú pháp có sẵn ở nhiều định dạng nhập khác nhau
 */
@Getter
public class CollectionParseResult {
    /**
     * Thông tin nhóm
     */
    private final RequestGroup group;

    /**
     * Danh sách các nút con (có thể là nhóm con hoặc yêu cầu)
     */
    private final List<CollectionNode> children;

    /**
     * Danh sách các biến môi trường (tùy chọn, dùng để tạo môi trường khi nhập)
     */
    private final List<Environment> environments;

    public CollectionParseResult(RequestGroup group) {
        this.group = group;
        this.children = new ArrayList<>();
        this.environments = new ArrayList<>();
    }

    public void addChild(CollectionNode child) {
        this.children.add(child);
    }

    /**
     * Thêm các biến môi trường
     */
    public void addEnvironment(Environment environment) {
        if (environment != null) {
            this.environments.add(environment);
        }
    }

    /**
     * Thêm nhiều biến môi trường
     */
    public void addEnvironments(List<Environment> envList) {
        if (envList != null && !envList.isEmpty()) {
            this.environments.addAll(envList);
        }
    }

    /**
     * Phương pháp nhà máy tiện lợi: Tạo cấu trúc phẳng của các kết quả được phân tích cú pháp
     *
     * @param group Thông tin nhóm
     * @param requests Danh sách yêu cầu
     * @return Phân tích kết quả
     */
    public static CollectionParseResult createFlat(RequestGroup group, List<HttpRequestItem> requests) {
        CollectionParseResult result = new CollectionParseResult(group);
        for (HttpRequestItem request : requests) {
            result.addChild(new CollectionNode(CollectionNodeType.REQUEST, request));
        }
        return result;
    }
}
