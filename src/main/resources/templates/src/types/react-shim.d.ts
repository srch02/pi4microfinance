declare module "react" {
  export type ReactNode = any;

  // Minimal useState type to satisfy TypeScript in this template
  export function useState<T>(
    initialState: T
  ): [T, (newState: T | ((prev: T) => T)) => void];

  const React: {
    createElement: (...args: any[]) => any;
  };

  export default React;
}

