package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadFormat;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopWindow;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Public API mixed-transfer application. Each window owns its payload seed and counters. */
@Route("flow-transfer-endurance")
public final class FlowTransferEnduranceRoute extends VerticalLayout {
    static final int LARGE = 256 * 1024 * 1024;
    static final int SMALL = 4 * 1024 * 1024;
    private final String owner = UUID.randomUUID().toString();
    private final int seed = UUID.randomUUID().hashCode() & 255;
    private final AtomicInteger active = new AtomicInteger();
    private final AtomicInteger invalid = new AtomicInteger();
    private final AtomicInteger rawSuccess = new AtomicInteger();
    private final AtomicInteger multipartSuccess = new AtomicInteger();
    private final AtomicInteger rawCancelled = new AtomicInteger();
    private final AtomicInteger downloads = new AtomicInteger();
    private final AtomicInteger ranges = new AtomicInteger();
    private final AtomicInteger downloadCancelled = new AtomicInteger();
    private final AtomicLong rawBytes = new AtomicLong();
    private final AtomicLong multipartBytes = new AtomicLong();
    private final AtomicInteger removed = new AtomicInteger();
    private final AtomicInteger finished = new AtomicInteger();

    public FlowTransferEnduranceRoute() {
        setId("transfer-endurance-route");
        Span identity = new Span(owner);
        identity.setId("transfer-owner");
        identity.getElement().setAttribute("data-seed", Integer.toString(seed));
        Span status = new Span("pending"); status.setId("transfer-status");
        Span windowStatus = new Span("pending"); windowStatus.setId("transfer-window-status");
        Button open = new Button("Open transfer window", e -> windowStatus.setText("open=" + Desktop.current().windows()
                .open(DesktopWindow.builder("transfer-secondary").title("Transfer secondary")
                        .size(800, 650).route("flow-transfer-endurance").build()).success()));
        open.setId("transfer-open");
        Button close = new Button("Close transfer window", e -> windowStatus.setText("close=" +
                Desktop.current().windows().close("transfer-secondary").success()));
        close.setId("transfer-close");
        Button confirm = new Button("Observe transfers", e -> status.setText(
                "owner=" + owner + ";raw=" + rawSuccess + ";multipart=" + multipartSuccess
                        + ";rawBytes=" + rawBytes + ";multipartBytes=" + multipartBytes
                        + ";cancelled=" + rawCancelled + ";downloads=" + downloads + ";ranges=" + ranges
                        + ";downloadCancelled=" + downloadCancelled + ";active=" + active
                        + ";invalid=" + invalid + ";removed=" + removed + ";finished=" + finished
                        + ";tempEmpty=" + FlowMultipartAcceptanceStorage.isEmpty() + ";client=" + e.isFromClient()));
        confirm.setId("transfer-confirm");
        Anchor download = new Anchor(new DownloadHandler() {
            @Override public void handleDownloadRequest(DownloadEvent event) throws IOException {
                int size;
                try { size = checkedSize(event.getRequest().getParameter("bytes")); }
                catch (RuntimeException ex) { event.getResponse().setStatus(400); return; }
                String range = event.getRequest().getHeader("Range");
                long start = 0;
                if (range != null) {
                    try {
                        if (!range.matches("bytes=[0-9]+-")) throw new IllegalArgumentException();
                        start = Long.parseLong(range.substring(6, range.length()-1));
                        if (start >= size) throw new IllegalArgumentException();
                    } catch (RuntimeException ex) { event.getResponse().setStatus(416); return; }
                    ranges.incrementAndGet(); event.getResponse().setStatus(206);
                    event.getResponse().setHeader("Content-Range", "bytes " + start + "-" + (size-1) + "/" + size);
                }
                event.getResponse().setHeader("Accept-Ranges", "bytes");
                event.setFileName("transfer.bin"); event.setContentType("application/octet-stream");
                event.setContentLength(size-start); active.incrementAndGet();
                try (var output = event.getOutputStream()) {
                    byte[] buffer = new byte[32768];
                    for (long offset=start;offset<size;) {
                        int count=(int)Math.min(buffer.length,size-offset);
                        for (int i=0;i<count;i++) buffer[i]=(byte)expected(offset+i,seed);
                        output.write(buffer,0,count); offset+=count;
                    }
                    downloads.incrementAndGet();
                } catch (IOException ex) { downloadCancelled.incrementAndGet(); throw ex; }
                finally { active.decrementAndGet(); }
            }
        }, "Download"); download.setId("transfer-download");
        add(identity, open, close, windowStatus, upload(false), upload(true), download, confirm, status);
    }

    private Upload upload(boolean multipart) {
        UploadHandler handler = new UploadHandler() {
            @Override public void handleUploadRequest(UploadEvent event) throws IOException {
                int size;
                try {
                    String name=event.getFileName();
                    if (!name.startsWith(owner + "-") || !name.endsWith(".bin")) throw new IllegalArgumentException();
                    size=checkedSize(name.substring(name.lastIndexOf('-')+1,name.length()-4));
                    if (event.getFileSize()!=size) throw new IllegalArgumentException();
                } catch (RuntimeException ex) { invalid.incrementAndGet(); throw new IOException("Invalid owner/size",ex); }
                active.incrementAndGet(); long count=0; boolean valid=true;
                try (var input=event.getInputStream()) {
                    byte[] buffer=new byte[32768]; int n;
                    while ((n=input.read(buffer))!=-1) {
                        for(int i=0;i<n;i++) if((buffer[i]&255)!=expected(count+i,seed)) valid=false;
                        count+=n;
                    }
                } catch(IOException ex) {
                    if(!multipart && count>0 && count<size && valid) rawCancelled.incrementAndGet();
                    else invalid.incrementAndGet();
                    throw ex;
                } finally { active.decrementAndGet(); }
                if(count!=size || !valid) { invalid.incrementAndGet(); throw new IOException("Invalid content"); }
                (multipart?multipartSuccess:rawSuccess).incrementAndGet();
                (multipart?multipartBytes:rawBytes).addAndGet(count);
            }
        };
        Upload upload=new Upload(handler); upload.setId(multipart?"transfer-multipart":"transfer-raw");
        upload.setUploadFormat(multipart?UploadFormat.MULTIPART:UploadFormat.RAW);
        upload.setAutoUpload(false); upload.setMaxFiles(1); upload.setMaxFileSize(LARGE);
        upload.addFileRemovedListener(e->removed.incrementAndGet());
        upload.addAllFinishedListener(e->finished.incrementAndGet());
        return upload;
    }
    static int expected(long offset,int seed) { return (int)((offset*31+seed)&255); }
    static int checkedSize(String value) {
        int size=Integer.parseInt(value);
        if(size!=LARGE && size!=SMALL) throw new IllegalArgumentException("Unsupported test size");
        return size;
    }
}
