package com.voiceofhand.meetinginterpreter.view.voice;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 自定义声音振动曲线view
 */
public class VoiceLineView extends View {
    private final static String TAG = VoiceLineView.class.getSimpleName();

    private final int COLOR_LEFT_START = Color.rgb(0x32, 0x8b, 0xb0);   //  默认起始颜色
    private final int COLOR_RIGHT_END = Color.rgb(0xf3, 0x7a, 0xdd);    //  默认结束颜色
    private final int MAX_AMPLITUDE = 20000;  //  最大振幅
    private final int VIEW_PADDING_WIDTH = 10;  //  宽度留出的空边
    private final int VIEW_PADDING_HEIGHT = 10; //  高度留出的空边
    private final int NIM_ITEM_HEIGHT = 10;

    //  保存矩形数量
    private int mRectViewCount = 70;

    private int mViewStep = 0;
    private int mViewSpace = 0;

    private int mViewDisplayWidth = 0;
    private int mViewDisplayHeight = 0;

    private List<Integer> mRectViewColorList = new ArrayList<>();
    private List<Rect> mRectViewPostionList = new ArrayList<>();

    private Context mContext = null;

    private boolean isSavedSpaceCanvas = false;

    public interface iDecibelListener {
        void changed(double max, double min);
    }

    private iDecibelListener mDecibelChangeListener = null;
    public void setDecibelChangeListener(iDecibelListener listener) {
        mDecibelChangeListener = listener;
    }

    public VoiceLineView(Context context) {
        super(context);
        mContext = context;
    }

    public VoiceLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public VoiceLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    private void initLayout() {
        mViewDisplayWidth = getWidth();
        mViewDisplayHeight = getHeight();

        int step = mViewDisplayWidth / mRectViewCount;
        mViewSpace = ((step / 6) == 0) ? 1 : (step / 6);
        mViewStep = step - mViewSpace;
    }

