package com.laker.postman.panel.collections.tree.coordinator;

import com.laker.postman.collection.CollectionIdNormalizer;
import com.laker.postman.common.component.notification.NotificationCenter;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Workspace;
import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.dialog.TextInputDialog;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.tree.action.TreeNodeCloner;
import com.laker.postman.panel.collections.tree.action.TreeStateHelper;
import com.laker.postman.panel.collections.tree.dialog.AddRequestDialog;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.panel.collections.tree.adapter.SwingCollectionTreePersistence;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.http.request.HttpRequestFactory;
import com.laker.postman.http.request.PreparedRequestFactory;
import com.laker.postman.http.request.PreparedRequestFinalizer;
import com.laker.postman.service.postman.PostmanCollectionExporter;
import com.laker.postman.panel.workspace.WorkspaceTransferCoordinator;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


import static com.laker.postman.panel.collections.tree.CollectionTreePanel.*;

/**
 * Request tree interaction coordinator.
 * Điều phối tập trung các thay đổi nút cây, hộp thoại, tính bền vững và liên kết bảng điều khiển liên quan.
 */
@Slf4j
public class RequestTreeCoordinator {
    private final JTree requestTree;
    private final CollectionTreePanel leftPanel;
    private final List<HttpRequestItem> copiedRequests = new ArrayList<>();

    /**
     * Đường cây cần bảo vệ: .addHttpRequestDirectly Đặt giá trị này sau khi tạo yêu cầu mới.
     * Chỉ bị chặn trong lần nhấp chuột này BasicTreeUI Ghi đè trạng thái đã chọn cho các yêu cầu mới.
     * mouseReleased Sau đó bởi finishRequestCreationSelection Thông thoáng.
     */
    private TreePath pendingSelectPath = null;

    public RequestTreeCoordinator(JTree requestTree, CollectionTreePanel leftPanel) {
        this.requestTree = requestTree;
        this.leftPanel = leftPanel;
        // Đăng ký bảo vệ đã chọn: chỉ pendingSelectPath Không null，Chỉ cần chặn bất kỳ ghi đè bên ngoài nào của trạng thái đã chọn
        requestTree.addTreeSelectionListener(e -> {
            TreePath guard = pendingSelectPath;
            if (guard == null) return;
            TreePath current = requestTree.getSelectionPath();
            if (!guard.equals(current)) {
                // BasicTreeUI Hoặc mã khác đã thay đổi lựa chọn, hãy sửa ngay
                pendingSelectPath = null; // Xóa trước để tránh đệ quy
                requestTree.setSelectionPath(guard);
                requestTree.scrollPathToVisible(guard);
            }
        });
    }

