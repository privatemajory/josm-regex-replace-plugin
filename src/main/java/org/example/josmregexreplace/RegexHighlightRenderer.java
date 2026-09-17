package org.example.josmregexreplace;

import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public final class RegexHighlightRenderer extends DefaultTableCellRenderer {

    private RegexReplacementRequest request;
    private final boolean oldValue;

    public RegexHighlightRenderer(boolean oldValue) {
        this.oldValue = oldValue;
    }

    public void setRequest(RegexReplacementRequest request) {
        this.request = request;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String text = value == null ? "" : value.toString();
        if (request == null || text.isEmpty()) {
            setText(text);
            return this;
        }
        if (!oldValue) {
            setText("<html><span bgcolor='#d8f3dc'>" + escape(text) + "</span></html>");
            return this;
        }
        try {
            String expression = switch (request.mode()) {
                case REGEX -> request.findRegex();
                case LITERAL -> Pattern.quote(request.findRegex());
                case WHOLE_VALUE -> "^(?:" + request.findRegex() + ")$";
            };
            Matcher matcher = Pattern.compile(expression, request.patternFlags()).matcher(text);
            StringBuilder html = new StringBuilder("<html>");
            int end = 0;
            while (matcher.find()) {
                html.append(escape(text.substring(end, matcher.start())));
                html.append("<span bgcolor='#ffe08a'>")
                    .append(escape(text.substring(matcher.start(), matcher.end())))
                    .append("</span>");
                end = matcher.end();
            }
            html.append(escape(text.substring(end))).append("</html>");
            setText(html.toString());
        } catch (RuntimeException ex) {
            setText(text);
        }
        return this;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
