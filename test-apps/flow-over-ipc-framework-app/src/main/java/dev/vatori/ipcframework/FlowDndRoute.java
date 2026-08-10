package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-dnd")
public final class FlowDndRoute extends VerticalLayout {
    public FlowDndRoute() {
        setId("framework-flow-dnd-route");
        setPadding(true);
        setSpacing(true);

        Span source = new Span("Drag Flow payload");
        source.setId("framework-flow-dnd-source");
        source.getStyle()
                .set("display", "inline-block")
                .set("padding", "12px")
                .set("border", "1px solid var(--lumo-primary-color)")
                .set("cursor", "grab");

        Span target = new Span("Drop Flow payload");
        target.setId("framework-flow-dnd-target");
        target.getStyle()
                .set("display", "inline-block")
                .set("min-width", "180px")
                .set("padding", "12px")
                .set("border", "1px dashed var(--lumo-contrast-50pct)");

        Span dragStartStatus = new Span("Flow DnD drag start: pending");
        dragStartStatus.setId("framework-flow-dnd-start-status");
        Span dropStatus = new Span("Flow DnD drop: pending");
        dropStatus.setId("framework-flow-dnd-drop-status");
        Span dragEndStatus = new Span("Flow DnD drag end: pending");
        dragEndStatus.setId("framework-flow-dnd-end-status");
        AtomicInteger dragStartChanges = new AtomicInteger();
        AtomicInteger dropChanges = new AtomicInteger();
        AtomicInteger dragEndChanges = new AtomicInteger();

        DragSource<Span> dragSource = DragSource.create(source);
        dragSource.setEffectAllowed(EffectAllowed.MOVE);
        dragSource.setDragData("flow-dnd-initial");
        dragSource.addDragStartListener(event -> {
            event.setDragData("flow-dnd-payload");
            dragStartStatus.setText(
                    "Flow DnD drag start: source=framework-flow-dnd-source"
                            + " data=flow-dnd-payload effectAllowed=MOVE"
                            + " client=" + event.isFromClient()
                            + " change="
                            + dragStartChanges.incrementAndGet());
        });
        dragSource.addDragEndListener(event -> {
            dragEndStatus.setText("Flow DnD drag end: dropEffect="
                    + event.getDropEffect() + " successful="
                    + event.isSuccessful()
                    + " client=" + event.isFromClient()
                    + " change="
                    + dragEndChanges.incrementAndGet());
            event.clearDragData();
        });

        DropTarget<Span> dropTarget = DropTarget.create(target);
        dropTarget.setDropEffect(DropEffect.MOVE);
        dropTarget.addDropListener(event -> {
            String sourceId = event.getDragSourceComponent()
                    .flatMap(component -> component.getId())
                    .orElse("none");
            String dragData = event.getDragData()
                    .map(String::valueOf)
                    .orElse("none");
            dropStatus.setText("Flow DnD drop: source=" + sourceId
                    + " data=" + dragData
                    + " effectAllowed=" + event.getEffectAllowed()
                    + " dropEffect=" + event.getDropEffect()
                    + " client=" + event.isFromClient()
                    + " change=" + dropChanges.incrementAndGet());
        });

        HorizontalLayout surface = new HorizontalLayout(source, target);
        surface.setId("framework-flow-dnd-surface");
        surface.setAlignItems(Alignment.CENTER);

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-dnd-return");

        add(
                new H1("Flow DnD pointer acceptance"),
                surface,
                dragStartStatus,
                dropStatus,
                dragEndStatus,
                returnToFramework);
    }
}
