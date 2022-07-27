package osy.kcg.mykotlin;

import java.util.ArrayList;

class Polygon {

    public class XY{
        double x, y;
        XY(double x, double y){
            this.x = x;
            this.y = y;
        }
    }

    private ArrayList<XY> xyList = new ArrayList<>();

    public void addPoint(double x, double y) {
        xyList.add(new XY(x, y));
    }

    boolean isInside(double x, double y) {
        int crossCount = 0;
        for (int i = 0; i < xyList.size() ; i++) {
            int j = (i + 1) % xyList.size();
            XY curPoint = xyList.get(i);
            XY nexPoint = xyList.get(j);
            if ((curPoint.y > y) != (nexPoint.y > y)) { //점 B가 선분 (curPoint, nexPoint)의 y좌표 사이에 있음
                double atX = (nexPoint.x - curPoint.x) * (y - curPoint.y) / (nexPoint.y - curPoint.y) + curPoint.x; //atX는 점 B를 지나는 수평선과 선분 (p[i], p[j])의 교점
                if (atX > x) crossCount++;   //atX가 오른쪽 반직선과의 교점이 맞으면 교점의 개수를 증가시킨다.
            }
        }
        return crossCount % 2 > 0;
    }

}

