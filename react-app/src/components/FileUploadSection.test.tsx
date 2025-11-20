import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FileUploadSection } from './FileUploadSection';

describe('FileUploadSection', () => {
  const defaultProps = {
    file: null,
    previewUrl: null,
    loading: false,
    onFileChange: vi.fn(),
    onUpload: vi.fn(),
  };

  it('should render file input and upload button', () => {
    render(<FileUploadSection {...defaultProps} />);

    expect(screen.getByText('Upload Receipt Image')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /upload & parse/i })).toBeInTheDocument();
  });

  it('should disable upload button when no file is selected', () => {
    render(<FileUploadSection {...defaultProps} />);

    const uploadButton = screen.getByRole('button', { name: /upload & parse/i });
    expect(uploadButton).toBeDisabled();
  });

  it('should enable upload button when file is selected', () => {
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

    render(
      <FileUploadSection
        {...defaultProps}
        file={mockFile}
      />
    );

    const uploadButton = screen.getByRole('button', { name: /upload & parse/i });
    expect(uploadButton).not.toBeDisabled();
  });

  it('should call onUpload when upload button is clicked', async () => {
    const user = userEvent.setup();
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
    const onUpload = vi.fn();

    render(
      <FileUploadSection
        {...defaultProps}
        file={mockFile}
        onUpload={onUpload}
      />
    );

    const uploadButton = screen.getByRole('button', { name: /upload & parse/i });
    await user.click(uploadButton);

    expect(onUpload).toHaveBeenCalledTimes(1);
  });

  it('should show loading state', () => {
    render(
      <FileUploadSection
        {...defaultProps}
        loading={true}
      />
    );

    expect(screen.getByText('Processing receipt with OCR...')).toBeInTheDocument();
    expect(screen.getByText(/this may take a few moments/i)).toBeInTheDocument();
  });

  it('should disable inputs when loading', () => {
    const { container } = render(
      <FileUploadSection
        {...defaultProps}
        loading={true}
      />
    );

    const fileInput = container.querySelector('input[type="file"]');
    const uploadButton = screen.getByRole('button');

    expect(fileInput).toBeDisabled();
    expect(uploadButton).toBeDisabled();
  });

  it('should display preview image when previewUrl is provided', () => {
    render(
      <FileUploadSection
        {...defaultProps}
        previewUrl="data:image/png;base64,mock-data"
      />
    );

    const preview = screen.getByAltText('Receipt preview');
    expect(preview).toBeInTheDocument();
    expect(preview).toHaveAttribute('src', 'data:image/png;base64,mock-data');
  });

  it('should not display preview when previewUrl is null', () => {
    render(<FileUploadSection {...defaultProps} />);

    expect(screen.queryByAltText('Receipt preview')).not.toBeInTheDocument();
  });

  it('should accept only image files', () => {
    const { container } = render(<FileUploadSection {...defaultProps} />);

    const fileInput = container.querySelector('input[type="file"]');
    expect(fileInput).toHaveAttribute('accept', 'image/*');
  });
});
