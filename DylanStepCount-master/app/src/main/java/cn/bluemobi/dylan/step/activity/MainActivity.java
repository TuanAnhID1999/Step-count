package cn.bluemobi.dylan.step.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import cn.bluemobi.dylan.step.R;
import cn.bluemobi.dylan.step.step.UpdateUiCallBack;
import cn.bluemobi.dylan.step.step.service.StepService;
import cn.bluemobi.dylan.step.step.utils.SharedPreferencesUtils;
import cn.bluemobi.dylan.step.view.StepArcView;

/**
 *
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_data;
    private StepArcView cc;
    private TextView tv_set;
    private TextView tv_isSupport;
    private SharedPreferencesUtils sp;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();
        initData();
        addListener();
    }

    private void assignViews() {
        tv_data = (TextView) findViewById(R.id.tv_data);
        cc = (StepArcView) findViewById(R.id.cc);
        tv_set = (TextView) findViewById(R.id.tv_set);
        tv_isSupport = (TextView) findViewById(R.id.tv_isSupport);
    }


    private void addListener() {
        tv_set.setOnClickListener(this);
        tv_data.setOnClickListener(this);
    }

    private void initData() {
        sp = new SharedPreferencesUtils(this);
        //Lấy số bước thực hiện theo kế hoạch do người dùng đặt. Nếu không được đặt, mặc định 7000
        String planWalk_QTY = (String) sp.getParam("planWalk_QTY", "7000");
        //Đặt số bước hiện tại thành 0
        cc.setCurrentCount(Integer.parseInt(planWalk_QTY), 0);
        tv_isSupport.setText("Từng bước..." );
        setupService();
    }


    private boolean isBind = false;

    /**
     * Bật service truy cập bước
     */
    private void setupService() {
        Intent intent = new Intent(this, StepService.class);
        isBind = bindService(intent, conn, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    /**
     * Giao diện truy vấn trạng thái của service  ứng dụng (service ứng dụng),
     * Để biết thêm thông tin chi tiết, hãy tham khảo các mô tả trong Service và bối cảnh.bindService ().
     * Giống như nhiều phương thức gọi lại từ hệ thống, các phương thức ServiceConnection được gọi trong luồng chính của quy trình.
     */
    ServiceConnection conn = new ServiceConnection() {
        /**
         * Phương pháp này được gọi khi thiết lập kết nối với Dịch vụ.
         * Hiện tại, Android triển khai kết nối với dịch vụ thông qua cơ chế IBind.
         * @param name Tên của thành phần Service được kết nối thực sự
         * @param service IBind của kênh liên lạc của Service, có thể truy cập dịch vụ tương ứng thông qua Service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepService stepService = ((StepService.StepBinder) service).getService();
            //Đặt dữ liệu khởi tạo
            String planWalk_QTY = (String) sp.getParam("planWalk_QTY", "7000");
            cc.setCurrentCount(Integer.parseInt(planWalk_QTY), stepService.getStepCount());

            //Đặt cuộc gọi lại bước nghe
            stepService.registerCallback(new UpdateUiCallBack() {
                @Override
                public void updateUi(int stepCount) {
                    String planWalk_QTY = (String) sp.getParam("planWalk_QTY", "7000");
                    cc.setCurrentCount(Integer.parseInt(planWalk_QTY), stepCount);
                }
            });
        }

        /**
         * Phương thức này được gọi khi mất kết nối với Service.
         *  Điều này thường xảy ra khi quá trình đặt Service gặp sự cố hoặc được gọi bởi Kill.
         *  Phương pháp này không xóa kết nối với Service, nó vẫn sẽ gọi onServiceConnected () khi dịch vụ được khởi động lại.
         *Tên @param Thiếu tên thành phần được kết nối
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_set:
                startActivity(new Intent(this, SetPlanActivity.class));
                break;
            case R.id.tv_data:
                startActivity(new Intent(this, HistoryActivity.class));
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBind) {
            this.unbindService(conn);
        }
    }
}
