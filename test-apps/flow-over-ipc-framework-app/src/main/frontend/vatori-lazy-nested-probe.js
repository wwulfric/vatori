const { formatNestedLazyMessage } = await import('./vatori-lazy-transitive.js');
const tagName = 'vatori-lazy-nested-probe';

if (!customElements.get(tagName)) {
  customElements.define(tagName, class extends HTMLElement {
    connectedCallback() {
      if (Object.prototype.hasOwnProperty.call(this, 'message')) {
        const message = this.message;
        delete this.message;
        this.message = message;
        return;
      }
      if (!this._message) {
        this._message = 'module ready';
      }
      this.render();
    }

    get message() {
      return this._message;
    }

    set message(value) {
      this._message = String(value ?? '');
      this.render();
    }

    render() {
      if (this.isConnected) {
        this.textContent =
          `Nested lazy route module: ${formatNestedLazyMessage(this._message)}`;
      }
    }
  });
}
