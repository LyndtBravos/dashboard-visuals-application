package com.mediahost.dashboard.util;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;

@Component
public class BusinessHoursChecker {

    public boolean shouldCheck() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        boolean isWeekday = dayOfWeek.getValue() >= 1 && dayOfWeek.getValue() <= 5;
        boolean isWeekend = dayOfWeek.getValue() >= 6 && dayOfWeek.getValue() <= 7;

        if (isWeekday) {
            LocalTime start = LocalTime.of(6, 0);
            LocalTime end = LocalTime.of(21, 0);
            return !currentTime.isBefore(start) && !currentTime.isAfter(end);
        }
        else if (isWeekend) {
            LocalTime start = LocalTime.of(8, 0);
            LocalTime end = LocalTime.of(12, 0);
            return !currentTime.isBefore(start) && !currentTime.isAfter(end);
        }

        return false;
    }

    public boolean shouldCheck(boolean businessHoursOnly, LocalTime businessStart,
                               LocalTime businessEnd, String businessDays) {
        if (!businessHoursOnly)
            return true;

        if (businessStart == null || businessEnd == null)
            return shouldCheck();

        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDayOfWeek = now.getDayOfWeek();
        int currentDay = currentDayOfWeek.getValue();

        String[] daysArray = businessDays.split(",");
        boolean isBusinessDay = false;
        for (String day : daysArray) {
            if (Integer.parseInt(day.trim()) == currentDay) {
                isBusinessDay = true;
                break;
            }
        }

        if (!isBusinessDay) return false;

        boolean afterStart = !currentTime.isBefore(businessStart);
        boolean beforeEnd = !currentTime.isAfter(businessEnd);

        return afterStart && beforeEnd;
    }

    public LocalDateTime getNextCheckTime(LocalDateTime lastCheck, int intervalMinutes) {
        if (lastCheck == null) return LocalDateTime.now();

        LocalDateTime nextCheck = lastCheck.plusMinutes(intervalMinutes);

        while (!shouldCheck()) {
            nextCheck = nextCheck.plusHours(1);

            if (shouldCheck())
                if (isWeekday(nextCheck))
                    nextCheck = nextCheck.withHour(6).withMinute(0).withSecond(0);
                else
                    nextCheck = nextCheck.withHour(8).withMinute(0).withSecond(0);
        }

        return nextCheck;
    }

    private boolean isWeekday(LocalDateTime dateTime) {
        int day = dateTime.getDayOfWeek().getValue();
        return day >= 1 && day <= 5;
    }
}