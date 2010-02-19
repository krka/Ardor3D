/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.google.common.collect.Lists;

/**
 * starter for Ardor3D examples
 */
public class ExampleRunner extends JFrame {

    private static final long serialVersionUID = 1L;
    private final Logger logger = Logger.getLogger(ExampleRunner.class.getCanonicalName());
    private final JTree tree;
    private final ClassTreeModel model;
    private final JSplitPane splitPane;
    private final JLabel lDescription;
    private final JSpinner spMemory;
    private final Action runSelectedAction;
    private final JCheckBox consoleBox;
    private final static String HEADER = "<html><body><h2 align=\"center\">Ardor3d Examples</h2><p align=\"center\"><img src=\""
            + ExampleRunner.class.getResource("/com/ardor3d/example/media/images/ardor3d_white_256.jpg")
            + "\"></p></body></html>";

    public ExampleRunner() {
        setTitle("Ardor3D SDK Examples");
        setLayout(new BorderLayout());
        model = new ClassTreeModel();
        tree = new JTree(model);
        tree.setCellRenderer(new ClassNameCellRenderer());
        runSelectedAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            {
                putValue(Action.NAME, "run");
            }

            public void actionPerformed(final ActionEvent e) {
                runSelected();
            }
        };
        lDescription = new JLabel();
        lDescription.setVerticalTextPosition(SwingConstants.TOP);
        lDescription.setVerticalAlignment(SwingConstants.TOP);
        lDescription.setHorizontalAlignment(SwingConstants.CENTER);
        spMemory = new JSpinner(new SpinnerNumberModel(64, 64, 8192, 1));
        consoleBox = new JCheckBox("Show console: ");
        consoleBox.setHorizontalTextPosition(SwingConstants.LEFT);
        final JPanel pExample = new JPanel(new BorderLayout());
        pExample.add(lDescription);
        final JPanel pSettings = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        pSettings.add(consoleBox);
        pSettings.add(new JLabel("Memory: "));
        pSettings.add(spMemory);
        pSettings.add(new JButton(runSelectedAction));
        pExample.add(pSettings, BorderLayout.SOUTH);

        splitPane = new JSplitPane();
        splitPane.setDividerLocation(300);
        splitPane.setLeftComponent(new JScrollPane(tree));
        splitPane.setRightComponent(pExample);
        add(splitPane);

        model.reload("com.ardor3d.example");