    /**
     * Hiển thị hộp thoại Thêm nhóm
     */
    public void showAddGroupDialog(DefaultMutableTreeNode parentNode) {
        if (!canContainGroup(parentNode)) return;

        TextInputDialog.showRequiredName(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_GROUP_TITLE),
                "",
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_GROUP_EMPTY)
        ).ifPresent(groupName -> addGroupToNode(parentNode, groupName));
    }

    /**
     * Thêm một nhóm dưới nút được chỉ định
     */
    private void addGroupToNode(DefaultMutableTreeNode parentNode, String groupName) {
        if (!canContainGroup(parentNode)) return;
        RequestGroup group = new RequestGroup(groupName);
        // Tham khảo Postman：Cấp độ đầu tiên Collection Mặc định No Auth，lớp phụ Folder Mặc định Inherit auth from parent
        boolean isRootLevel = ROOT.equals(String.valueOf(parentNode.getUserObject()));
        if (isRootLevel) {
            group.setAuthType(AuthType.NONE.getConstant());
        } else {
            group.setAuthType(AuthType.INHERIT.getConstant());
        }
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);

        int insertIdx = getGroupInsertIndex(parentNode);
        if (insertIdx >= 0 && insertIdx <= parentNode.getChildCount()) {
            parentNode.insert(groupNode, insertIdx);
        } else {
            parentNode.add(groupNode);
        }

        leftPanel.getTreeModel().reload(parentNode);
        requestTree.expandPath(new TreePath(parentNode.getPath()));

        leftPanel.getCollectionTreePersistence().saveCurrentTree();
    }

    private boolean canContainGroup(DefaultMutableTreeNode node) {
        return node != null && (CollectionTreeNodes.isGroup(node)
                || node == requestTree.getModel().getRoot()
                || (leftPanel != null && node == leftPanel.getRootTreeNode()));
    }

    /**
     * Thêm một nhóm dưới nút đã chọn
     */
    public void addGroupUnderSelected() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;
        showAddGroupDialog(selectedNode);
    }

    /**
     * Hiển thị hộp thoại Thêm yêu cầu
     */
    public void showAddRequestDialog(DefaultMutableTreeNode groupNode) {
        if (!CollectionTreeNodes.isGroup(groupNode)) return;
        AddRequestDialog dialog = new AddRequestDialog(groupNode, leftPanel);
        dialog.show();
    }

    /**
     * Tạo một mặc định trực tiếp trong nhóm được chỉ định HTTP GET Yêu cầu, không hiện hộp thoại (tương tự Postman Bấm vào "+" hành vi)
     */
    public void addHttpRequestDirectly(DefaultMutableTreeNode groupNode) {
        if (!CollectionTreeNodes.isGroup(groupNode)) return;
        HttpRequestItem item = HttpRequestFactory.createBlankRequest(RequestItemProtocolEnum.HTTP);
        item.setName("New Request");
        DefaultMutableTreeNode reqNode = CollectionTreeNodes.requestNode(item);
        leftPanel.getTreeModel().insertNodeInto(reqNode, groupNode, groupNode.getChildCount());
        JTree tree = leftPanel.getRequestTree();
        tree.expandPath(new TreePath(groupNode.getPath()));
        leftPanel.getCollectionTreePersistence().saveCurrentTree();

        TreePath newPath = new TreePath(reqNode.getPath());
        // Đặt đường dẫn bảo vệ:TreeSelectionListener Sẽ chặn BasicTreeUI Vào mouseReleased bên trong
        // Đã kích hoạt selectPathForEvent，Lựa chọn cuối cùng được đảm bảo ở trên nút được yêu cầu mới
        pendingSelectPath = newPath;
        UiSingletonFactory.getInstance(RequestEditorPanel.class).showOrCreateTab(item);
        tree.setSelectionPath(newPath);
        tree.scrollPathToVisible(newPath);
    }

    /**
     * Hoàn thành việc bảo vệ lựa chọn cho yêu cầu mới này để tránh ảnh hưởng đến thư mục hoặc yêu cầu được nhấp tiếp theo.
     */
    public void finishRequestCreationSelection() {
        TreePath newPath = pendingSelectPath;
        pendingSelectPath = null;
        if (newPath != null) {
            requestTree.setSelectionPath(newPath);
            requestTree.scrollPathToVisible(newPath);
        }
    }


    /**
     * Đổi tên mục đã chọn (nhóm, yêu cầu hoặc phản hồi đã lưu)
     */
    public void renameSelectedItem() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;

        if (CollectionTreeNodes.isGroup(selectedNode)) {
            renameGroup(selectedNode);
        } else if (CollectionTreeNodes.isRequest(selectedNode)) {
            renameRequest(selectedNode);
        } else if (CollectionTreeNodes.isSavedResponse(selectedNode)) {
            renameSavedResponse(selectedNode);
        }
    }

    /**
     * Đổi tên nhóm
     */
    private void renameGroup(DefaultMutableTreeNode selectedNode) {
        RequestGroup group = CollectionTreeNodes.group(selectedNode).orElse(null);
        if (group == null) {
            return;
        }
        String oldName = group.getName();

        TextInputDialog.showRequiredName(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_GROUP_TITLE),
                oldName,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_GROUP_EMPTY)
        ).ifPresent(newName -> {
            if (newName.equals(oldName)) {
                return;
            }
            updateGroupName(selectedNode, group, newName);
        });
    }

    /**
     * Cập nhật tên nhóm
     */
    private void updateGroupName(DefaultMutableTreeNode node, RequestGroup group, String newName) {
        group.setName(newName);
        leftPanel.getTreeModel().nodeChanged(node);

        leftPanel.getCollectionTreePersistence().saveCurrentTree();
    }

    /**
     * Yêu cầu đổi tên
     */
    private void renameRequest(DefaultMutableTreeNode selectedNode) {
        HttpRequestItem item = CollectionTreeNodes.request(selectedNode).orElse(null);
        if (item == null) {
            return;
        }
        String oldName = item.getName();

        TextInputDialog.showRequiredName(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_REQUEST_TITLE),
                oldName,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_REQUEST_EMPTY)
        ).ifPresent(newName -> {
            if (newName.equals(oldName)) {
                return;
            }
            updateRequestName(selectedNode, item, newName);
        });
    }

    /**
     * Cập nhật tên yêu cầu
     */
    private void updateRequestName(DefaultMutableTreeNode node, HttpRequestItem item, String newName) {
        item.setName(newName);
        leftPanel.getTreeModel().nodeChanged(node);
        leftPanel.getCollectionTreePersistence().saveCurrentTree();

        // Cập nhật đồng bộ hóa đang bậtTabtiêu đề
        updateOpenedTabsTitle(item, newName);
    }

    /**
     * Cập nhật đang được bậtTabtiêu đề
     */
    private void updateOpenedTabsTitle(HttpRequestItem item, String newName) {
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                HttpRequestItem tabItem = subPanel.getCurrentRequest();
                if (tabItem != null && item.getId().equals(tabItem.getId())) {
                    tabbedPane.setTitleAt(i, newName);
                    tabbedPane.setTabComponentAt(i,
                            ClosableTabComponent.forRequest(newName, item));
                    subPanel.initPanelData(item);
                }
            }
        }
    }

    /**
     * Đổi tên phản hồi đã lưu
     */
    private void renameSavedResponse(DefaultMutableTreeNode selectedNode) {
        SavedResponse savedResponse = CollectionTreeNodes.savedResponse(selectedNode).orElse(null);
        if (savedResponse == null) {
            return;
        }
        String oldName = savedResponse.getName();

        TextInputDialog.showRequiredName(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_SAVED_RESPONSE_TITLE),
                oldName,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_SAVED_RESPONSE_EMPTY)
        ).ifPresent(newName -> {
            if (newName.equals(oldName)) {
                return;
            }
            updateSavedResponseName(selectedNode, savedResponse, newName);
        });
    }

    /**
     * Cập nhật tên phản hồi đã lưu
     */
    private void updateSavedResponseName(DefaultMutableTreeNode node, SavedResponse savedResponse, String newName) {
        savedResponse.setName(newName);
        leftPanel.getTreeModel().nodeChanged(node);
        leftPanel.getCollectionTreePersistence().saveCurrentTree();

        // Cập nhật đồng bộ hóa đang bậtTabtiêu đề
        updateOpenedSavedResponseTabsTitle(savedResponse, newName);
    }

    /**
     * Cập nhật phản hồi lưu đã mởTabtiêu đề
     */
    private void updateOpenedSavedResponseTabsTitle(SavedResponse savedResponse, String newName) {
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                if (subPanel.isSavedResponseTab()) {
                    SavedResponse tabSavedResponse = subPanel.getSavedResponse();
                    if (tabSavedResponse != null && savedResponse.getId().equals(tabSavedResponse.getId())) {
                        tabbedPane.setTitleAt(i, newName);
                        tabbedPane.setTabComponentAt(i,
                                new ClosableTabComponent(newName, RequestItemProtocolEnum.SAVED_RESPONSE, false));
                    }
                }
            }
        }
    }

    /**
     * Xóa các mục đã chọn (hỗ trợ nhiều lựa chọn)
     */
    public void deleteSelectedItem() {
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) return;

        List<DefaultMutableTreeNode> nodesToDelete = filterDeletableNodes(selectedPaths);
        if (nodesToDelete.isEmpty()) return;

        if (!confirmDelete(nodesToDelete.size())) return;

        performDelete(nodesToDelete);
    }

    /**
     * Lọc các nút có thể bị xóa
     */
    private List<DefaultMutableTreeNode> filterDeletableNodes(TreePath[] paths) {
        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node != null && node != leftPanel.getRootTreeNode() && node.getParent() != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * Xác nhận xóa
     */
    private boolean confirmDelete(int count) {
        String message = count == 1
                ? I18nUtil.getMessage(MessageKeys.COLLECTIONS_DELETE_CONFIRM)
                : I18nUtil.getMessage(MessageKeys.COLLECTIONS_DELETE_BATCH_CONFIRM, count);

        int confirm = JOptionPane.showConfirmDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                message,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION
        );

        return confirm == JOptionPane.YES_OPTION;
    }

    /**
     * Thực hiện thao tác xóa
     */
    private void performDelete(List<DefaultMutableTreeNode> nodesToDelete) {
        List<TreePath> expandedPaths = TreeStateHelper.saveExpandedPaths(requestTree);

        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();

        // Liên quan chặt chẽTab
        for (DefaultMutableTreeNode node : nodesToDelete) {
            closeTabsForNode(node, tabbedPane);
        }

        // Xóa nút
        for (DefaultMutableTreeNode node : nodesToDelete) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            if (parent != null) {
                parent.remove(node);
            }
        }

        leftPanel.getTreeModel().reload();
        TreeStateHelper.restoreExpandedPaths(requestTree, expandedPaths, leftPanel.getRootTreeNode());

        // Điều chỉnhTabtrạng thái đã chọn
        if (tabbedPane.getTabCount() > 1) {
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
        }

        leftPanel.getCollectionTreePersistence().saveCurrentTree();
    }

    /**
     * Đóng nút liên quanTab
     */
    private void closeTabsForNode(DefaultMutableTreeNode node, JTabbedPane tabbedPane) {
        HttpRequestItem item = CollectionTreeNodes.request(node).orElse(null);
        if (item != null) {
            // Xóa danh sách các câu trả lời đã lưu để đảm bảo rằng các câu trả lời đã lưu không được giữ lại khi bị xóa
            if (item.getResponse() != null) {
                item.getResponse().clear();
            }
            closeRequestTabs(item, tabbedPane);
        } else if (CollectionTreeNodes.isGroup(node)) {
            closeTabsForGroup(node, tabbedPane);
        } else if (CollectionTreeNodes.isSavedResponse(node)) {
            // Xử lý việc xóa các nút phản hồi đã lưu
            deleteSavedResponseNode(node);
        }
    }

    /**
     * Đóng yêu cầuTab
     */
    private void closeRequestTabs(HttpRequestItem item, JTabbedPane tabbedPane) {
        RequestEditorPanel requestEditPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                HttpRequestItem tabItem = subPanel.getCurrentRequest();
                if (tabItem != null && item.getId().equals(tabItem.getId())) {
                    requestEditPanel.removeTabAtWithCleanup(i);
                }
            }
        }
    }

    /**
     * Đệ quy đóng tất cả các yêu cầu trong nhómTab
     */
    private void closeTabsForGroup(DefaultMutableTreeNode groupNode, JTabbedPane tabbedPane) {
        for (int i = 0; i < groupNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
            closeTabsForNode(child, tabbedPane);
        }
    }

    /**
     * Sao chép yêu cầu đã chọn (tạo bản sao)
     */
    public void duplicateSelectedRequests() {
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) return;

        List<RequestCopyInfo> copyInfos = collectCopyInfos(selectedPaths);
        if (copyInfos.isEmpty()) return;

        for (RequestCopyInfo info : copyInfos) {
            createDuplicateRequest(info);
        }

        leftPanel.getCollectionTreePersistence().saveCurrentTree();
        NotificationCenter.showSuccess(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_COPY_SUCCESS, copyInfos.size())
        );
    }

    /**
     * Thu thập thông tin sao chép
     */
    private List<RequestCopyInfo> collectCopyInfos(TreePath[] paths) {
        List<RequestCopyInfo> infos = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            HttpRequestItem item = CollectionTreeNodes.request(node).orElse(null);
            if (item != null) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                int idx = parent.getIndex(node);
                infos.add(new RequestCopyInfo(item, parent, idx));
            }
        }
        return infos;
    }

    /**
     * Tạo một yêu cầu nhân rộng
     */
    private void createDuplicateRequest(RequestCopyInfo info) {
        HttpRequestItem copy = copyWithSuffix(info.item);
        DefaultMutableTreeNode copyNode = CollectionTreeNodes.requestNodeWithExamples(copy);
        info.parent.insert(copyNode, info.index + 1);
        leftPanel.getTreeModel().reload(info.parent);
        requestTree.expandPath(new TreePath(info.parent.getPath()));
    }

    /**
     * Sao chép yêu cầu đã chọn vào clipboard
     */
    public void copySelectedRequests() {
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) return;

        copiedRequests.clear();
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            HttpRequestItem item = CollectionTreeNodes.request(node).orElse(null);
            if (item != null) {
                HttpRequestItem copy = JsonUtil.deepCopy(item, HttpRequestItem.class);
                copiedRequests.add(copy);
            }
        }

        if (!copiedRequests.isEmpty()) {
            NotificationCenter.showSuccess(
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_COPIED_TO_CLIPBOARD, copiedRequests.size())
            );
        }
    }

    /**
     * Dán yêu cầu
     */
    public void pasteRequests() {
        if (copiedRequests.isEmpty()) return;

        DefaultMutableTreeNode targetParent = determineTargetParent();
        if (targetParent == null) return;

        for (HttpRequestItem copiedItem : copiedRequests) {
            createPastedRequest(targetParent, copiedItem);
        }

        leftPanel.getTreeModel().reload(targetParent);
        requestTree.expandPath(new TreePath(targetParent.getPath()));
        leftPanel.getCollectionTreePersistence().saveCurrentTree();

        NotificationCenter.showSuccess(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_PASTE_SUCCESS, copiedRequests.size())
        );
    }

    /**
     * Xác định nút cha mục tiêu để dán
     */
    private DefaultMutableTreeNode determineTargetParent() {
        DefaultMutableTreeNode targetNode = getSelectedNode();
        if (targetNode == null) {
            return leftPanel.getRootTreeNode();
        }

        if (targetNode == leftPanel.getRootTreeNode() || ROOT.equals(targetNode.getUserObject())) {
            return leftPanel.getRootTreeNode();
        }

        if (CollectionTreeNodes.isGroup(targetNode)) {
            return targetNode;
        }
        if (CollectionTreeNodes.isRequest(targetNode)) {
            return (DefaultMutableTreeNode) targetNode.getParent();
        }

        return leftPanel.getRootTreeNode();
    }

    /**
     * Tạo một yêu cầu dán
     */
    private void createPastedRequest(DefaultMutableTreeNode parent, HttpRequestItem copiedItem) {
        parent.add(CollectionTreeNodes.requestNodeWithExamples(copyWithSuffix(copiedItem)));
    }

    private static HttpRequestItem copyWithSuffix(HttpRequestItem source) {
        HttpRequestItem copy = CollectionIdNormalizer.copyRequestWithNewIds(source);
        copy.setName(source.getName() + " " + I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_SUFFIX));
        return copy;
    }

    /**
     * Yêu cầu sao chép làcURLĐặt hàng
     */
    public void copySelectedRequestAsCurl() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;

        HttpRequestItem item = CollectionTreeNodes.request(selectedNode).orElse(null);
        if (item == null) return;
        try {
            PreparedRequest req = PreparedRequestFactory.build(item);
            PreparedRequestFinalizer.finalizeForSend(req, item);
            String curl = CurlParser.toCurl(req);

            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(curl), null);

            NotificationCenter.showSuccess(
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_CURL_SUCCESS)
            );
        } catch (Exception ex) {
            NotificationCenter.showError(
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_CURL_FAIL, ex.getMessage())
            );
        }
    }

    /**
     * Sao chép nhóm đã chọn
     */
    public void duplicateSelectedGroup() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;

        if (!CollectionTreeNodes.isGroup(selectedNode)) return;

        DefaultMutableTreeNode copyNode = TreeNodeCloner.deepCopyGroupNode(selectedNode);
        RequestGroup copiedGroup = CollectionTreeNodes.group(copyNode).orElse(null);
        if (copiedGroup == null) {
            return;
        }
        String originalName = copiedGroup.getName();
        String newName = originalName + I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_SUFFIX);
        copiedGroup.setName(newName);

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
        if (parent != null) {
            int idx = parent.getIndex(selectedNode) + 1;
            parent.insert(copyNode, idx);
            leftPanel.getTreeModel().reload(parent);
            requestTree.expandPath(new TreePath(parent.getPath()));

            leftPanel.getCollectionTreePersistence().saveCurrentTree();
        }
    }

    /**
     * Xuất được nhóm thànhPostman Collection
     */
    public void exportGroupAsPostman(DefaultMutableTreeNode groupNode) {
        if (!isValidGroupNode(groupNode)) {
            showInvalidGroupWarning();
            return;
        }

        String groupName = CollectionTreeNodes.group(groupNode).orElseThrow().getName();

        SystemFileChooser fileChooser = FileChooserUtil.createSaveFileChooser(
                "collections.export.postman",
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_EXPORT_POSTMAN_DIALOG_TITLE));
        fileChooser.setSelectedFile(new File(groupName + "-postman.json"));

        int userSelection = fileChooser.showSaveDialog(UiSingletonFactory.getInstance(MainFrame.class));
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            exportToFile(groupNode, groupName, fileChooser.getSelectedFile());
        }
    }

    /**
     * Xuất sang tập tin
     */
    private void exportToFile(DefaultMutableTreeNode groupNode, String groupName, File file) {
        try {
            JSONObject postmanCollection =
                    PostmanCollectionExporter.buildPostmanCollectionFromTreeNode(groupNode, groupName);
            FileUtil.writeUtf8String(postmanCollection.toStringPretty(), file);
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_SUCCESS));
        } catch (Exception ex) {
            log.error("Export Postman error", ex);
            NotificationCenter.showError(
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_FAIL, ex.getMessage())
            );
        }
    }

    /**
     * Chuyển bộ sưu tập sang không gian làm việc khác
     */
    public void moveCollectionToWorkspace(DefaultMutableTreeNode selectedNode) {
        if (!isValidGroupNode(selectedNode)) return;

        String collectionName = CollectionTreeNodes.group(selectedNode).orElseThrow().getName();

        WorkspaceTransferCoordinator.transferToWorkspace(
                collectionName,
                (targetWorkspace, itemName) -> performCollectionMove(selectedNode, targetWorkspace)
        );
    }

    /**
     * Bộ sưu tập thực thi được chuyển đến không gian làm việc đích
     * Di chuyển một bộ sưu tập từ không gian làm việc hiện tại sang không gian làm việc đích
     *
     * @param collectionNode  Nút thu thập sẽ di chuyển
     * @param targetWorkspace Mục tiêu không gian làm việc
     */
    private void performCollectionMove(DefaultMutableTreeNode collectionNode, Workspace targetWorkspace) {
        // 1. Sao chép sâu nút thu thập (bao gồm tất cả các nút con)
        DefaultMutableTreeNode copiedNode = TreeNodeCloner.deepCopyGroupNode(collectionNode);

        // 2. Lấy đường dẫn tệp bộ sưu tập của không gian làm việc đích
        String targetCollectionPath = SystemUtil.getCollectionPathForWorkspace(targetWorkspace);

        // 3. Tạo một công cụ kiên trì cho không gian làm việc mục tiêu
        DefaultMutableTreeNode targetRootNode = new DefaultMutableTreeNode(ROOT);
        DefaultTreeModel targetTreeModel = new DefaultTreeModel(targetRootNode);
        SwingCollectionTreePersistence targetPersistence = new SwingCollectionTreePersistence(
                targetCollectionPath, targetRootNode, targetTreeModel);

        // 4. Tải bộ sưu tập hiện có của không gian làm việc đích
        targetPersistence.loadIntoTree();

        // 5. Thêm bộ sưu tập vào không gian làm việc mục tiêu
        targetRootNode.add(copiedNode);

        // 6. Lưu vào không gian làm việc mục tiêu
        targetPersistence.saveCurrentTree();

        // 7. Xóa bộ sưu tập gốc khỏi không gian làm việc hiện tại
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) collectionNode.getParent();
        if (parent != null) {
            parent.remove(collectionNode);
            leftPanel.getTreeModel().reload();

            leftPanel.getCollectionTreePersistence().saveCurrentTree();
        }

        log.info("Successfully moved collection '{}' to workspace '{}'",
                CollectionTreeNodes.group(collectionNode).map(RequestGroup::getName).orElse(""), targetWorkspace.getName());
    }

    /**
     * Thêm các yêu cầu đã chọn vào các bài kiểm tra chức năng
     */
    public void addSelectedRequestsToFunctionalTest() {
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length == 0) return;

        List<HttpRequestItem> requestsToAdd = new ArrayList<>();
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            collectRequestsRecursively(node, requestsToAdd);
        }

        if (requestsToAdd.isEmpty()) return;

        try {
            FunctionalPanel functionalPanel = UiSingletonFactory.getInstance(FunctionalPanel.class);
            functionalPanel.loadRequests(requestsToAdd);

            // Chuyển sang thử nghiệm chức năngTab
            SidebarTabPanel sidebarPanel = UiSingletonFactory.getInstance(SidebarTabPanel.class);
            JTabbedPane tabbedPane = sidebarPanel.getTabbedPane();
            tabbedPane.setSelectedIndex(3); // Kiểm tra chức năngTabchỉ số
        } catch (Exception e) {
            log.error("Thêm yêu cầu vào kiểm tra chức năng không thành công", e);
        }
    }

    // ==================== Các phương pháp trợ giúp ====================

    private DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
    }

    private int getGroupInsertIndex(DefaultMutableTreeNode parent) {
        if (parent == null) return -1;
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (CollectionTreeNodes.isRequest(child)) {
                return i;
            }
        }
        return parent.getChildCount();
    }

    private boolean isValidGroupNode(DefaultMutableTreeNode node) {
        return node != null && CollectionTreeNodes.isGroup(node);
    }

    private void showInvalidGroupWarning() {
        JOptionPane.showMessageDialog(
                leftPanel,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_EXPORT_POSTMAN_SELECT_GROUP),
                I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void collectRequestsRecursively(DefaultMutableTreeNode node, List<HttpRequestItem> list) {
        CollectionTreeNodes.request(node).ifPresent(list::add);
        if (CollectionTreeNodes.isGroup(node)) {
            for (int i = 0; i < node.getChildCount(); i++) {
                collectRequestsRecursively((DefaultMutableTreeNode) node.getChildAt(i), list);
            }
        }
    }

    public boolean isCopiedRequestsEmpty() {
        return copiedRequests.isEmpty();
    }

    /**
     * Xóa phản hồi đã lưu khỏi yêu cầu gốc
     */
    private void deleteSavedResponseNode(DefaultMutableTreeNode savedResponseNode) {
        // Lấy nút cha (phải là REQUEST nút)
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) savedResponseNode.getParent();
        if (parentNode == null) return;

        HttpRequestItem parentRequest = CollectionTreeNodes.request(parentNode).orElse(null);
        SavedResponse toDelete = CollectionTreeNodes.savedResponse(savedResponseNode).orElse(null);
        if (parentRequest == null || toDelete == null) return;

        // Được yêu cầu từ Chúa Cha response bị loại khỏi danh sách
        if (parentRequest.getResponse() != null) {
            parentRequest.getResponse().removeIf(resp ->
                    resp.getId() != null && resp.getId().equals(toDelete.getId())
            );
        }
    }

    /**
     * Yêu cầu sao chép thông tin lớp bên trong
     */
    private static class RequestCopyInfo {
        final HttpRequestItem item;
        final DefaultMutableTreeNode parent;
        final int index;

        RequestCopyInfo(HttpRequestItem item, DefaultMutableTreeNode parent, int index) {
            this.item = item;
            this.parent = parent;
            this.index = index;
        }
    }
}
