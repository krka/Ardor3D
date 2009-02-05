/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class BetterFormatter extends Formatter {

    private final Date _date = new Date();
    // private final static String format = "{0,date} {0,time}";
    private final static String _format = "{0,time}";
    private MessageFormat _formatter;

    private final Object _args[] = new Object[1];

    // Line separator string. This is the value of the line.separator
    // property at the moment that the BetterFormatter was created.
    private final String _lineSeparator = System.getProperty("line.separator");

    /**
     * Format the given LogRecord.
     * 
     * @param record
     *            the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public synchronized String format(final LogRecord record) {
        final StringBuffer sb = new StringBuffer();

        _date.setTime(record.getMillis());
        _args[0] = _date;
        final StringBuffer text = new StringBuffer();
        if (_formatter == null) {
            _formatter = new MessageFormat(_format);
        }
        _formatter.format(_args, text, null);
        sb.append(text);

        sb.append(" [");
        sb.append(record.getThreadID());
        sb.append("] ");

        if (record.getSourceClassName() != null) {
            sb.append(record.getSourceClassName());
        } else {
            sb.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) {
            sb.append("->");
            sb.append(record.getSourceMethodName());
            sb.append("()");
        }

        sb.append(" - ");
        final String message = formatMessage(record);
        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");
        sb.append(message);
        sb.append(" ");

        final Object[] params = record.getParameters();
        if (params != null) {
            sb.append("{");
            for (int i = 0; i < params.length; i++) {
                sb.append(params[i]);
                if (i < params.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            // sb.append(lineSeparator);
        }

        // Find all the threads
        // ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        // while (root.getParent() != null) {
        // root = root.getParent();
        // }
        // visit(sb, root, 0);

        if (record.getThrown() != null) {
            sb.append(_lineSeparator);
            try {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (final Exception ex) {
            }
        }

        sb.append(_lineSeparator);

        return sb.toString();
    }

    // This method recursively visits all thread groups under `group'.
    private void visit(final StringBuffer sb, final ThreadGroup group, final int level) {
        // Get threads in `group'
        int numThreads = group.activeCount();
        final Thread[] threads = new Thread[numThreads * 2];
        numThreads = group.enumerate(threads, false);

        // Enumerate each thread in `group'
        for (int i = 0; i < numThreads; i++) {
            // Get thread
            final Thread thread = threads[i];

            sb.append(thread.getName() + "[" + thread.getId() + "]");
            sb.append(_lineSeparator);
        }

        // Get thread subgroups of `group'
        int numGroups = group.activeGroupCount();
        final ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
        numGroups = group.enumerate(groups, false);

        // Recursively visit each subgroup
        for (int i = 0; i < numGroups; i++) {
            visit(sb, groups[i], level + 1);
        }
    }
}
