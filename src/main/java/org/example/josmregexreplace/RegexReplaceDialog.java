package org.example.josmregexreplace;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.search.SearchParseError;

public final class RegexReplaceDialog extends JDialog {

    private final DataSet dataSet;
    private final String activeLayerName;
    private final JComboBox<String> keyField;
    private final JComboBox<String> recentField;
    private final JComboBox<String> presetField;
    private final JComboBox<RegexReplacementMode> modeField;
    private final JComboBox<RegexReplacementKeyOperation> keyOperationField;
    private final JTextField findField;
    private final JLabel findLabel;
    private final JTextField replaceField;
    private final JCheckBox selectionOnly;
    private final JCheckBox ignoreCase;
    private final JCheckBox multiline;
    private final JCheckBox dotAll;
    private final JCheckBox unicode;
    private final JCheckBox nodes;
    private final JCheckBox ways;
    private final JCheckBox relations;
    private final JCheckBox keepEmptyValues;
    private final JCheckBox allTags;
    private final JCheckBox createMissing;
    private final JCheckBox trimWhitespace;
    private final JCheckBox normalizeUnicode;
    private final JCheckBox preventEmptyResult;
    private final JTextField searchField;
    private final JTextField destinationKeyField;
    private final JComboBox<String> conditionKeyField;
    private final JTextField conditionPatternField;
    private final JTextField minimumMatchesField;
    private final JTextField maximumMatchesField;
    private final JLabel statusLabel;
    private final JButton applyButton;
    private final JButton resetButton;
    private final JButton helpButton;
    private final JButton exportButton;
    private final javax.swing.JProgressBar progressBar;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> tableSorter;
    private final JTextField tableFilter;
    private final Timer previewTimer;
    private final JPanel advancedPanel;
    private final JCheckBox advancedToggle;
    private final List<RegexReplacePreferences.State> recentStates;
    private SwingWorker<RegexReplacementPreview, Void> previewWorker;
    private long previewGeneration;
    private List<RegexReplacementPreviewRow> previewRows = List.of();
    private Result dialogResult;

