import { useState } from 'react';

export function useFileUpload() {
    const [file, setFile] = useState<File | null>(null);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFile = e.target.files?.[0];
        if (!selectedFile) return;

        setFile(selectedFile);

        const reader = new FileReader();
        reader.onload = () => setPreviewUrl(reader.result as string);
        reader.readAsDataURL(selectedFile);
    };

    const resetFile = () => {
        setFile(null);
        setPreviewUrl(null);
    };

    return {
        file,
        previewUrl,
        handleFileChange,
        resetFile,
    };
}
