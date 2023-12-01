package com.github.pister.tson.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DateTimeUtil {


    private DateTimeUtil() {
    }

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";
    public static final String LOCAL_DATE_FORMAT = "yyyy-MM-dd";
    public static final String LOCAL_TIME_FORMAT = "HH:mm:ss.SSSSSS";

    private static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(DATE_TIME_FORMAT);
        }
    };

    private static final ConcurrentMap<String, DateTimeFormatter> patternCache = new ConcurrentHashMap<>();


    public static DateTimeFormatter getDateTimeFormatter(String patternFormat) {
        return patternCache.computeIfAbsent(patternFormat, k -> DateTimeFormatter.ofPattern(patternFormat));
    }

    public static LocalDate parseDate(String input) {
        DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(LOCAL_DATE_FORMAT);
        return LocalDate.parse(input, dateTimeFormatter);
    }

    public static LocalDateTime parseDateTime(String input) {
        DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(LOCAL_DATE_TIME_FORMAT);
        return LocalDateTime.parse(input, dateTimeFormatter);
    }

    public static LocalTime parseTime(String input) {
        DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(LOCAL_TIME_FORMAT);
        return LocalTime.parse(input, dateTimeFormatter);
    }

    public static String format(Temporal input, String patternFormat) {
        DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(patternFormat);
        return dateTimeFormatter.format(input);
    }

    public static String format(Date date) {
        return dateFormatThreadLocal.get().format(date);
    }

    public static Date parseForDate(String input) {
        try {
            return dateFormatThreadLocal.get().parse(input);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
