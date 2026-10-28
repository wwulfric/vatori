package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Route("flow-localized-number-validation")
public final class FlowLocalizedNumberValidationRoute extends VerticalLayout {
    private static final String PREFIX =
            "framework-flow-localized-number-validation-";
    private static final BigDecimal MAXIMUM = new BigDecimal("100");

    private final BigDecimalField amount =
            new BigDecimalField("Validated decimal amount");
    private final AmountForm form = new AmountForm(new BigDecimal("42.50"));
    private final Binder<AmountForm> binder = new Binder<>();
    private final Span valueStatus = status("value-status");
    private final Span valueHistoryStatus = status("value-history");
    private final Span validationStatus = status("validation-status");
    private final Span validationHistoryStatus = status("validation-history");
    private final Span binderStatus = status("binder-status");
    private final Span binderHistoryStatus = status("binder-history");
    private final List<String> valueHistory = new ArrayList<>();
    private final List<String> validationHistory = new ArrayList<>();
    private final List<String> binderHistory = new ArrayList<>();
    private int valueChanges;
    private int validationChanges;
    private int binderChanges;
    private boolean lastComponentValid = true;
    private boolean lastBinderErrors;
    private String lastValueSummary;

    public FlowLocalizedNumberValidationRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);

        amount.setId(PREFIX + "field");
        amount.setAriaLabel("Validated decimal amount");
        amount.setWidth("26em");
        amount.setLocale(Locale.GERMANY);
        amount.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        amount.setI18n(new BigDecimalField.BigDecimalFieldI18n()
                .setBadInputErrorMessage("Amount is malformed"));

        binder.forField(amount)
                .asRequired("Amount is required")
                .withValidator(value -> value == null
                                || value.compareTo(MAXIMUM) <= 0,
                        "Amount must be at most 100")
                .bind(AmountForm::getAmount, AmountForm::setAmount);
        binder.setBean(form);

        // Register observers after binding so initial field population is not
        // presented as user input. Binder retains its own normal listeners.
        lastValueSummary = valueSummary(amount.getValue(), null, false, 0);
        amount.addValueChangeListener(event -> {
            lastValueSummary = valueSummary(event.getValue(),
                    event.getOldValue(), event.isFromClient(), ++valueChanges);
            valueHistory.add(lastValueSummary);
            scheduleProjection();
        });
        amount.addValidationStatusChangeListener(event -> {
            lastComponentValid = event.getNewStatus();
            validationHistory.add("valid=" + lastComponentValid
                    + ", change=" + ++validationChanges);
            scheduleProjection();
        });
        binder.addStatusChangeListener(event -> {
            lastBinderErrors = event.hasValidationErrors();
            binderHistory.add("errors=" + lastBinderErrors
                    + ", change=" + ++binderChanges);
            scheduleProjection();
        });

        Span configStatus = status("config-status");
        configStatus.setText("Localized number validation config: "
                + "component=BigDecimalField, mode=" + amount.getValueChangeMode()
                + ", locale=" + amount.getLocale().toLanguageTag()
                + ", required=" + amount.isRequiredIndicatorVisible()
                + ", defaultValidators=" + binder.isDefaultValidatorsEnabled()
                + ", validationStatusListeners="
                + binder.isFieldsValidationStatusChangeListenerEnabled()
                + ", maximum=" + MAXIMUM.toPlainString()
                + ", initialPlain=" + plain(form.getAmount())
                + ", initialScale=" + scale(form.getAmount()));

        // An ordinary enabled next control gives Tab a stable blur target.
        // It has no listener that forces validation or modifies the model.
        Button continueButton = new Button("Continue");
        continueButton.setId(PREFIX + "focus-target");
        add(new H1("Flow localized decimal validation"), amount, continueButton,
                configStatus, valueStatus, valueHistoryStatus,
                validationStatus, validationHistoryStatus,
                binderStatus, binderHistoryStatus);
        projectState();
    }

    private void scheduleProjection() {
        // All component and Binder listeners finish before this read-only
        // projection. In particular, null-to-null malformed input need not
        // produce a value-change event to update the validation presentation.
        getUI().ifPresent(ui -> ui.beforeClientResponse(this,
                context -> projectState()));
    }

    private void projectState() {
        valueStatus.setText("Localized number validation value: "
                + lastValueSummary);
        valueHistoryStatus.setText("Localized number validation value history: "
                + history(valueHistory));
        validationStatus.setText("Localized number validation component: valid="
                + lastComponentValid + ", change=" + validationChanges);
        validationHistoryStatus.setText(
                "Localized number validation component history: "
                        + history(validationHistory));

        String rawError = errorMessage(amount.getErrorMessage());
        binderStatus.setText("Localized number validation binder: errors="
                + lastBinderErrors
                + ", beanPlain=" + plain(form.getAmount())
                + ", beanScale=" + scale(form.getAmount())
                + ", fieldPlain=" + plain(amount.getValue())
                + ", fieldScale=" + scale(amount.getValue())
                + ", invalid=" + amount.isInvalid()
                + ", error=" + (amount.isInvalid() ? rawError : "none")
                + ", rawError=" + rawError
                + ", change=" + binderChanges);
        binderHistoryStatus.setText("Localized number validation binder history: "
                + history(binderHistory));
    }

    private String valueSummary(BigDecimal value, BigDecimal oldValue,
            boolean client, int change) {
        return "plain=" + plain(value)
                + ", scale=" + scale(value)
                + ", locale=" + amount.getLocale().toLanguageTag()
                + ", client=" + client
                + ", change=" + change
                + ", oldPlain=" + plain(oldValue)
                + ", oldScale=" + scale(oldValue);
    }

    private static Span status(String suffix) {
        Span span = new Span();
        span.setId(PREFIX + suffix);
        return span;
    }

    private static String history(List<String> entries) {
        return entries.isEmpty() ? "pending" : String.join(" | ", entries);
    }

    private static String plain(BigDecimal value) {
        return value == null ? "none" : value.toPlainString();
    }

    private static String scale(BigDecimal value) {
        return value == null ? "none" : Integer.toString(value.scale());
    }

    private static String errorMessage(String message) {
        return message == null || message.isBlank() ? "none" : message;
    }

    private static final class AmountForm {
        private BigDecimal amount;

        private AmountForm(BigDecimal amount) {
            this.amount = amount;
        }

        private BigDecimal getAmount() {
            return amount;
        }

        private void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
