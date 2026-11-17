package com.laker.postman.service.collections;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Optional;

/**
 * Giao diện kho nút cây
 * <p>
 * Trách nhiệm: Cung cấp chức năng tìm kiếm và truy cập nút cây, cách lyUIcấu trúc cây phân lớp
 * <p>
 * Mẫu thiết kế:Repository Pattern（mô hình kho)
 * <p>
 *
 * @author laker
 * @since 4.3.22
 */
public interface TreeNodeRepository {

    /**
     * Theo yêu cầuIDTìm các nút
     *
     * @param requestId yêu cầuID
     * @return nút (nếu có)
     */
    Optional<DefaultMutableTreeNode> findNodeByRequestId(String requestId);

    /**
     * Lấy nút gốc
     *
     * @return nút gốc
     */
    Optional<DefaultMutableTreeNode> getRootNode();

}
