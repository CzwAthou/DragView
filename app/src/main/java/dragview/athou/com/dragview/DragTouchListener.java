package dragview.athou.com.dragview;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Created by Administrator on 2017/6/21.
 */

public class DragTouchListener implements View.OnTouchListener {

    private TextView point;
    private Context context;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    DragView dragView;

    int count;

    public DragTouchListener(TextView point, int count) {
        this.point = point;
        this.count = count;
        this.context = point.getContext();
        init();
    }

    private void init() {
        dragView = new DragView(context);

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.format = PixelFormat.TRANSLUCENT; //设置半透明效果
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            this.point.setVisibility(View.INVISIBLE);

            this.dragView.setVisibility(View.VISIBLE);
            this.dragView.setStatusBarHeight(getStatusBarHeight(point));
            this.dragView.initCenter(event.getRawX(), event.getRawY());
            this.dragView.setContent("" + count);
            this.dragView.setOnDisappearListener(new DragView.OnDisappearListener() {
                @Override
                public void onDisappear(PointF mDragCenter) {
                    Log.i("dragView", "onDisappear");
//                    count--;
//                    point.setText("" + count);
//                    point.setVisibility(View.VISIBLE);
//                    dragView.setVisibility(View.GONE);

                    windowManager.removeView(dragView);
                }

                @Override
                public void onRest(boolean isOutOfRange) {
                    Log.i("dragView", "onRest");

//                    point.setVisibility(View.VISIBLE);
//                    dragView.setVisibility(View.GONE);
                }
            });
            if (dragView.getParent() == null) {
                windowManager.addView(dragView, layoutParams);
            }
        }
        return false;
    }

    private int getStatusBarHeight(View view) {
        if (view == null) {
            return 0;
        }
        Rect rect = new Rect();
        view.getWindowVisibleDisplayFrame(rect);
        return rect.top;
    }
}
