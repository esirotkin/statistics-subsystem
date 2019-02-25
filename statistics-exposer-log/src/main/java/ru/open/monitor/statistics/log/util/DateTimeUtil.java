package ru.open.monitor.statistics.log.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class DateTimeUtil {

    public static final String DATE_FORMAT = "dd.MM.yyyy";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final Pattern TIME_REGEX = Pattern.compile("^([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$");
    public static final String DATE_TIME_FORMAT = DATE_FORMAT + " " + TIME_FORMAT;

    private static DatatypeFactory FACTORY;

    static {
        try {
            FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static SimpleDateFormat getDateFormatter() {
        return getDateFormatter(DATE_FORMAT);
    }

    public static SimpleDateFormat getTimeFormatter() {
        return getDateFormatter(TIME_FORMAT);
    }

    public static SimpleDateFormat getDateTimeFormatter() {
        return getDateFormatter(DATE_TIME_FORMAT);
    }

    public static SimpleDateFormat getDateFormatter(String format) {
        return new SimpleDateFormat(format);
    }

    public static String formatDate(Object date) {
        return formatDate(date, DATE_FORMAT);
    }

    public static String formatDate(Date date) {
        return formatDate(date, DATE_FORMAT);
    }

    public static String formatTime(Object time) {
        return formatDate(time, TIME_FORMAT);
    }

    public static String formatTime(Date time) {
        return formatDate(time, TIME_FORMAT);
    }

    public static String formatDateTime(Object dateTime) {
        return formatDate(dateTime, DATE_TIME_FORMAT);
    }

    public static String formatDateTime(Date dateTime) {
        return formatDate(dateTime, DATE_TIME_FORMAT);
    }

    public static String formatDate(Object date, String format) {
        if (isDate(date)) {
            return formatDate(toDate(date), format);
        }

        return "'" + date + "'";
    }

    public static String formatDate(Date date, String format) {
        return date != null && format != null ? getDateFormatter(format).format(date) : "'" + date + "'";
    }

    public static Date parseDate(String date) throws ParseException {
        return parseDate(date, DATE_FORMAT);
    }

    public static Date parseDate(String date, String format) throws ParseException {
        if (format == null) {
            format = DATE_FORMAT;
        }

        return date != null ? getDateFormatter(format).parse(date) : null;
    }

    public static int[] getTimeFor(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MILLISECOND, 0);
        return new int[] { calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND) };
    }

    public static int[] getTimeFor(String time) throws ParseException {
        Matcher matcher = TIME_REGEX.matcher(time.trim());
        if (matcher.matches()) {
            int hour   = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            int second = Integer.parseInt(matcher.group(3));
            return new int[] { hour, minute, second };
        }
        throw new ParseException(time, 0);
    }

    public static boolean containsTime(Date date) {
        if (date == null) {
            return false;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR) != 0 || calendar.get(Calendar.MINUTE) != 0 || calendar.get(Calendar.SECOND) != 0;
    }

    /**
     * Compares the time values represented by two array objects.<br>
     * Each object must be an array of integers with the hour, minute and second as its first, second and third values.
     *
     * @param originalTime the original time value.
     * @param timeToCompare the time to be compared.
     * @return the value <code>0</code> if the time represented by the {@code timeToCompare} argument is equal to the time represented by {@code originalTime} argument;
     * a value less than <code>0</code> if the time of {@code originalTime} argument is before the time represented by the {@code timeToCompare} argument;
     * and a value greater than <code>0</code> if the time of {@code originalTime} argument is after the time represented by the {@code timeToCompare} argument.
     * @exception NullPointerException if some of specified arguments is <code>null</code>.
     * @exception IllegalArgumentException if the time value of some of the specified arguments can't be obtained due to any invalid described time values.
     */
    public static int compareTime(int[] originalTime, int[] timeToCompare) {
        if (originalTime == null || timeToCompare == null)
            throw new NullPointerException("The time parameters must be not null!");
        if (originalTime.length != 3 || timeToCompare.length != 3)
            throw new IllegalArgumentException("Time must be represented as array of integers with { hour, minute, second } as its values!");

        if (originalTime[0] == timeToCompare[0] && originalTime[1] == timeToCompare[1] && originalTime[2] == timeToCompare[2]) {
            return 0; // only for faster calculation
        }

        if (originalTime[0] != timeToCompare[0]) {
            if (originalTime[0] < timeToCompare[0]) {
                return -1;
            } else {
                return 1;
            }
        } else {
            if (originalTime[1] != timeToCompare[1]) {
                if (originalTime[1] < timeToCompare[1]) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                if (originalTime[2] != timeToCompare[2]) {
                    if (originalTime[2] < timeToCompare[2]) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    return 0;
                }
            }
        }
    }

    /**
     * Compares the date values represented by two {@link Date} objects.<br>
     *
     * @param originalDate the original date value.
     * @param dateToCompare the date to be compared.
     * @return the value <code>0</code> if the date represented by the {@code dateToCompare} argument is equal to the date represented by {@code originalDate} argument;
     * a value less than <code>0</code> if the date of {@code originalDate} argument is before the date represented by the {@code dateToCompare} argument;
     * and a value greater than <code>0</code> if the date of {@code originalDate} argument is after the date represented by the {@code dateToCompare} argument.
     * @exception NullPointerException if some of specified arguments is <code>null</code>.
     */
    public static int compareDate(Date originalDate, Date dateToCompare) {
        if (originalDate == null || dateToCompare == null)
            throw new NullPointerException("The date parameters must be not null!");

        return excludeTimeFrom(originalDate).compareTo(excludeTimeFrom(dateToCompare));
    }

    public static Date setTimeTo(Date date, int hour, int minute, int second) {
        return setTimeTo(date, hour, minute, second, 0);
    }

    public static Date setTimeTo(Date date, int hour, int minute, int second, int millisecond) {
        if (date == null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);
        return calendar.getTime();
    }

    public static Date excludeTimeFrom(Date date) {
        return setTimeTo(date, 0, 0, 0, 0);
    }

    public static Date toBeginnigOfDay(Date date) {
        return setTimeTo(date, 0, 0, 0, 0);
    }

    public static Date toEndOfDay(Date date) {
        return setTimeTo(date, 23, 59, 59, 999);
    }

    public static boolean isDateFromPeriod(Date date, Date dateFrom, Date dateTo) {
        if (date == null || dateFrom == null || dateTo == null) return false;
        return date.after(dateFrom) && date.before(dateTo) || (date.equals(dateFrom) || date.equals(dateTo));
    }

    public static boolean isDate(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof XMLGregorianCalendar) {
            return true;
        } else if (value instanceof Calendar) {
            return true;
        } else if (value instanceof Date) {
            return true;
        } else {
            return false;
        }
    }

    public static Date toDate(Object value) {
        if (value == null) {
            return null;
        }

        Date date;
        if (value instanceof XMLGregorianCalendar) {
            date = ((XMLGregorianCalendar) value).toGregorianCalendar().getTime();
        } else if (value instanceof Calendar) {
            date = ((Calendar) value).getTime();
        } else {
            date = toDate((Date) value);
        }
        return date;
    }

    public static <D extends Date> Date toDate(final D date) {
        return date != null ? new Date(date.getTime()) : null;
    }

    public static XMLGregorianCalendar toXmlGregorianCalendar(Date date) {
        if (date == null) {
            return null;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return FACTORY.newXMLGregorianCalendar(calendar);
    }

}
