package com.nvhuong.devworkbench.panel.functional;

import com.nvhuong.devworkbench.functional.model.RunnerRowData;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.common.UiSingletonPanel;
import com.nvhuong.devworkbench.common.component.CsvDataPanel;
import com.nvhuong.devworkbench.functional.model.FunctionalConfigSnapshot;
import com.nvhuong.devworkbench.functional.model.FunctionalCsvDataState;
import com.nvhuong.devworkbench.panel.functional.table.FunctionalRunnerTableModel;
import com.nvhuong.devworkbench.service.FunctionalPersistenceService;
import org.testng.annotations.Test;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class FunctionalPanelSaveTest {

    @Test(description = "Lưu rõ ràng sau khi gửi chỉnh sửa biểu mẫu sẽ hủy lưu chống rung được kích hoạt lại bởi sự kiện chỉnh sửa")
    public void shouldCancelAutosaveScheduledByCellEditingCommit() throws Exception {
        FunctionalPanel panel = newPanelWithoutInit();
        FunctionalRunnerTableModel tableModel = new FunctionalRunnerTableModel();
        JTable table = new JTable(tableModel);
        RecordingFunctionalPersistenceService persistenceService = new RecordingFunctionalPersistenceService();

        tableModel.addRow(new RunnerRowData(requestItem()));
        installSelectionAutosaveListener(panel, tableModel);
        setField(panel, "tableModel", tableModel);
        setField(panel, "table", table);
        setField(panel, "persistenceService", persistenceService);

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(table.editCellAt(0, 0));
            if (table.getEditorComponent() instanceof JCheckBox checkBox) {
                checkBox.setSelected(false);
            }
            panel.save();
        });

        assertEquals(persistenceService.syncSaveCount.get(), 1);
        assertFalse(
                persistenceService.asyncSaveLatch.await(800, TimeUnit.MILLISECONDS),
                "The autosave scheduled by stopCellEditing() must be canceled by the explicit save"
        );
    }

    @Test(description = "Collections Lưu đồng bộ hóa vào FunctionalPanel Ảnh chụp nhanh được yêu cầu của các hàng trong bảng phải được làm mới")
    public void shouldRefreshRowWhenSyncingCollectionItem() throws Exception {
        FunctionalPanel panel = newPanelWithoutInit();
        FunctionalRunnerTableModel tableModel = new FunctionalRunnerTableModel();
        HttpRequestItem oldItem = requestItem();
        tableModel.addRow(new RunnerRowData(oldItem));
        setField(panel, "tableModel", tableModel);

        HttpRequestItem latestItem = requestItem();
        latestItem.setUrl("https://new.example.com");

        panel.syncRequestItem(latestItem);

        RunnerRowData row = tableModel.getRow(0);
        assertEquals(row.url, "https://new.example.com");
        assertEquals(row.requestItem.getUrl(), "https://new.example.com");
    }

    @Test(description = "FunctionalPanel nên lưu lại khi lưu UI CSV Ảnh chụp nhanh được chuyển đổi sang trạng thái liên tục chức năng")
    public void shouldSaveFunctionalCsvDataStateFromCsvPanelSnapshot() throws Exception {
        FunctionalPanel panel = newPanelWithoutInit();
        FunctionalRunnerTableModel tableModel = new FunctionalRunnerTableModel();
        JTable table = new JTable(tableModel);
        CsvDataPanel csvDataPanel = new CsvDataPanel();
        RecordingFunctionalPersistenceService persistenceService = new RecordingFunctionalPersistenceService();

        SwingUtilities.invokeAndWait(() -> csvDataPanel.restoreState(new CsvDataPanel.CsvState(
                "users.csv",
                List.of("username"),
                List.of(Map.of("username", "alice"))
        )));

        setField(panel, "tableModel", tableModel);
        setField(panel, "table", table);
        setField(panel, "csvDataPanel", csvDataPanel);
        setField(panel, "persistenceService", persistenceService);

        SwingUtilities.invokeAndWait(panel::save);

        FunctionalCsvDataState savedState = persistenceService.lastCsvState.get();
        assertNotNull(savedState);
        assertEquals(savedState.getSourceName(), "users.csv");
        assertEquals(savedState.getHeaders(), List.of("username"));
        assertEquals(savedState.getRows().get(0).get("username"), "alice");
    }

    private static FunctionalPanel newPanelWithoutInit() throws Exception {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            return new FunctionalPanel();
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private static HttpRequestItem requestItem() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("req-save-order");
        item.setName("Save order");
        item.setMethod("GET");
        item.setUrl("https://example.com");
        return item;
    }

    private static void installSelectionAutosaveListener(FunctionalPanel panel,
                                                         FunctionalRunnerTableModel tableModel) throws Exception {
        Method scheduleSave = FunctionalPanel.class.getDeclaredMethod("scheduleSave");
        scheduleSave.setAccessible(true);
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
                try {
                    scheduleSave.invoke(panel);
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError(ex);
                }
            }
        });
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class RecordingFunctionalPersistenceService extends FunctionalPersistenceService {
        private final AtomicInteger syncSaveCount = new AtomicInteger();
        private final CountDownLatch asyncSaveLatch = new CountDownLatch(1);
        private final AtomicReference<FunctionalCsvDataState> lastCsvState = new AtomicReference<>();

        @Override
        public void save(FunctionalConfigSnapshot snapshot) {
            syncSaveCount.incrementAndGet();
            lastCsvState.set(snapshot.getCsvState());
        }

        @Override
        public void saveAsync(FunctionalConfigSnapshot snapshot) {
            asyncSaveLatch.countDown();
        }
    }
}
