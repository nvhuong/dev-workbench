package com.nvhuong.devworkbench.performance.core.threadgroup;

import com.nvhuong.devworkbench.util.MessageKeys;

/**
 * Mô hình dữ liệu nhóm luồng hỗ trợ nhiều chế độ luồng
 */
public class ThreadGroupData {
    public static final int MIN_THREADS = 1;
    public static final int DEFAULT_MAX_IN_FLIGHT_WAIT_SECONDS = 5;
    private static final int MIN_SECONDS = 1;
    private static final int MIN_LOOPS = 1;
    private static final int MIN_STEP = 1;

    // Loại nhóm chủ đề
    public enum ThreadMode {
        FIXED(MessageKeys.THREADGROUP_MODE_FIXED),           // Số lượng chủ đề cố định
        RAMP_UP(MessageKeys.THREADGROUP_MODE_RAMP_UP),       // Tăng số lượng chủ đề
        SPIKE(MessageKeys.THREADGROUP_MODE_SPIKE),           // Chế độ đột biến
        STAIRS(MessageKeys.THREADGROUP_MODE_STAIRS);         // Chế độ thang

        private final String messageKey;

        ThreadMode(String messageKey) {
            this.messageKey = messageKey;
        }

        public String getMessageKey() {
            return messageKey;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    // Tài sản công cộng
    public ThreadMode threadMode = ThreadMode.FIXED;  // Số lượng chủ đề cố định mặc định
    public int numThreads = 20;                        // Mẫu cố định-Số lượng người dùng mặc định
    public int duration = 60;                         // Tất cả các chế độ-Thời lượng mặc định(giây)
    public int loops = 1;                             // Mẫu cố định-Số vòng lặp mặc định
    public boolean useTime = true;                   // Có nên sử dụng thời gian thay vì số vòng lặp hay không
    public int maxInFlightWaitSeconds = DEFAULT_MAX_IN_FLIGHT_WAIT_SECONDS; // Khoảng thời gian tối đa để chờ hoàn tất các yêu cầu trên chuyến bay sau khi hết hạn

    // Thuộc tính chế độ tăng
    public int rampUpStartThreads = 1;                // Tăng số lượng chủ đề bắt đầu
    public int rampUpEndThreads = 20;                 // Tăng số lượng chủ đề cuối cùng
    public int rampUpTime = 30;                       // Tăng thời gian(giây)
    public int rampUpDuration = 60;                  // Chế độ tăng tổng thời lượng thử nghiệm(giây)

    // Thuộc tính chế độ tăng đột biến
    public int spikeMinThreads = 1;                   // Tăng số lượng chủ đề tối thiểu
    public int spikeMaxThreads = 20;                  // Số lượng chủ đề tối đa cho gai
    public int spikeRampUpTime = 20;                  // Spike tăng thời gian(giây)
    public int spikeHoldTime = 15;                     // Gai giữ thời gian(giây)
    public int spikeRampDownTime = 20;                // Thời gian giảm đột ngột(giây)
    public int spikeDuration = 60;                   // Tổng thời gian thử nghiệm chế độ tăng đột biến(giây)


    // Thuộc tính chế độ bậc thang
    public int stairsStartThreads = 5;                // Số lượng ren bắt đầu của thang
    public int stairsEndThreads = 20;                 // Số lượng chủ đề cuối cùng trong thang
    public int stairsStep = 5;                        // Chiều dài bậc thang
    public int stairsHoldTime = 15;                   // Giữ thời gian cho từng giai đoạn(giây)
    public int stairsDuration = 60;                  // Tổng thời gian thử nghiệm ở chế độ bậc thang(giây)

    public void normalize() {
        if (threadMode == null) {
            threadMode = ThreadMode.FIXED;
        }

        numThreads = atLeast(numThreads, MIN_THREADS);
        duration = atLeast(duration, MIN_SECONDS);
        loops = atLeast(loops, MIN_LOOPS);
        maxInFlightWaitSeconds = atLeast(maxInFlightWaitSeconds, MIN_SECONDS);

        rampUpStartThreads = atLeast(rampUpStartThreads, MIN_THREADS);
        rampUpEndThreads = atLeast(rampUpEndThreads, MIN_THREADS);
        if (rampUpStartThreads > rampUpEndThreads) {
            int previousStart = rampUpStartThreads;
            rampUpStartThreads = rampUpEndThreads;
            rampUpEndThreads = previousStart;
        }
        rampUpTime = atLeast(rampUpTime, MIN_SECONDS);
        rampUpDuration = atLeast(rampUpDuration, MIN_SECONDS);

        spikeMinThreads = atLeast(spikeMinThreads, MIN_THREADS);
        spikeMaxThreads = atLeast(spikeMaxThreads, MIN_THREADS);
        if (spikeMinThreads > spikeMaxThreads) {
            int previousMin = spikeMinThreads;
            spikeMinThreads = spikeMaxThreads;
            spikeMaxThreads = previousMin;
        }
        spikeRampUpTime = atLeast(spikeRampUpTime, MIN_SECONDS);
        spikeHoldTime = atLeast(spikeHoldTime, 0);
        spikeRampDownTime = atLeast(spikeRampDownTime, MIN_SECONDS);
        spikeDuration = atLeast(spikeDuration, MIN_SECONDS);

        stairsStartThreads = atLeast(stairsStartThreads, MIN_THREADS);
        stairsEndThreads = atLeast(stairsEndThreads, MIN_THREADS);
        if (stairsStartThreads > stairsEndThreads) {
            int previousStart = stairsStartThreads;
            stairsStartThreads = stairsEndThreads;
            stairsEndThreads = previousStart;
        }
        stairsStep = atLeast(stairsStep, MIN_STEP);
        stairsHoldTime = atLeast(stairsHoldTime, MIN_SECONDS);
        stairsDuration = atLeast(stairsDuration, MIN_SECONDS);
    }

    private static int atLeast(int value, int min) {
        return Math.max(min, value);
    }
}
