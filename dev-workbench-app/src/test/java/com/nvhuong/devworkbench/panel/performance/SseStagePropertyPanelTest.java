package com.nvhuong.devworkbench.panel.performance;

import com.nvhuong.devworkbench.common.component.DevWorkbenchTextField;
import com.nvhuong.devworkbench.common.component.DevWorkbenchJSpinner;
import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.core.model.SsePerformanceData;
import com.nvhuong.devworkbench.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SseStagePropertyPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldHideEventFilterForFixedDurationMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        PerformanceTreeNode node = new PerformanceTreeNode("SSE Read", NodeType.SSE_READ);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.FIXED_DURATION;
        node.ssePerformanceData.eventNameFilter = "done";

        panel.setNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
    }

    @Test
    public void shouldHideEventFilterForFirstEventMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        PerformanceTreeNode node = new PerformanceTreeNode("SSE Read", NodeType.SSE_READ);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.SINGLE_MESSAGE;
        node.ssePerformanceData.eventNameFilter = "done";

        panel.setNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
    }

    @Test
    public void shouldShowOnlyCloseTimeoutForStreamClosedMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        PerformanceTreeNode node = new PerformanceTreeNode("SSE Read", NodeType.SSE_READ);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.STREAM_CLOSED;

        panel.setNode(node);

        assertFalse(eventNameFilterField(panel).isVisible());
        assertFalse(messageFilterField(panel).isVisible());
        assertFalse(readTimeoutSpinner(panel).isVisible());
        assertTrue(holdConnectionSpinner(panel).isVisible());
    }

    @Test
    public void shouldUseOnlyReceiveTimeoutForMessageCountMode() throws Exception {
        SseStagePropertyPanel panel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        PerformanceTreeNode node = new PerformanceTreeNode("SSE Read", NodeType.SSE_READ);
        node.ssePerformanceData = new SsePerformanceData();
        node.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.MESSAGE_COUNT;

        panel.setNode(node);

        assertTrue(readTimeoutSpinner(panel).isVisible());
        assertFalse(holdConnectionSpinner(panel).isVisible());
    }

    private DevWorkbenchTextField eventNameFilterField(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("eventNameFilterField");
        field.setAccessible(true);
        return (DevWorkbenchTextField) field.get(panel);
    }

    private DevWorkbenchTextField messageFilterField(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("messageFilterField");
        field.setAccessible(true);
        return (DevWorkbenchTextField) field.get(panel);
    }

    private DevWorkbenchJSpinner readTimeoutSpinner(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("readTimeoutSpinner");
        field.setAccessible(true);
        return (DevWorkbenchJSpinner) field.get(panel);
    }

    private DevWorkbenchJSpinner holdConnectionSpinner(SseStagePropertyPanel panel) throws Exception {
        Field field = SseStagePropertyPanel.class.getDeclaredField("holdConnectionSpinner");
        field.setAccessible(true);
        return (DevWorkbenchJSpinner) field.get(panel);
    }
}
