package dragview.athou.com.dragview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Created by Administrator on 2017/6/21.
 */

public class DragView extends View {
    private PointF mDragCenter;
    private PointF mStickCenter;
    //拖拽点的半径
    private float dragCircleRadius = 0;
    //固定点的半径
    private float stickCircleRadius = 0;
    private float stickMinCircleRadius = 0;
    //固定点的半径，随着拖拽距离时刻变化的
    private float stickTempCircleRadius = stickCircleRadius;
    float farest = 0; //2点之间最大距离，超过这个距离就分离
    private Paint mPaintRed;
    private Paint mTextPaint;
    private ValueAnimator mAnimator;
    private boolean isOutOfRange = false;
    private boolean isDisappear = false;
    private Rect rect;
    private int mStatus;
    private int mStatusBarHeight; //标题栏高度
    private int restDistance;
    private String content;

    private OnDisappearListener onDisappearListener;

    interface OnDisappearListener {
        void onDisappear(PointF mDragCenter);

        void onRest(boolean isOutOfRange);
    }

    public DragView(Context context) {
        this(context, null);
    }

    public DragView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        rect = new Rect(0, 0, 50, 50);
        stickCircleRadius = dip2px(10);
        dragCircleRadius = dip2px(10);
        stickMinCircleRadius = dip2px(10);
        farest = dip2px(80);
        restDistance = dip2px(10);

