package com.example.modumessenger.common.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class TimeCalculator {

    public static String calculateTime(String sendTime) {
        DateTimeFormatter formatter;
        LocalDateTime chatTime, currentTime;
        Instant chatInstant, currentInstant;
        Date chatDate, currentDate;
        Calendar chatCalendar, currentCalendar;
        String currentTimeStr, result;

        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // chat time
        chatTime = LocalDateTime.parse(sendTime, formatter);
        chatInstant = chatTime.atZone(ZoneId.systemDefault()).toInstant();
        chatDate = Date.from(chatInstant);
        chatCalendar = Calendar.getInstance();
        chatCalendar.setTime(chatDate);

        // current time
        currentTime = LocalDateTime.now();
        currentTimeStr = currentTime.format(formatter);
        currentInstant = currentTime.atZone(ZoneId.systemDefault()).toInstant();
        currentDate = Date.from(currentInstant);
        currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(currentDate);

        result = sendTime;
        if(chatCalendar.compareTo(currentCalendar) >= 0) {
            result = currentTimeStr;
        } else {
            currentCalendar.add(Calendar.MINUTE, -1);

            if(chatCalendar.compareTo(currentCalendar) < 0) {
                result = currentTimeStr;
            }
        }

        return result;
    }
}
