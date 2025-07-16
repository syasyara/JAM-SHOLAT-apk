package com.example.jamsholat;

public class PrayerCalculator {

    private static double degToRad(double deg) {
        return deg * Math.PI / 180.0;
    }

    private static double radToDeg(double rad) {
        return rad * 180.0 / Math.PI;
    }

    private static double safeAcos(double x) {
        if (x < -1.0) x = -1.0;
        if (x > 1.0) x = 1.0;
        return Math.acos(x);
    }

    private static double moreLess360(double angle) {
        angle = angle % 360;
        return (angle < 0) ? angle + 360 : angle;
    }

    public static double[] calcPrayerTimes(int year, int month, int day,
                                           double longitude, double latitude, int timeZone,
                                           double fajrTwilight, double ishaTwilight) {

        double D = (367 * year) - ((year + (int) ((month + 9) / 12)) * 7 / 4)
                + (((int) (275 * month / 9)) + day - 730531.5);

        double L = moreLess360(280.461 + 0.9856474 * D);
        double M = moreLess360(357.528 + 0.9856003 * D);
        double Lambda = moreLess360(L + 1.915 * Math.sin(degToRad(M)) + 0.02 * Math.sin(degToRad(2 * M)));
        double Obliquity = 23.439 - 0.0000004 * D;

        double Alpha = radToDeg(Math.atan(Math.cos(degToRad(Obliquity)) * Math.tan(degToRad(Lambda))));
        Alpha = moreLess360(Alpha);
        Alpha += 90 * (Math.floor(Lambda / 90) - Math.floor(Alpha / 90));

        double ST = 100.46 + 0.985647352 * D;
        double Dec = radToDeg(Math.asin(Math.sin(degToRad(Obliquity)) * Math.sin(degToRad(Lambda))));
        double Durinal_Arc = radToDeg(Math.acos((Math.sin(degToRad(-0.8333)) - Math.sin(degToRad(Dec)) * Math.sin(degToRad(latitude))) /
                (Math.cos(degToRad(Dec)) * Math.cos(degToRad(latitude)))));

        double Noon = moreLess360(Alpha - ST);
        double UT_Noon = Noon - longitude;
        double zuhrTime = UT_Noon / 15.0 + timeZone;
        double angle = radToDeg(Math.atan(1 / (Math.tan(Math.abs(degToRad(latitude - Dec))))));

        double Asr_Arc = radToDeg(safeAcos(
                (Math.sin(degToRad(90 - angle)) - Math.sin(degToRad(Dec)) * Math.sin(degToRad(latitude))) /
                        (Math.cos(degToRad(Dec)) * Math.cos(degToRad(latitude)))
        ));

        double asrTime = zuhrTime + (Asr_Arc / 15.0);
        double sunRiseTime = zuhrTime - (Durinal_Arc / 15.0);
        double maghribTime = zuhrTime + (Durinal_Arc / 15.0);

        double Esha_Arc = radToDeg(Math.acos((Math.sin(degToRad(ishaTwilight)) - Math.sin(degToRad(Dec)) * Math.sin(degToRad(latitude))) /
                (Math.cos(degToRad(Dec)) * Math.cos(degToRad(latitude)))));
        double ishaTime = zuhrTime + (Esha_Arc / 15.0);

        double Fajr_Arc = radToDeg(Math.acos((Math.sin(degToRad(fajrTwilight)) - Math.sin(degToRad(Dec)) * Math.sin(degToRad(latitude))) /
                (Math.cos(degToRad(Dec)) * Math.cos(degToRad(latitude)))));
        double fajrTime = zuhrTime - (Fajr_Arc / 15.0);

        return new double[]{fajrTime, sunRiseTime, zuhrTime, asrTime, maghribTime, ishaTime};
    }
}