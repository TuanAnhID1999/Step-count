package cn.bluemobi.dylan.step.step.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.bluemobi.dylan.step.R;
import cn.bluemobi.dylan.step.activity.MainActivity;
import cn.bluemobi.dylan.step.step.UpdateUiCallBack;
import cn.bluemobi.dylan.step.step.accelerometer.StepCount;
import cn.bluemobi.dylan.step.step.accelerometer.StepValuePassListener;
import cn.bluemobi.dylan.step.step.bean.StepData;
import cn.bluemobi.dylan.step.step.utils.DbUtils;

public class StepService extends Service implements SensorEventListener {
    private String TAG = "StepService";
    /**
     * Mặc định là 30 giây để lưu trữ
     */
    private static int duration = 30 * 1000;
    /**
     * Ngày hiện tại
     */
    private static String CURRENT_DATE = "";
    /**
     * Đối tượng quản lý cảm biến
     */
    private SensorManager sensorManager;
    /**
     *BroadcastReceiver xử lý bắt sự kiện
     */
    private BroadcastReceiver mBatInfoReceiver;
    /**
     * Lưu đồng hồ bấm giờ
     */
    private TimeCount time;
    /**
     * Số lượng các bước hiện đang thực hiện
     */
    private int CURRENT_STEP;
    /**
     * Loại cảm biến bước Cảm biến.TYPE_STEP_COUNTER hoặc Cảm biến.TYPE_STEP_DETECTOR
     */
    private static int stepSensorType = -1;
    /**Số lượng bước hiện có được lấy từ hệ thống mỗi lần dịch vụ bước được bắt đầu lần đầu tiên
     *
     */
    private boolean hasRecord = false;
    /**
     * Số lượng các bước hiện có thu được trong hệ thống
     */
    private int hasStepCount = 0;
    /**
     * Bước cuối cùng
     */
    private int previousStepCount = 0;
    /**
     * Đối tượng quản lý thông báo
     */
    private NotificationManager mNotificationManager;
    /**
     * Số bước được thực hiện trong cảm biến gia tốc
     */
    private StepCount mStepCount;
    /**
     * IBinder对象，Cầu nối truyền dữ liệu đến Hoạt động
     */
    private StepBinder stepBinder = new StepBinder();
    /**
     * Cầu nối truyền dữ liệu đến Hoạt động...
     */
    private NotificationCompat.Builder mBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        initNotification();
        initTodayData();
        initBroadcastReceiver();
        new Thread(new Runnable() {
            public void run() {
                startStepDetector();
            }
        }).start();
        startTimeCount();

    }

    /**
     *Cầu nối và dữ liệu của họ ...
     *
     * @return
     */
    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    /**
     * Khởi tạo thanh thông báo
     */
    private void initNotification() {
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Today's steps" + CURRENT_STEP + "  steps")
                .setContentIntent(getDefalutIntent(Notification.FLAG_ONGOING_EVENT))
                .setWhen(System.currentTimeMillis())//Thời gian khi thông báo được tạo sẽ được hiển thị trong thông báo thông báo.
                .setPriority(Notification.PRIORITY_DEFAULT)//Đặt mức độ ưu tiên thông báo
                .setAutoCancel(false)//Đặt cờ này để cho phép thông báo tự động bị hủy khi người dùng nhấp vào bảng điều khiển
                .setOngoing(true)//ture，Đặt anh ta như một thông báo liên tục. Chúng thường được sử dụng để thể hiện một tác vụ nền, người dùng tích cực tham gia (như phát nhạc) hoặc đang chờ bằng cách nào đó, do đó chiếm thiết bị (như tải xuống tệp, hoạt động đồng bộ hóa, kết nối mạng đang hoạt động)
                .setSmallIcon(R.mipmap.logo);
        Notification notification = mBuilder.build();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(notifyId_Step, notification);
        Log.d(TAG, "initNotification()");
    }

    /**
     * Khởi tạo số bước trong ngày
     */
    private void initTodayData() {
        CURRENT_DATE = getTodayDate();
        DbUtils.createDb(this, "DylanStepCount");
        DbUtils.getLiteOrm().setDebugged(false);
        //获取当天的数据，用于展示
        List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENT_DATE});
        if (list.size() == 0 || list.isEmpty()) {
            CURRENT_STEP = 0;
        } else if (list.size() == 1) {
            Log.v(TAG, "StepData=" + list.get(0).toString());
            CURRENT_STEP = Integer.parseInt(list.get(0).getStep());
        } else {
            Log.v(TAG, "Sai ！");
        }
        if (mStepCount != null) {
            mStepCount.setSteps(CURRENT_STEP);
        }
        updateNotification();
    }

    /**
     * Đăng ký BroadcastReceiver
     */
    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        // Màn hình tắt phát sóng
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //Tắt máy phát sóng
        filter.addAction(Intent.ACTION_SHUTDOWN);
        //Màn hình phát sáng
        filter.addAction(Intent.ACTION_SCREEN_ON);
        //Mở khóa màn hình phát sóng
