import { ownerDependencyV123 } from './owner-dependency-v1_23.js';

class VatoriProductionOwnerFixture extends HTMLElement {
  connectedCallback() {
    this.setAttribute('data-owner-runtime', 'flow-ipc-production-owner-positive-runtime-v1_23');
  }
}

if (!customElements.get('vatori-production-owner-fixture')) {
  customElements.define('vatori-production-owner-fixture', VatoriProductionOwnerFixture);
}

const ownerRuntime = {
  imported: true,
  dependency: ownerDependencyV123,
  tags: ['vatori-production-owner-fixture'],
  globals: ['window.VaadinDesktopProductionOwnerV123'],
  source: 'repository-production-owner-positive-fixture-v1_23',
  executionVersion: 'flow-ipc-production-owner-positive-runtime-v1_23',
  timestamp: Date.now()
};

window.VaadinDesktopProductionOwnerV123 = ownerRuntime;
window.dispatchEvent(new CustomEvent('vatori-production-owner-positive-fixture-v1_23', {
  detail: ownerRuntime
}));

export { ownerRuntime };
