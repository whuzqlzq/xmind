package org.xmind.ui.datepicker;

import static java.util.Calendar.DATE;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.YEAR;
import static org.eclipse.jface.resource.JFaceResources.DEFAULT_FONT;

import java.util.Calendar;
import java.util.List;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener2;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.xmind.ui.dialogs.PopupDialog;
import org.xmind.ui.resources.ColorUtils;
import org.xmind.ui.resources.FontUtils;
import org.xmind.ui.viewers.MButton;
import org.xmind.ui.viewers.SWTUtils;

/**
 * A viewer to pick a date on the calendar.
 * 
 * @author Frank Shaka
 */
public class DatePicker extends Viewer {

    public static final String[] MONTHS = new String[] { Messages.January,
            Messages.Feburary, Messages.March, Messages.April, Messages.May,
            Messages.June, Messages.July, Messages.August, Messages.September,
            Messages.October, Messages.November, Messages.December };

    public static final String[] WEEK_SYMBOLS = new String[] { Messages.Sunday,
            Messages.Monday, Messages.Tuesday, Messages.Wednesday,
            Messages.Thursday, Messages.Friday, Messages.Saturday };

    public static final int CORNER = 8;

    private static final int FUTURE_YEARS = 7;

    private static final int PASSED_YEARS = 3;

    private static final String TEXT = "#000000"; //$NON-NLS-1$

    private static final String WEEKEND = "#EE0000"; //$NON-NLS-1$

    private static final String SEPARATOR = "#C0C0C0"; //$NON-NLS-1$

    private static final String TODAY = "#ff9900"; //$NON-NLS-1$

    private static final String WEEK_SYMBOL = "#808080"; //$NON-NLS-1$

    private static final String ARROW_BORDER = "#808080"; //$NON-NLS-1$

    private static final String ARROW_FILL = "#C0C0C0"; //$NON-NLS-1$

    private static final String CANCEL = "#D80000"; //$NON-NLS-1$

    protected static final int NORMAL_ALPHA = 0xff;

    protected static final int SIBLING_MONTH_ALPHA = 0x20;

    private static final int DURATION = 200;

    protected static final int TOTAL_DAYS = 42;

