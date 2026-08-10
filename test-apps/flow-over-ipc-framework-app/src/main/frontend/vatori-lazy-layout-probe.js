const tagName = 'vatori-lazy-layout-probe';

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
        this._message = 'layout ready';
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
        this.textContent = `Nested lazy layout module: ${this._message}`;
      }
    }
  });
}
