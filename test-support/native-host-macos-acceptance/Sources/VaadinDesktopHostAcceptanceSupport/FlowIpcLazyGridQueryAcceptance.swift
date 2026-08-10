import Foundation
import WebKit

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLazyGridQueryAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcLazyGridQueryAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcLazyGridQueryAcceptance {
    private enum Phase {
        case ready
        case farViewport
        case selection
        case returnViewport
        case sort
        case filter
        case filterReset
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.ready
    private var farViewportObserved = false
    private var selectionObserved = false
    private var returnViewportObserved = false
    private var sortObserved = false
    private var filterObserved = false
    private var filterResetObserved = false

    init(context: FlowIpcWindowContext, deadline: Date) {
        self.context = context
        self.deadline = deadline
    }

    func start() {
        poll()
    }

    private func poll() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(code: "lazyGridQueryInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "lazyGridQueryBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "lazyGridQueryReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.farViewportAction,
                    next: .farViewport,
                    failureCode: "lazyGridQueryFarViewportDispatchFailed")
            case .farViewport:
                guard snapshot.farViewportObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryFarViewportTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.farViewportObserved = true
                self.dispatch(
                    Self.selectionAction,
                    next: .selection,
                    failureCode: "lazyGridQuerySelectionDispatchFailed")
            case .selection:
                guard snapshot.selectionObserved else {
                    self.retryOrFail(
                        code: "lazyGridQuerySelectionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.selectionObserved = true
                self.dispatch(
                    Self.returnViewportAction,
                    next: .returnViewport,
                    failureCode: "lazyGridQueryReturnViewportDispatchFailed")
            case .returnViewport:
                guard snapshot.returnViewportObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryReturnViewportTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.returnViewportObserved = true
                self.dispatch(
                    Self.sortAction,
                    next: .sort,
                    failureCode: "lazyGridQuerySortDispatchFailed")
            case .sort:
                guard snapshot.sortObserved else {
                    self.retryOrFail(
                        code: "lazyGridQuerySortTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.sortObserved = true
                self.dispatch(
                    Self.filterAction,
                    next: .filter,
                    failureCode: "lazyGridQueryFilterDispatchFailed")
            case .filter:
                guard snapshot.filterObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryFilterTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.filterObserved = true
                self.dispatch(
                    Self.filterResetAction,
                    next: .filterReset,
                    failureCode: "lazyGridQueryFilterResetDispatchFailed")
            case .filterReset:
                guard snapshot.filterResetObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryFilterResetTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.filterResetObserved = true
                self.finish(snapshot)
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

    private func dispatch(
        _ script: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.phase = next
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
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
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": snapshot.errorFree
                && farViewportObserved
                && selectionObserved
                && returnViewportObserved
                && sortObserved
                && filterObserved
                && filterResetObserved,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 2,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 2,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "errorFree": snapshot.errorFree,
            "lazyGridFarViewportObserved": farViewportObserved,
            "lazyGridSelectionObserved": selectionObserved,
            "lazyGridReturnViewportObserved": returnViewportObserved,
            "lazyGridSortObserved": sortObserved,
            "lazyGridFilterObserved": filterObserved,
            "lazyGridFilterResetObserved": filterResetObserved
        ]) { _, replacement in replacement }
        trace("recv", "lazyGridQuery.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(code: String, details: [String: Any] = [:]) {
        var payload = details
        payload["code"] = code
        payload["phase"] = String(describing: phase)
        trace("send", "error", payload)
        context.window.close()
    }

    private struct Snapshot: Decodable {
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let queryStatus: String
        let countStatus: String
        let selectionStatus: String
        let filterValue: String
        let initialViewportObserved: Bool
        let farViewportObserved: Bool
        let selectionObserved: Bool
        let returnViewportObserved: Bool
        let sortObserved: Bool
        let filterObserved: Bool
        let filterResetObserved: Bool
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
                && initialViewportObserved
                && queryStatus.contains("sort=none")
                && queryStatus.contains("filter=all")
                && queryStatus.contains("matches=80")
                && countStatus.contains("filter=all")
                && countStatus.contains("matches=80")
                && requestCount >= 2
                && responseCount >= 2
                && flowClientCount > 0
                && errorFree
        }

        var tracePayload: [String: Any] {
            [
                "rootRendered": rootRendered,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "lazyGridQueryStatus": queryStatus,
                "lazyGridCountStatus": countStatus,
                "lazyGridSelectionStatus": selectionStatus,
                "lazyGridFilterValue": filterValue,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const grid = document.querySelector('#framework-lazy-grid');
      const queryStatus =
        document.querySelector('#framework-lazy-grid-query-status')?.textContent || '';
      const countStatus =
        document.querySelector('#framework-lazy-grid-count-status')?.textContent || '';
      const selectionStatus =
        document.querySelector('#framework-lazy-grid-selection-status')?.textContent || '';
      const filterValue = String(
        document.querySelector('#framework-lazy-grid-filter')?.value || '');
      const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients
        ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
        : [];
      const findDeepFrom = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const hasVisibleItem = (label) => {
        if (!grid) return false;
        const gridRect = grid.getBoundingClientRect();
        return !!findDeepFrom(grid, (element) => {
          if (element.localName !== 'vaadin-grid-cell-content'
              || (element.textContent || '').trim() !== label) {
            return false;
          }
          const rect = element.getBoundingClientRect();
          return rect.width > 0
            && rect.height > 0
            && rect.bottom > gridRect.top
            && rect.top < gridRect.bottom;
        });
      };
      return JSON.stringify({
        rootRendered: !!document.querySelector('#framework-root'),
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        queryStatus,
        countStatus,
        selectionStatus,
        filterValue,
        initialViewportObserved: hasVisibleItem('L-001'),
        farViewportObserved:
          hasVisibleItem('L-061')
            && !queryStatus.includes('range=0-'),
        selectionObserved:
          selectionStatus.includes('Lazy grid selection: L-061'),
        returnViewportObserved: hasVisibleItem('L-001'),
        sortObserved:
          queryStatus.includes('sort=owner:ascending')
            && queryStatus.includes('filter=all')
            && queryStatus.includes('matches=80')
            && hasVisibleItem('L-002'),
        filterObserved:
          filterValue === 'platform'
            && queryStatus.includes('sort=owner:ascending')
            && queryStatus.includes('filter=platform')
            && queryStatus.includes('matches=26')
            && countStatus.includes('filter=platform')
            && countStatus.includes('matches=26')
            && hasVisibleItem('L-003'),
        filterResetObserved:
          filterValue === ''
            && queryStatus.includes('sort=owner:ascending')
            && queryStatus.includes('filter=all')
            && queryStatus.includes('matches=80')
            && countStatus.includes('filter=all')
            && countStatus.includes('matches=80')
            && hasVisibleItem('L-002'),
        browserError: diagnostics.standardBundleBrowserError || '',
        requestError: diagnostics.standardBundleOpaqueRequestError || '',
        responseError: diagnostics.standardBundleOpaqueResponseError || '',
        pushError: diagnostics.standardBundlePushError || ''
      });
    })()
    """

    private static let farViewportAction = """
    (() => {
      const grid = document.querySelector('#framework-lazy-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(60);
      return true;
    })()
    """

    private static let selectionAction = """
    (() => {
      const grid = document.querySelector('#framework-lazy-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'vaadin-grid-cell-content'
            && (node.textContent || '').trim() === 'L-061') {
          return node;
        }
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child);
          if (result) return result;
        }
        return null;
      };
      const cell = visit(grid);
      if (!cell || typeof cell.click !== 'function') return false;
      cell.click();
      return true;
    })()
    """

    private static let returnViewportAction = """
    (() => {
      const grid = document.querySelector('#framework-lazy-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      grid.scrollToIndex(0);
      return true;
    })()
    """

    private static let sortAction = """
    (() => {
      const grid = document.querySelector('#framework-lazy-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'vaadin-grid-sorter'
            && (node.textContent || '').includes('Lazy owner')) {
          return node;
        }
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child);
          if (result) return result;
        }
        return null;
      };
      const sorter = visit(grid);
      if (!sorter || typeof sorter.click !== 'function') return false;
      sorter.click();
      setTimeout(() => grid.scrollToIndex?.(0), 400);
      return true;
    })()
    """

    private static let filterAction = setFilterAction("platform")
    private static let filterResetAction = setFilterAction("")

    private static func setFilterAction(_ value: String) -> String {
        """
        (() => {
          const field = document.querySelector('#framework-lazy-grid-filter');
          const grid = document.querySelector('#framework-lazy-grid');
          if (!field || !grid) return false;
          const value = '\(value)';
          const nativeInput = field.inputElement;
          field.scrollIntoView({ block: 'center', inline: 'center' });
          field.value = value;
          if (nativeInput) {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(nativeInput, value);
            } else {
              nativeInput.value = value;
            }
            nativeInput.dispatchEvent(
              new Event('input', { bubbles: true, composed: true }));
            nativeInput.dispatchEvent(
              new Event('change', { bubbles: true, composed: true }));
          }
          field.dispatchEvent(
            new Event('input', { bubbles: true, composed: true }));
          field.dispatchEvent(new CustomEvent('value-changed', {
            bubbles: true,
            composed: true,
            detail: { value }
          }));
          field.dispatchEvent(
            new Event('change', { bubbles: true, composed: true }));
          setTimeout(() => grid.scrollToIndex?.(0), 400);
          return true;
        })()
        """
    }
}