        final JPopupMenu ctxMenu = new JPopupMenu();
        ctxMenu.add(runSelectedAction);
        tree.setComponentPopupMenu(ctxMenu);
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(final TreeSelectionEvent e) {
                updateDescription();
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() > 1) {
                    runSelected();
                }
            }
        });

        updateDescription();
    }

    private void updateDescription() {
        Class<?> selectedClass = null;
        // just take the first selected node
        final TreePath tp = tree.getSelectionPath();
        if (tp != null) {
            final Object selected = tp.getLastPathComponent();
            if (selected instanceof Class<?>) {
                selectedClass = (Class<?>) selected;
            }
        }
        if (selectedClass == null) {
            lDescription.setText(HEADER);
        } else {
            final Purpose purpose = selectedClass.getAnnotation(Purpose.class);
            String imgURL = "";
            if (purpose != null) {
                try {
                    // Look for the example's thumbnail.
                    final URL imageURL = ExampleRunner.class.getResource(purpose.thumbnailPath());
                    if (imageURL != null) {
                        imgURL = "<br><img src=\"" + imageURL + "\">";
                    }
                } catch (final Exception ex) {
                }

                // Set our requested max heap.
                spMemory.setValue(purpose.maxHeapMemory());
            }

            // default to Ardor3D logo if no image available.
            if ("".equals(imgURL)) {
                imgURL = "<img src=\""
                        + ExampleRunner.class.getResource("/com/ardor3d/example/media/images/ardor3d_white_256.jpg")
                        + "\"/>";
            }

            // Set the description HTML
            lDescription.setText("<html><body><h2 align=\"center\">" + selectedClass.getSimpleName() + "</h2>"
                    + (purpose == null ? "" : "<p>" + purpose.htmlDescription() + "</p>") + "<p align=\"center\">"
                    + imgURL + "</p></body></html>");

        }
    }

    private void runSelected() {
        // just take the first selected node
        final TreePath tp = tree.getSelectionPath();
        if (tp == null) {
            return;
        }
        final Object selected = tp.getLastPathComponent();
        if (!(selected instanceof Class<?>)) {
            return;
        }
        final boolean showConsole = (consoleBox.isSelected());
        new Thread() {
            @Override
            public void run() {
                try {
                    final Class<?> clazz = (Class<?>) selected;

                    final boolean isWindows = System.getProperty("os.name").contains("Windows");
                    final List<String> args = Lists.newArrayList();
                    args.add(isWindows ? "javaw" : "java");
                    args.add("-Xmx" + spMemory.getValue() + "M");
                    args.add("-cp");
                    args.add(System.getProperty("java.class.path"));
                    args.add("-Djava.library.path=" + System.getProperty("java.library.path"));
                    args.add(clazz.getCanonicalName());
                    logger.info("start " + args.toString());
                    final ProcessBuilder pb = new ProcessBuilder(args);
                    pb.redirectErrorStream(true);
                    final Process p = pb.start();
                    final InputStream in = p.getInputStream();
                    final DisplayConsole console = showConsole ? new DisplayConsole(clazz.getCanonicalName(), p) : null;
                    new ConsoleStreamer(in, console).start();
                } catch (final Exception ex) {
                    JOptionPane.showMessageDialog(ExampleRunner.this, ex.toString());
                }
            }
        }.start();
    }

    class ClassTreeModel implements TreeModel {

        private final EventListenerList listeners = new EventListenerList();
        private final LinkedHashMap<String, Vector<Class<?>>> classes = new LinkedHashMap<String, Vector<Class<?>>>();
        private final String root = "all examples";

        public void addTreeModelListener(final TreeModelListener l) {
            listeners.add(TreeModelListener.class, l);
        }

        public Object getChild(final Object parent, final int index) {
            if (parent == root) {
                final Vector<String> vec = new Vector<String>(classes.keySet());
                return vec.get(index);
            }
            final Vector<Class<?>> cl = classes.get(parent);
            return cl == null ? null : cl.get(index);
        }

        public int getChildCount(final Object parent) {
            if (parent == root) {
                return classes.size();
            }
            final Vector<Class<?>> cl = classes.get(parent);
            return cl == null ? 0 : cl.size();
        }

        public int getIndexOfChild(final Object parent, final Object child) {
            if (parent == root) {
                return ((List<Vector<Class<?>>>) classes.values()).indexOf(child);
            }
            final Vector<Class<?>> cl = classes.get(parent);
            return cl == null ? 0 : cl.indexOf(child);
        }

        public Object getRoot() {
            return root;
        }

        public void addClassForPackage(final String packageName, final Class<?> clazz) {
            logger.fine("found " + clazz + " in " + packageName);
            if (clazz.equals(ExampleRunner.class)) {
                return;
            }
            Vector<Class<?>> cl = classes.get(packageName);
            if (cl == null) {
                cl = new Vector<Class<?>>();
                classes.put(packageName, cl);
            }
            cl.add(clazz);
        }

        public boolean isLeaf(final Object node) {
            return node instanceof Class<?>;
        }

        public void removeTreeModelListener(final TreeModelListener l) {
            listeners.remove(TreeModelListener.class, l);
        }

        public void valueForPathChanged(final TreePath path, final Object newValue) {
            ; // ignored
        }

        /**
         * @return FileFilter for searching class files (no inner classes, only those with "Test" in the name)
         */
        private FileFilter getFileFilter() {
            return new FileFilter() {
                /**
                 * @see FileFilter
                 */
                public boolean accept(final File pathname) {
                    return (pathname.isDirectory() && !pathname.getName().startsWith("."))
                            || (pathname.getName().endsWith(".class") && pathname.getName().indexOf('$') < 0);
                }

            };
        }

        /**
         * Load a class specified by a file- or entry-name
         * 
         * @param name
         *            name of a file or entry
         * @return class file that was denoted by the name, null if no class or does not contain a main method
         */
        private Class<?> load(final String name) {
            if (name.endsWith(".class") && name.indexOf('$') < 0) {
                String classname = name.substring(0, name.length() - ".class".length());

                if (classname.startsWith("/")) {
                    classname = classname.substring(1);
                }
                classname = classname.replace('/', '.');

                try {
                    final Class<?> cls = Class.forName(classname);
                    cls.getMethod("main", new Class[] { String[].class });
                    if (!getClass().equals(cls)) {
                        return cls;
                    }
                } catch (final NoClassDefFoundError e) {
                    // class has unresolved dependencies
                    return null;
                } catch (final ClassNotFoundException e) {
                    // class not in classpath
                    return null;
                } catch (final NoSuchMethodException e) {
                    // class does not have a main method
                    return null;
                }
            }
            return null;
        }

        /**
         * Used to descent in directories, loads classes via {@link #load}
         * 
         * @param directory
         *            where to search for class files
         * @param allClasses
         *            add loaded classes to this collection
         * @param packageName
         *            current package name for the diven directory
         * @param recursive
         *            true to descent into subdirectories
         */
        private void addAllFilesInDirectory(final File directory, final String packageName, final boolean recursive) {
            // Get the list of the files contained in the package
            final File[] files = directory.listFiles(getFileFilter());
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    // we are only interested in .class files
                    if (files[i].isDirectory()) {
                        if (recursive) {
                            addAllFilesInDirectory(files[i], packageName + files[i].getName() + ".", true);
                        }
                    } else {
                        final Class<?> result = load(packageName + files[i].getName());
                        if (result != null) {
                            addClassForPackage(packageName, result);
                        }
                    }
                }
            }
        }

        protected void fireTreeChanged() {
            for (final TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
                l.treeStructureChanged(new TreeModelEvent(this, new String[] { root }));
            }
        }

        public void reload(final String pckgname) {
            find(pckgname, true);
            fireTreeChanged();
        }

        protected void find(String pckgname, final boolean recursive) {
            URL url;

            // Translate the package name into an absolute path
            String name = pckgname;
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            name = name.replace('.', '/');

            // Get a File object for the package
            // URL url = UPBClassLoader.get().getResource(name);
            url = this.getClass().getResource(name);
            // URL url = ClassLoader.getSystemClassLoader().getResource(name);
            pckgname = pckgname + ".";

            File directory;
            try {
                directory = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException(e); // should never happen
            }

            if (directory.exists()) {
                logger.info("Searching for examples in \"" + directory.getPath() + "\".");
                addAllFilesInDirectory(directory, pckgname, recursive);
            } else {
                try {
                    // It does not work with the filesystem: we must
                    // be in the case of a package contained in a jar file.
                    logger.info("Searching for Demo classes in \"" + url + "\".");
                    final URLConnection urlConnection = url.openConnection();
                    if (urlConnection instanceof JarURLConnection) {
                        final JarURLConnection conn = (JarURLConnection) urlConnection;

                        final JarFile jfile = conn.getJarFile();
                        final Enumeration<JarEntry> e = jfile.entries();
                        while (e.hasMoreElements()) {
                            final ZipEntry entry = e.nextElement();
                            final Class<?> result = load(entry.getName());
                            if (result != null) {
                                addClassForPackage(pckgname, result);
                            }
                        }
                    }
                } catch (final IOException e) {
                    logger.logp(Level.SEVERE, this.getClass().toString(), "find(pckgname, recursive, classes)",
                            "Exception", e);
                } catch (final Exception e) {
                    logger.logp(Level.SEVERE, this.getClass().toString(), "find(pckgname, recursive, classes)",
                            "Exception", e);
                }
            }
        }

    }

    class ClassNameCellRenderer implements TreeCellRenderer {
        DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
        JLabel classNameLabel = new JLabel(" ");
        {
            classNameLabel.setOpaque(true);
        }

        public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
                final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
            Component returnValue = null;
            if ((value != null) && (value instanceof Class<?>)) {
                final Class<?> clazz = (Class<?>) value;
                classNameLabel.setText(clazz.getSimpleName());
                if (selected) {
                    classNameLabel.setBackground(defaultRenderer.getBackgroundSelectionColor());
                    classNameLabel.setForeground(defaultRenderer.getTextSelectionColor());
                } else {
                    classNameLabel.setBackground(defaultRenderer.getBackgroundNonSelectionColor());
                    classNameLabel.setForeground(defaultRenderer.getTextNonSelectionColor());
                }
                classNameLabel.setEnabled(tree.isEnabled());
                returnValue = classNameLabel;
            }
            if (returnValue == null) {
                returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row,
                        hasFocus);
            }
            return returnValue;
        }
    }

    class ConsoleStreamer extends Thread {
        InputStream is;
        DisplayConsole console;

        ConsoleStreamer(final InputStream is, final DisplayConsole console) {
            this.is = is;
            this.console = console;
        }

        @Override
        public void run() {
            try {
                final InputStreamReader isr = new InputStreamReader(is);
                final BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (console != null) {
                        console.appendLine(line);
                    }
                }
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    class DisplayConsole extends JFrame {
        private static final long serialVersionUID = 1L;

        final int MAX_CHARACTERS = 50000;
        private final JTextArea textArea;
        private final JScrollPane scrollPane;

        public DisplayConsole(final String className, final Process process) {
            textArea = new JTextArea(className + ": console started...");
            textArea.setEditable(false);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);

            scrollPane = new JScrollPane(textArea);
            scrollPane.setAutoscrolls(true);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(scrollPane, BorderLayout.CENTER);

            setSize(500, 300);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setLocationByPlatform(true);
            setVisible(true);

            // Close when process ends.
            new Thread() {
                @Override
                public void run() {
                    try {
                        process.waitFor();
                    } catch (final InterruptedException ex) {
                    }
                    setVisible(false);
                    dispose();
                }
            }.start();
        }

        public void appendLine(final String line) {
            String content = textArea.getText() + "\n" + line;
            if (content.length() > MAX_CHARACTERS) {
                content = content.substring(content.length() - MAX_CHARACTERS);
            }
            textArea.setText(content);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    textArea.setCaretPosition(textArea.getText().length());
                }
            });
        }
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
        }
        final ExampleRunner app = new ExampleRunner();
        app.setSize(700, 400);
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setLocationRelativeTo(null);
        app.setVisible(true);
    }
}
