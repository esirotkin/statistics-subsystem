package ru.open.monitor.statistics.log.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@SuppressWarnings("serial")
public class NumberUtil {

    public static class DecimalFormatter {
        private DecimalFormat formatter;

        public DecimalFormatter(Integer decimals) {
            this(decimals, null);
        }

        public DecimalFormatter(Integer decimals, Character groupingSeparator) {
            DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.getDefault());
            formatSymbols.setGroupingSeparator(groupingSeparator != null ? groupingSeparator : ' ');
            if (decimals != null) {
                if (decimals == 0) {
                    formatter = new DecimalFormat("###,###,###,###,##0", formatSymbols);
                } else {
                    StringBuilder formatString = new StringBuilder("###,###,###,###,##0.");
                    for (int i = 0; i < decimals; i++) {
                        formatString.append(i < 2 ? "0" : "#");
                    }
                    formatter = new DecimalFormat(formatString.toString(), formatSymbols);
                }
            } else {
                formatter = new DecimalFormat("###,###,###,###,##0.########", formatSymbols);
            }
        }

        public String format(Number value) {
            return value != null ? formatter.format(value) : "";
        }

    };

    public static final int SCALE = 2;

    public static boolean isNaN(final Number decimal) {
        if (decimal == null) {
            return false;
        } else if (decimal instanceof Float) {
            return ((Float) decimal).isNaN();
        } else if (decimal instanceof Double) {
            return ((Double) decimal).isNaN();
        } else {
            return false;
        }
    }

    public static boolean isInfinite(final Number decimal) {
        if (decimal == null) {
            return false;
        } else if (decimal instanceof Float) {
            return ((Float) decimal).isInfinite();
        } else if (decimal instanceof Double) {
            return ((Double) decimal).isInfinite();
        } else {
            return false;
        }
    }

    public static String format(Number value) {
        return format(value, null, null);
    }

    public static String format(Number value, Integer decimals) {
        return format(value, decimals, null);
    }

    public static String format(Number value, Integer decimals, Character groupingSeparator) {
        return new DecimalFormatter(decimals, groupingSeparator).format(value);
    }

    public static String formatPlain(double value) {
        return formatPlain(value, SCALE);
    }

    public static String formatPlain(double value, int decimals) {
        return new BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    public static double mathRound(double value) {
        return mathRound(value, SCALE);
    }

    public static double mathRound(double value, int decimals) {
        return Math.round(value * (Math.pow(10.0D, decimals))) / Math.pow(10.0D, decimals);
    }

    public static Double roundHalfUp(final Number decimal) {
        return roundHalfUp(decimal, SCALE);
    }

    public static Double roundHalfUp(final Number decimal, final int scale) {
        return round(decimal, scale, RoundingMode.HALF_UP);
    }

    public static Double roundUp(final Number decimal) {
        return roundUp(decimal, SCALE);
    }

    public static Double roundUp(final Number decimal, final int scale) {
        return round(decimal, scale, RoundingMode.UP);
    }

    public static Double roundHalfDown(final Number decimal) {
        return roundHalfDown(decimal, SCALE);
    }

    public static Double roundHalfDown(final Number decimal, final int scale) {
        return round(decimal, scale, RoundingMode.HALF_DOWN);
    }

    public static Double roundDown(final Number decimal) {
        return roundDown(decimal, SCALE);
    }

    public static Double roundDown(final Number decimal, final int scale) {
        return round(decimal, scale, RoundingMode.DOWN);
    }

    public static Double round(final Number decimal, final int scale, final RoundingMode mode) {
        if (decimal == null) {
            return null;
        }

        return isNaN(decimal) || isInfinite(decimal) ? decimal.doubleValue() : toBigDecimal(decimal).setScale(scale, mode).doubleValue();
    }

    public static BigDecimal toBigDecimal(final Number decimal) {
        if (decimal == null) {
            return null;
        }

        BigDecimal value;
        if (decimal instanceof BigDecimal) {
            value = (BigDecimal) decimal;
        } else {
            value = new BigDecimal(decimal.doubleValue());
        }

        return value;
    }

    public static boolean compareNumbers(final Number actual, final Number expected) {
        return compareNumbers(actual, expected, SCALE);
    }

    public static boolean compareNumbers(final Number actual, final Number expected, final int scale) {
        if (actual == expected) {
            return true;
        }

        if (actual == null || expected == null) {
            return false;
        }

        return Math.abs(actual.doubleValue() - expected.doubleValue()) <= Math.pow(10, scale * (-1));
    }

}
