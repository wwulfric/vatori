package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-localized-number")
public final class FlowLocalizedNumberRoute extends VerticalLayout {
    public FlowLocalizedNumberRoute() {
        setId("framework-flow-localized-number-route");
        setPadding(true);
        setSpacing(true);

        BigDecimalField amount = new BigDecimalField(
                "Localized decimal amount");
        amount.setId("framework-flow-localized-number-field");
        amount.setAriaLabel("Localized decimal amount");
        amount.setWidth("26em");
        amount.setLocale(Locale.GERMANY);
        amount.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        amount.setValue(new BigDecimal("42.50"));

        Span configStatus = new Span(
                "Localized number config: component=BigDecimalField, mode="
                        + amount.getValueChangeMode()
                        + ", initialLocale=" + amount.getLocale().toLanguageTag()
                        + ", initialPlain=" + plain(amount.getValue())
                        + ", initialScale=" + scale(amount.getValue()));
        configStatus.setId(
                "framework-flow-localized-number-config-status");
        Span valueStatus = new Span(
                "Localized number value: " + valueSummary(
                        amount.getValue(), amount.getLocale(), false, 0, null));
        valueStatus.setId(
                "framework-flow-localized-number-value-status");
        Span switchStatus = new Span("Localized number switch: pending");
        switchStatus.setId(
                "framework-flow-localized-number-switch-status");
        Span historyStatus = new Span("Localized number history: pending");
        historyStatus.setId(
                "framework-flow-localized-number-history-status");

        AtomicInteger valueChanges = new AtomicInteger();
        AtomicInteger switches = new AtomicInteger();
        List<String> valueHistory = new ArrayList<>();
        amount.addValueChangeListener(event -> {
            String entry = valueSummary(
                    event.getValue(), amount.getLocale(), event.isFromClient(),
                    valueChanges.incrementAndGet(), event.getOldValue());
            valueStatus.setText("Localized number value: " + entry);
            valueHistory.add(entry);
            historyStatus.setText("Localized number history: "
                    + String.join(" | ", valueHistory));
        });

        Button switchLocale = new Button("Use en-US decimal separator", event -> {
            Locale beforeLocale = amount.getLocale();
            BigDecimal beforeValue = amount.getValue();
            int beforeChanges = valueChanges.get();
            amount.setLocale(Locale.US);
            BigDecimal afterValue = amount.getValue();
            switchStatus.setText("Localized number switch: beforeLocale="
                    + beforeLocale.toLanguageTag()
                    + ", afterLocale=" + amount.getLocale().toLanguageTag()
                    + ", beforePlain=" + plain(beforeValue)
                    + ", beforeScale=" + scale(beforeValue)
                    + ", afterPlain=" + plain(afterValue)
                    + ", afterScale=" + scale(afterValue)
                    + ", beforeChanges=" + beforeChanges
                    + ", afterChanges=" + valueChanges.get()
                    + ", client=" + event.isFromClient()
                    + ", switches=" + switches.incrementAndGet()
                    + ", sameValue=" + Objects.equals(beforeValue, afterValue)
                    + ", sameInstance=" + (beforeValue == afterValue));
        });
        switchLocale.setId("framework-flow-localized-number-switch");

        add(new H1("Flow localized decimal input"), amount, switchLocale,
                configStatus, valueStatus, switchStatus, historyStatus);
    }

    private static String valueSummary(
            BigDecimal value, Locale locale, boolean client,
            int change, BigDecimal oldValue) {
        return "plain=" + plain(value)
                + ", scale=" + scale(value)
                + ", locale=" + locale.toLanguageTag()
                + ", client=" + client
                + ", change=" + change
                + ", oldPlain=" + plain(oldValue)
                + ", oldScale=" + scale(oldValue);
    }

    private static String plain(BigDecimal value) {
        return value == null ? "none" : value.toPlainString();
    }

    private static String scale(BigDecimal value) {
        return value == null ? "none" : Integer.toString(value.scale());
    }
}
