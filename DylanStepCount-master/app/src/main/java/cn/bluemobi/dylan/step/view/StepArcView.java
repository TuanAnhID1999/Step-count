package cn.bluemobi.dylan.step.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import cn.bluemobi.dylan.step.R;

/**
 *
 * Vòng cung hiển thị số bước
 */
public class StepArcView extends View {

    /**
     *Chiều rộng của vòng cung
     */
    private float borderWidth = dipToPx(14);
    /**
     *Cỡ chữ của số bước
     */
    private float numberTextSize = 0;
    /**
     * Số bước
     */
    private String stepNumber = "0";
    /**
     * Bắt đầu vẽ góc của vòng cung
     */
    private float startAngle = 145;
    /**
     * Góc giữa góc tương ứng với điểm cuối và góc tương ứng với điểm bắt đầu
     */
    private float angleLength = 270;
    /**
     * Góc giữa điểm cuối của cung đỏ của bước hiện tại được vẽ đến điểm bắt đầu
     */
    private float currentAngleLength = 0;
    /**
     *Thời lượng hoạt hình
     */
    private int animationLength = 3000;

    public StepArcView(Context context) {
        super(context);


    }

    public StepArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StepArcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /** Tọa độ x của điểm trung tâm*/
        float centerX = (getWidth()) / 2;
        /**Chỉ định hình chữ nhật đường viền ngoài của cung*/
        RectF rectF = new RectF(0 + borderWidth, borderWidth, 2 * centerX - borderWidth, 2 * centerX - borderWidth);

