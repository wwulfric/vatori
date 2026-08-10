import { compatibilityRootMarker } from './vatori-jar-frontend-support.js';

const dynamicMarker = import('./vatori-jar-frontend-dynamic.js');

class VatoriJarFrontendProbe extends HTMLElement {
  connectedCallback() {
    if (!this.querySelector('[data-role="activate"]')) {
      const button = document.createElement('button');
      button.type = 'button';
      button.dataset.role = 'activate';
      button.textContent = 'Activate dependency JAR frontend';
      button.addEventListener('click', () => {
        this.dataset.browserEvent = 'dispatched';
        this.dispatchEvent(new CustomEvent('vatori-addon-activate', {
          bubbles: true,
          composed: true
        }));
      });

      const status = document.createElement('span');
      status.dataset.role = 'status';
      this.append(button, status);
    }

    this.dataset.modernFrontendRoot = 'modern-root-ready';
    this.dataset.compatibilityFrontendRoot = compatibilityRootMarker;
    this.renderServerMessage();
    dynamicMarker.then(({ dynamicRootMarker }) => {
      this.dataset.dynamicImport = dynamicRootMarker;
    }).catch((error) => {
      this.dataset.dynamicImport = `error:${error}`;
    });
  }

  set serverMessage(value) {
    this._serverMessage = String(value ?? '');
    this.dataset.serverMessage = this._serverMessage;
    this.renderServerMessage();
  }

  get serverMessage() {
    return this._serverMessage ?? '';
  }

  renderServerMessage() {
    const status = this.querySelector('[data-role="status"]');
    if (status) {
      status.textContent = this.serverMessage;
    }
  }
}

customElements.define('vatori-jar-frontend-probe', VatoriJarFrontendProbe);
