package kgu.ykita.virtualmarathonbasic1_1;

import android.content.Context;
import android.content.res.AssetManager;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class CourseManager {
    final ArrayList<LatLng> course = new ArrayList<>();
    final ArrayList<Double> courseDistance = new ArrayList<>();

    public CourseManager(Context context) {
        try {
            AssetManager assets = context.getResources().getAssets();
            InputStream in = assets.open("KobeCourse.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String str;
            while ((str = br.readLine()) != null) {
                String[] mLatlng = str.split(",");
                course.add(new LatLng(
                        Double.parseDouble(mLatlng[0].trim()), Double
                        .parseDouble(mLatlng[1].trim())));
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        courseDistance.add(0.0);
        float distance = 0;
        for (int i = 0; i < course.size() - 1; i++) {
            float[] results = new float[3];
            Location.distanceBetween(course.get(i).latitude, course.get(i).longitude, course.get(i + 1).latitude, course.get(i + 1).longitude, results);
            distance += results[0];
            courseDistance.add((double)distance);
        }
    }

    public ArrayList<LatLng> getCourse() {
        return course;
    }

    public Location getLocation(double dis) {
        Location loc = new Location("");

        if (dis >= courseDistance.get(courseDistance.size() - 1)) {
            loc.setLatitude(course.get(courseDistance.size() - 1).latitude);
            loc.setLongitude(course.get(courseDistance.size() - 1).longitude);
            return loc;
        }

        int i;
        for (i = 0; i < courseDistance.size(); i++) {
            if (dis < courseDistance.get(i)) break;
        }

        double lat = course.get(i - 1).latitude + (course.get(i).latitude - course.get(i - 1).latitude) * (dis - courseDistance.get(i - 1)) / (courseDistance.get(i) - courseDistance.get(i - 1));
        double lng = course.get(i - 1).longitude + (course.get(i).longitude - course.get(i - 1).longitude) * (dis - courseDistance.get(i - 1)) / (courseDistance.get(i) - courseDistance.get(i - 1));

        loc.setLatitude(lat);
        loc.setLongitude(lng);

        return loc;
    }
}
