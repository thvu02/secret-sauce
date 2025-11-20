import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useFileUpload } from './useFileUpload';

describe('useFileUpload', () => {
  let mockFile: File;

  beforeEach(() => {
    mockFile = new File(['test content'], 'test.png', { type: 'image/png' });
  });

  it('should initialize with null file and previewUrl', () => {
    const { result } = renderHook(() => useFileUpload());

    expect(result.current.file).toBeNull();
    expect(result.current.previewUrl).toBeNull();
  });

  it('should set file and previewUrl when file is selected', () => {
    const { result } = renderHook(() => useFileUpload());

    const mockEvent = {
      target: {
        files: [mockFile],
      },
    } as unknown as React.ChangeEvent<HTMLInputElement>;

    act(() => {
      result.current.handleFileChange(mockEvent);
    });

    expect(result.current.file).toBe(mockFile);
    expect(result.current.previewUrl).toBe('data:image/png;base64,mock-data');
  });

  it('should not update state when no file is selected', () => {
    const { result } = renderHook(() => useFileUpload());

    const mockEvent = {
      target: {
        files: null,
      },
    } as React.ChangeEvent<HTMLInputElement>;

    act(() => {
      result.current.handleFileChange(mockEvent);
    });

    expect(result.current.file).toBeNull();
    expect(result.current.previewUrl).toBeNull();
  });

  it('should not update state when files array is empty', () => {
    const { result } = renderHook(() => useFileUpload());

    const mockEvent = {
      target: {
        files: [],
      },
    } as unknown as React.ChangeEvent<HTMLInputElement>;

    act(() => {
      result.current.handleFileChange(mockEvent);
    });

    expect(result.current.file).toBeNull();
    expect(result.current.previewUrl).toBeNull();
  });

  it('should reset file and previewUrl', () => {
    const { result } = renderHook(() => useFileUpload());

    // First, set a file
    const mockEvent = {
      target: {
        files: [mockFile],
      },
    } as unknown as React.ChangeEvent<HTMLInputElement>;

    act(() => {
      result.current.handleFileChange(mockEvent);
    });

    expect(result.current.file).toBe(mockFile);
    expect(result.current.previewUrl).toBe('data:image/png;base64,mock-data');

    // Then reset
    act(() => {
      result.current.resetFile();
    });

    expect(result.current.file).toBeNull();
    expect(result.current.previewUrl).toBeNull();
  });

  it('should update file when a new file is selected', () => {
    const { result } = renderHook(() => useFileUpload());

    // Select first file
    const firstFile = new File(['first'], 'first.png', { type: 'image/png' });
    const mockEvent1 = {
      target: {
        files: [firstFile],
      },
    } as unknown as React.ChangeEvent<HTMLInputElement>;

    act(() => {
      result.current.handleFileChange(mockEvent1);
    });

    expect(result.current.file).toBe(firstFile);

    // Select second file
    const secondFile = new File(['second'], 'second.png', { type: 'image/png' });
    const mockEvent2 = {
      target: {
        files: [secondFile],
      },
    } as unknown as React.ChangeEvent<HTMLInputElement>;

    act(() => {
      result.current.handleFileChange(mockEvent2);
    });

    expect(result.current.file).toBe(secondFile);
  });
});
