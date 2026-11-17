package com.laker.postman.panel.collections.tree.adapter;

import com.laker.postman.collection.CollectionInheritance;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.service.collections.CollectionTreeNodes;


import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;


/**
 * Swing collection tree Kế thừa bộ chuyển đổi bối cảnh.
 * <p>
 * Chỉ chịu trách nhiệm ở đây Swing tree node chiết xuất groupChain và nhóm các biến; xác thực, tập lệnh, tiêu đề yêu cầu, v.v. hợp nhất các quy tắc theo
 * collection-core của {@link CollectionInheritance} Xử lý thống nhất .
 */
@UtilityClass
public class SwingCollectionInheritanceAdapter {

    /**
     * Thu thập chuỗi nhóm (từ ngoài vào trong)
     * <p>
     * Điểm tối ưu hóa:
     * - Sử dụng phép lặp thay vì đệ quy để tránh tràn ngăn xếp
     * - Đi thẳng lên trên, hiệu suất là O(h)，h là chiều cao của cây
     */
    public static List<RequestGroup> collectGroupChain(DefaultMutableTreeNode startNode) {
        List<RequestGroup> groupChain = new ArrayList<>();

        if (startNode == null) {
            return groupChain;
        }

        TreeNode currentNode = startNode.getParent(); // Bắt đầu từ nút cha

        while (currentNode != null) {
            if (!(currentNode instanceof DefaultMutableTreeNode treeNode)) {
                break;
            }

            // Kiểm tra xem nó có phải là nút gốc không
            if (treeNode.getUserObject() == null || "root".equals(String.valueOf(treeNode.getUserObject()))) {
                break;
            }

            CollectionTreeNodes.group(treeNode)
                    .ifPresent(group -> groupChain.add(0, group)); // Chèn vào đầu danh sách, giữ nguyên thứ tự từ ngoài vào trong

            currentNode = currentNode.getParent();
        }

        return groupChain;
    }

    /**
     * Lấy các biến cấp độ nhóm đã hợp nhất
     * <p>
     * Được sử dụng bởi trình phân giải biến để thu được tất cả các biến nhóm được kế thừa bởi yêu cầu hiện tại
     * Mức độ ưu tiên thay đổi: nhóm bên trong ghi đè nhóm bên ngoài
     *
     * @param requestNode Yêu cầu nút trong cây
     * @return Danh sách biến đã hợp nhất được sắp xếp theo mức độ ưu tiên (lớp bên trong trước)
     */
    public static List<Variable> getMergedGroupVariables(DefaultMutableTreeNode requestNode) {
        if (requestNode == null) {
            return new ArrayList<>();
        }

        List<RequestGroup> groupChain = collectGroupChain(requestNode);
        if (groupChain.isEmpty()) {
            return new ArrayList<>();
        }

        return CollectionInheritance.mergeGroupVariables(groupChain);
    }

}
