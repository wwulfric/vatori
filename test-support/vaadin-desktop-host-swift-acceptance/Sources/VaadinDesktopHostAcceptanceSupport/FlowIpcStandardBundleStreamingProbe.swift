enum FlowIpcStandardBundleStreamingProbe {
    static func isEnabled(config: HostConfig) -> Bool {
        config.flowIpcAutoUploadReceiver && config.flowIpcAutoDownloadSaveOpen
    }

    static let source = #"""
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics =
        window.VaadinDesktopIpcDiagnostics || {};
      if (diagnostics.standardBundleStreamingProbeInstalled) {
        return;
      }
      diagnostics.standardBundleStreamingProbeInstalled = true;
      diagnostics.standardBundleStreamingProbeRequired = true;
      diagnostics.standardBundleStreamingProbeComplete = false;

      const delay = (milliseconds) => new Promise(
        (resolve) => setTimeout(resolve, milliseconds));

      async function waitFor(find, label, timeoutMillis = 30000) {
        const deadline = Date.now() + timeoutMillis;
        while (Date.now() < deadline) {
          const value = find();
          if (value) {
            return value;
          }
          await delay(50);
        }
        throw new Error(`standard streaming probe timed out waiting for ${label}`);
      }

      async function readResponse(response, pauseMillis = 0) {
        if (!response.ok || !response.body) {
          throw new Error(`streaming response failed with status ${response.status}`);
        }
        const reader = response.body.getReader();
        const chunks = [];
        let bytes = 0;
        for (;;) {
          const next = await reader.read();
          if (next.done) {
            break;
          }
          chunks.push(next.value);
          bytes += next.value.byteLength;
          if (pauseMillis > 0) {
            await delay(pauseMillis);
          }
        }
        const combined = new Uint8Array(bytes);
        let offset = 0;
        for (const chunk of chunks) {
          combined.set(chunk, offset);
          offset += chunk.byteLength;
        }
        return { bytes: combined, chunkCount: chunks.length };
      }

      async function verifyDownload(anchor) {
        const url = anchor.href || anchor.getAttribute('href');
        const response = await fetch(url);
        const expectedBytes = Number(response.headers.get('content-length') || 0);
        const result = await readResponse(response, 3);
        const prefix = new TextDecoder().decode(result.bytes.subarray(0, 96));
        diagnostics.standardBundleStreamingDownloadBytes = result.bytes.byteLength;
        diagnostics.standardBundleStreamingDownloadChunkCount = result.chunkCount;
        diagnostics.standardBundleStreamingDownloadComplete =
          result.bytes.byteLength >= 512 * 1024
          && (!expectedBytes || result.bytes.byteLength === expectedBytes)
          && prefix.includes('Flow IPC Standard Bundle Streaming Download');
        if (!diagnostics.standardBundleStreamingDownloadComplete) {
          throw new Error('standard streaming download body did not match');
        }

        const controller = new AbortController();
        const cancelledResponse = await fetch(url, { signal: controller.signal });
        const reader = cancelledResponse.body.getReader();
        const first = await reader.read();
        if (first.done || first.value.byteLength === 0) {
          throw new Error('cancel probe did not receive a response chunk');
        }
        const pendingRead = reader.read();
        controller.abort();
        let cancelled = false;
        try {
          await pendingRead;
        } catch (error) {
          cancelled = error && error.name === 'AbortError';
        }
        diagnostics.standardBundleStreamingDownloadCancelComplete = cancelled;
        if (!cancelled) {
          throw new Error('streaming download cancellation was not observed');
        }
      }

      function uploadUrl(upload) {
        const target = upload.target || upload.getAttribute('target') || '';
        if (!target) {
          throw new Error('standard streaming upload target is missing');
        }
        return new URL(target, document.baseURI).href;
      }

      async function verifyUpload(upload) {
        const marker = 'Flow IPC Upload Standard Streaming\n';
        const bodyBytes = 512 * 1024 + 31;
        const bytes = new Uint8Array(bodyBytes);
        bytes.fill(0x75);
        bytes.set(new TextEncoder().encode(marker));
        const response = await fetch(uploadUrl(upload), {
          method: 'POST',
          headers: {
            'Content-Type': 'text/plain; charset=UTF-8',
            'X-Filename': encodeURIComponent('standard-streaming-upload.txt')
          },
          body: new Blob([bytes], { type: 'text/plain; charset=UTF-8' })
        });
        await readResponse(response);
        const status = await waitFor(() => {
          const text = document.querySelector('#framework-upload-status')?.textContent || '';
          return text.includes(`bytes=${bodyBytes}`) && text.includes('marker=true') ? text : '';
        }, 'upload status');
        diagnostics.standardBundleStreamingUploadBytes = bodyBytes;
        diagnostics.standardBundleStreamingUploadStatus = status;
        diagnostics.standardBundleStreamingUploadComplete = true;

        const controller = new AbortController();
        let chunkIndex = 0;
        let producerCancelled = false;
        const slowBody = new ReadableStream({
          async pull(streamController) {
            await delay(25);
            if (producerCancelled) {
              return;
            }
            if (chunkIndex >= 64) {
              streamController.close();
              return;
            }
            const chunk = new Uint8Array(32 * 1024);
            chunk.fill(0x63);
            streamController.enqueue(chunk);
            chunkIndex += 1;
          },
          cancel() {
            producerCancelled = true;
          }
        });
        const uploadPromise = fetch(uploadUrl(upload), {
          method: 'POST',
          headers: {
            'Content-Type': 'application/octet-stream',
            'X-Filename': encodeURIComponent('cancelled-streaming-upload.bin')
          },
          body: slowBody,
          signal: controller.signal
        });
        setTimeout(() => controller.abort(), 90);
        let cancelled = false;
        try {
          await uploadPromise;
        } catch (error) {
          cancelled = error && error.name === 'AbortError';
        }
        diagnostics.standardBundleStreamingUploadCancelComplete =
          cancelled && producerCancelled && chunkIndex > 0;
        if (!diagnostics.standardBundleStreamingUploadCancelComplete) {
          throw new Error('streaming upload cancellation was not observed');
        }
      }

      (async () => {
        try {
          const upload = await waitFor(
            () => document.querySelector('#framework-upload'), 'Vaadin Upload');
          const download = await waitFor(
            () => document.querySelector('#framework-standard-streaming-download'),
            'streaming DownloadHandler');
          await verifyUpload(upload);
          await verifyDownload(download);
          diagnostics.standardBundleStreamingProbeComplete = true;
        } catch (error) {
          diagnostics.standardBundleStreamingProbeError = error?.message || String(error);
        }
      })();
    })();
    """#
}