//        filter.addAction(Intent.ACTION_USER_PRESENT);
        // Phát sóng này sẽ được phát khi nhấn nút nguồn trong một thời gian dài để bật lên hộp thoại "Tắt máy" hoặc màn hình khóa.
     /// ví dụ: Đôi khi hộp thoại hệ thống được sử dụng, các quyền có thể cao và nó sẽ được phủ lên trên màn hình khóa hoặc trên hộp thoại "Tắt máy".
    // Vì vậy, hãy nghe chương trình phát này và ẩn cuộc hội thoại của bạn khi bạn nhận được nó, chẳng hạn như nhấp vào hộp thoại bật lên ở góc dưới bên phải của bảng.
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //Nghe ngày thay đổi
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Log.d(TAG, "screen on");
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Log.d(TAG, "screen off");
                    //Thay đổi thành 60 giây để lưu trữ
                    duration = 60000;
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    Log.d(TAG, "screen unlock");
//                    save();
                    //Thay đổi thành 30 giây để lưu trữ
                    duration = 30000;
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                    Log.i(TAG, " receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
                    //Lưu một lần
                    save();
                } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                    Log.i(TAG, " receive ACTION_SHUTDOWN");
                    save();
                } else if (Intent.ACTION_DATE_CHANGED.equals(action)) {//Bước thay đổi ngày được đặt lại về 0
//                    Logger.d("重置步数" + StepDcretor.CURRENT_STEP);
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                    //Bước thay đổi thời gian được đặt lại về 0
                    isCall();
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_TICK.equals(action)) {//Bước thay đổi ngày được đặt lại về 0
                    isCall();
//                    Logger.d("重置步数" + StepDcretor.CURRENT_STEP);
                    save();
                    isNewDay();
                }
            }
        };
        registerReceiver(mBatInfoReceiver, filter);
    }


    /**
     * Giám sát dữ liệu khởi tạo thay đổi 0 điểm vào ban đêm
     */
    private void isNewDay() {
        String time = "00:00";
        if (time.equals(new SimpleDateFormat("HH:mm").format(new Date())) || !CURRENT_DATE.equals(getTodayDate())) {
            initTodayData();
        }
    }


    /**
     *Theo dõi sự thay đổi thời gian để cảnh báo người dùng tập thể dục
     */
    private void isCall() {
        String time = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("achieveTime", "21:00");
        String plan = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("planWalk_QTY", "7000");
        String remind = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("remind", "1");
        Logger.d("time=" + time + "\n" +
                "new SimpleDateFormat(\"HH: mm\").format(new Date()))=" + new SimpleDateFormat("HH:mm").format(new Date()));
        if (("1".equals(remind)) &&
                (CURRENT_STEP < Integer.parseInt(plan)) &&
                (time.equals(new SimpleDateFormat("HH:mm").format(new Date())))
                ) {
            remindNotify();
        }

    }

    /**
     * Bắt đầu lưu dữ liệu bước
     */
    private void startTimeCount() {
        if (time == null) {
            time = new TimeCount(duration, 1000);
        }
        time.start();
    }

    /**
     * Cập nhật bước thông báo
     */
    private void updateNotification() {
        //设置点击跳转
        Intent hangIntent = new Intent(this, MainActivity.class);
        PendingIntent hangPendingIntent = PendingIntent.getActivity(this, 0, hangIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = mBuilder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Today's steps" + CURRENT_STEP + "steps")
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示
                .setContentIntent(hangPendingIntent)
                .build();
        mNotificationManager.notify(notifyId_Step, notification);
        if (mCallback != null) {
            mCallback.updateUi(CURRENT_STEP);
        }
        Log.d(TAG, "updateNotification()");
    }

    /**
     * Đối tượng nghe UI
     */
    private UpdateUiCallBack mCallback;

    /**
     * Đăng ký người nghe cập nhật giao diện người dùng
     *
     * @param paramICallback
     */
    public void registerCallback(UpdateUiCallBack paramICallback) {
        this.mCallback = paramICallback;
    }

    /**
     * 记步Notification的ID
     */
    int notifyId_Step = 100;
    /**
     * ID nhắc nhở của thông báo tập luyện
     */
    int notify_remind_id = 200;

    /**
     * Nhắc nhở thanh thông báo bài tập
     */
    private void remindNotify() {

        //Đặt nhảy nhấp
        Intent hangIntent = new Intent(this, MainActivity.class);
        PendingIntent hangPendingIntent = PendingIntent.getActivity(this, 0, hangIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        String plan = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("planWalk_QTY", "7000");
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Today steps" + CURRENT_STEP + " step")
                .setContentText("Mục tiêu khoảng cách" + (Integer.valueOf(plan) - CURRENT_STEP) + "Bước đi！")
                .setContentIntent(hangPendingIntent)
                .setTicker(getResources().getString(R.string.app_name) + "Nhắc nhở bạn bắt đầu tập thể dục")//Thông báo xuất hiện lần đầu tiên trên thanh thông báo, với hiệu ứng hoạt hình tăng
                .setWhen(System.currentTimeMillis())//Thời gian khi thông báo được tạo sẽ được hiển thị trong thông báo thông báo.
                .setPriority(Notification.PRIORITY_DEFAULT)//Đặt mức độ ưu tiên thông báo
                .setAutoCancel(true)//Đặt cờ này để cho phép thông báo tự động bị hủy khi người dùng nhấp vào bảng điều khiển
                .setOngoing(false)//ture，Đặt anh ta như một thông báo liên tục. Chúng thường được sử dụng để thể hiện một tác vụ nền, người dùng tích cực tham gia (như phát nhạc) hoặc đang chờ bằng cách nào đó, do đó chiếm thiết bị (như tải xuống tệp, hoạt động đồng bộ hóa, kết nối mạng đang hoạt động)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)//Cách dễ nhất và nhất quán nhất để thêm hiệu ứng âm thanh, đèn flash và rung vào thông báo là sử dụng mặc định của người dùng hiện tại, sử dụng thuộc tính mặc định, có thể được kết hợp:
                //Notification.DEFAULT_ALL  Notification.DEFAULT_SOUND Thêm âm thanh // requires VIBRATE permission
                .setSmallIcon(R.mipmap.logo);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(notify_remind_id, mBuilder.build());
    }

    /**
     *@ Nhận được cấp phát mặc định, để tránh lỗi từ 2.3 trở xuống
     *       * Thuộc tính @flags: thường trú ở đầu: Thông báo.FLAG_ONGOING_EVENT
     *       * Nhấp để xóa: Thông báo.FLAG_AUTO_CANCEL
     */
    public PendingIntent getDefalutIntent(int flags) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(), flags);
        return pendingIntent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return stepBinder;
    }

    /**
     * 向Activity传递数据的纽带
     */
    public class StepBinder extends Binder {

        /**
         * 获取当前service对象
         *
         * @return StepService
         */
        public StepService getService() {
            return StepService.this;
        }
    }

    /**
     * 获取当前步数
     *
     * @return
     */
    public int getStepCount() {
        return CURRENT_STEP;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Lấy ví dụ cảm biến
     */
    private void startStepDetector() {
        if (sensorManager != null) {
            sensorManager = null;
        }
        // Lấy một phiên bản của Trình quản lý cảm biến
        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);
        //android4.4 Cảm biến bước truy cập có thể được sử dụng sau
        int VERSION_CODES = Build.VERSION.SDK_INT;
        if (VERSION_CODES >= 19) {
            addCountStepListener();
        } else {
            addBasePedometerListener();
        }
    }

    /*
    / **
      * Thêm giám sát cảm biến
      * 1. Việc giải thích API TYPE_STEP_COUNTER cho biết sẽ trả về số bước được tính kể từ khi khởi động được kích hoạt. Khi điện thoại được khởi động lại, dữ liệu bằng không.
      * Cảm biến là cảm biến phần cứng nên công suất thấp.
      * Để tiếp tục bước đếm, vui lòng không đảo ngược sự kiện đăng ký, ngay cả khi điện thoại ở trạng thái không hoạt động, nó vẫn được tính.
      * Số lượng các bước sẽ vẫn được báo cáo khi được kích hoạt. Cảm biến phù hợp cho nhu cầu đếm bước dài hạn.
      * <p>
      * 2. TYPE_STEP_DETECTOR chuyển thành phát hiện đi bộ,
      * Tài liệu API nói điều này, cảm biến chỉ được sử dụng để theo dõi bước đi, trả về số 1.0 mỗi lần.
      * Sử dụng TYPE_STEP_COUNTER nếu bạn cần số bước cho các sự kiện dài.
      * /
     */
    private void addCountStepListener() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_COUNTER;
            Log.v(TAG, "Sensor.TYPE_STEP_COUNTER");
            sensorManager.registerListener(StepService.this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (detectorSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_DETECTOR;
            Log.v(TAG, "Sensor.TYPE_STEP_DETECTOR");
            sensorManager.registerListener(StepService.this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.v(TAG, "Count sensor not available!");
            addBasePedometerListener();
        }
    }

    /**
     Cảm biến giám sát gọi lại
           * Mã khóa cho bước
           * 1. Việc giải thích API TYPE_STEP_COUNTER cho biết sẽ trả về số bước được tính kể từ khi khởi động được kích hoạt. Khi điện thoại được khởi động lại, dữ liệu bằng không.
           * Cảm biến là cảm biến phần cứng nên công suất thấp.
           * Để tiếp tục bước đếm, vui lòng không đảo ngược sự kiện đăng ký, ngay cả khi điện thoại ở trạng thái không hoạt động, nó vẫn được tính.
           * Số lượng các bước sẽ vẫn được báo cáo khi được kích hoạt. Cảm biến phù hợp cho nhu cầu đếm bước dài hạn.
           * <p>
           * 2. TYPE_STEP_DETECTOR chuyển thành phát hiện đi bộ,
           * Tài liệu API nói điều này, cảm biến chỉ được sử dụng để theo dõi bước đi, trả về số 1.0 mỗi lần.
           * Sử dụng TYPE_STEP_COUNTER nếu bạn cần số bước cho các sự kiện dài.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (stepSensorType == Sensor.TYPE_STEP_COUNTER) {
            //Lấy số bước tạm thời được cảm biến hiện tại trả về
            int tempStep = (int) event.values[0];
            //Lần đầu tiên, nếu bạn chưa đạt được số bước đã có trong hệ thống điện thoại di động, bạn sẽ nhận được số bước trong hệ thống mà APP chưa bắt đầu đếm.
            if (!hasRecord) {
                hasRecord = true;
                hasStepCount = tempStep;
            } else {
                //Nhận tổng số bước mà APP mở ra bây giờ = tổng số bước của cuộc gọi lại hệ thống - số bước tồn tại trước khi APP được mở
                int thisStepCount = tempStep - hasStepCount;
                //本次有效步数=（APP打开后所记录的总步数-上一次APP打开后所记录的总步数）
                int thisStep = thisStepCount - previousStepCount;
                //Tổng số bước = bước hiện tại + bước hiệu quả hiện tại
                CURRENT_STEP += (thisStep);
                //Ghi lại tổng số bước kể từ khi APP cuối cùng được mở
                previousStepCount = thisStepCount;
            }
            Logger.d("tempStep" + tempStep);
        } else if (stepSensorType == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values[0] == 1.0) {
                CURRENT_STEP++;
            }
        }
        updateNotification();
    }

    /**
     *
     */
    private void addBasePedometerListener() {
        mStepCount = new StepCount();
        mStepCount.setSteps(CURRENT_STEP);
        // Lấy loại cảm biến, loại thu được ở đây là cảm biến gia tốc
        // Lấy loại cảm biến, loại thu được ở đây là cảm biến gia tốc...
        Sensor sensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean isAvailable = sensorManager.registerListener(mStepCount.getStepDetector(), sensor,
                SensorManager.SENSOR_DELAY_UI);
        mStepCount.initListener(new StepValuePassListener() {
            @Override
            public void stepChanged(int steps) {
                CURRENT_STEP = steps;
                updateNotification();
            }
        });
        if (isAvailable) {
            Log.v(TAG, "Cảm biến gia tốc có thể được sử dụng");
        } else {
            Log.v(TAG, "Cảm biến gia tốc không có sẵn");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    /**
     * Lưu dữ liệu bước
     */
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // Nếu bộ hẹn giờ kết thúc bình thường, hãy bắt đầu đếm
            time.cancel();
            save();
            startTimeCount();
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

    }

    /**
     * Lưu dữ liệu bước
     */
    private void save() {
        int tempStep = CURRENT_STEP;

        List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENT_DATE});
        if (list.size() == 0 || list.isEmpty()) {
            StepData data = new StepData();
            data.setToday(CURRENT_DATE);
            data.setStep(tempStep + "");
            DbUtils.insert(data);
        } else if (list.size() == 1) {
            StepData data = list.get(0);
            data.setStep(tempStep + "");
            DbUtils.update(data);
        } else {
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //Hủy bỏ quá trình tiền cảnh
        stopForeground(true);
        DbUtils.closeDb();
        unregisterReceiver(mBatInfoReceiver);
        Logger.d("stepService关闭");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}
