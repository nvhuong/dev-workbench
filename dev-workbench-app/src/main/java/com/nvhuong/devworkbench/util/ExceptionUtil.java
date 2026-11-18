package com.nvhuong.devworkbench.util;

import lombok.experimental.UtilityClass;

/**
 * Logic phán đoán của bộ lọc ngoại lệ chưa được nắm bắt toàn cầu.
 */
@UtilityClass
public class ExceptionUtil {

    /**
     * Kiểm tra xem có nên bỏ qua ngoại lệ hay không (không hiển thị cho người dùng)
     * <p>
     * Phương pháp này được sử dụng để lọc ra các ngoại lệ vô hại, không ảnh hưởng đến hoạt động của người dùng và được tạo ra bởi quá trình dọn dẹp nội bộ của thư viện bên thứ ba.
     * </p>
     *
     * @param throwable Ngoại lệ để kiểm tra
     * @return Trả về nếu ngoại lệ bị bỏ quatrue，Còn không thì quay lạifalse
     */
    public static boolean shouldIgnoreException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        return isSshExecutorShutdownError(throwable);
    }

    /**
     * Kiểm tra xem cóSSH executor shutdownsai lầm
     * <p>
     * Những lỗi này thường xảy raGit SSHSau khi hoạt động hoàn tất, không đồng bộIOHoạt động đã cố gắng sử dụng một hoạt động đã đóngexecutor。
     * Đây làApache SSHD + JGitHành vi đã biết và không ảnh hưởng đến chức năng thực tế.
     * </p>
     *
     * @param throwable Ngoại lệ để kiểm tra
     * @return trong trường hợpSSH shutdownLỗi được trả vềtrue，Còn không thì quay lạifalse
     */
    private static boolean isSshExecutorShutdownError(Throwable throwable) {
        // Kiểm tra xem có IllegalStateException và tin nhắn chứa nội dung cụ thể
        if (!(throwable instanceof IllegalStateException)) {
            return false;
        }

        String message = throwable.getMessage();
        if (message == null || !message.contains("Executor has been shut down")) {
            return false;
        }

        // Kiểm tra xem ngăn xếp cuộc gọi có chứaSSHCác lớp liên quan
        return stackTraceContains(throwable,
                "org.apache.sshd",
                "NoCloseExecutor",
                "AsynchronousChannelGroup",
                "AsynchronousSocketChannel");
    }


    /**
     * Kiểm tra xem ngăn xếp cuộc gọi của ngoại lệ có chứa đoạn tên lớp được chỉ định hay không
     *
     * @param throwable      Ngoại lệ để kiểm tra
     * @param classNameParts Đoạn tên lớp (varargs)
     * @return Trả về nếu ngăn xếp cuộc gọi chứa bất kỳ đoạn nào của tên lớp đã chỉ địnhtrue
     */
    private static boolean stackTraceContains(Throwable throwable, String... classNameParts) {
        if (throwable == null || classNameParts == null || classNameParts.length == 0) {
            return false;
        }

        for (StackTraceElement element : throwable.getStackTrace()) {
            String className = element.getClassName();
            for (String part : classNameParts) {
                if (className.contains(part)) {
                    return true;
                }
            }
        }
        return false;
    }
}
