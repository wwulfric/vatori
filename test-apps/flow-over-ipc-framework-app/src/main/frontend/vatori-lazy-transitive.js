export function formatNestedLazyMessage(message) {
  return `transitive ${String(message ?? '')}`;
}
