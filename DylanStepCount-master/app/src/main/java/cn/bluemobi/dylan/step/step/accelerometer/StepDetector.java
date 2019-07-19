package cn.bluemobi.dylan.step.step.accelerometer;

/**
 * Created by dylan on 16/9/27.
 */

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
/*
* Phần chính của thuật toán, phát hiện xem đó có phải là một bước không
* */

public class StepDetector implements SensorEventListener {

    // Lưu trữ dữ liệu ba trục
    float[] oriValues = new float[3];
    final int ValueNum = 4;
    // được sử dụng để lưu trữ giá trị đỉnh của ngưỡng tính toán
    float[] tempValue = new float[ValueNum];
    int tempCount = 0;
    //Cho dù cờ tăng
    boolean isDirectionUp = false;
    //Tăng liên tục
    int continueUpCount = 0;
    //Số lần tăng liên tiếp của điểm trước đó, để ghi lại số lần tăng của đỉnh
    int continueUpFormerCount = 0;
    //Trạng thái của điểm trước, tăng hoặc giảm
    boolean lastStatus = false;
    //đỉnh són
    float peakOfWave = 0;
    //giá trị thung lũng
    float valleyOfWave = 0;
    // Thời gian của đỉnh này
    long timeOfThisPeak = 0;
    //Thời gian của đỉnh cuối cùng
    long timeOfLastPeak = 0;
    //thời gian hiện tại
    long timeOfNow = 0;
    //Giá trị của cảm biến hiện tại
    float gravityNew = 0;
    //Giá trị của cảm biến cuối cùng
    float gravityOld = 0;
    //Ngưỡng động yêu cầu dữ liệu động, giá trị này được sử dụng cho ngưỡng của các dữ liệu động này
    final float InitialValue = (float) 1.3;
    //ngưỡng ban đầu
    float ThreadValue = (float) 2.0;
    //Chênh lệch thời gian cao điểm
    int TimeInterval = 250;
    private StepCountListener mStepListeners;

    @Override
    public void onSensorChanged(SensorEvent event) {
        for (int i = 0; i < 3; i++) {
            oriValues[i] = event.values[i];
        }
        gravityNew = (float) Math.sqrt(oriValues[0] * oriValues[0]
                + oriValues[1] * oriValues[1] + oriValues[2] * oriValues[2]);
        detectorNewStep(gravityNew);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //
    }

    public void initListener(StepCountListener listener) {
        this.mStepListeners = listener;
    }

    /*
    * Phát hiện các bước và bắt đầu đếm
     * 1. Dữ liệu đến trong sersor
     * 2. Nếu đỉnh được phát hiện và chênh lệch thời gian và điều kiện ngưỡng được đáp ứng, nó được đánh giá là 1 bước.
     * 3. Theo điều kiện chênh lệch thời gian, nếu chênh lệch đỉnh-thung lũng lớn hơn giá trị ban đầu, chênh lệch được bao gồm trong tính toán ngưỡng.
    * */
    public void detectorNewStep(float values) {
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (detectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();
                if (timeOfNow - timeOfLastPeak >= TimeInterval
                        && (peakOfWave - valleyOfWave >= ThreadValue)) {
                    timeOfThisPeak = timeOfNow;
                    /*
                     * Cập nhật quá trình xử lý giao diện, không liên quan đến thuật toán
                      * Thường thêm xử lý sau trước giao diện cập nhật thông báo, để xử lý chuyển động không hợp lệ:
                      * 1. Ghi liên tục 10 để bắt đầu đếm
                      * 2. Ví dụ: nếu người dùng 9 bước của bản ghi dừng hơn 3 giây, bản ghi trước đó không hợp lệ và lần tiếp theo bắt đầu từ đầu.
                      * 3. Ghi liên tục 9 bước, người dùng vẫn đang tập thể dục, dữ liệu trước đó là hợp lệ.
                     * */
                    mStepListeners.countStep();
                }
                if (timeOfNow - timeOfLastPeak >= TimeInterval
                        && (peakOfWave - valleyOfWave >= InitialValue)) {
                    timeOfThisPeak = timeOfNow;
                    ThreadValue = peakValleyThread(peakOfWave - valleyOfWave);
                }
            }
        }
        gravityOld = values;
    }

    /*
     *Đỉnh phát hiện
      * Bốn điều kiện sau đây được đánh giá là đỉnh:
      * 1. Điểm hiện tại là xu hướng giảm: isDirectionUp là sai
      * 2. Điểm trước đó là xu hướng tăng: lastStatus là đúng
      * 3. Cho đến khi đạt đỉnh, tiếp tục tăng hơn hoặc bằng 2 lần
      * 4. Giá trị cực đại lớn hơn 20
      * Ghi lại giá trị máng
      * 1. Quan sát sơ đồ dạng sóng, bạn có thể thấy rằng ở nơi bước xuất hiện, sóng tiếp theo là cực đại, có các đặc điểm và sự khác biệt rõ ràng.
      * 2. Vì vậy, ghi lại giá trị máng cho mỗi lần, để so sánh với đỉnh tiếp theo
     * */
    public boolean detectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }

        if (!isDirectionUp && lastStatus
                && (continueUpFormerCount >= 2 || oldValue >= 20)) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }

    /*
     * Tính toán ngưỡng
      * 1. Tính ngưỡng bằng chênh lệch giữa các đỉnh và thung lũng
      * 2. Ghi lại 4 giá trị và lưu trữ chúng trong mảng tempValue [].
      * 3. Tính ngưỡng trong mảng được truyền cho hàm AverageValue
     * */
    public float peakValleyThread(float value) {
        float tempThread = ThreadValue;
        if (tempCount < ValueNum) {
            tempValue[tempCount] = value;
            tempCount++;
        } else {
            tempThread = averageValue(tempValue, ValueNum);
            for (int i = 1; i < ValueNum; i++) {
                tempValue[i - 1] = tempValue[i];
            }
            tempValue[ValueNum - 1] = value;
        }
        return tempThread;

    }

    /*
     * Ngưỡng độ dốc
      * 1. Tính giá trị trung bình của mảng
      * 2. Gradient ngưỡng trong một phạm vi bằng giá trị trung bình
      * * /
     * */
    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / ValueNum;
        if (ave >= 8)
            ave = (float) 4.3;
        else if (ave >= 7 && ave < 8)
            ave = (float) 3.3;
        else if (ave >= 4 && ave < 7)
            ave = (float) 2.3;
        else if (ave >= 3 && ave < 4)
            ave = (float) 2.0;
        else {
            ave = (float) 1.3;
        }
        return ave;
    }

}
