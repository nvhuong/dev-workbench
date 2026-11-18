package com.nvhuong.devworkbench.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CsvDataUtilTest {

    @Test(description = "tiêu chuẩn CSV Cần hỗ trợ ngắt dòng trong các trường được trích dẫn kép")
    public void shouldParseQuotedMultilineField() {
        CsvDataUtil.CsvTextData csvTextData = CsvDataUtil.parseCsvText(
                "name,comment\n" +
                        "alice,\"line1\nline2\"\n"
        );

        assertEquals(csvTextData.headers().size(), 2);
        assertEquals(csvTextData.rows().size(), 1);
        assertEquals(csvTextData.rows().get(0).get(1), "line1\nline2");
    }

    @Test(description = "tiêu chuẩn CSV Khoảng trắng trước và sau các trường không được cắt bớt vô điều kiện")
    public void shouldPreserveFieldWhitespace() {
        CsvDataUtil.CsvTextData csvTextData = CsvDataUtil.parseCsvText(
                "name,value\n" +
                        "alice,  keep-space  \n"
        );

        assertEquals(csvTextData.rows().size(), 1);
        assertEquals(csvTextData.rows().get(0).get(1), "  keep-space  ");
    }
}
