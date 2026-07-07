package me.azuredev.smartrewards.utils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class TimeUtil {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private TimeUtil() {}

    public static LocalDate today() {
        return LocalDate.now(MessageUtil.getTimezone());
    }

    public static long daysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(from, to);
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0с";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return hours + "ч " + minutes + "м";
        }
        if (minutes > 0) {
            return minutes + "м " + secs + "с";
        }
        return secs + "с";
    }

    public static String formatPlaytime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours + "ч " + minutes + "м";
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "N/A" : date.format(DATE_FORMAT);
    }

    public static long secondsUntilMidnight() {
        ZoneId zone = MessageUtil.getTimezone();
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime midnight = LocalDate.now(zone).plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Duration.between(now, midnight).getSeconds();
    }

    public static boolean isInDateRange(String start, String end, LocalDate date) {
        if (start == null || end == null) {
            return false;
        }
        try {
            int[] startParts = parseMonthDay(start);
            int[] endParts = parseMonthDay(end);
            int month = date.getMonthValue();
            int day = date.getDayOfMonth();

            int startValue = startParts[0] * 100 + startParts[1];
            int endValue = endParts[0] * 100 + endParts[1];
            int current = month * 100 + day;

            if (startValue <= endValue) {
                return current >= startValue && current <= endValue;
            }
            return current >= startValue || current <= endValue;
        } catch (Exception e) {
            return false;
        }
    }

    private static int[] parseMonthDay(String value) {
        String[] parts = value.split("-");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
}