    public RegexReplaceDialog(Window owner, DataSet dataSet, Set<String> tagKeys, String activeLayerName) {
        super(owner, "Regex Search/Replace", ModalityType.APPLICATION_MODAL);
        this.dataSet = dataSet;
        this.activeLayerName = activeLayerName;
        RegexReplacePreferences.State preferences = RegexReplacePreferences.load();
        recentStates = RegexReplacePreferences.loadRecent();

        keyField = new JComboBox<>(tagKeys.toArray(new String[0]));
        keyField.setEditable(true);
        keyField.setSelectedItem(preferences.key());
        recentField = new JComboBox<>(recentStates.stream()
            .map(RegexReplaceDialog::historyLabel)
            .toArray(String[]::new));
        presetField = new JComboBox<>(RegexReplacePreferences.loadPresetNames().toArray(new String[0]));
        modeField = new JComboBox<>(RegexReplacementMode.values());
        modeField.setSelectedItem(preferences.mode());
        keyOperationField = new JComboBox<>(RegexReplacementKeyOperation.values());
        keyOperationField.setSelectedItem(preferences.keyOperation());
        findField = new JTextField(preferences.findRegex(), 30);
        findLabel = new JLabel("Find (regex):");
        updateFindLabelText(findLabel, preferences.mode());
        replaceField = new JTextField(preferences.replacement(), 30);
        selectionOnly = new JCheckBox("Selected objects only", preferences.selectionOnly());
        ignoreCase = new JCheckBox("Ignore case", preferences.ignoreCase());
        multiline = new JCheckBox("Multiline", preferences.multiline());
        dotAll = new JCheckBox("Dot matches line breaks", preferences.dotAll());
        unicode = new JCheckBox("Unicode character classes", preferences.unicode());
        nodes = new JCheckBox("Nodes", preferences.nodes());
        ways = new JCheckBox("Ways", preferences.ways());
        relations = new JCheckBox("Relations", preferences.relations());
        keepEmptyValues = new JCheckBox("Keep empty tag values", preferences.keepEmptyValues());
        allTags = new JCheckBox("All existing tags", preferences.allTags());
        createMissing = new JCheckBox("Create missing source tag", preferences.createMissing());
        trimWhitespace = new JCheckBox("Trim whitespace", preferences.trimWhitespace());
        normalizeUnicode = new JCheckBox("Normalize Unicode", preferences.normalizeUnicode());
        preventEmptyResult = new JCheckBox("Prevent empty result", preferences.preventEmptyResult());
        searchField = new JTextField(preferences.searchExpression(), 30);
        destinationKeyField = new JTextField(preferences.destinationKey(), 20);
        conditionKeyField = new JComboBox<>(tagKeys.toArray(new String[0]));
        conditionKeyField.setEditable(true);
        conditionKeyField.setSelectedItem(preferences.conditionKey());
        conditionPatternField = new JTextField(preferences.conditionPattern(), 20);
        minimumMatchesField = new JTextField(Integer.toString(preferences.minimumMatches()), 5);
        maximumMatchesField = new JTextField(
            preferences.maximumMatches() == Integer.MAX_VALUE
                ? "" : Integer.toString(preferences.maximumMatches()), 5);
        // Pin small inputs to their natural width: rows use BoxLayout, which
        // would otherwise stretch them across the whole row.
        minimumMatchesField.setMaximumSize(minimumMatchesField.getPreferredSize());
        maximumMatchesField.setMaximumSize(maximumMatchesField.getPreferredSize());
        conditionKeyField.setMaximumSize(conditionKeyField.getPreferredSize());
        tableFilter = new JTextField(18);
        previewTimer = new Timer(250, event -> refreshPreview());
        previewTimer.setRepeats(false);
        statusLabel = new JLabel(" ");
        applyButton = new JButton("Apply");
        resetButton = new JButton("Reset");
        helpButton = new JButton("Help");
        JButton cancelButton = new JButton("Cancel");
        exportButton = new JButton("Export CSV");
        progressBar = new javax.swing.JProgressBar();
        progressBar.setIndeterminate(false);
        // Hidden unless calculating: the empty track looks like a permanent scrollbar groove.
        progressBar.setVisible(false);
        JButton selectAllButton = new JButton("Select all");
        JButton selectNoneButton = new JButton("Select none");
        JButton savePresetButton = new JButton("Save preset");
        JButton deletePresetButton = new JButton("Delete preset");
        JButton clearRecentButton = new JButton("Clear");

        tableModel = new DefaultTableModel(
            new Object[]{"Object", "Tag key", "Old value", "New value", "Matches", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(tableSorter);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getColumnModel().getColumn(2).setCellRenderer(new RegexHighlightRenderer(true));
        table.getColumnModel().getColumn(3).setCellRenderer(new RegexHighlightRenderer(false));

        // Basic section: always visible, covers the 90% workflow.
        // NB: GridBagLayout with fill=HORIZONTAL (never VERTICAL) keeps text
        // fields at their natural height. The previous GridLayout(0,1) forced
        // every row to the height of the tallest row, and labeled()'s
        // BorderLayout.CENTER stretched each field vertically.
        JPanel basicInputs = new JPanel();
        basicInputs.setLayout(new javax.swing.BoxLayout(basicInputs, javax.swing.BoxLayout.Y_AXIS));
        basicInputs.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JPanel historySection = new JPanel(new GridBagLayout());
        historySection.setBorder(BorderFactory.createTitledBorder("History & presets"));
        int row = 0;
        JPanel recentRow = new JPanel(new BorderLayout(8, 0));
        recentRow.add(recentField, BorderLayout.CENTER);
        recentRow.add(clearRecentButton, BorderLayout.EAST);
        clearRecentButton.setEnabled(!recentStates.isEmpty());
        clearRecentButton.addActionListener(event -> clearRecentOperations(clearRecentButton));
        row = addFormRow(historySection, row, "Recent:", recentRow);
        JPanel presets = flowRow(new JLabel("Preset:"), presetField, savePresetButton, deletePresetButton);
        row = addFullRow(historySection, row, presets);

        JPanel findSection = new JPanel(new GridBagLayout());
        findSection.setBorder(BorderFactory.createTitledBorder("Find and replace"));
        destinationKeyField.setToolTipText("Only used by Rename (moves the tag) and Copy (duplicates it). Ignored by Replace.");
        row = 0;
        JPanel tagKeyRow = new JPanel(new BorderLayout(8, 0));
        tagKeyRow.add(keyField, BorderLayout.CENTER);
        tagKeyRow.add(allTags, BorderLayout.EAST);
        row = addFormRow(findSection, row, "Tag key:", tagKeyRow);
        row = addFormRow(findSection, row, "Mode:", modeField);
        row = addFormRow(findSection, row, "Key operation:", keyOperationField);
        row = addFormRow(findSection, row, findLabel, findField);
        row = addFormRow(findSection, row, "Replace with:", replaceField);
        row = addFormRow(findSection, row, "Destination key:", destinationKeyField);

        JPanel scopeSection = new JPanel(new GridBagLayout());
        scopeSection.setBorder(BorderFactory.createTitledBorder("Scope"));
        row = 0;
        row = addFullRow(scopeSection, row,
            flowRow(selectionOnly, new JLabel("Types:"), nodes, ways, relations));

        basicInputs.add(historySection);
        basicInputs.add(findSection);
        basicInputs.add(scopeSection);

        // Advanced section: hidden by default, toggled on demand.
        advancedToggle = new JCheckBox("Show advanced options");
        advancedToggle.setSelected(false);
        advancedPanel = new JPanel();
        advancedPanel.setLayout(new javax.swing.BoxLayout(advancedPanel, javax.swing.BoxLayout.Y_AXIS));
        advancedPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JPanel filterSection = new JPanel(new GridBagLayout());
        filterSection.setBorder(BorderFactory.createTitledBorder("Filters"));
        row = 0;
        row = addFormRow(filterSection, row, "JOSM search (optional):", searchField);
        conditionKeyField.setToolTipText("Condition tag key");
        conditionPatternField.setToolTipText("Condition regex");
        row = addFormRow(filterSection, row, "Condition (optional):",
            flowRow(new JLabel("Tag:"), conditionKeyField,
                new JLabel("Pattern:"), conditionPatternField));
        JPanel thresholds = flowRow(new JLabel("Matches per value:"),
            new JLabel("Min:"), minimumMatchesField,
            new JLabel("Max:"), maximumMatchesField);
        row = addFullRow(filterSection, row, thresholds);

        JPanel matchingSection = new JPanel(new GridBagLayout());
        matchingSection.setBorder(BorderFactory.createTitledBorder("Matching"));
        row = 0;
        JPanel regexFlags = flowRow(ignoreCase, multiline, dotAll, unicode);
        row = addFullRow(matchingSection, row, regexFlags);
        row = addFullRow(matchingSection, row, createMissing);

        JPanel valuesSection = new JPanel(new GridBagLayout());
        valuesSection.setBorder(BorderFactory.createTitledBorder("Values"));
        row = 0;
        JPanel safetyOptions = flowRow(trimWhitespace, normalizeUnicode, preventEmptyResult);
        row = addFullRow(valuesSection, row, safetyOptions);
        row = addFullRow(valuesSection, row, keepEmptyValues);

        advancedPanel.add(filterSection);
        advancedPanel.add(matchingSection);
        advancedPanel.add(valuesSection);
        advancedPanel.setVisible(false);
        advancedToggle.addActionListener(event -> {
            advancedPanel.setVisible(advancedToggle.isSelected());
            pack();
        });

        JPanel togglePanel = flowRow(advancedToggle);
        togglePanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
        togglePanel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel inputs = new JPanel();
        inputs.setLayout(new javax.swing.BoxLayout(inputs, javax.swing.BoxLayout.Y_AXIS));
        basicInputs.setAlignmentX(LEFT_ALIGNMENT);
        advancedPanel.setAlignmentX(LEFT_ALIGNMENT);
        inputs.add(basicInputs);
        inputs.add(togglePanel);
        inputs.add(advancedPanel);

        // Filter row on top of the table (FlowLayout wraps on narrow windows);
        // summary text below the table, right-aligned.
        JPanel selectionControls = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        selectionControls.add(new JLabel("Filter:"));
        selectionControls.add(tableFilter);
        selectionControls.add(selectAllButton);
        selectionControls.add(selectNoneButton);
        selectionControls.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));

        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(exportButton, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 8));
        buttons.add(helpButton);
        buttons.add(resetButton);
        buttons.add(cancelButton);
        buttons.add(applyButton);