    protected class EventHandler implements MouseListener, MouseMotionListener,
            Listener {

        private boolean dayPressed = false;

        private BaseFigure target = null;

        public void attach(IFigure figure) {
            figure.addMouseListener(this);
            figure.addMouseMotionListener(this);
        }

        public void detach(IFigure figure) {
            figure.removeMouseListener(this);
            figure.removeMouseMotionListener(this);
        }

        public void install(Control control) {
            control.addListener(SWT.MouseUp, this);
            control.addListener(SWT.MouseWheel, this);
            control.addListener(SWT.KeyDown, this);
        }

        public void uninstall(Control control) {
            control.removeListener(SWT.MouseUp, this);
            control.removeListener(SWT.MouseWheel, this);
            control.removeListener(SWT.KeyDown, this);
        }

        public void handleEvent(Event event) {
            if (event.type == SWT.MouseUp) {
                dayPressed = false;
                if (target != null) {
                    final BaseFigure eventTarget = target;
                    target.setPressed(false);
                    target = null;
                    selected(eventTarget);
                }
            } else if (event.type == SWT.MouseWheel) {
                if (event.count == 0)
                    return;
                // wheel upwards, count > 0, month should decrease
                // wheel downwards, count < 0, month should increase
                rollMonth(event.count > 0 ? -1 : 1);
            } else if (event.type == SWT.KeyDown) {
                handleKeyPress(event.keyCode, event.stateMask);
            }
        }

        private void handleKeyPress(int key, int mask) {
            if (SWTUtils.matchKey(mask, key, 0, SWT.ARROW_UP)) {
                lastMonthSelected(true);
            } else if (SWTUtils.matchKey(mask, key, 0, SWT.ARROW_DOWN)) {
                nextMonthSelected(true);
            } else if (SWTUtils.matchKey(mask, key, 0, SWT.ARROW_LEFT)) {
                lastYearSelected(true);
            } else if (SWTUtils.matchKey(mask, key, 0, SWT.ARROW_RIGHT)) {
                nextYearSelected(true);
            }
        }

        public void mouseDoubleClicked(MouseEvent me) {
            // do nothing
        }

        public void mouseDragged(MouseEvent me) {
            // do nothing
        }

        public void mouseEntered(MouseEvent me) {
            if (target == null) {
                BaseFigure source = (BaseFigure) me.getSource();
                if (source instanceof DayFigure && dayPressed) {
                    source.setPressed(true);
                    source.setPreselected(false);
                } else {
                    if (source == monthFigure || source == lastMonth
                            || source == nextMonth) {
                        monthFigure.setPreselected(true);
                        lastMonth.getContent().setVisible(true);
                        nextMonth.getContent().setVisible(true);
                    } else if (source == yearFigure || source == lastYear
                            || source == nextYear) {
                        yearFigure.setPreselected(true);
                        lastYear.getContent().setVisible(true);
                        nextYear.getContent().setVisible(true);
                    }
                    source.setPreselected(true);
                }
            }
        }

        public void mouseExited(MouseEvent me) {
            if (target == null) {
                BaseFigure source = (BaseFigure) me.getSource();
                if (source instanceof DayFigure) {
                    source.setPreselected(false);
                    if (dayPressed) {
                        source.setPressed(false);
                    }
                } else {
                    if (source == monthFigure || source == lastMonth
                            || source == nextMonth) {
                        monthFigure.setPreselected(false);
                        lastMonth.getContent().setVisible(false);
                        nextMonth.getContent().setVisible(false);
                    } else if (source == yearFigure || source == lastYear
                            || source == nextYear) {
                        yearFigure.setPreselected(false);
                        lastYear.getContent().setVisible(false);
                        nextYear.getContent().setVisible(false);
                    }
                    source.setPreselected(false);
                }
            }
        }

        public void mouseHover(MouseEvent me) {
            // do nothing
        }

        public void mouseMoved(MouseEvent me) {
            // do nothing
        }

        public void mousePressed(MouseEvent me) {
            BaseFigure source = (BaseFigure) me.getSource();
            source.setPressed(true);
            source.setPreselected(false);
            if (source instanceof DayFigure) {
                dayPressed = true;
            } else {
                target = source;
            }
        }

        public void mouseReleased(MouseEvent me) {
            BaseFigure source = (BaseFigure) me.getSource();
            source.setPressed(false);
            if (source instanceof DayFigure) {
                if (dayPressed) {
                    daySelected((DayFigure) me.getSource());
                }
                source.setPreselected(true);
            } else {
                if (!source.isSelected()) {
                    source.setPreselected(true);
                }
            }
        }

    }

    private class DropdownDatePicker extends PopupDialog {

        public DropdownDatePicker(Shell parent) {
            super(parent, SWT.NO_TRIM, true, false, false, false, false, null,
                    null);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite = (Composite) super.createDialogArea(parent);
            createDatePicker(composite);
            datePicker.setLayoutData(new org.eclipse.swt.layout.GridData(
                    SWT.FILL, SWT.FILL, true, true));
            initDatePicker();
            update();
            return composite;
        }

        @Override
        public int open() {
            isPopupEditing = true;
            return super.open();
        }

        @Override
        public boolean close() {
            isPopupEditing = false;
            boolean closed = super.close();
            if (closed) {
                datePicker = null;
            }
            return closed;
        }

        @Override
        protected Point getInitialLocation(Point initialSize) {
            Control c = DatePicker.this.getControl();
            org.eclipse.swt.graphics.Rectangle r = c.getBounds();
            return c.toDisplay(0, r.height + 1);
        }

    }

    private class MonthAction extends Action {

        private int month;

        public MonthAction(int month) {
            super(MONTHS[month]);
            this.month = month;
        }

        public void run() {
            monthSelected(month);
        }
    }

    private class YearAction extends Action {

        private int year;

        public void setYear(int year) {
            this.year = year;
            setText("" + year); //$NON-NLS-1$
        }

        public int getYear() {
            return year;
        }

        public void run() {
            yearSelected(year);
        }
    }

    private class AnimationAdvisor implements IAnimationAdvisor {

        private int monthsToRoll = 0;

        private int yearsToRoll = 0;

        private int duration = -1;

        private int oldYear;

        private int oldMonth;

        private int newYear;

        private int newMonth;

        public void addMonthsToRoll(int count) {
            monthsToRoll += count;
            duration = -1;
        }

