import '@testing-library/jest-dom';
import { vi } from 'vitest';

// Mock window.confirm and window.alert
global.window.confirm = vi.fn();
global.window.alert = vi.fn();

// Mock URL.createObjectURL
global.URL.createObjectURL = vi.fn(() => 'mock-url');
global.URL.revokeObjectURL = vi.fn();

// Mock FileReader
class MockFileReader {
  result: string | ArrayBuffer | null = null;
  onload: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;

  readAsDataURL(blob: Blob) {
    this.result = 'data:image/png;base64,mock-data';
    if (this.onload) {
      this.onload({} as ProgressEvent<FileReader>);
    }
  }

  readAsText(blob: Blob) {
    this.result = 'mock-text';
    if (this.onload) {
      this.onload({} as ProgressEvent<FileReader>);
    }
  }
}

global.FileReader = MockFileReader as any;
