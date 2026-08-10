package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.Route;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-input-popup")
public final class FlowInputPopupRoute extends VerticalLayout {
    public FlowInputPopupRoute() {
        setId("framework-flow-input-popup-route");
        setPadding(true);
        setSpacing(true);

        Span comboValueStatus =
                new Span("Input popup ComboBox value: pending");
        comboValueStatus.setId(
                "framework-flow-input-popup-combo-value-status");
        AtomicInteger comboValueChanges = new AtomicInteger();
        ComboBox<String> comboBox =
                new ComboBox<>("Flow IPC popup reviewer");
        comboBox.setId("framework-flow-input-popup-combo");
        comboBox.setAriaLabel("Flow IPC popup reviewer");
        comboBox.setItems("platform", "desktop", "audit");
        comboBox.addValueChangeListener(event ->
                comboValueStatus.setText(
                        "Input popup ComboBox value: " + event.getValue()
                                + " client=" + event.isFromClient()
                                + " change="
                                + comboValueChanges.incrementAndGet()));

        Span selectValueStatus =
                new Span("Input popup Select value: standard");
        selectValueStatus.setId(
                "framework-flow-input-popup-select-value-status");
        Span selectLifecycleStatus =
                new Span("Input popup Select lifecycle: pending");
        selectLifecycleStatus.setId(
                "framework-flow-input-popup-select-lifecycle-status");
        AtomicInteger selectValueChanges = new AtomicInteger();
        AtomicInteger selectLifecycleChanges = new AtomicInteger();
        LifecycleSelect<String> select = new LifecycleSelect<>();
        select.setId("framework-flow-input-popup-select");
        select.setLabel("Flow IPC popup priority");
        select.setAriaLabel("Flow IPC popup priority");
        select.setItems("standard", "audit", "deferred");
        select.setValue("standard");
        select.addValueChangeListener(event ->
                selectValueStatus.setText(
                        "Input popup Select value: " + event.getValue()
                                + " client=" + event.isFromClient()
                                + " change="
                                + selectValueChanges.incrementAndGet()));
        select.observeLifecycle(
                selectLifecycleStatus,
                selectLifecycleChanges);

        Span dateValueStatus =
                new Span("Input popup DatePicker value: 2026-08-12");
        dateValueStatus.setId(
                "framework-flow-input-popup-date-value-status");
        Span dateLifecycleStatus =
                new Span("Input popup DatePicker lifecycle: pending");
        dateLifecycleStatus.setId(
                "framework-flow-input-popup-date-lifecycle-status");
        AtomicInteger dateValueChanges = new AtomicInteger();
        AtomicInteger dateLifecycleChanges = new AtomicInteger();
        DatePicker datePicker =
                new DatePicker("Flow IPC popup due date");
        datePicker.setId("framework-flow-input-popup-date");
        datePicker.setAriaLabel("Flow IPC popup due date");
        datePicker.setValue(LocalDate.of(2026, 8, 12));
        datePicker.setInitialPosition(LocalDate.of(2026, 8, 12));
        datePicker.addValueChangeListener(event ->
                dateValueStatus.setText(
                        "Input popup DatePicker value: " + event.getValue()
                                + " client=" + event.isFromClient()
                                + " change="
                                + dateValueChanges.incrementAndGet()));
        datePicker.addOpenedChangeListener(event ->
                dateLifecycleStatus.setText(
                        "Input popup DatePicker lifecycle: "
                                + (event.isOpened() ? "opened" : "closed")
                                + " client=" + event.isFromClient()
                                + " change="
                                + dateLifecycleChanges.incrementAndGet()));

        Span timeValueStatus =
                new Span("Input popup TimePicker value: 13:00");
        timeValueStatus.setId(
                "framework-flow-input-popup-time-value-status");
        AtomicInteger timeValueChanges = new AtomicInteger();
        TimePicker timePicker =
                new TimePicker("Flow IPC popup review time");
        timePicker.setId("framework-flow-input-popup-time");
        timePicker.setAriaLabel("Flow IPC popup review time");
        timePicker.setLocale(Locale.US);
        timePicker.setMin(LocalTime.of(12, 0));
        timePicker.setMax(LocalTime.of(14, 0));
        timePicker.setStep(Duration.ofMinutes(30));
        timePicker.setValue(LocalTime.of(13, 0));
        timePicker.addValueChangeListener(event ->
                timeValueStatus.setText(
                        "Input popup TimePicker value: " + event.getValue()
                                + " client=" + event.isFromClient()
                                + " change="
                                + timeValueChanges.incrementAndGet()));

        Span pickerValidationStatus = new Span(
                "Picker validation: value=2026-08-12T13:00"
                        + " invalid=false error=none client=false change=0");
        pickerValidationStatus.setId(
                "framework-flow-picker-validation-status");
        Span pickerValidationEventStatus = new Span(
                "Picker validation event: valid=true invalid=false"
                        + " error=none change=0");
        pickerValidationEventStatus.setId(
                "framework-flow-picker-validation-event-status");
        AtomicInteger pickerValidationChanges = new AtomicInteger();
        AtomicInteger pickerValidationEvents = new AtomicInteger();
        DateTimePicker pickerValidation = new DateTimePicker(
                "Flow IPC constrained review date and time");
        pickerValidation.setId("framework-flow-picker-validation");
        pickerValidation.setAriaLabel(
                "Flow IPC constrained review date and time");
        pickerValidation.setDateAriaLabel(
                "Flow IPC constrained review date");
        pickerValidation.setTimeAriaLabel(
                "Flow IPC constrained review time");
        pickerValidation.setLocale(Locale.US);
        pickerValidation.setRequiredIndicatorVisible(true);
        pickerValidation.setMin(LocalDateTime.of(2026, 8, 12, 12, 0));
        pickerValidation.setMax(LocalDateTime.of(2026, 8, 12, 14, 0));
        pickerValidation.setI18n(
                new DateTimePicker.DateTimePickerI18n()
                        .setRequiredErrorMessage(
                                "Picker validation value is required")
                        .setBadInputErrorMessage(
                                "Picker validation value is malformed")
                        .setIncompleteInputErrorMessage(
                                "Picker validation value is incomplete")
                        .setMinErrorMessage(
                                "Picker validation value is too early")
                        .setMaxErrorMessage(
                                "Picker validation value is too late"));
        pickerValidation.setValue(
                LocalDateTime.of(2026, 8, 12, 13, 0));
        pickerValidation.addValueChangeListener(event -> {
            pickerValidationStatus.setText(
                    "Picker validation: value=" + event.getValue()
                            + " invalid=" + pickerValidation.isInvalid()
                            + " error=" + validationError(pickerValidation)
                            + " client=" + event.isFromClient()
                            + " change="
                            + pickerValidationChanges.incrementAndGet());
        });
        pickerValidation.addValidationStatusChangeListener(event ->
                pickerValidationEventStatus.setText(
                        "Picker validation event: valid="
                                + event.getNewStatus()
                                + " invalid=" + pickerValidation.isInvalid()
                                + " error="
                                + validationError(pickerValidation)
                                + " change="
                                + pickerValidationEvents.incrementAndGet()));

        Button outsideFocusTarget = new Button("Input popup outside target");
        outsideFocusTarget.setId(
                "framework-flow-input-popup-outside-target");
        Span outsideActionStatus =
                new Span("Input popup outside action: pending");
        outsideActionStatus.setId(
                "framework-flow-input-popup-outside-status");
        AtomicInteger outsideActionChanges = new AtomicInteger();
        outsideFocusTarget.addClickListener(event ->
                outsideActionStatus.setText(
                        "Input popup outside action: clicked client="
                                + event.isFromClient()
                                + " change="
                                + outsideActionChanges.incrementAndGet()));

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-input-popup-return");

        add(
                new H1("Flow owner-bound input popup acceptance"),
                comboBox,
                comboValueStatus,
                select,
                selectValueStatus,
                selectLifecycleStatus,
                datePicker,
                dateValueStatus,
                dateLifecycleStatus,
                timePicker,
                timeValueStatus,
                pickerValidation,
                pickerValidationStatus,
                pickerValidationEventStatus,
                outsideFocusTarget,
                outsideActionStatus,
                returnToFramework);
    }

    private static String validationError(DateTimePicker picker) {
        String errorMessage = picker.getErrorMessage();
        return errorMessage == null || errorMessage.isBlank()
                ? "none"
                : errorMessage;
    }

    private static final class LifecycleSelect<T> extends Select<T> {
        private void observeLifecycle(
                Span lifecycleStatus,
                AtomicInteger lifecycleChanges) {
            addOpenedChangeListener(event ->
                    lifecycleStatus.setText(
                            "Input popup Select lifecycle: "
                                    + (event.isOpened()
                                    ? "opened" : "closed")
                                    + " client=" + event.isFromClient()
                                    + " change="
                                    + lifecycleChanges.incrementAndGet()));
        }
    }
}
