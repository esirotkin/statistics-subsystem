package ru.open.monitor.statistics.log.logback.rolling;

import java.io.File;

import ch.qos.logback.core.rolling.RollingFileAppender;

public abstract class AbstractRollingFileWithHeaderAppender extends RollingFileAppender<String> {

    @Override
    public void start() {
        boolean needAppendHeader = false;
        try {
            lock.lock();
            File currentFile = new File(getFile());
            if (!currentFile.exists() || currentFile.length() == 0) {
                needAppendHeader = true;
            }
        } catch (Exception e) {
            addError("Failed to add CSV file header!", e);
        } finally {
            lock.unlock();
        }

        super.start();

        if (needAppendHeader) {
            appendHeader();
        }
    }

    @Override
    public void rollover() {
        super.rollover();
        appendHeader();
    }

    private void appendHeader() {
        append(prepareHeader());
    }

    protected abstract String prepareHeader();

}
