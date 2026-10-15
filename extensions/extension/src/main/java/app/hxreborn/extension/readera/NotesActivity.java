/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.readera;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class NotesActivity extends Activity {

    private static final String READ_ACTIVITY = "org.readera.read.ReadActivity";

    private static final String READ_ACTION = "readera_intent_action_read_doc";

    private static final String DOCS_URI = "content://org.readera.provider/docs/";

    private static final String STARTUP_MODE_EXTRA = "READERA_STARTUP_MODE";

    private static final int TYPE_REVIEW = 1;

    private static final int TYPE_BOOKMARK = 2;

    private static final int TYPE_QUOTE = 3;

    private static final int[] PALETTE = { 0xff808080, 0xffff4444, 0xffffbb33, 0xff99cc00, 0xff33b5e5 };

    private static final String[] COLOR_NAMES = { "Gray", "Red", "Orange", "Green", "Blue" };

    private static final String BOOK_TITLE = "coalesce(nullif(d.user_title, ''), nullif(d.doc_title, ''), "
            + "d.doc_file_name_title)";

    private static final int BOOKS_GROUPING = 1;

    private static final Grouping[] GROUPINGS = {
            new Grouping("hx_notes_all", "hx_notes_empty_all", null, null, "", "", null),
            new Grouping("hx_notes_in_books", "hx_notes_empty_books", "n.doc_id", BOOK_TITLE, "", "",
                    "SELECT max(m.note_modified_time) FROM notes m WHERE m.doc_id = n.doc_id"),
            new Grouping("hx_notes_in_collections", "hx_notes_empty_collections", "g.coll_id", "g.coll_title",
                    ", docs_to_colls t, colls g", " AND t.doc_id = n.doc_id AND g.coll_id = t.coll_id",
                    "SELECT max(m.note_modified_time) FROM notes m, docs_to_colls u "
                            + "WHERE u.coll_id = g.coll_id AND m.doc_id = u.doc_id"),
            new Grouping("hx_notes_in_authors", "hx_notes_empty_authors", "g.author_id", "g.author_name",
                    ", docs_to_authors t, authors g", " AND t.doc_id = n.doc_id AND g.author_id = t.author_id",
                    "SELECT max(m.note_modified_time) FROM notes m, docs_to_authors u "
                            + "WHERE u.author_id = g.author_id AND m.doc_id = u.doc_id") };

    private final boolean[] typeFilter = new boolean[4];

    private final boolean[] colorFilter = new boolean[PALETTE.length];

    private final List<Row> rows = new ArrayList<>();

    private final RowAdapter adapter = new RowAdapter();

    private final TextView[] groupingTabs = new TextView[GROUPINGS.length];

    private final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();

    private int queryGeneration;

    private int groupingIndex;

    private String query = "";

    private int textColor;

    private int secondaryTextColor;

    private int accentColor;

    private TextView emptyView;

    private ListView listView;

    public static void open(Activity activity) {
        activity.startActivity(new Intent(activity, NotesActivity.class));
    }

    private static void postNotePosition(Cursor note, int type, long docId) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolveColors();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);
        root.addView(buildHeader());
        root.addView(buildSearch());
        root.addView(buildTabs());
        root.addView(buildFilters());
        FrameLayout content = new FrameLayout(this);
        this.listView = new ListView(this);
        this.listView.setAdapter(this.adapter);
        this.listView.setDivider(null);
        this.listView.setOnItemClickListener((parent, view, position, id) -> openRow(this.rows.get(position)));
        content.addView(this.listView,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.emptyView = text("", 16, this.secondaryTextColor);
        this.emptyView.setGravity(Gravity.CENTER);
        this.emptyView.setPadding(dp(32), dp(32), dp(32), dp(32));
        content.addView(this.emptyView,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    @Override
    protected void onDestroy() {
        this.queryExecutor.shutdownNow();
        super.onDestroy();
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setMinimumHeight(dp(56));
        View back = new BackArrow(this, this.textColor);
        back.setContentDescription(string("hx_notes_back"));
        back.setOnClickListener((view) -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView title = text(string("hx_notes_title"), 20, this.textColor);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);
        return header;
    }

    private View buildSearch() {
        EditText search = new EditText(this);
        search.setHint(string("hx_notes_search"));
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setTextColor(this.textColor);
        search.setHintTextColor(this.secondaryTextColor);
        search.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                NotesActivity.this.query = text.toString().trim();
                reload();
            }

        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(16);
        params.rightMargin = dp(16);
        search.setLayoutParams(params);
        return search;
    }

    private View buildTabs() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(16), dp(8), dp(16), 0);
        for (int index = 0; index < GROUPINGS.length; index++) {
            TextView tab = chip(string(GROUPINGS[index].labelResource));
            int selectedGrouping = index;
            tab.setOnClickListener((view) -> setGrouping(selectedGrouping));
            styleChip(tab, index == this.groupingIndex, this.accentColor);
            this.groupingTabs[index] = tab;
            tabs.addView(tab);
        }
        scroll.addView(tabs);
        return scroll;
    }

    private View buildFilters() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setGravity(Gravity.CENTER_VERTICAL);
        filters.setPadding(dp(16), dp(8), dp(16), dp(8));
        addTypeChip(filters, string("hx_notes_quotes"), TYPE_QUOTE);
        addTypeChip(filters, string("hx_notes_bookmarks"), TYPE_BOOKMARK);
        addTypeChip(filters, string("hx_notes_reviews"), TYPE_REVIEW);
        for (int color = 0; color < PALETTE.length; color++) {
            TextView chip = chip("");
            chip.setContentDescription(COLOR_NAMES[color]);
            chip.setMinWidth(dp(40));
            int selectedColor = color;
            chip.setOnClickListener((view) -> {
                this.colorFilter[selectedColor] = !this.colorFilter[selectedColor];
                styleChip(chip, this.colorFilter[selectedColor], PALETTE[selectedColor]);
                reload();
            });
            styleChip(chip, false, PALETTE[color]);
            filters.addView(chip);
        }
        scroll.addView(filters);
        return scroll;
    }

    private void addTypeChip(LinearLayout filters, String label, int type) {
        TextView chip = chip(label);
        chip.setOnClickListener((view) -> {
            this.typeFilter[type] = !this.typeFilter[type];
            styleChip(chip, this.typeFilter[type], this.accentColor);
            reload();
        });
        styleChip(chip, false, this.accentColor);
        filters.addView(chip);
    }

    private void setGrouping(int groupingIndex) {
        this.groupingIndex = groupingIndex;
        for (int index = 0; index < this.groupingTabs.length; index++) {
            styleChip(this.groupingTabs[index], index == groupingIndex, this.accentColor);
        }
        reload();
    }

    private void reload() {
        List<String> arguments = new ArrayList<>();
        Grouping grouping = GROUPINGS[this.groupingIndex];
        StringBuilder sql = new StringBuilder("SELECT n.note_id, n.note_type, n.doc_id, n.note_body, n.note_extra, "
                + "n.note_page, n.note_mark, n.note_modified_time, " + BOOK_TITLE);
        if (grouping.keyColumn != null) {
            sql.append(", ").append(grouping.keyColumn).append(", ").append(grouping.titleColumn);
        }
        sql.append(" FROM notes n, docs d")
            .append(grouping.extraTables)
            .append(" WHERE n.doc_id = d.doc_id AND d.doc_active > 0")
            .append(grouping.joinClause);
        String filter = filterClause();
        if (filter != null) {
            sql.append(" AND (").append(filter).append(')');
        }
        if (this.query.length() > 0) {
            sql.append(" AND n.note_id IN (SELECT docid FROM fts_notes WHERE fts_notes MATCH ?)");
            arguments.add(this.query.replace('"', ' ') + "*");
        }
        if (grouping.keyColumn != null) {
            sql.append(" ORDER BY (")
                .append(grouping.latestNoteQuery)
                .append(") DESC, ")
                .append(grouping.keyColumn)
                .append(", n.doc_id, n.note_index");
        } else {
            sql.append(" ORDER BY n.note_modified_time DESC");
        }
        int generation = ++this.queryGeneration;
        String[] argumentArray = arguments.toArray(new String[0]);
        this.queryExecutor.execute(() -> {
            List<Row> result = queryRows(grouping, sql.toString(), argumentArray);
            runOnUiThread(() -> showRows(generation, grouping, result));
        });
    }

    private List<Row> queryRows(Grouping grouping, String sql, String[] arguments) {
        List<Row> result = new ArrayList<>();
        try (SQLiteDatabase database = openDatabase(); Cursor cursor = database.rawQuery(sql, arguments)) {
            String sectionKey = null;
            while (cursor.moveToNext()) {
                Row row = new Row(cursor.getLong(0), cursor.getInt(1), cursor.getLong(2), cursor.getString(3),
                        cursor.getString(4), cursor.getInt(5), cursor.getInt(6), cursor.getLong(7), cursor.getString(8),
                        null);
                String rowSectionKey;
                String rowSectionTitle;
                if (grouping.keyColumn != null) {
                    rowSectionKey = cursor.getString(9);
                    rowSectionTitle = cursor.getString(10);
                } else {
                    rowSectionKey = formatDate(row.modified);
                    rowSectionTitle = rowSectionKey;
                }
                if (!TextUtils.equals(sectionKey, rowSectionKey)) {
                    sectionKey = rowSectionKey;
                    result.add(Row.header(rowSectionTitle));
                }
                result.add(row);
            }
        } catch (RuntimeException ex) {
            result.clear();
        }
        return result;
    }

    private void showRows(int generation, Grouping grouping, List<Row> result) {
        if (generation != this.queryGeneration || isFinishing()) {
            return;
        }
        this.rows.clear();
        this.rows.addAll(result);
        this.adapter.notifyDataSetChanged();
        this.emptyView.setText(string(grouping.emptyMessageResource));
        this.emptyView.setVisibility(this.rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String filterClause() {
        List<String> types = new ArrayList<>();
        for (int type : new int[] { TYPE_QUOTE, TYPE_BOOKMARK, TYPE_REVIEW }) {
            if (this.typeFilter[type]) {
                types.add(Integer.toString(type));
            }
        }
        List<String> colors = new ArrayList<>();
        for (int color = 0; color < this.colorFilter.length; color++) {
            if (this.colorFilter[color]) {
                colors.add(Integer.toString(color));
            }
        }
        String colored = colors.isEmpty() ? null
                : "n.note_type = " + TYPE_QUOTE + " AND n.note_mark IN (" + TextUtils.join(",", colors) + ")";
        if (colored != null) {
            types.remove(Integer.toString(TYPE_QUOTE));
        }
        String typed = types.isEmpty() ? null : "n.note_type IN (" + TextUtils.join(",", types) + ")";
        if (colored != null && typed != null) {
            return "(" + colored + ") OR " + typed;
        }
        return (colored != null) ? colored : typed;
    }

    private SQLiteDatabase openDatabase() {
        return SQLiteDatabase.openDatabase(getDatabasePath("readera.db").getPath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    private void openRow(Row row) {
        if (row.isHeader()) {
            return;
        }
        if (row.type != TYPE_REVIEW) {
            try (SQLiteDatabase database = openDatabase();
                    Cursor note = database.rawQuery("SELECT * FROM notes WHERE note_id = ?",
                            new String[] { Long.toString(row.id) })) {
                if (note.moveToFirst()) {
                    postNotePosition(note, row.type, row.docId);
                }
            } catch (RuntimeException ex) {
            }
        }
        Intent intent = new Intent(READ_ACTION, Uri.parse(DOCS_URI + row.docId));
        intent.setClassName(getPackageName(), READ_ACTIVITY);
        intent.putExtra(STARTUP_MODE_EXTRA, 0);
        startActivity(intent);
    }

    private int stringId(String name) {
        return getResources().getIdentifier(name, "string", getPackageName());
    }

    private String string(String name) {
        return getString(stringId(name));
    }

    private String formatDate(long time) {
        return DateFormat.getLongDateFormat(this).format(new Date(time));
    }

    private void resolveColors() {
        TypedArray attributes = obtainStyledAttributes(new int[] { android.R.attr.textColorPrimary,
                android.R.attr.textColorSecondary, android.R.attr.colorAccent });
        this.textColor = attributes.getColor(0, Color.BLACK);
        this.secondaryTextColor = attributes.getColor(1, Color.GRAY);
        this.accentColor = attributes.getColor(2, PALETTE[4]);
        attributes.recycle();
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        return view;
    }

    private TextView chip(String label) {
        TextView chip = text(label, 14, this.textColor);
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(36));
        chip.setPadding(dp(14), 0, dp(14), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        params.rightMargin = dp(8);
        chip.setLayoutParams(params);
        return chip;
    }

    private void styleChip(TextView chip, boolean selected, int color) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), color);
        background.setColor(selected ? color : Color.TRANSPARENT);
        chip.setBackground(background);
        chip.setSelected(selected);
        chip.setTextColor(selected ? Color.WHITE : this.textColor);
    }

    private int dp(float value) {
        return Math
            .round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    private final class RowAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return NotesActivity.this.rows.size();
        }

        @Override
        public Object getItem(int position) {
            return NotesActivity.this.rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return NotesActivity.this.rows.get(position).isHeader() ? 0 : 1;
        }

        @Override
        public boolean isEnabled(int position) {
            return !NotesActivity.this.rows.get(position).isHeader();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Row row = NotesActivity.this.rows.get(position);
            return row.isHeader() ? headerView(row) : noteView(row);
        }

        private View headerView(Row row) {
            TextView header = text(row.sectionTitle, 14, NotesActivity.this.accentColor);
            header.setTypeface(Typeface.DEFAULT_BOLD);
            header.setPadding(dp(16), dp(16), dp(16), dp(4));
            return header;
        }

        private View noteView(Row row) {
            LinearLayout item = new LinearLayout(NotesActivity.this);
            item.setPadding(dp(16), dp(10), dp(16), dp(10));
            View bar = new View(NotesActivity.this);
            bar.setBackgroundColor(
                    (row.type == TYPE_QUOTE) ? PALETTE[Math.max(0, Math.min(4, row.mark))] : Color.TRANSPARENT);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(4),
                    ViewGroup.LayoutParams.MATCH_PARENT);
            barParams.rightMargin = dp(12);
            item.addView(bar, barParams);
            LinearLayout texts = new LinearLayout(NotesActivity.this);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView body = text(primaryText(row), 16, NotesActivity.this.textColor);
            body.setMaxLines(8);
            body.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(body);
            if (row.type == TYPE_QUOTE && !TextUtils.isEmpty(row.extra)) {
                TextView comment = text(row.extra, 14, NotesActivity.this.textColor);
                comment.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                comment.setPadding(0, dp(4), 0, 0);
                texts.addView(comment);
            }
            TextView meta = text(metaText(row), 13, NotesActivity.this.secondaryTextColor);
            meta.setPadding(0, dp(4), 0, 0);
            texts.addView(meta);
            item.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            return item;
        }

        private String primaryText(Row row) {
            String body = (row.body != null) ? row.body : "";
            switch (row.type) {
                case TYPE_BOOKMARK:
                    return string("hx_notes_bookmark") + ": " + body + ", "
                            + getString(stringId("hx_notes_page"), row.page + 1);
                case TYPE_REVIEW:
                    return string("hx_notes_review") + " " + stars(row.mark) + "\n" + body;
                default:
                    return body;
            }
        }

        private String metaText(Row row) {
            return (NotesActivity.this.groupingIndex == BOOKS_GROUPING) ? formatDate(row.modified)
                    : row.bookTitle + ", " + formatDate(row.modified);
        }

        private String stars(int rating) {
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stars.append((i < rating) ? '★' : '☆');
            }
            return stars.toString();
        }

    }

    private static final class Row {

        final long id;

        final int type;

        final long docId;

        final String body;

        final String extra;

        final int page;

        final int mark;

        final long modified;

        final String bookTitle;

        final String sectionTitle;

        Row(long id, int type, long docId, String body, String extra, int page, int mark, long modified,
                String bookTitle, String sectionTitle) {
            this.id = id;
            this.type = type;
            this.docId = docId;
            this.body = body;
            this.extra = extra;
            this.page = page;
            this.mark = mark;
            this.modified = modified;
            this.bookTitle = bookTitle;
            this.sectionTitle = sectionTitle;
        }

        static Row header(String title) {
            return new Row(-1, 0, -1, null, null, 0, 0, 0, null, title);
        }

        boolean isHeader() {
            return this.sectionTitle != null;
        }

    }

    private static final class Grouping {

        final String labelResource;

        final String emptyMessageResource;

        final String keyColumn;

        final String titleColumn;

        final String extraTables;

        final String joinClause;

        final String latestNoteQuery;

        Grouping(String labelResource, String emptyMessageResource, String keyColumn, String titleColumn,
                String extraTables, String joinClause, String latestNoteQuery) {
            this.labelResource = labelResource;
            this.emptyMessageResource = emptyMessageResource;
            this.keyColumn = keyColumn;
            this.titleColumn = titleColumn;
            this.extraTables = extraTables;
            this.joinClause = joinClause;
            this.latestNoteQuery = latestNoteQuery;
        }

    }

    private static final class BackArrow extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Path path = new Path();

        BackArrow(Context context, int color) {
            super(context);
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    context.getResources().getDisplayMetrics()));
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float size = Math.min(getWidth(), getHeight());
            float left = (getWidth() - size) / 2 + size * 0.3f;
            float right = (getWidth() - size) / 2 + size * 0.7f;
            float middle = getHeight() / 2f;
            float arm = size * 0.18f;
            this.path.reset();
            this.path.moveTo(right, middle);
            this.path.lineTo(left, middle);
            this.path.moveTo(left + arm, middle - arm);
            this.path.lineTo(left, middle);
            this.path.lineTo(left + arm, middle + arm);
            canvas.drawPath(this.path, this.paint);
        }

    }

}
