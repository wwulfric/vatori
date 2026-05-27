const loadOnDemand = (key) => {
  const pending = [];
  if (key === 'vatori-flow-ipc-generated-lazy-chunk-v1_22') {
    pending.push(import('./chunks/chunk-v1_22.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;

export { loadOnDemand };
