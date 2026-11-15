import Foundation

// Passive test-only observation of opaque envelope metadata; never inspect UIDL,
// synthesize an ACK, retry delivery, or replace the official Flow callback.
enum FlowIpcRecoveryPushSettlement {
    static let source = """
    (() => {
      if (window.VatoriRecoveryPushSettlement) return;
      const push = window.VaadinDesktopPush;
      if (!push || typeof push.receive !== 'function') return;
      const receive = push.receive;
      let target = null;
      let invalid = false;
      push.receive = function(envelope) {
        if (envelope?.type === 'opaquePush') {
          const p = envelope.payload || {};
          if (!Number.isSafeInteger(p.pushSequence) || p.pushSequence <= 0
              || !Number.isSafeInteger(p.flowServerSyncId) || p.flowServerSyncId < 0
              || typeof p.connectionId !== 'string' || !p.connectionId) invalid = true;
          target = {sequence:p.pushSequence, sync:p.flowServerSyncId, connection:p.connectionId};
        }
        return Reflect.apply(receive, this, arguments);
      };
      window.VatoriRecoveryPushSettlement = {consumed: () => {
        const d = window.VaadinDesktopIpcDiagnostics || {};
        return !invalid && target !== null
          && d.standardBundlePushActiveConnectionId === target.connection
          && d.standardBundlePushLastAckSequence === target.sequence
          && Number.isSafeInteger(d.standardBundlePushLastAckServerSyncId)
          && d.standardBundlePushLastAckServerSyncId >= target.sync;
      }};
    })();
    """
}