        public void addYearsToRoll(int count) {
            yearsToRoll += count;
            duration = -1;
        }

        public int getDuration() {
            if (duration < 0) {
                int steps = Math.abs(monthsToRoll) + Math.abs(yearsToRoll);
                duration = steps == 0 ? 0 : DURATION / steps;
            }
            return duration;
        }

        public IFigure getLayer() {
            return DatePicker.this.dateLayer;
        }

        public int getMonthsToRoll() {
            return monthsToRoll;
        }

        public int getNewMonth() {
            return newMonth;
        }

        public int getNewYear() {
            return newYear;
        }

        public int getOldMonth() {
            return oldMonth;
        }

        public int getOldYear() {
            return oldYear;
        }

        public IFigure getPanel() {
            return DatePicker.this.datePanel;
        }

        public int getYearsToRoll() {
            return yearsToRoll;
        }

        public void initNewDay(DayFigure figure) {
            updateDayFigure(figure, oldYear, oldMonth);
        }

        public void initOldDay(DayFigure figure) {
            figure.setPreselected(false);
            figure.setSelected(isSameDay(figure.getDate(), getSelectedDate()));
            eventHandler.detach(figure);
        }

        public boolean isDone() {
            return monthsToRoll == 0 && yearsToRoll == 0;
        }

        public void setEndMonth(int newYear, int newMonth) {
            this.newYear = newYear;
            this.newMonth = newMonth;
        }

        public void setStartMonth(int oldYear, int oldMonth) {
            this.oldYear = oldYear;
            this.oldMonth = oldMonth;
        }

        public void updateNewDay(DayFigure figure) {
            figure.setPreselected(false);
            figure.setSelected(isSameDay(figure.getDate(), getSelectedDate()));
            eventHandler.attach(figure);
            updateDayFigure(figure, newYear, newMonth);
        }

    }

    private int style;

    private Control control;

    private FigureCanvas datePicker;

    private MButton placeholder;

    private ILabelProvider dateLabelProvider;

    private DropdownDatePicker dropdownDatePicker;

    private Calendar today;

    private int currentMonth;

    private int currentYear;

    private Calendar selection;

    private IFigure dateLayer;

    private IFigure datePanel;

    private MonthFigure monthFigure;

    private YearFigure yearFigure;

    private BaseFigure todayFigure;

    private BaseFigure cancelFigure;

    private EventHandler eventHandler;

    private MenuManager monthMenu;

    private MonthAction[] monthActions;

    private MenuManager yearMenu;

    private YearAction[] yearActions;

    private ArrowFigure lastYear;

    private ArrowFigure nextYear;

    private ArrowFigure lastMonth;

    private ArrowFigure nextMonth;

    private boolean firingSelectionChange = false;

    private boolean animating = false;

    private IAnimationAdvisor animationAdvisor = new AnimationAdvisor();

    private boolean isPopupEditing = false;

    /**
     * Constructs a new instance of this class given its parent and a style
     * value describing its behavior and appearance.
     * 
     * @param parent
     *            a composite control which will be the parent of the new
     *            instance (cannot be null)
     * @param style
     *            the style of control to construct
     * 
     * @see SWT#SIMPLE
     * @see SWT#DROP_DOWN
     * @see SWT#CANCEL
     */
    public DatePicker(Composite parent, int style) {
        this(parent, style, Calendar.getInstance());
    }

    public DatePicker(Composite parent, int style, Calendar today) {
        this.today = today;
        this.currentMonth = today.get(MONTH);
        this.currentYear = today.get(YEAR);
        this.style = style;
        if ((style & SWT.DROP_DOWN) != 0) {
            createPlaceholder(parent);
            this.control = placeholder.getControl();
            initPlaceholder();
        } else {
            createDatePicker(parent);
            this.control = datePicker;
            initDatePicker();
        }
        update();
    }

    private void createPlaceholder(Composite parent) {
        this.placeholder = new MButton(parent, MButton.NORMAL);
    }

    private void createDatePicker(Composite parent) {
        this.datePicker = new FigureCanvas(parent);
    }

    public void setLabelProvider(ILabelProvider labelProvider) {
        this.dateLabelProvider = labelProvider;
        update();
    }

    public ILabelProvider getLabelProvider() {
        if (this.dateLabelProvider == null) {
            this.dateLabelProvider = new DateLabelProvider();
        }
        return this.dateLabelProvider;
    }

