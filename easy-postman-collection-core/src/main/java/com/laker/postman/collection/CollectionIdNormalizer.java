package com.laker.postman.collection;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Đảm bảo nhóm, yêu cầu trong một bộ sưu tập.Example（SavedResponse）của ID chỉ một.
 * <p>
 * Biên tập viên Tab Nhấn ID Tìm các yêu cầu mở/Example，ID Trùng lặp sẽ khiến nhấp chuột ở lại một cái khác Tab（issue #194）。
 * phiên bản lịch sử Postman Nhập khẩu một lần {@code _postman_previewlanguage}（Chẳng hạn như json）Như Example ID，
 * Nhập cùng một bản sao nhiều lần EasyPostman JSON Nó cũng sẽ mang lại những yêu cầu trùng lặp/Example ID。
 */
@UtilityClass
public class CollectionIdNormalizer {

    /**
     * Đã có trong bộ sưu tập ID，Tách các bản ghi theo loại nút.
     */
    public record UsedIds(Set<String> groups, Set<String> requests, Set<String> examples) {
        public static UsedIds empty() {
            return new UsedIds(new HashSet<>(), new HashSet<>(), new HashSet<>());
        }

        private UsedIds copy() {
            return new UsedIds(new HashSet<>(groups), new HashSet<>(requests), new HashSet<>(examples));
        }
    }

    /**
     * Thu thập tất cả các khoảng trống không trống trong tài liệu ID。
     */
    public static UsedIds collectIds(CollectionDocument document) {
        UsedIds ids = UsedIds.empty();
        forEachNode(document, node -> {
            if (node.isGroup()) {
                addIfPresent(ids.groups(), node.asGroup().getId());
            } else if (node.isRequest()) {
                HttpRequestItem request = node.asRequest();
                addIfPresent(ids.requests(), request.getId());
                for (SavedResponse example : examples(request)) {
                    addIfPresent(ids.examples(), example.getId());
                }
            }
        });
        return ids;
    }

    /**
     * Để trống hoặc trùng lặp ID phân phối lại UUID，xuất hiện lần đầu tiên ID vẫn không thay đổi.
     *
     * @return được bổ nhiệm lại ID số lượng nút
     */
    public static int ensureUniqueIds(CollectionDocument document) {
        return ensureUniqueIds(document, UsedIds.empty());
    }

    /**
     * Để trống, lặp lại hoặc AND {@code reserved} Xung đột ID phân phối lại UUID，Được sử dụng để tránh xung đột tài liệu chéo khi nhập vào bộ sưu tập hiện có.
     *
     * @return được bổ nhiệm lại ID số lượng nút
     */
    public static int ensureUniqueIds(CollectionDocument document, UsedIds reserved) {
        UsedIds seen = reserved == null ? UsedIds.empty() : reserved.copy();
        int[] reassigned = {0};
        forEachNode(document, node -> {
            if (node.isGroup()) {
                RequestGroup group = node.asGroup();
                String newId = uniqueId(group.getId(), seen.groups());
                if (newId != null) {
                    group.setId(newId);
                    reassigned[0]++;
                }
            } else if (node.isRequest()) {
                HttpRequestItem request = node.asRequest();
                String newId = uniqueId(request.getId(), seen.requests());
                if (newId != null) {
                    request.setId(newId);
                    reassigned[0]++;
                }
                for (SavedResponse example : examples(request)) {
                    String newExampleId = uniqueId(example.getId(), seen.examples());
                    if (newExampleId != null) {
                        example.setId(newExampleId);
                        reassigned[0]++;
                    }
                }
            }
        });
        return reassigned[0];
    }

    /**
     * Sao chép sâu yêu cầu, và cho yêu cầu và yêu cầu của nó Example Chỉ định mới ID，Tránh xung đột với yêu cầu xuất xứ.
     */
    public static HttpRequestItem copyRequestWithNewIds(HttpRequestItem source) {
        HttpRequestItem copy = JsonUtil.deepCopy(source, HttpRequestItem.class);
        copy.setId(newId());
        for (SavedResponse example : examples(copy)) {
            example.setId(newId());
        }
        return copy;
    }

    /**
     * hiện tại ID Đăng ký và quay lại khi có sẵn null，Nếu không thì đăng ký và trả lại một cái mới UUID。
     */
    private static String uniqueId(String id, Set<String> seen) {
        if (id != null && !id.isBlank() && seen.add(id)) {
            return null;
        }
        String newId = newId();
        seen.add(newId);
        return newId;
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private static void addIfPresent(Set<String> ids, String id) {
        if (id != null && !id.isBlank()) {
            ids.add(id);
        }
    }

    private static List<SavedResponse> examples(HttpRequestItem request) {
        if (request.getResponse() == null) {
            return List.of();
        }
        return request.getResponse().stream().filter(Objects::nonNull).toList();
    }

    private static void forEachNode(CollectionDocument document, Consumer<CollectionNode> visitor) {
        if (document == null) {
            return;
        }
        for (CollectionNode root : document.getRoots()) {
            visit(root, visitor);
        }
    }

    private static void visit(CollectionNode node, Consumer<CollectionNode> visitor) {
        if (node == null) {
            return;
        }
        if ((node.isGroup() && node.asGroup() == null) || (node.isRequest() && node.asRequest() == null)) {
            return;
        }
        visitor.accept(node);
        for (CollectionNode child : node.getChildren()) {
            visit(child, visitor);
        }
    }
}
