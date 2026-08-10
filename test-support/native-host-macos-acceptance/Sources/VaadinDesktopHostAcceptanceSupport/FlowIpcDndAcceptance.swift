import AppKit
import CoreGraphics
import Foundation
import WebKit

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleFlowDndAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcDndAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

enum FlowIpcDndAcceptanceScripts {
    static let dragStartAction = """
    (() => {
      const source = document.querySelector('#framework-flow-dnd-source');
      const target = document.querySelector('#framework-flow-dnd-target');
      if (!source || !target || source.draggable !== true) return false;
      source.scrollIntoView({ block: 'center', inline: 'center' });
      const data = new Map();
      const transfer = {
        effectAllowed: 'uninitialized',
        dropEffect: 'none',
        setData(type, value) {
          data.set(String(type), String(value));
        },
        getData(type) {
          return data.get(String(type)) || '';
        },
        clearData(type) {
          if (type === undefined) data.clear();
          else data.delete(String(type));
        },
        setDragImage() {}
      };
      const event = new Event('dragstart', {
        bubbles: true,
        cancelable: true,
        composed: true
      });
      Object.defineProperty(event, 'dataTransfer', { value: transfer });
      const record = {
        transfer,
        sourceConfigured: source.__effectAllowed === 'move',
        targetConfigured:
          target.__active === true && target.__dropEffect === 'move'
      };
      window.VaadinDesktopFlowDnd = record;
      source.dispatchEvent(event);
      record.dragStartClassObserved =
        source.classList.contains('v-dragged');
      record.effectAllowedAfterStart = transfer.effectAllowed;
      return record.sourceConfigured
        && record.targetConfigured
        && record.dragStartClassObserved
        && record.effectAllowedAfterStart === 'move';
    })()
    """

    static let dropAction = """
    (() => {
      const target = document.querySelector('#framework-flow-dnd-target');
      const record = window.VaadinDesktopFlowDnd;
      const transfer = record?.transfer;
      if (!target || !record || !transfer) return false;
      const eventWithTransfer = (type) => {
        const event = new Event(type, {
          bubbles: true,
          cancelable: true,
          composed: true
        });
        Object.defineProperty(event, 'dataTransfer', { value: transfer });
        return event;
      };
      target.dispatchEvent(eventWithTransfer('dragenter'));
      record.dragOverClassObserved =
        target.classList.contains('v-drag-over-target');
      target.dispatchEvent(eventWithTransfer('dragover'));
      const drop = eventWithTransfer('drop');
      target.dispatchEvent(drop);
      record.dropDefaultPrevented = drop.defaultPrevented;
      record.dropEffectAfterDrop = transfer.dropEffect;
      record.dragOverClassCleared =
        !target.classList.contains('v-drag-over-target');
      return record.dragOverClassObserved
        && record.dropDefaultPrevented
        && record.dropEffectAfterDrop === 'move'
        && record.dragOverClassCleared;
    })()
    """

    static let dragEndAction = """
    (() => {
      const source = document.querySelector('#framework-flow-dnd-source');
      const record = window.VaadinDesktopFlowDnd;
      const transfer = record?.transfer;
      if (!source || !record || !transfer) return false;
      const event = new Event('dragend', {
        bubbles: true,
        cancelable: true,
        composed: true
      });
      Object.defineProperty(event, 'dataTransfer', { value: transfer });
      source.dispatchEvent(event);
      record.draggedClassCleared =
        !source.classList.contains('v-dragged');
      return record.draggedClassCleared;
    })()
    """
}

private final class FlowIpcDndAcceptance {
    private enum Phase {
        case ready
        case dedicatedRoute
        case nativePointerGesture
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private let inputMode: InputMode
    private var phase = Phase.ready
    private var configuredObserved = false
    private var scriptedBrowserEventsObserved = false
    private var pointerEventsPosted = false
    private var nativePointerObserved = false
    private var trustedBrowserEventsObserved = false
    private var dragStartObserved = false
    private var dropObserved = false
    private var dragEndObserved = false