    public Control getControl() {
        return control;
    }

    public FigureCanvas getDatePicker() {
        return datePicker;
    }

    public boolean isPopupEditing() {
        return isPopupEditing;
    }

    public void setBackground(Color color) {
        if (control != null && !control.isDisposed()) {
            control.setBackground(color);
        }
    }

    public void setEnabled(boolean enabled) {
        if (placeholder != null) {
            placeholder.setEnabled(enabled);
        }
    }

    public MButton getPlaceholder() {
        return placeholder;
    }

    public ISelection getSelection() {
        return new DateSelection(selection);
    }

    @Override
    public void setSelection(ISelection selection) {
        setSelection(selection, true);
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        if (selection instanceof DateSelection) {
            setDateSelection(((DateSelection) selection).getDate(), reveal);
        } else if (selection instanceof IStructuredSelection) {
            Object sel = ((IStructuredSelection) selection).getFirstElement();
            if (sel instanceof Calendar) {
                setDateSelection((Calendar) sel, reveal);
            }
        }
    }

    public void setDateSelection(Calendar date, boolean reveal) {
        changeDate(date);
        if (reveal && date != null) {
            changeCalendar(date.get(YEAR), date.get(MONTH));
        }
        update();
    }

    private void initPlaceholder() {
        placeholder.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                if (dropdownDatePicker == null
                        || dropdownDatePicker.getShell() == null
                        || dropdownDatePicker.getShell().isDisposed()
                        || !dropdownDatePicker.isClosing())
                    showDropdown();
            }
        });
    }

    private void showDropdown() {
        placeholder.setForceFocus(true);
        createDropdownDatePicker();
        dropdownDatePicker.open();
        Shell shell = dropdownDatePicker.getShell();
        if (shell != null && !shell.isDisposed()) {
            shell.addListener(SWT.Dispose, new Listener() {
                public void handleEvent(Event event) {
                    if (placeholder != null
                            && !placeholder.getControl().isDisposed()) {
                        placeholder.setForceFocus(false);
                    }
                }
            });
        } else {
            placeholder.setForceFocus(false);
        }
    }

    /**
     * Shows the drop-down menu if this date picker is created with
     * <code>SWT.DROP_DOWN</code> style bit.
     */
    public void open() {
        showDropdown();
    }

    private void createDropdownDatePicker() {
        if (dropdownDatePicker != null)
            return;
        dropdownDatePicker = new DropdownDatePicker(control.getShell());
    }

    private void initDatePicker() {
        datePicker.setScrollBarVisibility(FigureCanvas.NEVER);
//        if (Util.isWindows()) {
        //datePicker.setBackground(datePicker.getParent().getBackground());
//        } else {
//            datePicker.getLightweightSystem().getRootFigure().setOpaque(false);
//        }

        eventHandler = new EventHandler();
        eventHandler.install(datePicker);
        datePicker.addListener(SWT.Dispose, new Listener() {
            public void handleEvent(Event event) {
                eventHandler.uninstall(datePicker);
            }
        });

        Viewport viewport = new Viewport(true);
        viewport.setContentsTracksHeight(true);
        viewport.setContentsTracksWidth(true);
        datePicker.setViewport(viewport);

        IFigure container = new Layer();
        datePicker.setContents(container);

        GridLayout containerLayout = new GridLayout(1, true);
        containerLayout.horizontalSpacing = 3;
        containerLayout.verticalSpacing = 3;
        containerLayout.marginHeight = 3;
        containerLayout.marginWidth = 3;
        container.setLayoutManager(containerLayout);
        createTopPanel(container);
        createSeparator(container);
        createWeekPanel(container);
        createDaysPanel(container);
        createSeparator(container);
        createBottomPanel(container);
    }

    private void createTopPanel(IFigure parent) {
        IFigure panel = new Layer();
        GridData panelConstraint = new GridData(SWT.FILL, SWT.FILL, true, false);
        parent.add(panel, panelConstraint);

        GridLayout panelLayout = new GridLayout(12, true);
        panelLayout.horizontalSpacing = 0;
        panelLayout.verticalSpacing = 0;
        panelLayout.marginHeight = 0;
        panelLayout.marginWidth = 0;
        panel.setLayoutManager(panelLayout);

        lastMonth = createArrowFigure(panel, ArrowFigure.UP);
        monthFigure = createMonthFigure(panel);
        nextMonth = createArrowFigure(panel, ArrowFigure.DOWN);
        lastYear = createArrowFigure(panel, ArrowFigure.LEFT);
        yearFigure = createYearFigure(panel);
        nextYear = createArrowFigure(panel, ArrowFigure.RIGHT);
    }

    private MonthFigure createMonthFigure(IFigure parent) {
        MonthFigure figure = new MonthFigure();
        figure.setTextCandidates(MONTHS);
        GridData constraint = new GridData(SWT.FILL, SWT.FILL, true, true);
        constraint.horizontalSpan = 5;
        parent.add(figure, constraint);
        eventHandler.attach(figure);
        return figure;
    }

    private YearFigure createYearFigure(IFigure parent) {
        YearFigure figure = new YearFigure();
        figure.setTextCandidates(new String[] { "0000" }); //$NON-NLS-1$
        GridData constraint = new GridData(SWT.FILL, SWT.FILL, true, true);
        constraint.horizontalSpan = 3;
        parent.add(figure, constraint);
        eventHandler.attach(figure);
        return figure;
    }

    private ArrowFigure createArrowFigure(IFigure parent, int orientation) {
        ArrowFigure arrow = new ArrowFigure();
        arrow.setOrientation(orientation);
        arrow.setForegroundColor(ColorUtils.getColor(ARROW_BORDER));
        arrow.setBackgroundColor(ColorUtils.getColor(ARROW_FILL));
        arrow.getContent().setVisible(false);
        GridData constraint = new GridData(SWT.FILL, SWT.FILL, true, true);
        parent.add(arrow, constraint);
        eventHandler.attach(arrow);
        return arrow;
    }

    private void createSeparator(IFigure parent) {
        HorizontalLine line = new HorizontalLine();
        line.setMargin(3);
        line.setForegroundColor(ColorUtils.getColor(SEPARATOR));
        GridData constraint = new GridData(SWT.FILL, SWT.FILL, true, false);
        constraint.heightHint = 3;
        parent.add(line, constraint);
    }

    private void createWeekPanel(IFigure parent) {
        IFigure panel = new Layer();
        GridData panelConstraint = new GridData(SWT.FILL, SWT.FILL, true, false);
        parent.add(panel, panelConstraint);
        GridLayout panelLayout = new GridLayout(7, true);
        panelLayout.horizontalSpacing = 0;
        panelLayout.verticalSpacing = 0;
        panelLayout.marginHeight = 0;
        panelLayout.marginWidth = 0;
        panel.setLayoutManager(panelLayout);
        Font symbolFont = FontUtils.getRelativeHeight(DEFAULT_FONT, -2);
        for (int i = 0; i < 7; i++) {
            TextLayer symbol = new TextLayer();
            symbol.setFont(symbolFont);
            symbol.setText(WEEK_SYMBOLS[i]);
            if (i == 0 || i == 6) {
                symbol.setForegroundColor(ColorUtils.getColor(WEEKEND));
            } else {
                symbol.setForegroundColor(ColorUtils.getColor(WEEK_SYMBOL));
            }
            GridData symbolConstraint = new GridData(SWT.FILL, SWT.FILL, true,
                    true);
            panel.add(symbol, symbolConstraint);
        }
    }

    private void createDaysPanel(IFigure parent) {
        dateLayer = new Layer();
        GridData layerConstraint = new GridData(SWT.FILL, SWT.FILL, true, true);
        parent.add(dateLayer, layerConstraint);
        dateLayer.setLayoutManager(new ConstraintStackLayout());

        datePanel = new Layer();
        dateLayer.add(datePanel, null);
        datePanel.setLayoutManager(new DatePanelLayout());
        for (int i = 0; i < TOTAL_DAYS; i++) {
            DayFigure dayFigure = new DayFigure();
            eventHandler.attach(dayFigure);
            datePanel.add(dayFigure);
        }
    }

    private void createBottomPanel(IFigure parent) {
        boolean hasCancel = (style & SWT.CANCEL) != 0;
        IFigure panel = new Layer();
        GridData panelConstraint = new GridData(SWT.FILL, SWT.FILL, true, false);
        parent.add(panel, panelConstraint);
        GridLayout panelLayout = new GridLayout(hasCancel ? 2 : 1, false);
        panelLayout.horizontalSpacing = 0;
        panelLayout.verticalSpacing = 0;
        panelLayout.marginHeight = 0;
        panelLayout.marginWidth = 0;
        panel.setLayoutManager(panelLayout);

        todayFigure = createTodayFigure(panel);
        if (hasCancel) {
            cancelFigure = createCancelFigure(panel);
        }
    }

    private BaseFigure createTodayFigure(IFigure parent) {
        BaseFigure figure = new BaseFigure();
        figure.setFont(FontUtils.getRelativeHeight(DEFAULT_FONT, -2));
        figure.setForegroundColor(ColorUtils.getColor(TODAY));
        GridData constraint = new GridData(SWT.FILL, SWT.FILL, true, true);
        parent.add(figure, constraint);
        eventHandler.attach(figure);
        return figure;
    }

    private BaseFigure createCancelFigure(IFigure parent) {
        BaseFigure figure = new BaseFigure();
        figure.setText(" X "); //$NON-NLS-1$
        figure.setFont(FontUtils.getRelativeHeight(DEFAULT_FONT, -2));
        figure.setForegroundColor(ColorUtils.getColor(CANCEL));
        GridData constraint = new GridData(SWT.FILL, SWT.FILL, false, true);
        parent.add(figure, constraint);
        eventHandler.attach(figure);
        return figure;
    }

    private Calendar getSelectedDate() {
        return selection;
    }

    private static boolean isSameDay(Calendar date1, Calendar date2) {
        if (date1 == null)
            return date2 == null;
        if (date2 == null)
            return false;
        return date1.get(DATE) == date2.get(DATE)
                && date1.get(MONTH) == date2.get(MONTH)
                && date1.get(YEAR) == date2.get(YEAR);
    }

    protected void changeCalendar(int newYear, int newMonth) {
        changeCalendar(newYear, newMonth, false);
    }

    protected void changeCalendar(int newYear, int newMonth, boolean smooth) {
        boolean calendarChanged = newMonth != currentMonth
                || newYear != currentYear;
        if (!calendarChanged)
            return;
        if (smooth) {
            if (datePicker != null) {
                int months = (newYear - currentYear) * 12 + newMonth
                        - currentMonth;
                animationAdvisor.addYearsToRoll(months / 12);
                animationAdvisor.addMonthsToRoll(months % 12);
                performRollCalendarAnimation(currentYear, currentMonth);
            }
            currentYear = newYear;
            currentMonth = newMonth;
        } else {
            currentYear = newYear;
            currentMonth = newMonth;
            if (datePicker != null) {
                updateCalendar();
            }
            updateSelection();
        }
    }

    private void updateCalendar() {
        today = Calendar.getInstance();
        updateDayFigures(datePanel.getChildren(), currentYear, currentMonth);
        monthFigure.setMonth(currentMonth);
        yearFigure.setYear(currentYear);
        todayFigure.setText(NLS.bind(Messages.TodayPattern,
                String.format("%1$tb %1$te, %1$tY", today))); //$NON-NLS-1$
    }

    private void updateDayFigures(List dayFigures, int year, int month) {
        Calendar date = getCalendarStart(today, year, month);
        for (int i = 0; i < dayFigures.size(); i++) {
            DayFigure dayFigure = (DayFigure) dayFigures.get(i);
            date = (Calendar) date.clone();
            if (i > 0)
                date.add(DATE, 1);
            dayFigure.setDate(date);
            updateDayFigure(dayFigure, year, month);
        }
    }

    void updateDayFigure(DayFigure figure, int year, int month) {
        figure.setFont(FontUtils.getBold(DEFAULT_FONT));
        Calendar date = figure.getDate();
        if (isSameDay(date, today)) {
            figure.setForegroundColor(ColorUtils.getColor(TODAY));
        } else if (isWeekend(date)) {
            figure.setForegroundColor(ColorUtils.getColor(WEEKEND));
        } else {
            figure.setForegroundColor(ColorUtils.getColor(TEXT));
        }
        if (date.get(MONTH) == month && date.get(YEAR) == year) {
            figure.setTextAlpha(NORMAL_ALPHA);
        } else {
            figure.setTextAlpha(SIBLING_MONTH_ALPHA);
        }
    }

    private void updateSelection() {
        if (datePicker != null) {
            for (Object figure : datePanel.getChildren()) {
                DayFigure dayFigure = (DayFigure) figure;
                dayFigure.setSelected(isSameDay(dayFigure.getDate(),
                        getSelectedDate()));
            }
        }
        if (placeholder != null) {
            String text = getLabelProvider().getText(selection);
            placeholder.setText(text);
        }
    }

    protected void update() {
        if (datePicker != null) {
            updateCalendar();
        }
        updateSelection();
    }

    private void performRollCalendarAnimation(int oldYear, int oldMonth) {
        if (animating)
            return;

        animating = true;

        final int newYear, newMonth;
        final CalendarAnimation animation;
        if (animationAdvisor.getYearsToRoll() != 0) {
            if (animationAdvisor.getYearsToRoll() < 0) {
                newYear = oldYear - 1;
                newMonth = oldMonth;
                animation = new LastYearAnimation(animationAdvisor);
            } else {
                newYear = oldYear + 1;
                newMonth = oldMonth;
                animation = new NextYearAnimation(animationAdvisor);
            }
        } else if (animationAdvisor.getMonthsToRoll() != 0) {
            if (animationAdvisor.getMonthsToRoll() < 0) {
                newYear = oldMonth <= 0 ? oldYear - 1 : oldYear;
                newMonth = oldMonth <= 0 ? 11 : oldMonth - 1;
                animation = new LastMonthAnimation(animationAdvisor);
            } else {
                newYear = oldMonth >= 11 ? oldYear + 1 : oldYear;
                newMonth = oldMonth >= 11 ? 0 : oldMonth + 1;
                animation = new NextMonthAnimation(animationAdvisor);
            }
        } else {
            newYear = oldYear;
            newMonth = oldMonth;
            animation = null;
        }
        monthFigure.setMonth(newMonth);
        yearFigure.setYear(newYear);
        if (animation != null) {
            animationAdvisor.setStartMonth(oldYear, oldMonth);
            animationAdvisor.setEndMonth(newYear, newMonth);
            animation.callback(new Runnable() {
                public void run() {
                    datePanel = animation.getNewPanel();
                    if (animationAdvisor.isDone()) {
                        animating = false;
                    } else {
                        Display.getCurrent().asyncExec(new Runnable() {
                            public void run() {
                                animating = false;
                                performRollCalendarAnimation(newYear, newMonth);
                            }
                        });
                    }

                }
            }).start();
        }
    }

    static int calc(int start, int end, int current, int total) {
        return start + (end - start) * current / total;
    }

    protected void selected(final BaseFigure target) {
        if (target instanceof MonthFigure) {
            target.setSelected(true);
            showMonthPopup();
        } else if (target instanceof YearFigure) {
            target.setSelected(true);
            showYearPopup();
        } else if (target == todayFigure) {
            todaySelected();
        } else if (target == lastMonth) {
            lastMonthSelected(true);
        } else if (target == nextMonth) {
            nextMonthSelected(true);
        } else if (target == lastYear) {
            lastYearSelected(true);
        } else if (target == nextYear) {
            nextYearSelected(true);
        } else if (target == cancelFigure) {
            cancelSelected();
        }
    }

    protected void showMonthPopup() {
        createMonthMenu();
        for (int month = 0; month < monthActions.length; month++) {
            MonthAction action = monthActions[month];
            action.setChecked(month == currentMonth);
        }
        Rectangle b = monthFigure.getBounds();
        Point loc = control.toDisplay(b.x, b.y + b.height);
        final Menu menu = monthMenu.createContextMenu(control);
        menu.setLocation(loc.x + 10, loc.y + 1);
        menu.setVisible(true);
    }

    protected void createMonthMenu() {
        if (monthMenu != null)
            return;
        monthMenu = new MenuManager();
        monthMenu.addMenuListener(new IMenuListener2() {
            public void menuAboutToShow(IMenuManager manager) {
                // do nothing
            }

            public void menuAboutToHide(IMenuManager manager) {
                monthFigure.setSelected(false);
            }
        });
        monthActions = new MonthAction[12];
        for (int month = 0; month < 12; month++) {
            MonthAction action = new MonthAction(month);
            monthMenu.add(action);
            monthActions[month] = action;
        }
    }

    protected void showYearPopup() {
        createYearMenu();
        int start = currentYear - PASSED_YEARS;
        for (int year = 0; year < yearActions.length; year++) {
            YearAction action = yearActions[year];
            action.setYear(start + year);
            action.setChecked(action.getYear() == currentYear);
        }
        Rectangle b = yearFigure.getBounds();
        Point loc = control.toDisplay(b.x, b.y + b.height);
        Menu menu = yearMenu.createContextMenu(control);
        menu.setLocation(loc.x, loc.y + 1);
        menu.setVisible(true);
    }

    protected void createYearMenu() {
        if (yearMenu != null)
            return;
        yearMenu = new MenuManager();
        yearMenu.addMenuListener(new IMenuListener2() {
            public void menuAboutToShow(IMenuManager manager) {
                // do nothing
            }

            public void menuAboutToHide(IMenuManager manager) {
                yearFigure.setSelected(false);
            }
        });
        yearActions = new YearAction[FUTURE_YEARS + PASSED_YEARS + 1];
        for (int year = 0; year < yearActions.length; year++) {
            YearAction action = new YearAction();
            yearMenu.add(action);
            yearActions[year] = action;
        }
    }

    protected void monthSelected(int month) {
        changeCalendar(currentYear, month);
    }

    protected void yearSelected(int year) {
        changeCalendar(year, currentMonth);
    }

    protected void daySelected(DayFigure day) {
        Calendar date = day.getDate();
        changeDate(date);
        if (date != null && date.get(MONTH) != currentMonth) {
            changeCalendar(date.get(YEAR), date.get(MONTH), true);
        }
    }

    protected void todaySelected() {
        changeDate(today);
        changeCalendar(today.get(YEAR), today.get(MONTH));
    }

    protected void lastMonthSelected(boolean smooth) {
        if (currentMonth <= 0) {
            changeCalendar(currentYear - 1, 11, smooth);
        } else {
            changeCalendar(currentYear, currentMonth - 1, smooth);
        }
    }

    protected void nextMonthSelected(boolean smooth) {
        if (currentMonth >= 11) {
            changeCalendar(currentYear + 1, 0, smooth);
        } else {
            changeCalendar(currentYear, currentMonth + 1, smooth);
        }
    }

    protected void lastYearSelected(boolean smooth) {
        changeCalendar(currentYear - 1, currentMonth, smooth);
    }

    protected void nextYearSelected(boolean smooth) {
        changeCalendar(currentYear + 1, currentMonth, smooth);
    }

    protected void cancelSelected() {
        changeDate(null);
    }

    protected void rollMonth(int count) {
        Calendar temp = (Calendar) today.clone();
        temp.set(YEAR, currentYear);
        temp.set(MONTH, currentMonth);
        temp.add(MONTH, count);
        changeCalendar(temp.get(YEAR), temp.get(MONTH), true);
    }

    protected void changeDate(Calendar date) {
        this.selection = date;
        updateSelection();

        if (firingSelectionChange)
            return;
        firingSelectionChange = true;
        if (dropdownDatePicker != null) {
            control.getDisplay().asyncExec(new Runnable() {
                public void run() {
                    dropdownDatePicker.close();
                }
            });
        }
        fireSelectionChanged(new SelectionChangedEvent(DatePicker.this,
                getSelection()));
        firingSelectionChange = false;
//        control.getDisplay().asyncExec(new Runnable() {
//            public void run() {
//            }
//        });
    }

    @Override
    public Object getInput() {
        return today;
    }

    @Override
    public void refresh() {
        update();
    }

    @Override
    public void setInput(Object input) {
        if (input instanceof Calendar) {
            Calendar oldInput = this.today;
            this.today = (Calendar) input;
            inputChanged(input, oldInput);
        }
    }

    @Override
    protected void inputChanged(Object input, Object oldInput) {
        update();
    }

    private static boolean isWeekend(Calendar date) {
        int dow = date.get(DAY_OF_WEEK);
        return dow == SUNDAY || dow == SATURDAY;
    }

    static Calendar getCalendarStart(Calendar date, int year, int month) {
        date = (Calendar) date.clone();
        date.set(year, month, 1);
        while (date.get(DAY_OF_WEEK) != SUNDAY) {
            date.add(DATE, -1);
        }
        return date;
    }

}