        /**【Bước đầu tiên là vẽ vòng cung màu vàng tổng thể*/
        drawArcYellow(canvas, rectF);
        /**Bước đầu tiên là vẽ vòng cung màu vàng tổng thể...*/
        drawArcRed(canvas, rectF);
        /**[Bước 3] Vẽ số màu đỏ của tiến trình hiện tại*/
        drawTextNumber(canvas, centerX);
        /**[Bước 4] Vẽ số màu đỏ của "Các bước"*/
        drawTextStepString(canvas, centerX);
    }

    /**
     * 1.Vẽ một vòng cung màu vàng của tổng số bước
     *
     * @param canvas 画笔
     * @param rectF  参考的矩形
     */
    private void drawArcYellow(Canvas canvas, RectF rectF) {
        Paint paint = new Paint();
        /** Vẽ một vòng cung màu vàng của tổng số bước... */
        paint.setColor(getResources().getColor(R.color.yellow));
        /** Màu cọ mặc định, màu vàng...*/
        paint.setStrokeJoin(Paint.Join.ROUND);
        /** Khớp là một vòng cung...*/
        paint.setStrokeCap(Paint.Cap.ROUND);
        /** Đặt kiểu của cọ Paint.Cap.Round, Cap.SQUARE, v.v. là hình tròn, hình vuông...*/
        paint.setStyle(Paint.Style.STROKE);
        /**Đặt kiểu tô màu của cọ Paint.Style.FILL: điền vào bên trong; Paint.Style.FILL_AND_STROKE: lấp đầy bên trong và các nét; Paint.Style.STROKE: chỉ các nét...*/
        paint.setAntiAlias(true);
        /**Khử răng cưa...*/
        paint.setStrokeWidth(borderWidth);

        /**Phương pháp vẽ hình vòng cung
                   * drawArc (RectF oval, float startAngle, float scanAngle, boolean useCenter, Paint paint) // vẽ cung tròn,
                   Tham số đầu tiên là đối tượng RectF và giới hạn của hình chữ nhật elip được sử dụng để xác định hình dạng, kích thước và cung.
                   Tham số thứ hai là góc bắt đầu (độ) ở đầu cung, góc bắt đầu của cung, tính bằng độ.
                   Góc mà ba cung của tham số được quét, theo chiều kim đồng hồ, tính bằng độ, bắt đầu từ giữa bên phải đến không độ.
                   Tham số thứ tư là nếu điều này là đúng, tâm của vòng tròn được bao gồm khi vẽ vòng cung, thường được sử dụng để vẽ một cái quạt, nếu nó sai, đây sẽ là một vòng cung.
                   Tham số năm là đối tượng Paint;
         */
        canvas.drawArc(rectF, startAngle, angleLength, false, paint);

    }

    /**
     * 2.Vẽ vòng cung màu đỏ của số bước hiện tại
     */
    private void drawArcRed(Canvas canvas, RectF rectF) {
        Paint paintCurrent = new Paint();
        paintCurrent.setStrokeJoin(Paint.Join.ROUND);
        paintCurrent.setStrokeCap(Paint.Cap.ROUND);//圆角弧度
        paintCurrent.setStyle(Paint.Style.STROKE);//设置填充样式
        paintCurrent.setAntiAlias(true);//抗锯齿功能
        paintCurrent.setStrokeWidth(borderWidth);//设置画笔宽度
        paintCurrent.setColor(getResources().getColor(R.color.red));//设置画笔颜色
        canvas.drawArc(rectF, startAngle, currentAngleLength, false, paintCurrent);
    }

    /**
     * 3Số lượng các bước ở trung tâm của vòng
     */
    private void drawTextNumber(Canvas canvas, float centerX) {
        Paint vTextPaint = new Paint();
        vTextPaint.setTextAlign(Paint.Align.CENTER);
        vTextPaint.setAntiAlias(true);//抗锯齿功能
        vTextPaint.setTextSize(numberTextSize);
        Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        vTextPaint.setTypeface(font);//字体风格
        vTextPaint.setColor(getResources().getColor(R.color.red));
        Rect bounds_Number = new Rect();
        vTextPaint.getTextBounds(stepNumber, 0, stepNumber.length(), bounds_Number);
        canvas.drawText(stepNumber, centerX, getHeight() / 2 + bounds_Number.height() / 2, vTextPaint);

    }

    /**
     * 4.Số lượng các bước ở trung tâm của vòng...
     */
    private void drawTextStepString(Canvas canvas, float centerX) {
        Paint vTextPaint = new Paint();
        vTextPaint.setTextSize(dipToPx(16));
        vTextPaint.setTextAlign(Paint.Align.CENTER);
        vTextPaint.setAntiAlias(true);//抗锯齿功能
        vTextPaint.setColor(getResources().getColor(R.color.grey));
        String stepString = "Steps";
        Rect bounds = new Rect();
        vTextPaint.getTextBounds(stepString, 0, stepString.length(), bounds);
        canvas.drawText(stepString, centerX, getHeight() / 2 + bounds.height() + getFontHeight(numberTextSize), vTextPaint);
    }

    /**
     *Lấy chiều cao của số bước hiện tại
     *
     * @param fontSize 字体大小
     * @return 字体高度
     */
    public int getFontHeight(float fontSize) {
        Paint paint = new Paint();
        paint.setTextSize(fontSize);
        Rect bounds_Number = new Rect();
        paint.getTextBounds(stepNumber, 0, stepNumber.length(), bounds_Number);
        return bounds_Number.height();
    }

    /**
     * dip 转换成px
     *
     * @param dip
     * @return
     */

    private int dipToPx(float dip) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dip * density + 0.5f * (dip >= 0 ? 1 : -1));
    }

    /**
     * Số lượng các bước thực hiện
     *
     * @param totalStepNum  设置的步数
     * @param currentCounts 所走步数
     */
    public void setCurrentCount(int totalStepNum, int currentCounts) {
        /**Số lượng các bước thực hiện...*/
        if (currentCounts > totalStepNum) {
            currentCounts = totalStepNum;
        }

        /**Nếu số bước hiện tại vượt quá tổng số bước, vòng cung vẫn là 270 độ và không thể là một khu vườn....*/
        float scalePrevious = (float) Integer.valueOf(stepNumber) / totalStepNum;
        /**Độ dài của góc được chuyển đổi thành cung cuối cùng -> chiều dài cung*/
        float previousAngleLength = scalePrevious * angleLength;

        /**Số bước được thực hiện là tỷ lệ phần trăm của tổng số bước*/
        float scale = (float) currentCounts / totalStepNum;
        /**Độ dài của góc được chuyển đổi thành cung cuối cùng -> chiều dài cung*/
        float currentAngleLength = scale * angleLength;
        /**Bắt đầu thực hiện hoạt hình*/
        setAnimation(previousAngleLength, currentAngleLength, animationLength);

        stepNumber = String.valueOf(currentCounts);
        setTextSize(currentCounts);
    }

    /**
     *Làm sinh động sự tiến bộ
     *       * ValueAnimator là cốt lõi của toàn bộ cơ chế hoạt hình thuộc tính. Cơ chế hoạt động của hoạt hình thuộc tính được thực hiện bằng cách liên tục hoạt động trên các giá trị.
     *       * Chuyển đổi hình ảnh động giữa các giá trị ban đầu và kết thúc được tính bởi lớp ValueAnimator.
     *       * Nó sử dụng cơ chế vòng lặp thời gian để tính toán chuyển đổi hình ảnh động giữa các giá trị và giá trị.
     *       * Chúng tôi chỉ cần cung cấp các giá trị ban đầu và kết thúc cho ValueAnimator và cho nó biết thời gian hoạt hình cần chạy.
     *       * Sau đó, ValueAnimator sẽ tự động giúp chúng tôi chuyển đổi suôn sẻ từ giá trị ban đầu sang giá trị cuối.
     *       *
     *       * @param bắt đầu giá trị ban đầu
     *       * @param giá trị cuối hiện tại
     *       * Thời lượng hoạt hình @param
     */
    private void setAnimation(float start, float current, int length) {
        ValueAnimator progressAnimator = ValueAnimator.ofFloat(start, current);
        progressAnimator.setDuration(length);
        progressAnimator.setTarget(currentAngleLength);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                /**Mỗi khi giá trị chuyển tiếp trơn tru được tạo ra giữa giá trị ban đầu và giá trị cuối, tiến trình được cập nhật dần dần.*/
                currentAngleLength = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        progressAnimator.start();
    }

    /**
     * Đặt kích thước văn bản để ngăn số lượng bước quá lớn để tự động đặt kích thước phông chữ.
     *
     * @param num
     */
    public void setTextSize(int num) {
        String s = String.valueOf(num);
        int length = s.length();
        if (length <= 4) {
            numberTextSize = dipToPx(50);
        } else if (length > 4 && length <= 6) {
            numberTextSize = dipToPx(40);
        } else if (length > 6 && length <= 8) {
            numberTextSize = dipToPx(30);
        } else if (length > 8) {
            numberTextSize = dipToPx(25);
        }
    }

}

