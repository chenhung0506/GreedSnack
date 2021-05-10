package com.example.drixen.grid_snake;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.p000v4.internal.view.SupportMenu;
import android.support.p000v4.view.InputDeviceCompat;
import android.support.p000v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import java.lang.reflect.Array;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainBoard extends View {
    public int beginPoint = 10;
    public Bitmap bitmap;
    public int bodylength = 0;
    public int boxSize = 50;
    public int boxX = (this.max_x / this.boxSize);
    public int boxY = (this.max_y / this.boxSize);
    public Canvas buffer;
    int[][] cell = ((int[][]) Array.newInstance(Integer.TYPE, new int[]{this.boxX * this.boxY, 2}));
    float densityDpi = ((float) (this.f21dm.densityDpi * 2));
    public int direction = 1;

    /* renamed from: dm */
    DisplayMetrics f21dm = getResources().getDisplayMetrics();
    public long downTime;
    public float downX;
    public float downY;
    public Thread dropThread;
    Random food = new Random();
    public int foodX = this.food.nextInt(5);
    public int foodY = this.food.nextInt(5);

    /* renamed from: h */
    public int f22h;
    public boolean isRun = true;
    public HandlerThread mThread;
    public Handler mThreadHandler;
    public Handler mUI_Handler = new Handler();
    public int max_x = 550;
    public int max_y = 550;
    public Paint paint;
    public Paint paint2;
    public Paint paint3;
    public Paint paint4;
    public Paint paint5 = new Paint();

    /* renamed from: r1 */
    public Runnable f23r1 = new Runnable() {
        public void run() {
            while (!MainBoard.this.isRun) {
                try {
                    Thread.sleep(3000);
                    System.out.println("init2運作正常");
                    MainBoard.this.snakeView(MainBoard.this.direction);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
    public int runOrPause = 0;
    public int score = 0;
    public int startX = 5;
    public int startY = 5;
    Timer timer;
    TimerTask timerTask;
    public long upTime;
    public float upX;
    public float upY;

    /* renamed from: w */
    public int f24w;

    public MainBoard(Context context) {
        super(context);
        this.paint5.setColor(InputDeviceCompat.SOURCE_ANY);
        this.paint5.setStyle(Paint.Style.FILL);
        this.paint = new Paint();
        this.paint.setColor(ViewCompat.MEASURED_STATE_MASK);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint2 = new Paint();
        this.paint2.setColor(ViewCompat.MEASURED_STATE_MASK);
        this.paint2.setStyle(Paint.Style.FILL);
        this.paint3 = new Paint();
        this.paint3.setColor(SupportMenu.CATEGORY_MASK);
        this.paint3.setStyle(Paint.Style.FILL);
        this.paint4 = new Paint();
        this.paint4.setColor(ViewCompat.MEASURED_STATE_MASK);
        this.paint4.setTextSize(60.0f);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(this.bitmap, 0.0f, 0.0f, this.paint);
        int i = this.beginPoint;
        while (i < this.max_x) {
            int j = this.beginPoint;
            while (j < this.max_y) {
                canvas.drawRoundRect(new RectF((float) i, (float) j, (float) (this.boxSize + i), (float) (this.boxSize + j)), 8.0f, 8.0f, this.paint);
                j += this.boxSize;
            }
            i += this.boxSize;
        }
        invalidate();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.f22h = View.MeasureSpec.getSize(heightMeasureSpec);
        this.f24w = View.MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(this.f24w, this.f22h);
        this.bitmap = Bitmap.createBitmap(this.f24w, this.f22h, Bitmap.Config.ARGB_8888);
        this.buffer = new Canvas(this.bitmap);
        invalidate();
    }

    public void clear() {
        this.bitmap.eraseColor(0);
    }

    public void startTimer() {
        this.runOrPause++;
        if (this.runOrPause % 2 == 1) {
            System.out.println("開始運作");
            this.timer = new Timer();
            this.timerTask = new TimerTask() {
                public void run() {
                    MainBoard.this.snakeView(MainBoard.this.direction);
                }
            };
            this.timer.schedule(this.timerTask, 10, 1500);
            return;
        }
        System.out.println("結束運作");
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
        if (this.timerTask != null) {
            this.timerTask.cancel();
            this.timerTask = null;
        }
    }

    public void init() {
        this.dropThread = new Thread(new Runnable() {
            public void run() {
                while (MainBoard.this.isRun) {
                    try {
                        Thread.sleep((long) (750 - (MainBoard.this.bodylength * 4)));
                        MainBoard.this.snakeView(MainBoard.this.direction);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        this.dropThread.start();
    }

    public void init2() {
        this.mThread = new HandlerThread("name");
        this.mThread.start();
        this.mThreadHandler = new Handler(this.mThread.getLooper());
        this.mThreadHandler.post(this.f23r1);
    }

    public int[][] cellchanging(int x, int y) {
        this.cell[0][0] = this.foodX;
        this.cell[0][1] = this.foodY;
        if (x == this.foodX && y == this.foodY) {
            this.bodylength += 3;
            this.score += 10;
            this.foodX = this.food.nextInt(this.boxX);
            this.foodY = this.food.nextInt(this.boxY);
            this.cell[0][0] = this.foodX;
            this.cell[0][1] = this.foodY;
        }
        for (int i = 1; i <= this.bodylength; i++) {
            if (this.cell[i][0] == x && this.cell[i][1] == y) {
                this.score -= 5;
            }
        }
        for (int i2 = (this.boxX * this.boxY) - 2; i2 >= 1; i2--) {
            this.cell[i2 + 1][0] = this.cell[i2][0];
            this.cell[i2 + 1][1] = this.cell[i2][1];
        }
        this.cell[1][0] = x;
        this.cell[1][1] = y;
        return this.cell;
    }

    public void drawSnakeView(int x, int y) {
        int[][] newcell = cellchanging(x, y);
        int[][] bodyArr = (int[][]) Array.newInstance(Integer.TYPE, new int[]{this.boxX, this.boxY});
        int[][] foodArr = (int[][]) Array.newInstance(Integer.TYPE, new int[]{this.boxX, this.boxY});
        int[][] headArr = (int[][]) Array.newInstance(Integer.TYPE, new int[]{this.boxX, this.boxY});
        for (int k = 0; k <= this.bodylength; k++) {
            for (int i = 0; i < this.boxX; i++) {
                for (int j = 0; j < this.boxY; j++) {
                    if (i == newcell[0][0] && j == newcell[0][1]) {
                        foodArr[i][j] = 1;
                    }
                    if (i == newcell[k + 1][0] && j == newcell[k + 1][1]) {
                        bodyArr[i][j] = 1;
                    }
                    if (i == newcell[1][0] && j == newcell[1][1]) {
                        headArr[i][j] = 1;
                    }
                }
            }
        }
        if (this.score >= 0) {
            this.buffer.drawText("Score: " + Integer.toString(this.score), 300.0f, 1200.0f, this.paint4);
        } else if (this.bodylength > 119) {
            this.isRun = false;
            this.buffer.drawText("Already full", 100.0f, 600.0f, this.paint4);
        } else {
            this.isRun = false;
            this.buffer.drawText("Game Over", 100.0f, 600.0f, this.paint4);
        }
        for (int i2 = 0; i2 < this.boxX; i2++) {
            for (int j2 = 0; j2 < this.boxY; j2++) {
                if (headArr[i2][j2] == 1) {
                    this.buffer.drawRoundRect(new RectF((float) ((this.boxSize * i2) + this.beginPoint + 0), (float) ((this.boxSize * j2) + this.beginPoint + 0), (float) ((this.boxSize * i2) + this.boxSize + 10), (float) ((this.boxSize * j2) + this.boxSize + 10)), 8.0f, 8.0f, this.paint5);
                } else if (foodArr[i2][j2] == 1) {
                    this.buffer.drawRoundRect(new RectF((float) ((this.boxSize * i2) + this.beginPoint + 0), (float) ((this.boxSize * j2) + this.beginPoint + 0), (float) ((this.boxSize * i2) + this.boxSize + 10), (float) ((this.boxSize * j2) + this.boxSize + 10)), 8.0f, 8.0f, this.paint3);
                }
                if (bodyArr[i2][j2] == 1) {
                    this.buffer.drawRoundRect(new RectF((float) ((this.boxSize * i2) + this.beginPoint + 10), (float) ((this.boxSize * j2) + this.beginPoint + 10), (float) ((this.boxSize * i2) + this.boxSize), (float) ((this.boxSize * j2) + this.boxSize)), 8.0f, 8.0f, this.paint2);
                }
            }
        }
    }

    public void snakeView(int direction2) {
        switch (direction2) {
            case 1:
                if (this.startX == 0) {
                    this.startX = 11;
                }
                this.startX--;
                clear();
                drawSnakeView(this.startY, this.startX);
                return;
            case 2:
                if (this.startX == 10) {
                    this.startX = -1;
                }
                this.startX++;
                clear();
                drawSnakeView(this.startY, this.startX);
                return;
            case 3:
                if (this.startY == 0) {
                    this.startY = 11;
                }
                this.startY--;
                clear();
                drawSnakeView(this.startY, this.startX);
                return;
            case 4:
                if (this.startY == 10) {
                    this.startY = -1;
                }
                this.startY++;
                clear();
                drawSnakeView(this.startY, this.startX);
                return;
            default:
                return;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case 0:
                this.downX = event.getX();
                this.downY = event.getY();
                this.downTime = event.getEventTime();
                return true;
            case 1:
                this.upTime = event.getEventTime();
                this.upX = event.getX();
                this.upY = event.getY();
                float x = Math.abs(this.upX - this.downX);
                float y = Math.abs(this.upY - this.downY);
                double z = Math.sqrt((double) ((x * x) + (y * y)));
                int jiaodu = Math.round((float) ((Math.asin(((double) y) / z) / 3.141592653589793d) * 180.0d));
                if (this.upTime - this.downTime > 1000 && z < 800.0d) {
                    this.runOrPause++;
                    System.out.println("on");
                    if (this.runOrPause == 1) {
                        this.isRun = true;
                        init();
                    } else if (this.runOrPause % 2 == 1) {
                        System.out.println("1111");
                        this.isRun = true;
                        init();
                    } else {
                        System.out.println("2222");
                        this.isRun = false;
                    }
                }
                if (this.upY < this.downY && jiaodu > 45) {
                    this.direction = 1;
                    return true;
                } else if (this.upY > this.downY && jiaodu > 45) {
                    this.direction = 2;
                    return true;
                } else if (this.upX < this.downX && jiaodu <= 45) {
                    this.direction = 3;
                    return true;
                } else if (this.upX <= this.downX || jiaodu > 45) {
                    return true;
                } else {
                    this.direction = 4;
                    return true;
                }
            case 2:
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
