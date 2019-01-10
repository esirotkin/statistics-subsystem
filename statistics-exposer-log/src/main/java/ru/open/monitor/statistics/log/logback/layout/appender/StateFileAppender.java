package ru.open.monitor.statistics.log.logback.layout.appender;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import ch.qos.logback.core.status.ErrorStatus;

public class StateFileAppender<E> extends FileAppender<E> {

    public StateFileAppender() {
        setPrudent(false);
        setAppend(false);
    }

    protected void clearFile() {
        lock.lock();

        ResilientFileOutputStream resilientFOS = (ResilientFileOutputStream) getOutputStream();
        FileChannel fileChannel = resilientFOS.getChannel();
        if (fileChannel == null) {
            return;
        }

        FileLock fileLock = null;
        try {
            fileLock = fileChannel.lock();
            fileChannel.truncate(0);
        } catch (IOException e) {
            resilientFOS.postIOFailure(e);
        } finally {
            if (fileLock != null && fileLock.isValid()) {
                try { fileLock.release(); } catch (Throwable t) {}
            }
            lock.unlock();
        }
    }

    @Override
    protected void subAppend(E event) {
        if (!isStarted()) {
            return;
        }

        clearFile();

        try {
            byte[] byteArray = this.encoder.encode(event);
            writeBytes(byteArray);
        } catch (IOException ioe) {
            // as soon as an exception occurs, move to non-started state
            // and add a single ErrorStatus to the SM.
            this.started = false;
            addStatus(new ErrorStatus("IO failure in appender", this, ioe));
        }
    }

    private void writeBytes(byte[] byteArray) throws IOException {
        if (byteArray == null || byteArray.length == 0) {
            return;
        }

        lock.lock();

        ResilientFileOutputStream resilientFOS = (ResilientFileOutputStream) getOutputStream();
        FileChannel fileChannel = resilientFOS.getChannel();
        if (fileChannel == null) {
            return;
        }
        FileLock fileLock = null;
        try {
            fileLock = fileChannel.lock();
            long position = fileChannel.position();
            long size = fileChannel.size();
            if (size != position) {
                fileChannel.position(size);
            }

            resilientFOS.write(byteArray);
            if (isImmediateFlush()) {
                resilientFOS.flush();
            }
        } catch (IOException e) {
            resilientFOS.postIOFailure(e);
        } finally {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
            }
            lock.unlock();
        }
    }

}
