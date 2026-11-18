package com.nvhuong.devworkbench.platform.update.source;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

/**
 * Cập nhật giao diện nguồn - Xác định các thông số kỹ thuật thu thập thông tin phát hành và kiểm tra phiên bản thống nhất
 *
 * <p>Các nguồn cập nhật khác nhau (GitHub、Gitee v.v.) cần triển khai giao diện này để cung cấp phương thức truy cập thống nhất.</p>
 */
public interface UpdateSource {

    /** Trường nội bộ để đánh dấu nguồn trong thông báo phát hành. */
    String SOURCE_METADATA_KEY = "_source";

    /**
     * Lấy tên của nguồn cập nhật
     *
     * @return Cập nhật tên nguồn (ví dụ "GitHub", "Gitee"）
     */
    String getName();

    /**
     * Nhận phiên bản mới nhất API URL
     *
     * @return API địa chỉ
     */
    String getApiUrl();

    /**
     * Nhận tất cả các phiên bản được sắp xếp theo ngày phát hành từ mới nhất đến cũ nhất API URL
     *
     * @return Tất cả các phiên bản API địa chỉ
     */
    String getAllReleasesApiUrl();

    /**
     * Nhận trang phát hành web URL
     *
     * @return Địa chỉ trang phát hành web
     */
    String getWebUrl();

    /**
     * Kiểm tra tốc độ kết nối của nguồn cập nhật
     *
     * @return Thời gian phản hồi (mili giây), được trả về nếu kết nối không thành công Long.MAX_VALUE
     */
    long testConnectionSpeed();

    /**
     * Nhận thông tin phát hành phiên bản mới nhất
     *
     * @return Công bố thông tin JSON Đối tượng, được trả lại nếu việc mua lại không thành công null
     */
    JSONObject fetchLatestReleaseInfo();

    /**
     * Nhận thông tin phiên bản riêng lẻ theo nhãn phát hành.
     *
     * <p>Các nguồn cập nhật tùy chỉnh không hỗ trợ truy vấn theo thẻ có thể giữ nguyên cách triển khai mặc định.</p>
     *
     * @param tagName Thẻ phát hành đầy đủ, ví dụ {@code v6.3.1}
     * @return Thông tin phát hành tương ứng; được trả về khi thẻ không tồn tại, yêu cầu không thành công hoặc nguồn cập nhật hiện tại không hỗ trợ thẻ đó {@code null}
     */
    default JSONObject fetchReleaseByTag(String tagName) {
        return null;
    }

    /**
     * Nhận tất cả thông tin phát hành phiên bản
     *
     * @param limit Giới hạn số lượng phiên bản được trả về.0 Cho biết không có hạn chế
     * @return Công bố thông tin JSON Mảng, được trả về nếu quá trình thu thập không thành công null
     */
    JSONArray fetchAllReleases(int limit);
}
