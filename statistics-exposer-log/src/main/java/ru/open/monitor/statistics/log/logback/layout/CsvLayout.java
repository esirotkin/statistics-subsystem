package ru.open.monitor.statistics.log.logback.layout;

import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;

public class CsvLayout<E> extends LayoutBase<E> {

    public enum LineSeparator {
        SYSTEM(LINE_SEPARATOR), UNIX("\n"), MAC("\r"), WIN("\r\n");

        private final String lineSeparator;

        LineSeparator(final String lineSeparator) {
            this.lineSeparator = lineSeparator;
        }

        public String getLineSeparator() {
            return lineSeparator;
        }

        @Override
        public String toString() {
            return lineSeparator;
        }
    };

    private LineSeparator lineSeparator = LineSeparator.SYSTEM;

    public void setLineSeparator(LineSeparator lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public LineSeparator getLineSeparator() {
        return lineSeparator;
    }

    public String doLayout(E event) {
        StringBuilder line = new StringBuilder();
        if (event instanceof ILoggingEvent) {
            line.append(((ILoggingEvent) event).getFormattedMessage());
        } else {
            line.append(event);
        }
        line.append(lineSeparator);
        return line.toString();
    }

}
