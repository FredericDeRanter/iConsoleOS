package eu.le_tian.iConsoleOS.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GraphView extends View {
    private static final String TAG = "GraphView";
    private int yAxisMax; //level is max 32
    private int xAxisMax; //time in seconds!
    private int xAxisInterval; //the interval between grids in seconds
    private int xAxisExtensionPerTime; //also in seconds! (how much the graph shows more everytime

    // offsets are used to shift the graph a bit to show the axis
    private int xOffset;
    private int yOffset;

    /**
     * The two lists to define the graph:
     * pointsAll will define the points in the future. It starts with point (0,startlevel) and has a new point for every change of level.
     * PointsDone defines the front layer. This list does not start at 0, the first point will be (currentTime, currentlevel) until the level changes. Then there will be 2 points: (timeOfLevelChange, previousLevel) and (currentTime, currentlevel). So the last point is always (currentTime, currentlevel).
     * futureLevelChange: is added to all levels for points in the future (so past points done)
     */
    private List<Point> pointsDone;
    private List<Point> pointsAll;
    private int futureLevelChange;

    private Color toDoColor;
    private Color doneColor;

    //old items
    /*private int gridDimension;
    private int lineSlope;
    private int lineYintercept;*/

    // Appearance fields
    private Paint gridPaint;
    private Paint axisPaint;
    private Paint linePaint;
    private Paint rectangleDonePaint;
    private Paint rectanglePaint;
    private Paint textPaint;


    public int getxOffset() {
        return xOffset;
    }

    public void setxOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public void setyOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public int getyAxisMax() {
        return yAxisMax;
    }

    public void setyAxisMax(int yAxisMax) {
        this.yAxisMax = yAxisMax;
    }

    public int getxAxisMax() {
        return xAxisMax;
    }

    public void setxAxisMax(int xAxisMax) {
        this.xAxisMax = xAxisMax;
    }

    public int getxAxisExtensionPerTime() {
        return xAxisExtensionPerTime;
    }

    public void setxAxisExtensionPerTime(int xAxisExtensionPerTime) {
        this.xAxisExtensionPerTime = xAxisExtensionPerTime;
    }

    public int getxAxisInterval() {
        return xAxisInterval;
    }

    public void setxAxisInterval(int xAxisInterval) {
        this.xAxisInterval = xAxisInterval;
    }

    public Color getToDoColor() {
        return toDoColor;
    }

    public void setToDoColor(Color toDoColor) {
        this.toDoColor = toDoColor;
    }

    public Color getDoneColor() {
        return doneColor;
    }

    public void setDoneColor(Color doneColor) {
        this.doneColor = doneColor;
    }

    public List<Point> getPointsDone() {
        return pointsDone;
    }

    public int getFutureLevelChange() {
        return futureLevelChange;
    }

    public void setFutureLevelChange(int futureLevelChange) {
        this.futureLevelChange = futureLevelChange;
    }

    public void setPointsDone(List<Point> pointsDone) {
        this.pointsDone = pointsDone;
        checkXaxisMaxDone();
        //need to redraw when this is set
        invalidate();
    }

    public List<Point> getPointsAll() {
        return pointsAll;
    }

    public void setPointsAll(List<Point> pointsAll) {
        this.pointsAll = pointsAll;
        checkXaxisMaxAll();
        //make function to check if pointsAll needs to extend xAxisMax
        //call invalidate to update drawing
        invalidate();
    }

    public void addPointToPointsAll(Point pointToBeAdded) {
        this.pointsAll.add(pointToBeAdded);
        checkXaxisMaxAll();
        //make function to check if pointsAll needs to extend xAxisMax
        //call invalidate to update drawing
    }

    public void checkXaxisMaxAll() {
        if (pointsAll != null && !(pointsAll.isEmpty())) {
            if (pointsAll.get(pointsAll.size() - 1).x + 60 > getxAxisMax()) {
                //need to extend xAxisMax
                setxAxisMax(getxAxisMax() + getxAxisExtensionPerTime());
                checkXaxisMaxAll();
            }
        }
    }

    public void checkXaxisMaxDone() {
        if (pointsDone != null && !(pointsDone.isEmpty())) {
            if (pointsDone.get(pointsDone.size() - 1).x + 60 > getxAxisMax()) {
                //need to extend xAxisMax
                setxAxisMax(getxAxisMax() + getxAxisExtensionPerTime());
                checkXaxisMaxDone();
            }
        }
    }

    /*public int getGridDimension() {
        return gridDimension;
    }
    public void setGridDimension(int gridDimension) {
        this.gridDimension = gridDimension;
    }
    public int getLineSlope() {
        return lineSlope;
    }
    public void setLineSlope(int lineSlope) {
        this.lineSlope = lineSlope;
    }
    public int getLineYintercept() {
        return lineYintercept;
    }
    public void setLineYintercept(int lineYintercept) {
        this.lineYintercept = lineYintercept;
    }*/


    public GraphView(Context context) {
        super(context);
        Init();
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Init();
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Init();
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Init();
    }

    public void Init() {
        setxAxisExtensionPerTime(300); //5 minutes extra per time
        setxAxisInterval(300); //grid on X-axis every 5 minutes
        setxAxisMax(1200); //20 minutes to start
        setyAxisMax(32);
        setxOffset(45);
        setyOffset(40);
        setFutureLevelChange(0);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);
        gridPaint.setColor(Color.GRAY);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(3);
        axisPaint.setColor(Color.BLACK);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5);
        linePaint.setColor(Color.BLUE);

        rectangleDonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectangleDonePaint.setStyle(Paint.Style.FILL);
        rectangleDonePaint.setColor(Color.argb(240, 0, 150, 0));

        rectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectanglePaint.setStyle(Paint.Style.FILL);
        rectanglePaint.setColor(Color.argb(150, 50, 0, 255));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(35);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeWidth(1);
        textPaint.setColor(Color.BLACK);

        pointsDone = new ArrayList<Point>();
        pointsAll = new ArrayList<Point>();
        //exampleDrawing();
    }

    public void exampleDrawing() {
        List<Point> tempPoints = new ArrayList<Point>();
        tempPoints.add(new Point(0, 10));
        tempPoints.add(new Point(240, 5));
        tempPoints.add(new Point(600, 10));
        tempPoints.add(new Point(720, 20));
        tempPoints.add(new Point(1500, 6));
        tempPoints.add(new Point(2400, 4));

        this.setPointsAll(tempPoints);

        List<Point> tempDonePoints = new ArrayList<Point>();
        tempDonePoints.add(new Point(240, 10));
        tempDonePoints.add(new Point(600, 5));
        tempDonePoints.add(new Point(720, 10));
        tempDonePoints.add(new Point(900, 20));
        tempDonePoints.add(new Point(960, 18));

        this.setPointsDone(tempDonePoints);
        this.setFutureLevelChange(-2);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        //draw axis and grids (xAxis in seconds)
        for (int x = 0; x <= this.getxAxisMax(); x += xAxisInterval) {
            canvas.drawLine(interpretX(x), interpretY(this.getyAxisMax()), interpretX(x), interpretY(0), (x == 0) ? axisPaint : gridPaint);

        }
        for (int y = 0; y <= this.getyAxisMax(); y += 5) {
            canvas.drawLine(interpretX(0), interpretY(y), interpretX(this.getxAxisMax()), interpretY(y), (y == 0) ? axisPaint : gridPaint);
        }

        /**
         * First Draw Points that are done.
         * Then draw all the other points where x > than lastpointDone and adjusted with futureLevelChange.
         * */

        //save the last second that is done, to know where to start drawing the rest
        int lastSecondDone = 0;

        //draw pointsDone
        int lastLevelDone = 0;
        if (!(pointsDone.isEmpty())) {
            for (int i = 0; i < pointsDone.size(); i++) {
                //draw all the points available
                //Log.d(TAG, "in onDoneDraw: " + lastSecondDone + "| second: " + pointsDone.get(i).x + "| Level: " + pointsDone.get(i).y);
                canvas.drawRect(interpretX(lastSecondDone), interpretY(pointsDone.get(i).y), interpretX(pointsDone.get(i).x), interpretY(0), rectangleDonePaint);
                lastSecondDone = pointsDone.get(i).x;
                lastLevelDone = pointsDone.get(i).y;
            }
        }

        Log.d(TAG, "in onDraw: total points in profile:" + pointsAll.size() + "| Points Done: " + pointsDone.size() + "| LastSecondDone: " + lastSecondDone + "| level printed: " + lastLevelDone);

        //draw pointsAll
        for (int i = 0; i < pointsAll.size(); i++) {
            //draw all the points available
            int nextX;
            int currentX = pointsAll.get(i).x;
            if (i + 1 == pointsAll.size()) {
                nextX = getxAxisMax();
            } else {
                nextX = pointsAll.get(i + 1).x;
            }

            if (nextX > lastSecondDone) {
                //we need to draw something. If x < lastSecondDone, we start from lastSecondDone. Else from x)
                if (currentX < lastSecondDone) {
                    currentX = lastSecondDone;
                }
                canvas.drawRect(interpretX(currentX), interpretY(pointsAll.get(i).y + futureLevelChange), interpretX(nextX), interpretY(0), rectanglePaint);
            }
        }


        /**draw numbers on axis:
         * on y: 0,5,10,15,20,25,30
         * on x: depending on xAxisInterval
         * */
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int x = xAxisInterval; x <= this.getxAxisMax(); x += xAxisInterval) {
            //Log.d(TAG, "text: X: " + x + "| xAxisMax: " + this.getxAxisMax() + "| Interval: " + xAxisInterval);
            canvas.drawText(Integer.toString(x / 60), interpretX(x), interpretY(0) + getyOffset(), textPaint);
        }

        textPaint.setTextAlign(Paint.Align.LEFT);
        for (int y = 5; y <= this.getyAxisMax(); y += 5) {
            //Log.d(TAG, "text: X: " + x + "| xAxisMax: " + this.getxAxisMax() + "| Interval: " + xAxisInterval);
            canvas.drawText(Integer.toString(y), interpretX(0) - getxOffset(), interpretY(y), textPaint);
        }


    }


    private float interpretX(double x) {
        int offset = getxOffset();
        double width = (double) this.getWidth();
        //calculation for -widthdim to +widthdim
        //return (float) ((x+this.getGridDimension()) / (this.getGridDimension()*2)*width);
        //calculation for 0 to xmax
        return (float) ((x / this.getxAxisMax()) * (width - offset)) + offset;
    }

    private float interpretY(double y) {
        int offset = getyOffset();
        double height = (double) this.getHeight();
        //calculation for -heightdim to +heightdim
        //return (float) ((y+this.getGridDimension()) / (this.getGridDimension()*2) * -height + height);
        return (float) ((1 - (y / this.getyAxisMax())) * (height - offset));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = measureHeight(heightMeasureSpec);
        int measuredWidth = measureWidth(widthMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private int measureWidth(int widthMeasureSpec) {
        int specSize = MeasureSpec.getSize(widthMeasureSpec) - this.getPaddingLeft() - this.getPaddingRight();
        return specSize;
    }

    private int measureHeight(int heightMeasureSpec) {
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        return specSize;
    }


}