    private void initColorList() {
        int s_red = Color.red(COLOR_LEFT_START);
        int s_green = Color.green(COLOR_LEFT_START);
        int s_blue = Color.blue(COLOR_LEFT_START);

        int e_red = Color.red(COLOR_RIGHT_END);
        int e_green = Color.green(COLOR_RIGHT_END);
        int e_blue = Color.blue(COLOR_RIGHT_END);

        for (int ii=0; ii < mRectViewCount; ii++) {
            int i_red = (int)((((double)(e_red - s_red)) / mRectViewCount) * ii);
            int i_green = (int)((((double)(e_green - s_green)) / mRectViewCount) * ii);
            int i_blue = (int)((((double)(e_blue - s_blue)) / mRectViewCount) * ii);

            int rectColor = Color.rgb(s_red + i_red, s_green + i_green, s_blue + i_blue);
            mRectViewColorList.add(rectColor);

            int rt_left = VIEW_PADDING_WIDTH + ii * (mViewStep + mViewSpace);
            int rt_right = rt_left + mViewStep;

            int globalHeight = mViewDisplayHeight - VIEW_PADDING_HEIGHT * 2;
            double itemHeight = NIM_ITEM_HEIGHT;
            double spaceHeight = globalHeight - itemHeight;
            int rt_top = (int)(VIEW_PADDING_HEIGHT + spaceHeight / 2);
            int rt_bottom = rt_top + (int)itemHeight;

            mRectViewPostionList.add(new Rect(rt_left, rt_top, rt_right, rt_bottom));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        initLayout();
        initColorList();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (this) {
            if (canvas == null) {
                return;
            }

            canvas.save();

            for (int ii = 0; ii < mRectViewCount; ii++) {
                int itemColor = mRectViewColorList.get(ii);
                Rect itemRect = mRectViewPostionList.get(ii);

                Paint itemPaint = new Paint();
                itemPaint.setColor(itemColor);
                itemPaint.setStyle(Paint.Style.FILL);

                canvas.drawRect(itemRect, itemPaint);
            }

            canvas.restore();
        }
    }

    double maxVoc = 0;
    double minVoc = 0;
    public void putVoiceBuffer(byte[] audio, int rdSize) {
        synchronized (this) {
            if (rdSize == 0) {
                return;
            }

            mRectViewPostionList.clear();

            int step = rdSize / mRectViewCount;
            for (int ii=0, pos=0; pos < mRectViewCount; ii += step, pos++) {
                short voc = (short) ((audio[ii] & 0xFF) | (audio[ii+1] & 0xFF) << 8);

                int vocInteger = Math.abs(voc);
                maxVoc = Math.max(maxVoc, vocInteger);
                minVoc = Math.min(minVoc, vocInteger);

                if (vocInteger > MAX_AMPLITUDE) {
                    vocInteger = MAX_AMPLITUDE;
                }

                int rt_left = VIEW_PADDING_WIDTH + pos * (mViewStep + mViewSpace);
                int rt_right = rt_left + mViewStep;

                int globalHeight = mViewDisplayHeight - VIEW_PADDING_HEIGHT * 2;
                double itemHeight = (((double)vocInteger / MAX_AMPLITUDE) * globalHeight);
                if (itemHeight < NIM_ITEM_HEIGHT) {
                    itemHeight = NIM_ITEM_HEIGHT;
                }

                double spaceHeight = globalHeight - itemHeight;
                int rt_top = (int)(VIEW_PADDING_HEIGHT + spaceHeight / 2);
                int rt_bottom = rt_top + (int)itemHeight;

                mRectViewPostionList.add(new Rect(rt_left, rt_top, rt_right, rt_bottom));
            }

            if (mDecibelChangeListener != null) {
                mDecibelChangeListener.changed(maxVoc, minVoc);
            }
            invalidate();
        }
    }


    //  传入声音大小绘制伪波形
    //  20以内，产生3个波形
    //  20-80， 产生4个波形
    //  80-100，产生5个波形
    //  波形大小，总高度的%分贝数，随机加减5%
    //  波峰两边，各有3个条纹配合波峰（若够）
    //  波峰不出现在两端5个条柱之内
    private int noCrest = 5;
    public void putVolumeValue(int vol) {
        synchronized (this) {

            Random random = new Random();
            ArrayList<Integer> crestRandsList = new ArrayList<>();

            if (vol < 20) {
                int step = (mRectViewCount - noCrest * 2) / 3;

                crestRandsList.add(noCrest + random.nextInt(step));
                crestRandsList.add(noCrest + step + random.nextInt(step));
                crestRandsList.add(noCrest + step * 2 + random.nextInt(step));

            } else if (vol < 80) {
                int step = (mRectViewCount - noCrest * 2) / 4;

                crestRandsList.add(noCrest + random.nextInt(step));
                crestRandsList.add(noCrest + step + random.nextInt(step));
                crestRandsList.add(noCrest + step * 2 + random.nextInt(step));
                crestRandsList.add(noCrest + step * 3 + random.nextInt(step));
            } else {
                int step = (mRectViewCount - noCrest * 2) / 5;

                crestRandsList.add(noCrest + random.nextInt(step));
                crestRandsList.add(noCrest + step + random.nextInt(step));
                crestRandsList.add(noCrest + step * 2 + random.nextInt(step));
                crestRandsList.add(noCrest + step * 3 + random.nextInt(step));
                crestRandsList.add(noCrest + step * 4 + random.nextInt(step));
            }

            int globalHeight = mViewDisplayHeight - VIEW_PADDING_HEIGHT * 2;


            mRectViewPostionList.clear();
            initColorList();

            for (int ii = 0; ii < crestRandsList.size(); ii++) {
                int pos = crestRandsList.get(ii);
                if (pos < 5 || pos + 5 > mRectViewCount) {
                    continue;
                }

                int randomHeightRatio = vol + random.nextInt(5);
                if (randomHeightRatio <= 0) {
                    continue;
                }

                randomHeightRatio = Math.min(randomHeightRatio, 100);


                int itemHeight = (globalHeight * randomHeightRatio) / 100;
                if (itemHeight > NIM_ITEM_HEIGHT) {

                    Rect midRect = mRectViewPostionList.get(pos);
                    midRect.top = (globalHeight - itemHeight) / 2 + VIEW_PADDING_HEIGHT;
                    midRect.bottom = midRect.top + itemHeight;
                    mRectViewPostionList.set(pos, midRect);
                }

                int secondHeight = (itemHeight * 2) / 3;
                if (secondHeight > NIM_ITEM_HEIGHT) {

                    Rect secondRect = mRectViewPostionList.get(pos - 1);
                    secondRect.top = (globalHeight - secondHeight) / 2 + VIEW_PADDING_HEIGHT;
                    secondRect.bottom = secondRect.top + secondHeight;
                    mRectViewPostionList.set(pos - 1, secondRect);

                    Rect secondRect2 = mRectViewPostionList.get(pos + 1);
                    secondRect2.top = secondRect.top;
                    secondRect2.bottom = secondRect.bottom;
                    mRectViewPostionList.set(pos + 1, secondRect2);
                }

                int thirdHeight = (itemHeight / 3);
                if (thirdHeight > NIM_ITEM_HEIGHT) {

                    Rect thirdRect = mRectViewPostionList.get(pos - 2);
                    thirdRect.top = (globalHeight - thirdHeight) / 2 + VIEW_PADDING_HEIGHT;
                    thirdRect.bottom = thirdRect.top + thirdHeight;
                    mRectViewPostionList.set(pos - 2, thirdRect);

                    Rect thirdRect2 = mRectViewPostionList.get(pos + 2);
                    thirdRect2.top = thirdRect.top;
                    thirdRect2.bottom = thirdRect.bottom;
                    mRectViewPostionList.set(pos + 2, thirdRect2);
                }
            }

            invalidate();
        }
    }
}