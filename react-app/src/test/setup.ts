import '@testing-library/jest-dom';
import { vi } from 'vitest';

// Mock localStorage
const storage: Record<string, string> = {};
const localStorageMock = {
  getItem: vi.fn((key: string) => storage[key] || null),
  setItem: vi.fn((key: string, value: string) => {
    storage[key] = value;
  }),
  removeItem: vi.fn((key: string) => {
    delete storage[key];
  }),
  clear: vi.fn(() => {
    for (const key in storage) {
      delete storage[key];
    }
  }),
};
global.localStorage = localStorageMock as any;

// Mock window.URL.createObjectURL
global.URL.createObjectURL = vi.fn(() => 'mock-url');
global.URL.revokeObjectURL = vi.fn();

// Mock FileReader
class FileReaderMock implements Partial<FileReader> {
  result: string | ArrayBuffer | null = null;
  onload: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;
  onerror: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;
  onabort: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;
  onloadstart: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;
  onloadend: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;
  onprogress: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;
  error: DOMException | null = null;
  readyState: 0 | 1 | 2 = 0;
  EMPTY = 0 as const;
  LOADING = 1 as const;
  DONE = 2 as const;

  readAsDataURL() {
    if (this.onload) {
      this.result = 'data:image/png;base64,mock-data';
      this.onload.call(this as any, new Event('load') as any);
    }
  }

  readAsText() {}
  readAsArrayBuffer() {}
  readAsBinaryString() {}
  abort() {}
  addEventListener() {}
  removeEventListener() {}
  dispatchEvent(): boolean {
    return true;
  }
}

global.FileReader = FileReaderMock as any;