        JScrollPane preview = new JScrollPane(table);
        preview.setPreferredSize(new Dimension(850, 320));
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.add(selectionControls, BorderLayout.NORTH);
        previewPanel.add(preview, BorderLayout.CENTER);
        previewPanel.add(statusBar, BorderLayout.SOUTH);

        // Progress lives at the very bottom, below the button row.
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttons, BorderLayout.CENTER);
        southPanel.add(progressBar, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(inputs, BorderLayout.NORTH);
        add(previewPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.PAGE_END);

        applyButton.setEnabled(false);
        exportButton.setEnabled(false);
        getRootPane().setDefaultButton(applyButton);
        cancelButton.addActionListener(event -> dispose());
        applyButton.addActionListener(event -> applySelection());
        resetButton.addActionListener(event -> resetFields());
        helpButton.addActionListener(event -> showRegexHelp());
        exportButton.addActionListener(event -> exportCsv());
        selectAllButton.addActionListener(event -> selectAllRows());
        selectNoneButton.addActionListener(event -> table.clearSelection());
        keyField.addActionListener(event -> schedulePreviewRefresh());
        modeField.addActionListener(event -> {
            updateFindLabel();
            schedulePreviewRefresh();
        });
        keyOperationField.addActionListener(event -> {
            updateDestinationEnabled();
            schedulePreviewRefresh();
        });
        allTags.addActionListener(event -> {
            updateTagKeyEnabled();
            schedulePreviewRefresh();
        });
        createMissing.addActionListener(event -> schedulePreviewRefresh());
        selectionOnly.addActionListener(event -> schedulePreviewRefresh());
        ignoreCase.addActionListener(event -> schedulePreviewRefresh());
        multiline.addActionListener(event -> schedulePreviewRefresh());
        dotAll.addActionListener(event -> schedulePreviewRefresh());
        unicode.addActionListener(event -> schedulePreviewRefresh());
        nodes.addActionListener(event -> schedulePreviewRefresh());
        ways.addActionListener(event -> schedulePreviewRefresh());
        relations.addActionListener(event -> schedulePreviewRefresh());
        trimWhitespace.addActionListener(event -> schedulePreviewRefresh());
        normalizeUnicode.addActionListener(event -> schedulePreviewRefresh());
        preventEmptyResult.addActionListener(event -> schedulePreviewRefresh());
        recentField.addActionListener(event -> loadRecentOperation());
        presetField.addActionListener(event -> loadPreset());
        savePresetButton.addActionListener(event -> savePreset());
        deletePresetButton.addActionListener(event -> deletePreset());

        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                schedulePreviewRefresh();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                schedulePreviewRefresh();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                schedulePreviewRefresh();
            }
        };
        findField.getDocument().addDocumentListener(listener);
        replaceField.getDocument().addDocumentListener(listener);
        destinationKeyField.getDocument().addDocumentListener(listener);
        searchField.getDocument().addDocumentListener(listener);
        conditionKeyField.addActionListener(event -> schedulePreviewRefresh());
        java.awt.Component conditionEditor = conditionKeyField.getEditor().getEditorComponent();
        if (conditionEditor instanceof javax.swing.text.JTextComponent conditionText) {
            conditionText.getDocument().addDocumentListener(listener);
        }
        conditionPatternField.getDocument().addDocumentListener(listener);
        minimumMatchesField.getDocument().addDocumentListener(listener);
        maximumMatchesField.getDocument().addDocumentListener(listener);
        tableFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateTableFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateTableFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateTableFilter();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        updateDestinationEnabled();
        updateTagKeyEnabled();
        updateFindLabel();
        maybeRevealAdvanced(preferences);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent event) {
                keyField.requestFocusInWindow();
            }
        });
        pack();
        setLocationRelativeTo(owner);
        refreshPreview();
    }

    private void updateDestinationEnabled() {
        boolean needsDestination =
            keyOperationField.getSelectedItem() != RegexReplacementKeyOperation.REPLACE;
        destinationKeyField.setEnabled(needsDestination);
    }

    private void updateTagKeyEnabled() {
        keyField.setEnabled(!allTags.isSelected());
    }

    private void maybeRevealAdvanced(RegexReplacePreferences.State state) {
        if (hasAdvancedValues(state) && !advancedToggle.isSelected()) {
            advancedToggle.setSelected(true);
            advancedPanel.setVisible(true);
        }
    }

    private static boolean hasAdvancedValues(RegexReplacePreferences.State state) {
        return !state.searchExpression().isBlank()
            || !state.conditionKey().isBlank()
            || !state.conditionPattern().isBlank()
            || state.minimumMatches() != 0
            || state.maximumMatches() != Integer.MAX_VALUE
            || state.ignoreCase()
            || state.multiline()
            || state.dotAll()
            || state.unicode()
            || state.allTags()
            || state.createMissing()
            || state.trimWhitespace()
            || state.normalizeUnicode()
            || state.preventEmptyResult()
            || state.keepEmptyValues();
    }

    private void revealAdvanced() {
        if (!advancedToggle.isSelected()) {
            advancedToggle.setSelected(true);
            advancedPanel.setVisible(true);
            pack();
        }
    }

    public Result showDialog() {
        setVisible(true);
        return dialogResult;
    }

    @Override
    public void dispose() {
        previewGeneration++;
        if (previewWorker != null) {
            previewWorker.cancel(true);
        }
        super.dispose();
    }

    private void schedulePreviewRefresh() {
        previewTimer.restart();
    }

    private void refreshPreview() {
        tableModel.setRowCount(0);
        previewRows = List.of();

        String key = currentKey();
        String findRegex = findField.getText();
        if ((!allTags.isSelected() && key.isEmpty()) || findRegex.isEmpty()) {
            cancelPreview();
            setStatus("Tag key and search pattern are required.", false);
            return;
        }
        if (keyOperationField.getSelectedItem() != RegexReplacementKeyOperation.REPLACE
            && destinationKeyField.getText().trim().isEmpty()) {
            cancelPreview();
            setStatus("A destination key is required for rename and copy.", false);
            return;
        }

        RegexReplacementRequest request = new RegexReplacementRequest(
            findRegex,
            replaceField.getText(),
            patternFlags(),
            (RegexReplacementMode) modeField.getSelectedItem(),
            trimWhitespace.isSelected(),
            normalizeUnicode.isSelected(),
            preventEmptyResult.isSelected()
        );
        try {
            RegexReplacementService.validate(request);
            int minimumMatches = parseThreshold(minimumMatchesField.getText(), 0);
            int maximumMatches = parseThreshold(maximumMatchesField.getText(), Integer.MAX_VALUE);
            if (minimumMatches < 0 || maximumMatches < minimumMatches) {
                setStatus("Invalid match thresholds.", false);
                return;
            }
            RegexReplacementCondition condition = buildCondition();
            RegexReplacementScope scope = selectionOnly.isSelected()
                ? RegexReplacementScope.SELECTED
                : RegexReplacementScope.ALL_OBJECTS;
            RegexReplacementFilter filter = new RegexReplacementFilter(selectedPrimitiveTypes());
            startPreview(
                request, currentKeys(), scope, filter, searchField.getText(), condition,
                minimumMatches, maximumMatches, (RegexReplacementKeyOperation) keyOperationField.getSelectedItem(),
                createMissing.isSelected());
        } catch (PatternSyntaxException ex) {
            cancelPreview();
            setStatus("Invalid regular expression: " + ex.getDescription(), false);
            return;
        } catch (NumberFormatException ex) {
            cancelPreview();
            setStatus("Invalid match threshold: " + ex.getMessage(), false);
            return;
        } catch (IllegalArgumentException ex) {
            cancelPreview();
            String prefix = ex.getMessage() != null && ex.getMessage().startsWith("Condition")
                ? "Invalid condition: " : "Invalid replacement: ";
            setStatus(prefix + ex.getMessage(), false);
            return;
        }
    }

    private void cancelPreview() {
        previewGeneration++;
        if (previewWorker != null) {
            previewWorker.cancel(true);
        }
        hideProgress();
    }

    private void showProgress() {
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
    }

    private void hideProgress() {
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
    }

    private void startPreview(
        RegexReplacementRequest request,
        List<String> keys,
        RegexReplacementScope scope,
        RegexReplacementFilter filter,
        String searchExpression,
        RegexReplacementCondition condition,
        int minimumMatches,
        int maximumMatches,
        RegexReplacementKeyOperation operation,
        boolean createMissingValue) {
        if (previewWorker != null) {
            previewWorker.cancel(true);
        }
        long generation = ++previewGeneration;
        setStatus("Calculating preview...", false);
        showProgress();
        previewWorker = new SwingWorker<>() {
            @Override
            protected RegexReplacementPreview doInBackground() throws Exception {
                List<RegexReplacementPreviewRow> rows = new ArrayList<>();
                int scanned = 0;
                int matched = 0;
                int changed = 0;
                int skipped = 0;
                boolean truncated = false;
                for (String key : keys) {
                    String targetKey = operation == RegexReplacementKeyOperation.REPLACE
                        ? key : destinationKeyField.getText().trim();
                    RegexReplacementPreview part = RegexReplacementPreviewService.preview(
                        dataSet, key, targetKey,
                        operation == RegexReplacementKeyOperation.RENAME,
                        createMissingValue,
                        request, scope, filter, searchExpression,
                        condition, minimumMatches, maximumMatches, 5_000);
                    int remainingRows = Math.max(0, 5_000 - rows.size());
                    if (part.rows().size() > remainingRows) {
                        rows.addAll(part.rows().subList(0, remainingRows));
                        truncated = true;
                    } else {
                        rows.addAll(part.rows());
                    }
                    scanned += part.scannedCount();
                    matched += part.matchedCount();
                    changed += part.changedCount();
                    skipped += part.skippedCount();
                    truncated |= part.truncated();
                }
                return new RegexReplacementPreview(rows, scanned, matched, changed, skipped, truncated);
            }

            @Override
            protected void done() {
                if (generation != previewGeneration || isCancelled()) {
                    return;
                }
                try {
                    renderPreview(get());
                } catch (CancellationException ex) {
                    return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    hideProgress();
                } catch (ExecutionException ex) {
                    hideProgress();
                    Throwable cause = ex.getCause();
                    if (cause instanceof SearchParseError searchError) {
                        setStatus("Invalid JOSM search expression: " + searchError.getMessage(), false);
                    } else {
                        setStatus("Preview failed: " + cause.getMessage(), false);
                    }
                }
            }
        };
        previewWorker.execute();
    }

    private void renderPreview(RegexReplacementPreview preview) {
        previewRows = preview.rows();
        hideProgress();
        ((RegexHighlightRenderer) table.getColumnModel().getColumn(2).getCellRenderer())
            .setRequest(request());
        ((RegexHighlightRenderer) table.getColumnModel().getColumn(3).getCellRenderer())
            .setRequest(request());
        updatePreviewStatus(preview);
        for (RegexReplacementPreviewRow row : previewRows) {
            OsmPrimitive primitive = row.primitive();
            tableModel.addRow(new Object[]{
                primitive.getClass().getSimpleName() + " " + primitive.getUniqueId(),
                row.key(),
                row.oldValue(),
                row.newValue(),
                row.matchCount(),
                row.newValue().isEmpty() && !keepEmptyValues.isSelected()
                    ? "Delete tag" : "Changed"
            });
        }

        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, table.getRowCount() - 1);
        }
    }

    private void updatePreviewStatus(RegexReplacementPreview preview) {
        if (preview.truncated()) {
            setStatus(
                preview.changedCount() + " change(s) found, but the preview is limited to "
                    + preview.rows().size() + " rows. Narrow the scope before applying.",
                false
            );
            return;
        }
        if (preview.changedCount() == 0) {
            String message = preview.matchedCount() == 0
                ? "No matching tag values found."
                : "Matches found, but no values would change.";
            setStatus(message, false);
            return;
        }
        setStatus(
            preview.changedCount() + " change(s) from " + preview.matchedCount()
                + " match(es); " + preview.scannedCount() + " scanned, "
                + preview.skippedCount() + " skipped.",
            true
        );
    }

    private void applySelection() {
        List<RegexReplacementPreviewRow> selectedRows = selectedRows();
        if (!selectedRows.isEmpty()) {
            if (selectedRows.size() >= 1_000) {
                int confirmation = JOptionPane.showConfirmDialog(
                    this,
                    selectedRows.size() + " objects will be changed. Apply this broad operation?",
                    "Confirm broad regex replacement",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (confirmation != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            RegexReplacePreferences.State state = currentPreferences();
            RegexReplacePreferences.save(state);
            RegexReplacePreferences.saveRecent(state);
            dialogResult = new Result(
                currentKey(), request(), selectedRows,
                keepEmptyValues.isSelected()
                    ? RegexReplacementEmptyValuePolicy.KEEP_EMPTY
                    : RegexReplacementEmptyValuePolicy.DELETE_TAG);
            dispose();
        }
    }

    private List<RegexReplacementPreviewRow> selectedRows() {
        List<RegexReplacementPreviewRow> selectedRows = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            selectedRows.add(previewRows.get(table.convertRowIndexToModel(viewRow)));
        }
        return selectedRows;
    }

    private void exportCsv() {
        if (previewRows.isEmpty()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("regex-replace-preview.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("Object,Tag key,Old value,New value,Matches,Status");
            writer.newLine();
            for (RegexReplacementPreviewRow row : previewRows) {
                OsmPrimitive primitive = row.primitive();
                writeCsvRow(writer, List.of(
                    primitive.getClass().getSimpleName() + " " + primitive.getUniqueId(),
                    row.key(), row.oldValue(), row.newValue(),
                    Integer.toString(row.matchCount()), "Changed"));
            }
            setStatus("Exported " + previewRows.size() + " preview row(s).", true);
        } catch (IOException ex) {
            setStatus("CSV export failed: " + ex.getMessage(), false);
        }
    }

    private static void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write('"');
            writer.write(values.get(i).replace("\"", "\"\""));
            writer.write('"');
        }
        writer.newLine();
    }

    private void selectAllRows() {
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, table.getRowCount() - 1);
        }
    }

    private void clearRecentOperations(JButton clearRecentButton) {
        if (recentStates.isEmpty()) {
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(
            this,
            "Clear all " + recentStates.size() + " recent operation(s)?",
            "Clear recent operations",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirmation != JOptionPane.OK_OPTION) {
            return;
        }
        RegexReplacePreferences.clearRecent();
        recentStates.clear();
        recentField.removeAllItems();
        clearRecentButton.setEnabled(false);
    }

    private void loadRecentOperation() {
        int index = recentField.getSelectedIndex();
        if (index < 0 || index >= recentStates.size()) {
            return;
        }
        RegexReplacePreferences.State state = recentStates.get(index);
        keyField.setSelectedItem(state.key());
        modeField.setSelectedItem(state.mode());
        keyOperationField.setSelectedItem(state.keyOperation());
        findField.setText(state.findRegex());
        replaceField.setText(state.replacement());
        destinationKeyField.setText(state.destinationKey());
        selectionOnly.setSelected(state.selectionOnly());
        ignoreCase.setSelected(state.ignoreCase());
        multiline.setSelected(state.multiline());
        dotAll.setSelected(state.dotAll());
        unicode.setSelected(state.unicode());
        nodes.setSelected(state.nodes());
        ways.setSelected(state.ways());
        relations.setSelected(state.relations());
        keepEmptyValues.setSelected(state.keepEmptyValues());
        allTags.setSelected(state.allTags());
        createMissing.setSelected(state.createMissing());
        trimWhitespace.setSelected(state.trimWhitespace());
        normalizeUnicode.setSelected(state.normalizeUnicode());
        preventEmptyResult.setSelected(state.preventEmptyResult());
        searchField.setText(state.searchExpression());
        conditionKeyField.setSelectedItem(state.conditionKey());
        conditionPatternField.setText(state.conditionPattern());
        minimumMatchesField.setText(Integer.toString(state.minimumMatches()));
        maximumMatchesField.setText(
            state.maximumMatches() == Integer.MAX_VALUE ? "" : Integer.toString(state.maximumMatches()));
        updateDestinationEnabled();
        updateTagKeyEnabled();
        maybeRevealAdvanced(state);
    }

    private void loadPreset() {
        Object selected = presetField.getSelectedItem();
        if (selected == null) {
            return;
        }
        applyPreferences(RegexReplacePreferences.loadPreset(selected.toString()));
    }

    private void savePreset() {
        String name = JOptionPane.showInputDialog(this, "Preset name:", "Save preset", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        RegexReplacePreferences.savePreset(name.trim(), currentPreferences());
        presetField.addItem(name.trim());
        presetField.setSelectedItem(name.trim());
    }

    private void deletePreset() {
        Object selected = presetField.getSelectedItem();
        if (selected == null) {
            return;
        }
        RegexReplacePreferences.deletePreset(selected.toString());
        presetField.removeItem(selected);
    }

    private void applyPreferences(RegexReplacePreferences.State state) {
        if (state == null) {
            return;
        }
        keyField.setSelectedItem(state.key());
        modeField.setSelectedItem(state.mode());
        keyOperationField.setSelectedItem(state.keyOperation());
        findField.setText(state.findRegex());
        replaceField.setText(state.replacement());
        destinationKeyField.setText(state.destinationKey());
        selectionOnly.setSelected(state.selectionOnly());
        ignoreCase.setSelected(state.ignoreCase());
        multiline.setSelected(state.multiline());
        dotAll.setSelected(state.dotAll());
        unicode.setSelected(state.unicode());
        nodes.setSelected(state.nodes());
        ways.setSelected(state.ways());
        relations.setSelected(state.relations());
        keepEmptyValues.setSelected(state.keepEmptyValues());
        allTags.setSelected(state.allTags());
        createMissing.setSelected(state.createMissing());
        trimWhitespace.setSelected(state.trimWhitespace());
        normalizeUnicode.setSelected(state.normalizeUnicode());
        preventEmptyResult.setSelected(state.preventEmptyResult());
        searchField.setText(state.searchExpression());
        conditionKeyField.setSelectedItem(state.conditionKey());
        conditionPatternField.setText(state.conditionPattern());
        minimumMatchesField.setText(Integer.toString(state.minimumMatches()));
        maximumMatchesField.setText(
            state.maximumMatches() == Integer.MAX_VALUE ? "" : Integer.toString(state.maximumMatches()));
        updateDestinationEnabled();
        updateTagKeyEnabled();
        maybeRevealAdvanced(state);
    }

    private RegexReplacePreferences.State currentPreferences() {
        return new RegexReplacePreferences.State(
            currentKey(),
            findField.getText(),
            replaceField.getText(),
            (RegexReplacementMode) modeField.getSelectedItem(),
            selectionOnly.isSelected(),
            ignoreCase.isSelected(),
            multiline.isSelected(),
            dotAll.isSelected(),
            unicode.isSelected(),
            nodes.isSelected(),
            ways.isSelected(),
            relations.isSelected(),
            keepEmptyValues.isSelected(),
            searchField.getText(),
            (RegexReplacementKeyOperation) keyOperationField.getSelectedItem(),
            allTags.isSelected(),
            createMissing.isSelected(),
            destinationKeyField.getText(),
            currentConditionKey(),
            conditionPatternField.getText(),
            parseThreshold(minimumMatchesField.getText(), 0),
            parseThreshold(maximumMatchesField.getText(), Integer.MAX_VALUE),
            trimWhitespace.isSelected(),
            normalizeUnicode.isSelected(),
            preventEmptyResult.isSelected()
        );
    }

    private static String historyLabel(RegexReplacePreferences.State state) {
        return state.key() + " | " + state.findRegex();
    }

    private void updateTableFilter() {
        String filterText = tableFilter.getText().trim();
        tableSorter.setRowFilter(filterText.isEmpty()
            ? null
            : RowFilter.regexFilter("(?i)" + Pattern.quote(filterText)));
    }

    private RegexReplacementRequest request() {
        return new RegexReplacementRequest(
            findField.getText(),
            replaceField.getText(),
            patternFlags(),
            (RegexReplacementMode) modeField.getSelectedItem(),
            trimWhitespace.isSelected(),
            normalizeUnicode.isSelected(),
            preventEmptyResult.isSelected()
        );
    }

    private Set<RegexReplacementPrimitiveType> selectedPrimitiveTypes() {
        Set<RegexReplacementPrimitiveType> types = EnumSet.noneOf(RegexReplacementPrimitiveType.class);
        if (nodes.isSelected()) {
            types.add(RegexReplacementPrimitiveType.NODE);
        }
        if (ways.isSelected()) {
            types.add(RegexReplacementPrimitiveType.WAY);
        }
        if (relations.isSelected()) {
            types.add(RegexReplacementPrimitiveType.RELATION);
        }
        return types;
    }

    private int patternFlags() {
        int flags = 0;
        if (ignoreCase.isSelected()) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        if (multiline.isSelected()) {
            flags |= Pattern.MULTILINE;
        }
        if (dotAll.isSelected()) {
            flags |= Pattern.DOTALL;
        }
        if (unicode.isSelected()) {
            flags |= Pattern.UNICODE_CHARACTER_CLASS;
        }
        return flags;
    }

    private String currentKey() {
        Object item = keyField.getEditor().getItem();
        return item == null ? "" : item.toString().trim();
    }

    private String currentConditionKey() {
        Object item = conditionKeyField.getEditor().getItem();
        return item == null ? "" : item.toString().trim();
    }

    private void updateFindLabel() {
        updateFindLabelText(findLabel, (RegexReplacementMode) modeField.getSelectedItem());
    }

    private static void updateFindLabelText(JLabel label, RegexReplacementMode mode) {
        if (mode == RegexReplacementMode.LITERAL) {
            label.setText("Find (text):");
        } else if (mode == RegexReplacementMode.WHOLE_VALUE) {
            label.setText("Find (whole value):");
        } else {
            label.setText("Find (regex):");
        }
    }

    private List<String> currentKeys() {
        if (allTags.isSelected()) {
            Set<String> keys = new java.util.TreeSet<>();
            for (OsmPrimitive primitive : dataSet.allPrimitives()) {
                keys.addAll(primitive.keySet());
            }
            return new ArrayList<>(keys);
        }
        return List.of(currentKey().split(",")).stream()
            .map(String::trim)
            .filter(key -> !key.isEmpty())
            .distinct()
            .toList();
    }

    private RegexReplacementCondition buildCondition() {
        String conditionKey = currentConditionKey();
        String conditionPattern = conditionPatternField.getText();
        if (conditionKey.isEmpty() && conditionPattern.isEmpty()) {
            return null;
        }
        if (conditionKey.isEmpty() || conditionPattern.isEmpty()) {
            throw new IllegalArgumentException("Condition tag and regex must be provided together.");
        }
        return new RegexReplacementCondition(conditionKey, conditionPattern, patternFlags());
    }

    private static int parseThreshold(String value, int defaultValue) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : Integer.parseInt(trimmed);
    }

    private void setStatus(String message, boolean canApply) {
        statusLabel.setText(activeLayerName + " | selected: " + dataSet.getSelected().size() + " | " + message);
        applyButton.setEnabled(canApply);
        exportButton.setEnabled(canApply);
    }

    private void resetFields() {
        keyField.setSelectedItem("");
        keyOperationField.setSelectedItem(RegexReplacementKeyOperation.REPLACE);
        destinationKeyField.setText("");
        modeField.setSelectedItem(RegexReplacementMode.REGEX);
        findField.setText("");
        replaceField.setText("");
        searchField.setText("");
        conditionKeyField.setSelectedItem("");
        conditionPatternField.setText("");
        minimumMatchesField.setText("");
        maximumMatchesField.setText("");
        selectionOnly.setSelected(true);
        ignoreCase.setSelected(false);
        multiline.setSelected(false);
        dotAll.setSelected(false);
        unicode.setSelected(false);
        nodes.setSelected(true);
        ways.setSelected(true);
        relations.setSelected(true);
        keepEmptyValues.setSelected(false);
        allTags.setSelected(false);
        createMissing.setSelected(false);
        trimWhitespace.setSelected(false);
        normalizeUnicode.setSelected(false);
        preventEmptyResult.setSelected(false);
        tableFilter.setText("");
        updateDestinationEnabled();
        updateTagKeyEnabled();
        schedulePreviewRefresh();
    }

    private void showRegexHelp() {
        // Native components instead of JEditorPane HTML: the HTML engine's
        // CSS is unreliable across look-and-feels (code tokens shrank,
        // headings blended into the body). Real labels follow the active
        // LAF's fonts and colors exactly.
        // Scrollable + tracking the viewport width: otherwise the content keeps
        // the width it was first laid out at — widening stretches it but
        // narrowing only clips it instead of reflowing the text and columns.
        ScrollablePanel content = new ScrollablePanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));

        content.add(helpHeader("How it works"));
        content.add(helpParagraph("Find uses Java regular expressions with partial matching "
            + "(Matcher.find / replaceAll): the pattern only needs to match part of the value. "
            + "Unlike JOSM's name~\"...\" search, no trailing .* is needed. "
            + "Errors (bad pattern, unknown $ group) are reported in the status line below the table."));
        content.add(helpHeader("Modes"));
        content.add(helpGrid(new String[][]{
            {"REGEX", "pattern used as typed."},
            {"LITERAL", "find text matched literally (Pattern.quote)."},
            {"WHOLE_VALUE", "pattern must match the entire value."}}));
        content.add(helpHeader("Replace with"));
        content.add(helpGrid(new String[][]{
            {"$0  $1  $2  ...", "whole match, capture groups."},
            {"\\$  \\\\", "literal dollar sign, literal backslash."}}));
        content.add(helpHeader("Quick reference"));
        // GridBag (not GridLayout): every GridLayout column is forced to the
        // width of the widest cell, which inflates the minimum width and
        // blocks shrinking. Here each column only takes what it needs and the
        // meaning columns absorb/shrink with the available space.
        JPanel ref = new JPanel(new GridBagLayout());
        ref.setAlignmentX(LEFT_ALIGNMENT);
        String[][] pairs = {
            {".", "any character", ".*", "any run of characters"},
            {".+", "one or more", "\\d", "digit (\\D is non-digit)"},
            {"\\w", "word character", "\\s", "whitespace"},
            {"[abc]", "one of a, b, c", "[^abc]", "none of a, b, c"},
            {"^", "start of value", "$", "end of value"},
            {"(...)", "capture group", "(a|b)", "a or b"},
            {"?", "optional", "{2,4}", "repeat 2 to 4 times"},
            {"\\t", "tab", "\\Q...\\E", "literal span"}};
        for (int i = 0; i < pairs.length; i++) {
            ref.add(helpCode(pairs[i][0]), helpCellConstraints(0, i, 0, 0));
            ref.add(new JLabel(pairs[i][1]), helpCellConstraints(1, i, 1, 12));
            ref.add(helpCode(pairs[i][2]), helpCellConstraints(2, i, 0, 0));
            ref.add(new JLabel(pairs[i][3]), helpCellConstraints(3, i, 1, 0));
        }
        content.add(ref);
        content.add(helpHeader("Flags (checkboxes)"));
        content.add(helpParagraph("Ignore case, Multiline (^/$ per line), "
            + "Dot matches line breaks, Unicode character classes."));
        content.add(helpHeader("Examples"));
        JTextArea examples = new JTextArea(
            "Find: ^District d(e |')   Replace: (empty)\n"
            + "  \"District de Paris\" -> \"Paris\".\n\n"
            + "Find: (\\d+)\\s*(km|m)   Replace: $1 $2\n"
            + "  normalises spacing before units.");
        examples.setEditable(false);
        examples.setFocusable(false);
        examples.setFont(new java.awt.Font(
            java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, examples.getFont().getSize()));
        examples.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        examples.setAlignmentX(LEFT_ALIGNMENT);
        content.add(examples);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setPreferredSize(new Dimension(620, 480));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JDialog helpDialog = new JDialog(this, "Regex replacement help", true);
        helpDialog.setResizable(true);
        helpDialog.setLayout(new BorderLayout());
        helpDialog.add(scroll, BorderLayout.CENTER);
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event -> helpDialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.add(closeButton);
        helpDialog.add(buttonPanel, BorderLayout.SOUTH);
        helpDialog.pack();
        helpDialog.setLocationRelativeTo(this);
        helpDialog.setVisible(true);
    }

    private static JLabel helpHeader(String text) {
        JLabel header = new JLabel(text);
        java.awt.Font base = header.getFont();
        header.setFont(base.deriveFont(java.awt.Font.BOLD, base.getSize() + 2f));
        header.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));
        header.setAlignmentX(LEFT_ALIGNMENT);
        return header;
    }

    private static JTextArea helpParagraph(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setColumns(60);
        // Small minimum: the tracking viewport shrinks the content down to
        // this width and the text rewraps. Without it, the area's minimum
        // stays at the 60-column preferred width and narrowing only clips.
        area.setMinimumSize(new Dimension(48, 24));
        area.setAlignmentX(LEFT_ALIGNMENT);
        return area;
    }

    private static JLabel helpCode(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new java.awt.Font(
            java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, label.getFont().getSize()));
        return label;
    }

    private static JPanel helpGrid(String[][] rows) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < rows.length; i++) {
            grid.add(helpCode(rows[i][0]), helpCellConstraints(0, i, 0, 12));
            grid.add(new JLabel(rows[i][1]), helpCellConstraints(1, i, 1, 0));
        }
        return grid;
    }

    /** Token columns take what they need; description columns flex. */
    private static GridBagConstraints helpCellConstraints(int x, int y, double weightx, int rightInset) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = weightx;
        constraints.weighty = 0;
        constraints.insets = new Insets(1, 0, 1, rightInset);
        return constraints;
    }

    /**
     * Panel that follows the scroll-pane viewport width (down to its minimum),
     * so wrapped text and columns reflow when the dialog is resized instead of
     * freezing at the width of the first layout.
     */
    @SuppressWarnings("serial")
    private static final class ScrollablePanel extends JPanel implements javax.swing.Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            java.awt.Container parent = getParent();
            if (parent instanceof javax.swing.JViewport) {
                return parent.getWidth() >= getMinimumSize().width;
            }
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Adds a label + field row; field fills horizontally but keeps natural height. */
    private static int addFormRow(JPanel panel, int row, String label, javax.swing.JComponent field) {
        return addFormRow(panel, row, new JLabel(label), field);
    }

    private static int addFormRow(JPanel panel, int row, javax.swing.JComponent label, javax.swing.JComponent field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_START;
        labelConstraints.fill = GridBagConstraints.NONE;
        labelConstraints.weightx = 0;
        labelConstraints.weighty = 0;
        labelConstraints.insets = new Insets(2, 4, 2, 8);
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.anchor = GridBagConstraints.LINE_START;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.weighty = 0;
        fieldConstraints.insets = new Insets(2, 0, 2, 4);
        panel.add(field, fieldConstraints);
        return row + 1;
    }

    /** Adds a full-width row (checkbox groups, option rows); natural height preserved. */
    private static int addFullRow(JPanel panel, int row, javax.swing.JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.weighty = 0;
        constraints.insets = new Insets(2, 4, 2, 4);
        panel.add(component, constraints);
        return row + 1;
    }

    /**
     * Left-aligned option row with no leading gap, so its first component lines up
     * with the form labels above (a FlowLayout hgap would indent the whole row).
     */
    private static JPanel flowRow(java.awt.Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.X_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                panel.add(javax.swing.Box.createHorizontalStrut(8));
            }
            panel.add(components[i]);
        }
        return panel;
    }

    public record Result(
        String key,
        RegexReplacementRequest request,
        List<RegexReplacementPreviewRow> rows,
        RegexReplacementEmptyValuePolicy emptyValuePolicy
    ) {
    }
}