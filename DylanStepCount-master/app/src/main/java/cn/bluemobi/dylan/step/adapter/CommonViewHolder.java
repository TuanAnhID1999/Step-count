package cn.bluemobi.dylan.step.adapter;


import android.util.SparseArray;
import android.view.View;


public class CommonViewHolder {
    /**
     * @param view Root View của tất cả các lượt xem được lưu trữ
     *       * @param id lưu trữ định danh duy nhất của Chế độ xem
     * @return
     */
    public static <T extends View> T get(View view, int id) {

        SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
        //Nếu chế độ xem gốc không có bộ sưu tập để lưu bộ đệm thì Chế độ xem
        if (viewHolder == null) {
            viewHolder = new SparseArray<View>();
            view.setTag(viewHolder);//Tạo một bộ sưu tập và liên kết xem gốc
        }
        View chidlView = viewHolder.get(id);//Nhận chế độ xem con của chế độ xem gốc được lưu trữ trong bộ sưu tập
        if (chidlView == null) {//Nếu bạn không thay đổi giấy
            //找到该孩纸
            chidlView = view.findViewById(id);
            viewHolder.put(id, chidlView);//Lưu vào bộ sưu tập
        }
        return (T) chidlView;
    }
}
