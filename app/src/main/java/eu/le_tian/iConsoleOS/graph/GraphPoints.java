package eu.le_tian.iConsoleOS.graph;

public class GraphPoints {
    private int level;
    private int startPoint;

    public GraphPoints(int level, int startPoint) {
        this.level = level;
        this.startPoint = startPoint;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(int startPoint) {
        this.startPoint = startPoint;
    }

    public void setLevelAndStartPoint(int level, int StartPoint) {
        this.level = level;
        this.startPoint = startPoint;
    }

}
