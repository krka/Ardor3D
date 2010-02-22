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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.Document;
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
    private final JSlider sliderMemory;
    private final Action runSelectedAction;
    private final ErasableTextField tfPattern;
    private final JTabbedPane tabbedPane;
    private final DisplayConsole console;
    private final int[] memorySettings = { 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096, 6144,
            8192 };

    private final static String HEADER = "<html><body><h2 align=\"center\">Ardor3d Examples</h2><p align=\"center\"><img src=\""
            + ExampleRunner.class.getResource("/com/ardor3d/example/media/images/ardor3d_white_256.jpg")
            + "\"></p></body></html>";
    private static Comparator<Class<?>> classComparator = new Comparator<Class<?>>() {

        public int compare(final Class<?> o1, final Class<?> o2) {
            return o1.getCanonicalName().compareTo(o2.getCanonicalName());
        }
    };

    public ExampleRunner() {
        setTitle("Ardor3D SDK Examples");
        setLayout(new BorderLayout());

        model = new ClassTreeModel();
        tree = new JTree(model);
        tree.setCellRenderer(new ClassNameCellRenderer(model));
        tfPattern = new ErasableTextField(10);
        tfPattern.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(final DocumentEvent e) {
                search();
            }

            public void insertUpdate(final DocumentEvent e) {
                search();
            }

            public void changedUpdate(final DocumentEvent e) {
                search();
            }
        });
        final JToolBar toolbar = new JToolBar();
        final AbstractAction expandAction = new AbstractAction() {

            private static final long serialVersionUID = 1L;
            {
                putValue(Action.SMALL_ICON, new ImageIcon(ExampleRunner.class
                        .getResource("/com/ardor3d/example/media/icons/view-list-tree.png")));
                putValue(Action.SHORT_DESCRIPTION, "Expand all branches");
            }

            public void actionPerformed(final ActionEvent e) {
                if (((JToggleButton) e.getSource()).isSelected()) {
                    for (int row = 0; row < tree.getRowCount(); row++) {
                        tree.expandRow(row);
                    }
                } else {
                    for (int row = 1; row < tree.getRowCount(); row++) {
                        tree.collapseRow(row);
                    }
                }
            }
        };
        final JToggleButton btExpand = new JToggleButton(expandAction);
        final JPanel pTree = new JPanel(new BorderLayout());
        final JScrollPane scrTree = new JScrollPane(tree);
        scrTree.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        pTree.add(scrTree);
        pTree.add(toolbar, BorderLayout.NORTH);

        runSelectedAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            {
                putValue(Action.SMALL_ICON, new ImageIcon(ExampleRunner.class
                        .getResource("/com/ardor3d/example/media/icons/media-playback-start.png")));
                putValue(Action.SHORT_DESCRIPTION, "Run the selected example.");
                putValue(Action.NAME, "Run");
            }

            public void actionPerformed(final ActionEvent e) {
                runSelected();
            }
        };
        final JButton runButton = new JButton(runSelectedAction);
        runButton.setBorder(null);
        toolbar.add(btExpand);
        toolbar.add(runButton);
        toolbar.add(tfPattern);

        lDescription = new JLabel();
        lDescription.setVerticalTextPosition(SwingConstants.TOP);
        lDescription.setVerticalAlignment(SwingConstants.TOP);
        lDescription.setHorizontalAlignment(SwingConstants.CENTER);
        sliderMemory = new JSlider(0, memorySettings.length - 1, 0);
        sliderMemory.setToolTipText("Set the maximum heap memory for this demo.");
        final JPanel pExample = new JPanel(new BorderLayout());
        pExample.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        pExample.add(lDescription);
        final JPanel pSettings = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        pSettings.add(new JLabel("Memory: "));
        final JLabel lMemory = new JLabel("64M");
        sliderMemory.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                lMemory.setText(memorySettings[sliderMemory.getValue()] + "M");
            }
        });
        pSettings.add(lMemory);
        pSettings.add(sliderMemory);
        pSettings.add(new JButton(runSelectedAction));
        pExample.add(pSettings, BorderLayout.SOUTH);
        console = new DisplayConsole();
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(SwingConstants.BOTTOM);
        tabbedPane.addTab("Description", new ImageIcon(ExampleRunner.class
                .getResource("/com/ardor3d/example/media/icons/declaration.png")), pExample);
        tabbedPane.addTab("Console", new ImageIcon(ExampleRunner.class
                .getResource("/com/ardor3d/example/media/icons/console.png")), console);

        splitPane = new JSplitPane();
        splitPane.setDividerLocation(300);
        splitPane.setLeftComponent(pTree);
        splitPane.setRightComponent(tabbedPane);
        add(splitPane);

        model.reload("com.ardor3d.example");

        final JPopupMenu ctxMenu = new JPopupMenu();
        ctxMenu.add(runSelectedAction);
        tree.setComponentPopupMenu(ctxMenu);
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(final TreeSelectionEvent e) {
                updateDescription();
                updateActionStatus();
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
        updateActionStatus();
    }

    private void search() {
        final String pattern = tfPattern.getText();
        final int matches = model.updateMatches(pattern);
        tfPattern.setWarning(pattern.length() > 0 && matches == 0);
        tree.repaint();
    }

    private void updateActionStatus() {
        final TreePath tp = tree.getSelectionPath();
        final boolean canRun = tp != null && tp.getLastPathComponent() instanceof Class<?>;
        runSelectedAction.setEnabled(canRun);
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
                // spMemory.setValue(purpose.maxHeapMemory());
                int memIndex = Arrays.binarySearch(memorySettings, purpose.maxHeapMemory());
                if (memIndex < 0) {
                    memIndex = -memIndex;
                }
                sliderMemory.setValue(memIndex);
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
        new Thread() {
            @Override
            public void run() {
                try {
                    final Class<?> clazz = (Class<?>) selected;

                    final boolean isWindows = System.getProperty("os.name").contains("Windows");
                    final List<String> args = Lists.newArrayList();
                    args.add(isWindows ? "javaw" : "java");
                    args.add("-Xmx" + memorySettings[sliderMemory.getValue()] + "M");
                    args.add("-cp");
                    args.add(System.getProperty("java.class.path"));
                    args.add("-Djava.library.path=" + System.getProperty("java.library.path"));
                    args.add(clazz.getCanonicalName());
                    logger.info("start " + args.toString());
                    final ProcessBuilder pb = new ProcessBuilder(args);
                    pb.redirectErrorStream(true);
                    final Process p = pb.start();
                    final InputStream in = p.getInputStream();
                    console.started(clazz.getCanonicalName(), p);
                    new ConsoleStreamer(in, console).start();
                } catch (final Exception ex) {
                    JOptionPane.showMessageDialog(ExampleRunner.this, ex.toString());
                }
            }
        }.start();
    }

    interface SearchFilter {
        public boolean matches(final Object value);

        public int updateMatches(final String p);
    }

    class ClassTreeModel implements TreeModel, SearchFilter {

        private final EventListenerList listeners = new EventListenerList();
        private final LinkedHashMap<Package, Vector<Class<?>>> classes = new LinkedHashMap<Package, Vector<Class<?>>>();
        // the next two maps are for caching the status for the search filter
        private final HashMap<String, Boolean> classMatches = new HashMap<String, Boolean>();
        private final HashMap<Package, Boolean> packageMatches = new HashMap<Package, Boolean>();
        private String root = "all examples";
        private FileFilter classFileFilter;

        public void addTreeModelListener(final TreeModelListener l) {
            listeners.add(TreeModelListener.class, l);
        }

        public Object getChild(final Object parent, final int index) {
            if (parent == root) {
                final Vector<Package> vec = new Vector<Package>(classes.keySet());
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

        public void addClassForPackage(final Class<?> clazz) {
            logger.fine("found " + clazz);
            if (clazz.equals(ExampleRunner.class)) {
                return;
            }
            packageMatches.put(clazz.getPackage(), false);
            classMatches.put(clazz.getCanonicalName(), false);
            Vector<Class<?>> cl = classes.get(clazz.getPackage());
            if (cl == null) {
                cl = new Vector<Class<?>>();
                classes.put(clazz.getPackage(), cl);
            }
            cl.add(clazz);
            Collections.sort(cl, classComparator);
        }

        public int updateMatches(final String pattern) {
            int numberMatches = 0;
            final String lcPattern = pattern.toLowerCase();
            packageMatches.clear();
            for (final Entry<String, Boolean> entry : classMatches.entrySet()) {
                final String className = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
                final boolean bool = !pattern.equals("") && className.toLowerCase().contains(lcPattern);
                entry.setValue(bool);
                logger.fine(pattern + ": " + entry.getKey() + " set to " + bool);
                if (bool) {
                    numberMatches++;
                    Package pkg;
                    try {
                        pkg = Class.forName(entry.getKey()).getPackage();
                        packageMatches.put(pkg, true);
                    } catch (final ClassNotFoundException ex) {
                    }
                }
            }
            return numberMatches;
        }

        public boolean matches(final Object value) {
            if (value instanceof Class<?>) {
                return classMatches.get(((Class<?>) value).getCanonicalName());
            }
            final Boolean res = packageMatches.get(value);
            logger.fine("check " + value + " results in: " + res);
            return res == null ? false : res;
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
         * @return FileFilter for searching class files (no inner classes)
         */
        private FileFilter getFileFilter() {
            if (classFileFilter == null) {
                classFileFilter = new FileFilter() {
                    /**
                     * @see FileFilter
                     */
                    public boolean accept(final File pathname) {
                        return (pathname.isDirectory() && !pathname.getName().startsWith("."))
                                || (pathname.getName().endsWith(".class") && pathname.getName().indexOf('$') < 0);
                    }

                };
            }
            return classFileFilter;
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
            logger.fine(directory + " -> " + packageName);
            final File[] files = directory.listFiles(getFileFilter());
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    // we are only interested in .class files
                    if (files[i].isDirectory()) {
                        if (recursive) {
                            addAllFilesInDirectory(files[i], packageName + "." + files[i].getName(), true);
                        }
                    } else {
                        final Class<?> result = load(packageName + "." + files[i].getName());
                        if (result != null) {
                            addClassForPackage(result);
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
            root = pckgname;
            find(pckgname, true);
            fireTreeChanged();
        }

        protected void find(final String pckgname, final boolean recursive) {
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
            // pckgname = pckgname + ".";

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
                                addClassForPackage(result);
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
        Font defaultFont = classNameLabel.getFont();
        Font matchFont = defaultFont.deriveFont(Font.BOLD);
        SearchFilter searchFilter;

        public ClassNameCellRenderer(final SearchFilter searchFilter) {
            this.searchFilter = searchFilter;
        }

        public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
                final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
            if ((value != null) && (value instanceof Class<?>)) {
                final Class<?> clazz = (Class<?>) value;
                classNameLabel.setText(clazz.getSimpleName());
            } else if ((value != null) && value instanceof Package) {
                String name = ((Package) value).getName();
                if (name.startsWith(tree.getModel().getRoot().toString())) {
                    name = name.substring(tree.getModel().getRoot().toString().length() + 1);
                }
                classNameLabel.setText(name);
            } else {
                classNameLabel.setText(value.toString());
            }
            classNameLabel.setFont(searchFilter.matches(value) ? matchFont : defaultFont);
            if (selected) {
                classNameLabel.setBackground(defaultRenderer.getBackgroundSelectionColor());
                classNameLabel.setForeground(defaultRenderer.getTextSelectionColor());
            } else {
                classNameLabel.setBackground(defaultRenderer.getBackgroundNonSelectionColor());
                classNameLabel.setForeground(defaultRenderer.getTextNonSelectionColor());
            }
            classNameLabel.setEnabled(tree.isEnabled());
            return classNameLabel;
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

    class DisplayConsole extends JPanel {
        private static final long serialVersionUID = 1L;

        final int MAX_CHARACTERS = 50000;
        private final JTextArea textArea;
        private final JScrollPane scrollPane;

        public DisplayConsole() {
            textArea = new JTextArea("Nothing started yet.");
            textArea.setEditable(false);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);

            scrollPane = new JScrollPane(textArea);
            scrollPane.setAutoscrolls(true);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            setLayout(new BorderLayout());
            add(scrollPane, BorderLayout.CENTER);

            // setSize(500, 300);
            // setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            // setLocationByPlatform(true);
            // setVisible(true);

            // Close when process ends.
        }

        public void started(final String className, final Process process) {
            textArea.setText(className + ": started...");
            new Thread() {
                @Override
                public void run() {
                    try {
                        process.waitFor();
                    } catch (final InterruptedException ex) {
                    }
                    // setVisible(false);
                    // dispose();
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

    class ErasableTextField extends JPanel {

        private static final long serialVersionUID = 1L;
        private final JTextField textField;
        private final JButton btClear;
        private final Color defaultTextBackground;

        public ErasableTextField(final int len) {
            super(new BorderLayout());
            textField = new JTextField(len);
            defaultTextBackground = textField.getBackground();
            btClear = new JButton(new AbstractAction() {
                private static final long serialVersionUID = 1L;

                {
                    putValue(Action.SHORT_DESCRIPTION, "Clear search pattern");
                    putValue(Action.SMALL_ICON, new ImageIcon(ExampleRunner.class
                            .getResource("/com/ardor3d/example/media/icons/edit-clear-locationbar-rtl.png")));
                }

                public void actionPerformed(final ActionEvent e) {
                    textField.setText("");
                }
            });
            btClear.setPreferredSize(new Dimension(20, 20));
            btClear.setFocusable(false);
            btClear.setBorder(null);
            add(textField);
            add(btClear, BorderLayout.EAST);
        }

        public void setWarning(final boolean warn) {
            textField.setBackground(warn ? Color.yellow : defaultTextBackground);
        }

        public Document getDocument() {
            return textField.getDocument();
        }

        public String getText() {
            return textField.getText();
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
        app.setIconImage(new ImageIcon(ExampleRunner.class
                .getResource("/com/ardor3d/example/media/icons/ardor3d_white_24.png")).getImage());
        app.setSize(800, 400);
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setLocationRelativeTo(null);
        app.setVisible(true);
    }
}