        mPaintRed = new Paint();
        mPaintRed.setAntiAlias(true);
        mPaintRed.setColor(getResources().getColor(R.color.color_red_bg));

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.textSize));
    }

    public void initCenter(float x, float y) {
        mDragCenter = new PointF(x, y);
        mStickCenter = new PointF(x, y);
        invalidate();
    }

    public void setContent(String content) {
        this.content = content;
        invalidate();
    }

    public void setStatusBarHeight(int mStatusBarHeight) {
        this.mStatusBarHeight = mStatusBarHeight;
    }

    public void setOnDisappearListener(OnDisappearListener onDisappearListener) {
        this.onDisappearListener = onDisappearListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mAnimator != null && mAnimator.isRunning()) {
                    return false;
                }
                isDisappear = false;
                isOutOfRange = false;
                updateCenter(event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (getCircleDistance(mDragCenter, mStickCenter) > farest) {
                    isOutOfRange = true;
                    updateCenter(event.getRawX(), event.getRawY());
                    return false;
                }
                updateCenter(event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_UP:
                handActionUp();
                break;
            default:
                isOutOfRange = false;
                break;
        }
        return true;
    }

    private void handActionUp() {
        if (isOutOfRange) {
            //手指滑动的距离超过范围
            if (getCircleDistance(mDragCenter, mStickCenter) < restDistance) {
                if (onDisappearListener != null) {
                    onDisappearListener.onRest(isOutOfRange);
                }
                return;
            }
            isDisappear = true;
            invalidate();
            if (onDisappearListener != null) {
                onDisappearListener.onDisappear(mDragCenter);
            }
        } else { //没有超过距离，执行回弹动画
            mAnimator = ValueAnimator.ofFloat(1.0f);
            mAnimator.setInterpolator(new OvershootInterpolator(4.0f));//减速插值器

            final PointF start = new PointF(mDragCenter.x, mDragCenter.y);
            final PointF end = new PointF(mStickCenter.x, mStickCenter.y);

            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fractin = animation.getAnimatedFraction();
                    float x = start.x + (end.x - start.x) * fractin;
                    float y = start.y + (end.y - start.y) * fractin;
                    updateCenter(x, y);
                }
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onDisappearListener != null) {
                        onDisappearListener.onRest(isOutOfRange);
                    }
                }
            });

            if (getCircleDistance(start, end) < 10) {
                mAnimator.setDuration(10);
            } else {
                mAnimator.setDuration(500);
            }
            mAnimator.start();
        }
    }

    private void updateCenter(float rawX, float rawY) {
        this.mDragCenter.x = rawX;
        this.mDragCenter.y = rawY;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(0, -mStatusBarHeight);

        //是否显示
        if (!isDisappear) {
            //当移动的范围在80px内
            if (!isOutOfRange) {
                ShapeDrawable shapeDrawable = makeShapeDrawable();
                shapeDrawable.setBounds(rect);
                shapeDrawable.draw(canvas);

                canvas.drawCircle(mStickCenter.x, mStickCenter.y, stickTempCircleRadius, mPaintRed);
            }

            float textWidth = mTextPaint.measureText(content);
            Paint.FontMetrics fm = mTextPaint.getFontMetrics();
            float textHeight = fm.descent - fm.ascent;
            canvas.drawCircle(mDragCenter.x, mDragCenter.y, dragCircleRadius, mPaintRed);
//            canvas.drawText(content, mDragCenter.x - dragCircleRadius / 2f, mDragCenter.y + dragCircleRadius / 2f, mTextPaint);
            canvas.drawText(content, mDragCenter.x - textWidth / 2f, mDragCenter.y + textHeight / 2f - fm.descent, mTextPaint);
            canvas.restore();
        }
    }

    private ShapeDrawable makeShapeDrawable() {
        Path path = new Path();
        float distance = getCircleDistance(mDragCenter, mStickCenter);
        stickTempCircleRadius = getCircleRadius(distance);

        float xDiff = mStickCenter.x - mDragCenter.x;
        double dragTan = 0;
        if (xDiff != 0) {
            dragTan = (mStickCenter.y - mDragCenter.y) / xDiff;
        }
        PointF[] dragPoint = getIntersectionPoints(mDragCenter, dragCircleRadius, dragTan);
        PointF[] stickPoint = getIntersectionPoints(mStickCenter, stickTempCircleRadius, dragTan);
        PointF pointByCenter = getPointCenter(mDragCenter, mStickCenter);

        path.moveTo(stickPoint[0].x, stickPoint[0].y);
        path.quadTo(pointByCenter.x, pointByCenter.y, dragPoint[0].x, dragPoint[0].y);

        path.lineTo(dragPoint[1].x, dragPoint[1].y);
        path.quadTo(pointByCenter.x, pointByCenter.y, stickPoint[1].x, stickPoint[1].y);
        path.close();

        ShapeDrawable shapeDrawable = new ShapeDrawable(new PathShape(path, 50, 50));
        shapeDrawable.getPaint().setColor(getResources().getColor(R.color.color_red_bg));
        return shapeDrawable;
    }

    /**
     * get the point of intresection between circle and line.
     * 获取通过指定圆心，，斜率为lineK的直线与圆的交点
     *
     * @param pMiddle The circle center point
     * @param radius  The circle radius
     * @param lineK   The slope of line which cross the pMiddle
     * @return
     */
    public static PointF[] getIntersectionPoints(PointF pMiddle, float radius, Double lineK) {
        PointF[] points = new PointF[2];
        float radian, xOffset = 0, yOffset = 0;
        if (lineK != null) {
            radian = (float) Math.atan(lineK); //得到角度
            xOffset = (float) (Math.sin(radian) * radius);
            yOffset = (float) (Math.cos(radian) * radius);
        } else {
            xOffset = radius;
            yOffset = 0;
        }
        points[0] = new PointF(pMiddle.x + xOffset, pMiddle.y - yOffset);
        points[1] = new PointF(pMiddle.x - xOffset, pMiddle.y + yOffset);
        return points;
    }

    /**
     * 获取2中间的控制点
     *
     * @param dragCenter
     * @param stickCenter
     * @return
     */
    private PointF getPointCenter(PointF dragCenter, PointF stickCenter) {
        float percent = 0.618f; //黄金分割比例
        PointF pointF = new PointF(mDragCenter.x + (mStickCenter.x - mDragCenter.x) * percent,
                mDragCenter.y + (mStickCenter.y - mDragCenter.y) * percent);
        return pointF;
    }

    //更加拉动的距离动态计算圆的半径
    private float getCircleRadius(float distance) {
        distance = Math.min(distance, farest);
        float fraction = 1 - 0.8f * distance / farest;
        return fraction * stickCircleRadius;
    }

    private float getCircleDistance(PointF dragCenter, PointF stickCenter) {
        return (float) Math.sqrt(Math.pow(dragCenter.x - stickCenter.x, 2)
                + Math.pow(dragCenter.y - stickCenter.y, 2));
    }

    private static int dip2px(float dp) {
        return (int) (Resources.getSystem().getDisplayMetrics().density * dp + 0.5f);
    }
}
