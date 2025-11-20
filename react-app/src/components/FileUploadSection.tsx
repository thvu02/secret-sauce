import React from 'react';

interface FileUploadSectionProps {
    file: File | null;
    previewUrl: string | null;
    loading: boolean;
    onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onUpload: () => void;
}

export const FileUploadSection: React.FC<FileUploadSectionProps> = ({
    file,
    previewUrl,
    loading,
    onFileChange,
    onUpload,
}) => {
    return (
        <>
            <h2 className="text-section mb-6">Upload Receipt Image</h2>

            <div className="mb-4">
                <input
                    type="file"
                    accept="image/*"
                    onChange={onFileChange}
                    disabled={loading}
                />
                <button
                    className="ml-2 px-3 py-1 bg-blue-600 text-white rounded disabled:bg-gray-400"
                    onClick={onUpload}
                    disabled={loading || !file}
                >
                    {loading ? 'Processing receipt with OCR...' : 'Upload & Parse'}
                </button>
                {loading && (
                    <div className="mt-2 text-sm text-gray-600">
                        This may take a few moments while we process your receipt...
                    </div>
                )}
            </div>

            {previewUrl && (
                <div className="mb-4">
                    <h3 className="font-medium">Preview</h3>
                    <img
                        src={previewUrl}
                        alt="Receipt preview"
                        style={{ maxWidth: '320px', maxHeight: '480px' }}
                    />
                </div>
            )}
        </>
    );
};
