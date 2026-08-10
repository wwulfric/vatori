const tagName = 'vatori-vite-hmr-probe';
const moduleRevision = 'baseline';
let connected = Boolean(import.meta.hot?.data.connected);

export function render() {
  document.querySelectorAll(tagName).forEach((element) => {
    element.dataset.hmrAvailable = String(Boolean(import.meta.hot));
    element.dataset.hmrConnected = String(connected);
    element.dataset.moduleRevision = moduleRevision;
    element.textContent =
      `Vite HMR: ${connected ? 'connected' : 'waiting'} | ${moduleRevision}`;
  });
}

if (!customElements.get(tagName)) {
  customElements.define(tagName, class extends HTMLElement {
    connectedCallback() {
      render();
    }
  });
}

render();

if (import.meta.hot) {
  import.meta.hot.on('vite:ws:connect', () => {
    connected = true;
    import.meta.hot.data.connected = true;
    render();
  });
  import.meta.hot.accept((module) => {
    module?.render();
  });
}