    init(context: FlowIpcWindowContext, deadline: Date) {
        self.context = context
        self.deadline = deadline
        self.inputMode = InputMode.current
    }

    func start() {
        poll()
    }

    private func poll() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(code: "flowDndInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "flowDndBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "flowDndReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.navigateToDedicatedRoute()
            case .dedicatedRoute:
                guard snapshot.dedicatedRouteReady else {
                    self.retryOrFail(
                        code: "flowDndDedicatedRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.configuredObserved = snapshot.configuredObserved
                if self.inputMode == .scriptedBrowserEvents {
                    self.performScriptedBrowserGesture()
                } else {
                    self.prepareNativePointerGesture()
                }
            case .nativePointerGesture:
                let cycleObserved = self.inputMode == .scriptedBrowserEvents
                    ? snapshot.componentCycleObserved
                    : snapshot.pointerCycleObserved
                guard cycleObserved else {
                    self.retryOrFail(
                        code: self.inputMode == .scriptedBrowserEvents
                            ? "flowDndScriptedBrowserTimeout"
                            : "flowDndNativePointerTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.nativePointerObserved =
                    self.pointerEventsPosted && snapshot.nativePointerObserved
                self.trustedBrowserEventsObserved =
                    snapshot.trustedBrowserEventsObserved
                self.dragStartObserved = snapshot.dragStartObserved
                self.dropObserved = snapshot.dropObserved
                self.dragEndObserved = snapshot.dragEndObserved
                self.finish(snapshot)
            }
        }
    }

    private func performScriptedBrowserGesture() {
        phase = .nativePointerGesture
        context.webView.evaluateJavaScript(
            FlowIpcDndAcceptanceScripts.dragStartAction
        ) { result, error in
            guard error == nil, result as? Bool == true else {
                self.fail(
                    code: "flowDndScriptedDragStartFailed",
                    details: ["message": error?.localizedDescription ?? ""])
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                self.context.webView.evaluateJavaScript(
                    FlowIpcDndAcceptanceScripts.dropAction
                ) { result, error in
                    guard error == nil, result as? Bool == true else {
                        self.fail(
                            code: "flowDndScriptedDropFailed",
                            details: [
                                "message": error?.localizedDescription ?? ""
                            ])
                        return
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                        self.context.webView.evaluateJavaScript(
                            FlowIpcDndAcceptanceScripts.dragEndAction
                        ) { result, error in
                            guard error == nil, result as? Bool == true else {
                                self.fail(
                                    code: "flowDndScriptedDragEndFailed",
                                    details: [
                                        "message":
                                            error?.localizedDescription ?? ""
                                    ])
                                return
                            }
                            self.scriptedBrowserEventsObserved = true
                            DispatchQueue.main.asyncAfter(
                                deadline: .now() + 0.5
                            ) {
                                self.poll()
                            }
                        }
                    }
                }
            }
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                completion(nil)
                return
            }
            completion(snapshot)
        }
    }

    private func prepareNativePointerGesture() {
        context.webView.evaluateJavaScript(Self.preparationScript) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(
                    PointerGeometry.self,
                    from: data) else {
                self.retryOrFail(
                    code: "flowDndNativePointerPreparationFailed",
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }

            self.activateAndPerformNativePointerGesture(geometry)
        }
    }

    private func activateAndPerformNativePointerGesture(
        _ geometry: PointerGeometry
    ) {
        context.window.makeKeyAndOrderFront(nil)
        NSRunningApplication.current.activate(options: [.activateAllWindows])
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            guard NSApplication.shared.isActive,
                  self.context.window.isKeyWindow else {
                guard Date() < self.deadline else {
                    self.fail(
                        code: "flowDndNativePointerActivationTimeout",
                        details: [
                            "activeApplication": NSApplication.shared.isActive,
                            "keyWindow": self.context.window.isKeyWindow
                        ])
                    return
                }
                self.activateAndPerformNativePointerGesture(geometry)
                return
            }
            self.phase = .nativePointerGesture
            NativePointerDriver(
                window: self.context.window,
                webView: self.context.webView
            ).perform(geometry) { result in
                switch result {
                case .success:
                    self.pointerEventsPosted = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                        self.poll()
                    }
                case .failure(let error):
                    self.fail(
                        code: "flowDndNativePointerDispatchFailed",
                        details: ["message": error.localizedDescription])
                }
            }
        }
    }

    private func navigateToDedicatedRoute() {
        context.webView.evaluateJavaScript(Self.navigationScript) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: "flowDndDedicatedRouteDispatchFailed",
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.phase = .dedicatedRoute
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.poll()
            }
        }
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:]
    ) {
        if Date() >= deadline {
            fail(code: code, details: details)
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.poll()
        }
    }

    private func finish(_ snapshot: Snapshot) {
        let inputObserved = inputMode == .scriptedBrowserEvents
            ? scriptedBrowserEventsObserved
            : nativePointerObserved && trustedBrowserEventsObserved
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": snapshot.errorFree
                && configuredObserved
                && snapshot.dedicatedRouteReady
                && inputObserved
                && dragStartObserved
                && dropObserved
                && dragEndObserved,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 2,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 2,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "errorFree": snapshot.errorFree,
            "flowDndInputMode": inputMode.rawValue,
            "flowDndDedicatedRouteObserved": snapshot.dedicatedRouteReady,
            "flowDndConfiguredObserved": configuredObserved,
            "flowDndBrowserProgrammaticObserved":
                scriptedBrowserEventsObserved,
            "flowDndNativePointerObserved": nativePointerObserved,
            "flowDndTrustedBrowserEventsObserved": trustedBrowserEventsObserved,
            "flowDndDragStartObserved": dragStartObserved,
            "flowDndDropObserved": dropObserved,
            "flowDndDragEndObserved": dragEndObserved
        ]) { _, replacement in replacement }
        trace("recv", "flowDnd.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(code: String, details: [String: Any] = [:]) {
        var payload = details
        payload["code"] = code
        payload["phase"] = String(describing: phase)
        trace("send", "error", payload)
        context.window.close()
    }

    private struct PointerGeometry: Decodable {
        let source: Point
        let target: Point

        struct Point: Decodable {
            let x: Double
            let y: Double
        }
    }

    private struct Snapshot: Decodable {
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let dedicatedRouteReady: Bool
        let path: String
        let configuredObserved: Bool
        let nativePointerObserved: Bool
        let trustedBrowserEventsObserved: Bool
        let dragStartObserved: Bool
        let dropObserved: Bool
        let dragEndObserved: Bool
        let documentPointerDownTrusted: Bool
        let documentPointerDownTarget: String
        let documentPointerDownX: Double
        let documentPointerDownY: Double
        let pointerDownTrusted: Bool
        let dragStartTrusted: Bool
        let dragEnterTrusted: Bool
        let dragOverTrusted: Bool
        let dropTrusted: Bool
        let dragEndTrusted: Bool
        let dragStartStatus: String
        let dropStatus: String
        let dragEndStatus: String
        let eventSequence: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        var errorFree: Bool {
            browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
        }

        var ready: Bool {
            rootRendered
                && firstMeaningfulPaint
                && configuredObserved
                && requestCount >= 2
                && responseCount >= 2
                && pushMessageCount >= 1
                && flowClientCount > 0
                && errorFree
        }

        var componentCycleObserved: Bool {
            dragStartObserved && dropObserved && dragEndObserved
        }

        var pointerCycleObserved: Bool {
            nativePointerObserved
                && trustedBrowserEventsObserved
                && componentCycleObserved
        }

        var tracePayload: [String: Any] {
            [
                "rootRendered": rootRendered,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "flowDndDedicatedRouteReady": dedicatedRouteReady,
                "flowDndPath": path,
                "flowDndConfiguredObserved": configuredObserved,
                "flowDndNativePointerObserved": nativePointerObserved,
                "flowDndTrustedBrowserEventsObserved": trustedBrowserEventsObserved,
                "flowDndDocumentPointerDownTrusted":
                    documentPointerDownTrusted,
                "flowDndDocumentPointerDownTarget":
                    documentPointerDownTarget,
                "flowDndDocumentPointerDownX": documentPointerDownX,
                "flowDndDocumentPointerDownY": documentPointerDownY,
                "flowDndPointerDownTrusted": pointerDownTrusted,
                "flowDndDragStartTrusted": dragStartTrusted,
                "flowDndDragEnterTrusted": dragEnterTrusted,
                "flowDndDragOverTrusted": dragOverTrusted,
                "flowDndDropTrusted": dropTrusted,
                "flowDndDragEndTrusted": dragEndTrusted,
                "flowDndDragStartObserved": dragStartObserved,
                "flowDndDropObserved": dropObserved,
                "flowDndDragEndObserved": dragEndObserved,
                "flowDndDragStartStatus": dragStartStatus,
                "flowDndDropStatus": dropStatus,
                "flowDndDragEndStatus": dragEndStatus,
                "flowDndEventSequence": eventSequence,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private static let preparationScript = """
    (() => {
      const source = document.querySelector('#framework-flow-dnd-source');
      const target = document.querySelector('#framework-flow-dnd-target');
      if (!source || !target || source.draggable !== true) return '';

      source.scrollIntoView({ block: 'center', inline: 'nearest' });
      const sourceRect = source.getBoundingClientRect();
      const targetRect = target.getBoundingClientRect();
      const visible = (rect) =>
        rect.width > 0
          && rect.height > 0
          && rect.left >= 0
          && rect.top >= 0
          && rect.right <= window.innerWidth
          && rect.bottom <= window.innerHeight;
      if (!visible(sourceRect) || !visible(targetRect)) return '';

      const containsPointTarget = (element, pointTarget) =>
        pointTarget === element || element.contains(pointTarget);
      const pointTarget = (rect) => document.elementFromPoint(
        rect.left + rect.width / 2,
        rect.top + rect.height / 2
      );
      if (!containsPointTarget(source, pointTarget(sourceRect))
          || !containsPointTarget(target, pointTarget(targetRect))) {
        return '';
      }

      const record = {
        sourceConfigured: source.__effectAllowed === 'move',
        targetConfigured:
          target.__active === true && target.__dropEffect === 'move',
        events: []
      };
      const observe = (element, type, handler) => {
        element.addEventListener(type, (event) => {
          if (record.events.length < 32) {
            record.events.push(`${type}:${event.isTrusted}`);
          }
          handler(event);
        });
      };
      document.addEventListener('mousedown', (event) => {
        record.documentPointerDownTrusted = event.isTrusted;
        record.documentPointerDownTarget =
          event.target?.id || event.target?.tagName || '';
        record.documentPointerDownX = event.clientX;
        record.documentPointerDownY = event.clientY;
        if (record.events.length < 32) {
          record.events.push(
            `document-mousedown:${event.isTrusted}:${record.documentPointerDownTarget}`
          );
        }
      }, { capture: true });
      for (const type of ['dragenter', 'dragover', 'drop']) {
        document.addEventListener(type, (event) => {
          if (record.events.length < 32) {
            const targetId = event.target?.id || event.target?.tagName || '';
            record.events.push(
              `document-${type}:${event.isTrusted}:${targetId}:${event.clientX}:${event.clientY}`
            );
          }
        }, { capture: true });
      }
      observe(source, 'mousedown', (event) => {
        record.pointerDownTrusted =
          record.pointerDownTrusted === true || event.isTrusted;
      });
      observe(source, 'dragstart', (event) => {
        record.dragStartTrusted =
          record.dragStartTrusted === true || event.isTrusted;
        record.dragStartClassObserved =
          record.dragStartClassObserved === true
            || source.classList.contains('v-dragged');
        record.effectAllowedAfterStart = event.dataTransfer?.effectAllowed || '';
      });
      observe(target, 'dragenter', (event) => {
        record.dragEnterTrusted =
          record.dragEnterTrusted === true || event.isTrusted;
        record.dragOverClassObserved =
          record.dragOverClassObserved === true
            || target.classList.contains('v-drag-over-target');
      });
      observe(target, 'dragover', (event) => {
        record.dragOverTrusted =
          record.dragOverTrusted === true || event.isTrusted;
        record.dragOverClassObserved =
          record.dragOverClassObserved === true
            || target.classList.contains('v-drag-over-target');
        record.dropEffectDuringDragOver = event.dataTransfer?.dropEffect || '';
      });
      observe(target, 'drop', (event) => {
        record.dropTrusted = record.dropTrusted === true || event.isTrusted;
        record.dropDefaultPrevented = event.defaultPrevented;
        record.dropEffectAfterDrop = event.dataTransfer?.dropEffect || '';
        record.dragOverClassCleared =
          !target.classList.contains('v-drag-over-target');
      });
      observe(source, 'dragend', (event) => {
        record.dragEndTrusted =
          record.dragEndTrusted === true || event.isTrusted;
        record.draggedClassCleared =
          !source.classList.contains('v-dragged');
        record.dropEffectAfterDragEnd = event.dataTransfer?.dropEffect || '';
      });
      window.VaadinDesktopFlowDndPointer = record;

      return JSON.stringify({
        source: {
          x: sourceRect.left + sourceRect.width / 2,
          y: sourceRect.top + sourceRect.height / 2
        },
        target: {
          x: targetRect.left + targetRect.width / 2,
          y: targetRect.top + targetRect.height / 2
        }
      });
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const source = document.querySelector('#framework-flow-dnd-source');
      const target = document.querySelector('#framework-flow-dnd-target');
      const scriptedRecord = window.VaadinDesktopFlowDnd || null;
      const pointerRecord = window.VaadinDesktopFlowDndPointer || null;
      const record = pointerRecord || scriptedRecord || {};
      const scriptedBrowserEvents = scriptedRecord !== null
        && pointerRecord === null;
      const dragStartStatus =
        document.querySelector('#framework-flow-dnd-start-status')?.textContent || '';
      const dropStatus =
        document.querySelector('#framework-flow-dnd-drop-status')?.textContent || '';
      const dragEndStatus =
        document.querySelector('#framework-flow-dnd-end-status')?.textContent || '';
      const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients
        ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
        : [];
      const pointerDownTrusted = record.pointerDownTrusted === true;
      const dragStartTrusted = record.dragStartTrusted === true;
      const dragEnterTrusted = record.dragEnterTrusted === true;
      const dragOverTrusted = record.dragOverTrusted === true;
      const dropTrusted = record.dropTrusted === true;
      const dragEndTrusted = record.dragEndTrusted === true;
      const trustedBrowserEventsObserved =
        dragStartTrusted
          && dragEnterTrusted
          && dragOverTrusted
          && dropTrusted
          && dragEndTrusted;
      return JSON.stringify({
        rootRendered:
          !!document.querySelector('#framework-root')
            || !!document.querySelector('#framework-flow-dnd-route'),
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        dedicatedRouteReady:
          window.location.pathname.endsWith('/flow-dnd')
            && !!document.querySelector('#framework-flow-dnd-route'),
        path: window.location.pathname,
        configuredObserved:
          source?.draggable === true
            && source?.__effectAllowed === 'move'
            && target?.__active === true
            && target?.__dropEffect === 'move',
        documentPointerDownTrusted:
          record.documentPointerDownTrusted === true,
        documentPointerDownTarget: record.documentPointerDownTarget || '',
        documentPointerDownX: Number(record.documentPointerDownX || 0),
        documentPointerDownY: Number(record.documentPointerDownY || 0),
        nativePointerObserved: pointerDownTrusted,
        trustedBrowserEventsObserved,
        pointerDownTrusted,
        dragStartTrusted,
        dragEnterTrusted,
        dragOverTrusted,
        dropTrusted,
        dragEndTrusted,
        dragStartObserved:
          dragStartStatus.includes(
            'Flow DnD drag start: source=framework-flow-dnd-source')
            && dragStartStatus.includes('data=flow-dnd-payload')
            && dragStartStatus.includes('effectAllowed=MOVE')
            && dragStartStatus.includes('client=true change=1')
            && (scriptedBrowserEvents || dragStartTrusted)
            && record.dragStartClassObserved === true
            && record.effectAllowedAfterStart === 'move',
        dropObserved:
          dropStatus.includes(
            'Flow DnD drop: source=framework-flow-dnd-source')
            && dropStatus.includes('data=flow-dnd-payload')
            && (dropStatus.includes('effectAllowed=MOVE')
              || (!scriptedBrowserEvents
                && dropStatus.includes('effectAllowed=COPY_MOVE')))
            && dropStatus.includes('dropEffect=MOVE')
            && dropStatus.includes('client=true change=1')
            && (scriptedBrowserEvents
              || (dragEnterTrusted && dragOverTrusted && dropTrusted))
            && record.dragOverClassObserved === true
            && record.dropDefaultPrevented === true
            && record.dropEffectAfterDrop === 'move'
            && record.dragOverClassCleared === true,
        dragEndObserved:
          dragEndStatus.includes(
            'Flow DnD drag end: dropEffect=MOVE successful=true')
            && dragEndStatus.includes('client=true change=1')
            && record.draggedClassCleared === true
            && (scriptedBrowserEvents
              || (dragEndTrusted
                && record.dropEffectAfterDragEnd === 'move')),
        dragStartStatus,
        dropStatus,
        dragEndStatus,
        eventSequence: Array.isArray(record.events)
          ? record.events.join(',')
          : '',
        browserError: diagnostics.standardBundleBrowserError || '',
        requestError: diagnostics.standardBundleOpaqueRequestError || '',
        responseError: diagnostics.standardBundleOpaqueResponseError || '',
        pushError: diagnostics.standardBundlePushError || ''
      });
    })()
    """

    private static let navigationScript = """
    (() => {
      const element = document.querySelector('#framework-flow-dnd-open');
      if (!element || typeof element.click !== 'function') return false;
      element.click();
      return true;
    })()
    """

    private enum InputMode: String {
        case scriptedBrowserEvents = "scripted-browser-events"
        case appKitEventQueue = "appkit-event-queue-pointer"

        static var current: InputMode {
            let value = ProcessInfo.processInfo.environment[
                "VAADIN_DESKTOP_FLOW_IPC_DND_INPUT_MODE"]
            return InputMode(rawValue: value ?? "") ?? .scriptedBrowserEvents
        }
    }

    private final class NativePointerDriver {
        private let window: NSWindow
        private let webView: WKWebView

        init(window: NSWindow, webView: WKWebView) {
            self.window = window
            self.webView = webView
        }

        func perform(
            _ geometry: PointerGeometry,
            completion: @escaping (Result<Void, Error>) -> Void
        ) {
            guard window.isVisible, window.windowNumber > 0 else {
                completion(.failure(DriverError.windowUnavailable))
                return
            }
            let source = windowPoint(geometry.source)
            let target = windowPoint(geometry.target)
            guard window.contentLayoutRect.contains(source),
                  window.contentLayoutRect.contains(target) else {
                completion(.failure(DriverError.pointerOutsideWindow))
                return
            }

            let sourceScreen = quartzPoint(
                window.convertPoint(toScreen: source))
            let targetScreen = quartzPoint(
                window.convertPoint(toScreen: target))
            trace("recv", "flowDnd.nativePointerDispatch", [
                "inputMode": InputMode.appKitEventQueue.rawValue,
                "webViewFlipped": webView.isFlipped,
                "webViewWidth": webView.bounds.width,
                "webViewHeight": webView.bounds.height,
                "sourceWindowX": source.x,
                "sourceWindowY": source.y,
                "targetWindowX": target.x,
                "targetWindowY": target.y,
                "sourceScreenX": sourceScreen.x,
                "sourceScreenY": sourceScreen.y,
                "targetScreenX": targetScreen.x,
                "targetScreenY": targetScreen.y
            ])
            let originalPointer = CGEvent(source: nil)?.location
            var events: [PointerEvent] = [
                PointerEvent(type: .mouseMoved, screenPoint: sourceScreen),
                PointerEvent(type: .leftMouseDown, screenPoint: sourceScreen)
            ]
            let thresholdProgress = min(
                1,
                8 / max(abs(target.x - source.x), 1))
            events.append(PointerEvent(
                type: .leftMouseDragged,
                screenPoint: CGPoint(
                    x: sourceScreen.x
                        + (targetScreen.x - sourceScreen.x)
                            * thresholdProgress,
                    y: sourceScreen.y
                        + (targetScreen.y - sourceScreen.y)
                            * thresholdProgress)))
            for progress in [0.25, 0.5, 0.75, 1.0, 1.0, 1.0] {
                events.append(PointerEvent(
                    type: .leftMouseDragged,
                    screenPoint: CGPoint(
                        x: sourceScreen.x
                            + (targetScreen.x - sourceScreen.x) * progress,
                        y: sourceScreen.y
                            + (targetScreen.y - sourceScreen.y) * progress)))
            }
            events.append(PointerEvent(
                type: .leftMouseUp,
                screenPoint: targetScreen))
            guard events.count >= 4 else {
                completion(.failure(DriverError.eventCreationFailed))
                return
            }
            DispatchQueue.global(qos: .userInitiated).async {
                guard let eventSource = CGEventSource(stateID: .hidSystemState)
                else {
                    DispatchQueue.main.async {
                        completion(.failure(DriverError.eventCreationFailed))
                    }
                    return
                }
                for (index, specification) in events.enumerated() {
                    guard let event = CGEvent(
                        mouseEventSource: eventSource,
                        mouseType: specification.type,
                        mouseCursorPosition: specification.screenPoint,
                        mouseButton: .left) else {
                        DispatchQueue.main.async {
                            completion(.failure(DriverError.eventCreationFailed))
                        }
                        return
                    }
                    event.setIntegerValueField(.mouseEventClickState, value: 1)
                    if specification.type == .leftMouseDown
                        || specification.type == .leftMouseDragged {
                        event.setDoubleValueField(.mouseEventPressure, value: 1)
                    }
                    event.post(tap: .cghidEventTap)
                    let isLastDrag = index == events.count - 2
                    Thread.sleep(forTimeInterval: isLastDrag ? 0.25 : 0.12)
                }
                Thread.sleep(forTimeInterval: 0.5)
                if let originalPointer {
                    CGWarpMouseCursorPosition(originalPointer)
                }
                DispatchQueue.main.async {
                    completion(.success(()))
                }
            }
        }

        private func windowPoint(_ point: PointerGeometry.Point) -> NSPoint {
            let webViewPoint = NSPoint(
                x: point.x,
                y: webView.isFlipped
                    ? point.y
                    : webView.bounds.height - point.y)
            return webView.convert(webViewPoint, to: nil)
        }

        private func quartzPoint(_ appKitPoint: NSPoint) -> CGPoint {
            let primaryScreenTop = NSScreen.screens.first?.frame.maxY ?? 0
            return CGPoint(
                x: appKitPoint.x,
                y: primaryScreenTop - appKitPoint.y)
        }

        private struct PointerEvent {
            let type: CGEventType
            let screenPoint: CGPoint
        }

        private enum DriverError: LocalizedError {
            case windowUnavailable
            case pointerOutsideWindow
            case eventCreationFailed

            var errorDescription: String? {
                switch self {
                case .windowUnavailable:
                    return "Flow DnD acceptance window is not available"
                case .pointerOutsideWindow:
                    return "Flow DnD pointer geometry is outside the host window"
                case .eventCreationFailed:
                    return "Unable to create an AppKit pointer event"
                }
            }
        }
    }
}
